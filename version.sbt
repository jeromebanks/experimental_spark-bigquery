version in ThisBuild := "0.2.10-DB"


/// Local publishing
///publishTo := Some("Demandbase SBT Snapshot" at "https://artifactory.demandbase.com/artifactory/sbt-snapshot-local/")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
