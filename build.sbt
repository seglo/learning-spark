lazy val commonSettings = Seq(
  organization := "randonom",
  version := "0.1.0",
  scalaVersion := "2.10.4"
)

// intellij sbt plugin will create new modules for each project instantiated here (based on val name)
lazy val `learning-spark` = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "learning-spark",
    resolvers ++= Seq(
    ),
    libraryDependencies ++= Seq(
      "org.apache.spark" % "spark-core_2.10" % "1.4.1",
      "org.apache.spark" % "spark-streaming_2.10" % "1.4.1",
      "org.apache.spark" % "spark-streaming-kafka_2.10" % "1.4.1",
      "org.apache.kafka" % "kafka-clients" % "0.8.2.1",
      "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
      "com.typesafe.play" % "play-json_2.10" % "2.4.2",
      "com.typesafe" % "config" % "1.3.0",
      "org.specs2" %% "specs2-core" % "2.4.15" % "test")
  )
