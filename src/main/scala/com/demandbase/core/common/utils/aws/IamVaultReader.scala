package com.demandbase.core.common.utils.aws

import java.text.SimpleDateFormat
import java.util.{Base64, Calendar}

///import com.demandbase.core.common.utils.{DomainUtils, HttpUtils}
///import scalaj.http.{Http, HttpRequest, HttpResponse}

import scala.util.Try

/**
*    Stub version of IamVaultReader,
  *     so that we can assume that it is provided,
  *       without including dependency
  */

trait IamVaultReader {
  // no pun intended

  val auth_retries_max_count: Int = 10
  val auth_delay_base_ms: Int = 200
  val auth_delay_cap: Int = 10000

  def sts_region: String
  def vault_address: String
  def vault_namespace: String
  def iam_server_id: String
  def iam_role_name: String
  def vault_role_name: String = iam_role_name
  // Note: vault_role_name is a role, defined by Vault, not the IAM role.
  // If in your vault config they don't match, you have to override!

  def getSecretValue(key: String): String = getSecrets(Seq(key)).last._2.get

  def getSecrets(keys: Iterable[String]): Map[String, Try[String]] =  ???

}
