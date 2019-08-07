package com.demandbase.spark.storageTransfer

import com.google.api.services.storagetransfer.v1.{Storagetransfer, StoragetransferScopes}
import com.google.api.services.storagetransfer.v1.model._
import com.google.api.services.storagetransfer.v1.model.Date
import com.google.api.services.storagetransfer.v1.model.TimeOfDay
import com.google.api.client.googleapis.util.Utils
import com.demandbase.shimSham._
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.HttpRequest
import com.google.cloud.storage.storagetransfer.samples.RetryHttpInitializerWrapper
import com.demandbase.core.common.utils.aws.IamVaultReader

import scala.collection.JavaConversions._
import org.joda.time.{DateTime => JodaDateTime}

import scala.annotation.tailrec

object TransferToGcs extends VaultGcpEnvironment with VaultSwitch with IamVaultReader with Configurable  with Loggable {
  override def vault_address: String = getProperty("vault.address", "https://vault.demandbase.com:8200/")
  ///override def vault_namespace: String = getProperty("vault.namespace", s"secret/environments/development/dummy_app")
  override def vault_namespace= vaultPath

  override def iam_server_id: String = getProperty("spark.vault.iam_server_id", "vault.demandbase.com")
  override def vault_role_name: String = getProperty("spark.vault.role_name", "qubole-service-dev")
  override def iam_role_name: String = getProperty("spark.vault.iam_role_name", "qubole-service-dev")
  override def sts_region: String = getProperty("spark.vault.sts_region", "us-east-1")


  def transferToGcs( sourceURL : java.net.URI, gcpBucket : String) = {

    val transfer  = transferJob( sourceURL, gcpBucket)

    val create = storageTransfer.transferJobs().create( transfer).execute()

    log(s" Submitted Job ${create.getName}")

    waitForJob( create.getName())

  }


  ///@tailrec
  def waitForJob( jobName : String) : Unit = {
    /// XXX Doesn't seem like the correct thing ... Adding a thread sleep
    log(s" Sleeping before checking for jobs ${jobName} ")
    Thread.sleep( 15*1000)
    val filter =  s""" {"project_id": "${getProjectId()}", "job_names": [ "${jobName}" ] } """
    log( s" Checking for any Jobs with name ${jobName} and filter ${filter}")
     val pollJobs = storageTransfer
          .transferOperations()
          .list("transferOperations")
          .setFilter(filter).execute()

    if( pollJobs.getOperations().size() > 0) {
      pollJobs.getOperations.foreach(  job => {
        log(s" Job is ${job.toPrettyString}")

        if (job.getMetadata().get("status") == "FAILED") {
            throw new IllegalStateException(job.getError.toPrettyString)
        } else if( job.getMetadata().get("status") == "SUCCESS") {
            log(s" FINISHED JOB ${job.getMetadata}")
             return
        }
      })

      log(s" TRYING AGAIN ")
      waitForJob(jobName)
    } else{

      log(s" NO OPERATIONS FOUND")
      waitForJob(jobName)
    }

  }


  def isS3( uri : java.net.URI) = {
    uri.getScheme() == "s3" || uri.getScheme() == "s3n" || uri.getScheme() == "s3a"
  }

  def isGFS( uri : java.net.URI) = {
    uri.getScheme() == "gs" || uri.getScheme() == "gfs" || uri.getScheme() == "gcs"
  }


  lazy val storageTransfer : Storagetransfer = {
    val httpTransport = Utils.getDefaultTransport
    val jsonFactory = Utils.getDefaultJsonFactory

    val unscopedCreds = GcpCredentialsFactory.gcpCredentials
    val creds : GoogleCredential = if (unscopedCreds.createScopedRequired()) {
       unscopedCreds.createScoped(StoragetransferScopes.all());
    } else {
      unscopedCreds
    }

    val initializer = new RetryHttpInitializerWrapper(creds)
    new Storagetransfer.Builder(httpTransport, jsonFactory, initializer)
      .setApplicationName("TransferToGCS")
      .build
  }

  def transferJob( sourceURL : java.net.URI, gcpBucket : String) : TransferJob = {
    if( !isS3( sourceURL)) {
      throw new IllegalArgumentException(s" Invalid Transfer From ${sourceURL} to GCP Bucket ${gcpBucket}; Source must be S3... ")
    }

    val jobName = s"TRANSFER_${sourceURL.toString
        .replace(';','_')
        .replace('/','_')
        .replace('.','_')}_TO_GCP_${gcpBucket}"

    val tsSpec = gcsSpec( awsSpec( sourceURL), gcpBucket )
                  .setTransferOptions( (new TransferOptions())
                            .setOverwriteObjectsAlreadyExistingInSink(true)
                            .setDeleteObjectsUniqueInSink(true))

    val transferJob = new TransferJob()
                .setProjectId( getProjectId() )
                ///.setName( jobName )
                .setDescription(s" Transfer from ${sourceURL} to ${gcpBucket} ")
                .setTransferSpec(tsSpec )
                .setSchedule(scheduleNow)
                .setStatus("ENABLED")


    /// XXX Dont show AWS creds
    log( s" Creating Transfer Job ${transferJob.toPrettyString} ")
    return transferJob
  }


  def gcsSpec( ts : TransferSpec,  gcpBucket : String ) : TransferSpec = {

    val gcsSink = new GcsData().setBucketName( gcpBucket)

    ts.setGcsDataSink(gcsSink)
  }


  def awsSpec( sourceURL : java.net.URI) : TransferSpec = {
    val bucket = sourceURL.getHost()
    val aws = new AwsS3Data().setBucketName(bucket).setAwsAccessKey( awsAccessKey )

    val filterPaths = new ObjectConditions().setIncludePrefixes( List( sourceURL.getPath().substring(1) ))

    val transferSpec = new TransferSpec().setAwsS3DataSource( aws)
      .setObjectConditions(  filterPaths)

    return transferSpec
  }


  /**
  *   Assume that the AWS secrets are in the Vault
    *   Safer than
    * @return
    */
  def awsAccessKey : AwsAccessKey = {

    val awsAccessKeyId = getProperty( "AWS_ACCESS_KEY_ID") match {
      case Some(keyId) => {
        keyId
      }
      case None => {
        log("AWS_ACCESS_KEY_ID not set in config; Going to Vault ")
        vaultSecret( "AWS_ACCESS_KEY_ID")
      }
    }
    val awsSecretAccessKey = getProperty( "AWS_SECRET_ACCESS_KEY") match {
      case Some(secret) => secret
      case None => {
        log("AWS_SECRET_ACCESS_KEY not set in config; Going to Vault ")
        vaultSecret( "AWS_SECRET_ACCESS_KEY")
      }
    }
    new AwsAccessKey().setAccessKeyId( awsAccessKeyId )
            .setSecretAccessKey(  awsSecretAccessKey )

  }


  /**
    *    Just Schedule it now
    * @return
    */
  def scheduleNow : Schedule = {
    val jodaTime = JodaDateTime.now()
    val today = new Date().setYear( jodaTime.getYear ).setMonth( jodaTime.getMonthOfYear ).setDay( jodaTime.getDayOfMonth )

    val later = jodaTime.plusSeconds(10)
    val inTenSeconds = new TimeOfDay().setHours( later.getHourOfDay).setMinutes( later.getMinuteOfHour).setSeconds( later.getSecondOfMinute )

    new Schedule().setScheduleStartDate( today).setScheduleEndDate( today).setStartTimeOfDay( inTenSeconds)
  }


}
