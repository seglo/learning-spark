import sbtassembly.AssemblyPlugin.autoImport._
import sys.process._
import scala.util.Try

enablePlugins(DockerPlugin, DockerComposePlugin)

lazy val dockerComposeTag = "DockerComposeTag"



lazy val exactlyonce = (project in file(".")).
  settings(
    name := "exactlyonce",
    /* A note on "provided" deps.

       It would be nice to keep these provided, but I've had so many problems battling compatibility problems between
       IntelliJ, sbt-assembly, and spark-jobserver that I decided to just include them all.  The fat jar is about 200%
       the size it would be without these deps, but I can live with that.  Can you? :) */
    libraryDependencies ++= Seq(
      "com.datastax.spark" %% "spark-cassandra-connector" % "1.6.0-M2",
      "org.apache.kafka" %% "kafka" % "0.8.2.1",
      "org.apache.spark" %% "spark-core" % "1.6.1", // % "provided",
      "org.apache.spark" %% "spark-sql" % "1.6.1", // % "provided",
      "org.apache.spark" %% "spark-streaming" % "1.6.1", // % "provided",
      "org.apache.spark" %% "spark-streaming-kafka" % "1.6.1",
      "spark.jobserver" %% "job-server-api" % "0.6.2", // % "provided",
      "spark.jobserver" %% "job-server-extras" % "0.6.2", // % "provided",
      "org.json4s" %% "json4s-native" % "3.2.10", // pinned version for compatibility with spark json4s
      "com.101tec" % "zkclient" % "0.4" % "test",
      "com.holdenkarau" %% "spark-testing-base" % "1.6.1_0.3.2" % "test",
      "org.scalatest" %% "scalatest" % "2.2.6" % "test"
    ),
    dependencyOverrides ++= Set(
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.4",
      "com.typesafe" % "config" % "1.2.1"
    ),
    // There is a conflict between Guava versions in spark-cassandra-connector and Hadoop
    // Shading Guava Package
    // http://arjon.es/2015/10/12/making-hadoop-2-dot-6-plus-spark-cassandra-driver-play-nice-together/
    assemblyShadeRules in assembly := Seq(
      ShadeRule.rename("com.google.**" -> "shadeio.@1").inAll
    ),
    scalacOptions := Seq("-deprecation", "-unchecked", "-feature"),

    // sbt-docker-compose settings:
    // Omit any tests tagged as DockerComposeTag when doing a `sbt test`
    testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-l", dockerComposeTag),
    composeFile := baseDirectory.value + "/docker/sbt-docker-compose.yml",
    // Only execute tests tagged DockerComposeTag for `sbt dockerComposeTest`
    testTagsToExecute := dockerComposeTag,
    //Set the image creation Task to be the one used by sbt-docker
    dockerImageCreationTask := docker.value,
    dockerfile in docker := {
      new Dockerfile {
        val dockerAppPath = "/app/"
        val mainClassString = "com.seglo.learningspark.exactlyonce.EventStreamingApp"
        val classpath = (fullClasspath in Compile).value
        from("java")
        add(classpath.files, dockerAppPath)
        entryPoint("java", "-cp", s"$dockerAppPath:$dockerAppPath/*", s"$mainClassString", s" > $dockerAppPath/out")
      }
    },
    imageNames in docker := Seq(ImageName(
      repository = name.value.toLowerCase,
      tag = Some(version.value))
    ),
    // Prevent Spark integration tests trampling one another
    parallelExecution in Test := false,

    // sbt-assembly settings:
    assemblyJarName in assembly := s"${name.value}-${version.value}.jar",
      assemblyMergeStrategy in assembly := {
        case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
        case _ => MergeStrategy.first
      },

    // Include "provided" Spark deps are in classpath when running, but not during packaging with sbt-assembly
    runMain in Compile <<= Defaults.runMainTask(fullClasspath in Compile, runner in (Compile, run)),

    // spark-jobserver
    resolvers += "Job Server Bintray" at "https://dl.bintray.com/spark-jobserver/maven"
  )