package com.seglo.learningspark.githubstream

import com.seglo.learningspark.githubstream.util.{SparkTest, TestAvroSerializer}
import org.apache.log4j.{Level, Logger}
import org.specs2.mutable.Specification

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
      println(collector)
      collector must containTheSameElementsAs(Seq(
        ("IssueCommentEvent", 1),
        ("PushEvent", 12),
        ("PullRequestEvent", 1),
        ("CreateEvent", 2),
        ("ForkEvent", 1),
        ("WatchEvent", 3)))
    }

    "count events and group by their language if it's supplied" in new SparkTest(testData)(g.countLanguage) {
      println(collector)
      collector must containTheSameElementsAs(Seq(
        ("",315),
        ("Python",1),
        ("JavaScript",4),
        ("C++",2),
        ("Java",2),
        ("PHP",2),
        ("Ruby",3),
        ("C",1)))
    }

    "count events that are emoting" in new SparkTest(testData)(g.emoting) {
      println(collector)
      collector must containTheSameElementsAs(Seq(
        ("amusement",1,""), ("swearing",1,"")))
    }

    "count events and map their languages to githubarchive dataset" in new SparkTest(testData)(g.countLanguageLookup) {
      println(collector)
      val mappedLangs = collector.filter(_._2 != None)
      collector.foreach(println)
      println(s"${mappedLangs.length} of ${collector.length} events were mapped to languages")
      success
    }
  }
}
