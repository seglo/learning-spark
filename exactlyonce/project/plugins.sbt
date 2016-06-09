logLevel := Level.Warn

// required for sbt-docker-compose SNAPSHOT version for testing
//resolvers += Resolver.sonatypeRepo("public")

addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.2.0")

addSbtPlugin("com.tapad" % "sbt-docker-compose" % "1.0.4")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.0")
