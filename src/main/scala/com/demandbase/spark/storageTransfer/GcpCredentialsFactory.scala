package com.demandbase.spark.storageTransfer

import java.io.ByteArrayInputStream

import com.demandbase.core.common.utils.aws.IamVaultReader
import com.demandbase.shimSham.GcpEnvironment
import com.demandbase.shimSham._
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
/**
  *  Actually same as in BigQueryServiceFactory .... ( refactor that later )
  *    Using our GCP Environment, create a GcpCredentials object for making API calls
  */
object GcpCredentialsFactory extends  GcpEnvEnabled  with Configurable  with Loggable {

   ///override def vault_address: String = getProperty("vault.address", "https://vault.demandbase.com:8200/")
   ///override def vault_namespace: String = getProperty("vault.namespace", s"secret/environments/development/dummy_app")
   ///override def vault_namespace= vaultPath

  ////override def iam_server_id: String = getProperty("spark.vault.iam_server_id", "vault.demandbase.com")
   ///override def vault_role_name: String = getProperty("spark.vault.role_name", "qubole-service-dev")
  ///override def iam_role_name: String = getProperty("spark.vault.iam_role_name", "qubole-service-dev")
   ///override def sts_region: String = getProperty("spark.vault.sts_region", "us-east-1")

  def gcpCredentials : GoogleCredential = {

    val jsonKey = getJsonKeyText()
    if(jsonKey != null && jsonKey.trim.length > 0) {
      val byteStream = new ByteArrayInputStream( jsonKey.getBytes)
      GoogleCredential.fromStream( byteStream)

    } else {
      GoogleCredential.getApplicationDefault
    }
  }

}
