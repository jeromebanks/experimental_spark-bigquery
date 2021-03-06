/*
 * Copyright 2016 samelamin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

name := "spark-bigquery"
organization := "com.demandbase"
scalaVersion := {
  if (sparkVersion.value >= "2.0.0") {
    "2.11.11"
  } else {
    "2.10.6"
  }
}
crossScalaVersions := {
  if (sparkVersion.value >= "2.3.0") {
    Seq("2.11.11")
  } else {
    Seq("2.10.6", "2.11.11")
  }
}
spName := "samelamin/spark-bigquery"
sparkVersion := "2.3.1"
sparkComponents := Seq("core", "sql","streaming")
spAppendScalaVersion := false
spIncludeMaven := true
spIgnoreProvided := true
credentials += Credentials(Path.userHome / ".ivy2" / ".sbtcredentials")
parallelExecution in Test := false
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-hive" % "2.3.1" % "test",
  "com.databricks" %% "spark-avro" % "4.0.0",
  "com.google.guava" % "guava" % "24.0-jre",
  "com.holdenkarau" %% "spark-testing-base" % "2.3.1_0.10.0" % "test",
 ///"com.google.cloud.bigdataoss" % "bigquery-connector" % "0.11.0-hadoop2"
    ///exclude ("org.apache.avro", "avro-ipc"),
 "com.google.cloud.bigdataoss" % "bigquery-connector" % "hadoop2-1.9.9-jdb-SNAPSHOT"
    exclude ("org.apache.avro", "avro-ipc") excludeAll(
       ExclusionRule(organization = "com.sun.jdmk"),
       ExclusionRule(organization = "com.sun.jmx"),
       ExclusionRule(organization = "javax.jms") ),
  "com.google.cloud.bigdataoss" % "gcs-connector" % "hadoop2-1.9.9-jdb-SNAPSHOT" excludeAll(
       ExclusionRule(organization = "com.sun.jdmk"),
       ExclusionRule(organization = "com.sun.jmx"),
       ExclusionRule(organization = "javax.jms") ),
  "joda-time" % "joda-time" % "2.9.3",
  "org.mockito" % "mockito-core" % "1.8.5" % "test",
  "org.scalatest" %% "scalatest" % "2.2.5" % "test"
) 


resolvers += Resolver.mavenLocal


assemblyShadeRules in assembly := Seq(
  ShadeRule.rename("com.google.common.**" -> "shade.com.google.@1").inAll
)

// Release settings
licenses += "Apache-2.0" -> url("http://opensource.org/licenses/Apache-2.0")
releaseCrossBuild             := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value

////pomExtra                      := {
////  <url>https://github.com/samelamin/spark-bigquery</url>
////  <scm>
////    <url>git@github.com/samelamin/spark-bigquery.git</url>
////    <connection>scm:git:git@github.com:samelamin/spark-bigquery.git</connection>
////  </scm>
////  <developers>
////    <developer>
////      <id>samelamin</id>
////      <name>Sam Elamin</name>
////      <email>hussam.elamin@gmail.com</email>
////      <url>https://github.com/samelamin</url>
////    </developer>
////  </developers>
////}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishTo := Some("Demandbase SBT Snapshot" at "https://artifactory.demandbase.com/artifactory/sbt-snapshot-local/")



