package com.seglo.learningspark.exactlyonce

import com.seglo.learningspark.exactlyonce.Defaults._
import com.seglo.learningspark.exactlyonce.Health._
import org.joda.time.LocalDateTime
import org.json4s.native.Serialization.{write => swrite}

import scala.util.Random

/**
  * Generate test Event's to produce onto a Kafka topic.
  */
object TestMessageGenerator {
  def main(args: Array[String]): Unit = {
    val defaultNumEvents = 200
    val defaultDelayEveryTen = true

    val brokers = args.headOption.getOrElse(defaultBrokers)
    val topic = args.lift(1).getOrElse(defaultTopic)
    val numEventsPerPartitionKey = args.lift(2).map(_.toInt).getOrElse(defaultNumEvents)
    val delayEveryTen = args.lift(3).map(_.toBoolean).getOrElse(defaultDelayEveryTen)

    start(brokers, topic, numEventsPerPartitionKey, delayEveryTen)
  }

  def start(brokers: String, topic: String, numEventsPerPartition: Int, delayEveryTen: Boolean): Unit = {
    val producer = new EventProducer(topic, brokers)

    val t = System.currentTimeMillis()

    val clientNetworks = Vector(
      "Airmiles Rewards Network",
      "Spongebob's Undersea Adventure Network",
      "Dr. Evil's Secret Comms Network")

    val source = "My Router"

    try {
      for {
        nEvents <- Range(0, numEventsPerPartition)
        key <- clientNetworks
      } {
        val id = Random.nextLong()
        val ts = LocalDateTime.now().toDate.getTime
        val event = Event(id, ts, source, client = key, Critical)
        val eventJson = swrite(event)
        producer.send(eventJson, key)
        if (delayEveryTen && nEvents % 10 == 0) Thread.sleep(500)
      }
    } finally {
      producer.close()
    }

    println("sent per second: " + numEventsPerPartition * 1000 / (System.currentTimeMillis() - t))
  }
}
