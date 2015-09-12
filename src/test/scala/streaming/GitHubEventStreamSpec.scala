package streaming

import util.SparkTest
import org.apache.log4j.{Level, Logger}
import org.specs2.mutable.Specification

import Predef.{conforms => _, _}

class GitHubEventStreamSpec extends Specification {

  // turn off INFO logging that Spark floods the console with
  Logger.getRootLogger.setLevel(Level.ERROR)

  sequential

  "GitHubEventStream" should {
    val g = new GitHubEventStream

    val testData = Seq(GitHubEvent(123L, "createdAt", "eventType", "login", 123L, "avatar", 123L, "repoName"))
    "count the number of events per interval" in new SparkTest(testData)(g.count) {
      println(collector)
      success
    }
  }
}
