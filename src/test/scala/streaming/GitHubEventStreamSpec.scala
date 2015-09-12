package streaming

import org.apache.log4j.{Level, Logger}
import org.specs2.mutable.Specification
import util.{TestAvroSerializer, SparkTest}

class GitHubEventStreamSpec extends Specification {

  // turn off INFO logging that Spark floods the console with
  Logger.getRootLogger.setLevel(Level.ERROR)

  sequential

  val testData = new TestAvroSerializer().read(
    "src/test/resources/streaming/GitHubEvents.avro.backup",
    GitHubEvent.schema).map(GitHubEvent.toCaseClass)

  "GitHubEventStream" should {
    val g = new GitHubEventStream

    "count the number of events per interval" in new SparkTest(testData.take(10))(g.count) {
      collector must have size 1
      collector(0) mustEqual 10
    }
  }
}
