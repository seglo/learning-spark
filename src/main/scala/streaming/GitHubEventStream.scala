package streaming

import java.util.Properties
import _root_.kafka.serializer.StringDecoder
import org.apache.avro.generic.GenericData
import org.apache.spark.streaming.dstream.DStream

import scala.collection.JavaConverters._

import io.confluent.kafka.serializers.KafkaAvroDecoder

import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.apache.spark.SparkConf

class GitHubEventStream {
  def makeEvents(stream: DStream[(String, Object)]): DStream[GitHubEvent] =
    stream.map { case (key, record: GenericData.Record) =>
      GitHubEvent.toCaseClass(record)}

  def count(stream: DStream[GitHubEvent]) = {
    stream.map { event =>
      println(event)
      event.repoName
    }.count()
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
    g.count(eventStream).print()

    // Start the computation
    ssc.start()
    ssc.awaitTermination()
  }
}
