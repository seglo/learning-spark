lazy val stackanalysis = (project in file(".")).
  settings(
    name := "stackanalysis",
    libraryDependencies ++= Seq(
      "org.apache.spark" % "spark-core_2.10" % "1.6.1",
      "org.apache.spark" % "spark-streaming_2.10" % "1.6.1",
      "org.apache.spark" % "spark-streaming-kafka_2.10" % "1.6.1",
      "com.typesafe" % "config" % "1.3.0",
      // scalavro is just used for generating avro schema's, can't use lib to it's full effect
      // because I couldn't get it working with Spark
      "com.gensler" %% "scalavro" % "0.6.2" % "test",
      "org.specs2" %% "specs2-core" % "3.6.4" % "test"
    )
  )