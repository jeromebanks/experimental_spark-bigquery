package com.demandbase.spark.bigquery

import com.google.api.services.bigquery.model.{TableReference, TableSchema}
import com.google.cloud.hadoop.io.bigquery._
import com.google.gson._
import com.demandbase.spark.bigquery.converters.{BigQueryAdapter, SchemaConverters}
import com.demandbase.spark.storageTransfer.TransferToGcs
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.io.{LongWritable, NullWritable}
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat
import org.apache.spark.sql.DataFrame
import org.slf4j.LoggerFactory

import scala.util.Random
/**
  * Created by samelamin on 12/08/2017.
  */
class BigQueryDataFrame(self: DataFrame) extends Serializable {
  val adaptedDf = BigQueryAdapter(self)
  private val logger = LoggerFactory.getLogger(classOf[BigQueryClient])

  @transient
  lazy val hadoopConf = self.sparkSession.sparkContext.hadoopConfiguration
  lazy val bq = BigQueryClient.getInstance(self.sqlContext )
    ///hadoopConf.get( "google.cloud.auth.service.account.json.keytext")
  //)

  @transient
  lazy val jsonParser = new JsonParser()

  /**
    * Save DataFrame data into BigQuery table using Hadoop writer API
    *
    * @param fullyQualifiedOutputTableId output-table id of the form
    *                                    [optional projectId]:[datasetId].[tableId]
    * @param isPartitionedByDay partion the table by day
    */
  def saveAsBigQueryTable(fullyQualifiedOutputTableId: String,
                          isPartitionedByDay: Boolean = false,
                          timePartitionExpiration: Long = 0,
                          writeDisposition: WriteDisposition.Value = null,
                          createDisposition: CreateDisposition.Value = null,
                          mirroredPath : Option[String]= None): Unit = {
    val destinationTable = BigQueryStrings.parseTableReference(fullyQualifiedOutputTableId)
    val bigQuerySchema = SchemaConverters.SqlToBQSchema(adaptedDf)
    val gcsPath = writeDFToGoogleStorage(adaptedDf,destinationTable,bigQuerySchema, mirroredPath)
    bq.load(destinationTable,
      bigQuerySchema,
      gcsPath,
      isPartitionedByDay,
      timePartitionExpiration,
      writeDisposition,
      createDisposition)
    delete(new Path(gcsPath))
  }



  def writeDFToGoogleStorage(adaptedDf: DataFrame,
                             destinationTable: TableReference,
                             bqSchema: TableSchema,
                             mirroredPath : Option[String]): String = {
    val tableName = BigQueryStrings.toString(destinationTable)

    BigQueryConfiguration.configureBigQueryOutput(hadoopConf, tableName, bqSchema.toPrettyString())
    hadoopConf.set("mapreduce.job.outputformat.class", classOf[BigQueryOutputFormat[_, _]].getName)
    val gcpBucket = self.sparkSession.conf.get(BigQueryConfiguration.GCS_BUCKET_KEY)
    val s3Bucket = self.sparkSession.conf.get("mapred.bq.s3.bucket" , "demandbase-insights")

    val s3Prefix = s"s3a://${s3Bucket}"
    val s3Path = mirroredPath match {
      case Some(path) => {
       s"${s3Prefix}/${mirroredPath}"
      }
      case None => {
        val temp = s"spark-bigquery-${System.currentTimeMillis()}=${Random.nextInt(Int.MaxValue)}"
        s"${s3Prefix}/hadoop/tmp/spark-bigquery/${temp}"

      }
    }



    /// Write to S3, and then transfer to GCS
    ///val gcsPath = s"gs://$bucket/hadoop/tmp/spark-bigquery/$temp"
    if(hadoopConf.get(BigQueryConfiguration.TEMP_GCS_PATH_KEY) == null) {
      hadoopConf.set(BigQueryConfiguration.TEMP_GCS_PATH_KEY, s3Path)
    }

    logger.info(s"Writing to s3 mirror ${s3Path} ")
    adaptedDf
      .toJSON
      .rdd
      .map(json => (null, jsonParser.parse(json)))
      .saveAsNewAPIHadoopFile(s3Path,
        classOf[GsonBigQueryInputFormat],
        classOf[LongWritable],
        classOf[TextOutputFormat[NullWritable, JsonObject]],
        hadoopConf)


    logger.info(s" Transfering to bucket GCP ${gcpBucket}")
    TransferToGcs.transferToGcs( new java.net.URI(s3Path), gcpBucket )

    val gcsPath =  s"gs://${gcpBucket}" + s3Path.substring(s3Prefix.length )
    logger.info(s" Dest GCS Path is ${gcsPath}")
    gcsPath
  }



  /**
  *   Write
    * @param df
    * @param s3Loc
    * @param gcpBucket
    */
  def writeToGFS( s3Loc: java.net.URI,  gcpBucket : String) = {
      self.write.save( s3Loc.toURL.toString)
      TransferToGcs.transferToGcs( s3Loc, gcpBucket)
  }



  private def delete(path: Path): Unit = {
    val fs = FileSystem.get(path.toUri, hadoopConf)
    fs.delete(path, true)
  }
}
