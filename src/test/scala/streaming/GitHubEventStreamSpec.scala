package streaming

import org.apache.log4j.{Level, Logger}
import org.specs2.mutable.Specification
import util.{TestAvroSerializer, SparkTest}

class GitHubEventStreamSpec extends Specification {
  sequential

  // turn off INFO logging that Spark floods the console with
  Logger.getRootLogger.setLevel(Level.ERROR)

  val testData = new TestAvroSerializer().read(
    "src/test/resources/streaming/GitHubEvents.avro",
    GitHubEvent.schema).map(GitHubEvent.toCaseClass)

  "GitHubEventStream" should {
    val g = new GitHubEventStream

    "count the number of events" in new SparkTest(testData.take(10))(g.count) {
      collector must have size 1
      collector.head mustEqual 10
    }

    "count the number of each type of event" in new SparkTest(testData.take(20))(g.countEventType) {
      collector must have size 6
      collector must containTheSameElementsAs(Seq(
        ("IssueCommentEvent", 1),
        ("PushEvent", 12),
        ("PullRequestEvent", 1),
        ("CreateEvent", 2),
        ("ForkEvent", 1),
        ("WatchEvent", 3)))
      println(collector)
      success
    }
  }
}
