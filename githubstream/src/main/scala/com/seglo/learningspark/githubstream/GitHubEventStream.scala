package com.seglo.learningspark.githubstream

import java.util.{Date, Properties}

import _root_.kafka.serializer.StringDecoder
import io.confluent.kafka.serializers.KafkaAvroDecoder
import org.apache.avro.generic.GenericData
import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka._

import scala.collection.JavaConverters._
import scala.concurrent.Await

class GitHubEventStream {
  def makeEvents(stream: DStream[(String, Object)]): DStream[GitHubEvent] =
    stream.map { case (key, record: GenericData.Record) =>
      GitHubEvent.toCaseClass(record)}

  def count(stream: DStream[GitHubEvent]) = stream.count()

  def countEventType(stream: DStream[GitHubEvent]) =
    stream
      .map(e => (e.eventType, 1))
      .reduceByKey(_ + _)

  def countLanguage(stream: DStream[GitHubEvent]) =
    stream
      .map(e => (e.language, 1))
      .reduceByKey(_ + _)

  def countLanguageLookup(stream: DStream[GitHubEvent]) = {
    val m = new MongoConnection
    stream.transform { rdd =>
      val repoUrls = rdd.collect().map(e => s"https://github.com/${e.repoName}")
      val langLookup = Await.result(m.languageLookup(repoUrls), scala.concurrent.duration.Duration.Inf).toMap
      rdd.map { e =>
        (e, langLookup.getOrElse(s"https://github.com/${e.repoName}", if (e.language == "") None else Some(e.language)))
      }
    }
  }

  def emoting(stream: DStream[GitHubEvent]) = {
    val emotingExp = Map(
      // concatenated anger and swearing expressions
      "anger" -> """(?i)\b(a+rgh|angry|annoyed|annoying|appalled|bitter|cranky|hate|hating|mad|wtf|wth|omfg|hell|ass|bitch|bullshit|bloody|fucking?|shit+y?|crap+y?)\b\b(fuck|damn|piss|screw|suck)e?d?\b""".r,
      "joy" -> """(?i)\b(yes|yay|hallelujah|hurray|bingo|amused|cheerful|excited|glad|proud)\b""".r,
      "amusement" -> """(?i)\b(ha(ha)+|he(he)+|lol|rofl|lmfao|lulz|lolz|rotfl|lawl|hilarious)\b""".r,
      "surprise" -> """(?i)\b(yikes|gosh|baffled|stumped|surprised|shocked)\b""".r,
      "swearing" -> """(?i)\b(wtf|wth|omfg|hell|ass|bitch|bullshit|bloody|fucking?|shit+y?|crap+y?)\b|\b(fuck|damn|piss|screw|suck)e?d?\b""".r)

    stream
      .filter(e => e.commentBody.length > 0 || e.prBody.length > 0 || e.commitMessages.length > 0)
      .flatMap { e =>
        val msg = (List(e.commentBody, e.prBody) ++ e.commitMessages).mkString("\n")

        for{
          (emotion, exp) <- emotingExp
          isDefined <- exp.findFirstIn(msg)
        } yield (emotion, 1)/*, e.language, msg)*/
      }
  }

  def emotingByWindow(stream: DStream[GitHubEvent]) = {
    emoting(stream)
      .reduceByKeyAndWindow((a,b)=>a+b, Seconds(60))
  }

  def reduceByWindow(stream: DStream[GitHubEvent]) = {
    stream
      .map(e => (e.eventType, 1))
      .reduceByKeyAndWindow((a,b) => a+b, Seconds(20))
  }

  def languageByWindow(stream: DStream[GitHubEvent]) = {

    countLanguageLookup(stream)
      .map {
        case (e:GitHubEvent, lang:Option[String]) => (lang.getOrElse("(Unknown)"), 1)
      }
      .reduceByKeyAndWindow((a,b)=>a+b, Seconds(180))
      .foreachRDD { rdd =>
        val date = new Date().getTime
        rdd.foreachPartition { records =>
          //records.foreach(println)
          records.foreach(c => MongoConnection.connection.insert(c, date))
        }
      }
    // using reduceByKeyAndWindow with inverted reduce function requires checkpointing (on fault tolerant disk, i.e. HDFS)
    //.reduceByKeyAndWindow((a,b)=>a+b, (a,b)=>a-b, Seconds(3600))
  }
}

object GitHubEventStream {
  def main(args: Array[String]) {
    //val Array(brokers, topics) = args
    val brokers = "localhost:9092"
    val topics = "GitHubEventStream"

    // Create context with 2 second batch interval
    val sparkConf = new SparkConf().setAppName("GitHubEventStream").setMaster("local[*]")
    val ssc = new StreamingContext(sparkConf, Seconds(2))

    // Create direct kafka stream with brokers and topics
    val topicsSet = topics.split(",").toSet


    val props = new Properties()
    props.put("metadata.broker.list", brokers)
    props.put("schema.registry.url", "http://localhost:8081")

    val kProps = props.asScala.toMap[String, String]

    val messages = KafkaUtils.
      createDirectStream[String, Object, StringDecoder, KafkaAvroDecoder](ssc, kProps, topicsSet)

    val g = new GitHubEventStream
    val eventStream = g.makeEvents(messages)
    g.languageByWindow(eventStream)//.print(100)

    // Start the computation
    ssc.start()
    ssc.awaitTermination()
  }
}
