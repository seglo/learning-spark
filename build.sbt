lazy val commonSettings = Seq(
  organization := "com.seglo",
  version := "0.1.0",
  scalaVersion := "2.10.6"
)

// intellij sbt plugin will create new modules for each project instantiated here (based on val name)
lazy val `learning-spark` = (project in file("."))
    .settings(commonSettings: _*)
    .settings(
      name := "learning-spark"
    )

lazy val stackanalysis = (project in file("stackanalysis"))
    .settings(commonSettings: _*)

lazy val githubstream = (project in file("githubstream"))
    .settings(commonSettings: _*)