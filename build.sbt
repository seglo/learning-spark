val sparkCore = "org.apache.spark" % "spark-core_2.10" % "1.2.0"

// test deps
val specs2 = "org.specs2" %% "specs2-core" % "2.4.15" % "test"

lazy val commonSettings = Seq(
  organization := "randonom",
  version := "0.1.0",
  scalaVersion := "2.10.4"
)

// intellij sbt plugin will create new modules for each project instantiated here (based on val name)
lazy val `twitter-sentiment-stream` = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "twitter-sentiment-stream",
    libraryDependencies ++= Seq(sparkCore, specs2)
  )