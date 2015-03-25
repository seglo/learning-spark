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
      Resolver.sonatypeRepo("public"),
      Resolver.bintrayRepo("scalaz", "releases")
    ),
    libraryDependencies ++= Seq(
      "commons-io"        %  "commons-io"         % "2.4",
      "org.apache.spark"  %  "spark-core_2.10"    % "1.2.0",
      "edu.stanford.nlp"  %  "stanford-corenlp"   % "3.3.1",
      "edu.stanford.nlp"  %  "stanford-corenlp"   % "3.3.1"   classifier "models",
      "org.specs2"        %% "specs2-core"        % "2.4.15"  % "test")
  )
