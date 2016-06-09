package com.seglo.learningspark.exactlyonce

import com.datastax.spark.connector.streaming._
import com.datastax.spark.connector.{SomeColumns, _}
import com.seglo.learningspark.exactlyonce.Health._
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.dstream.DStream
import org.json4s.NoTypeHints
import org.json4s.native.Serialization.{read, write => swrite}

import scala.util.{Failure, Success, Try}

class EventStreaming() {
  /**
    * Create Event's by deserializing JSON from Kafka message.  Include Kafka partition and offset
    * information so we know how much of the events topic we've already consumed in case of failure.
    *
    * @param rawEvents Events serialized as JSON.
    * @return A stream of Event's with Kafka partition's and offsets.
    */
  def makeEvents(rawEvents: DStream[(PartitionAndOffset, String)]): DStream[EventWithOffset] =
    // deserialize Events and filter out records that failed deserialization
    rawEvents.flatMap { case (offset, json) =>
      println(s"offset: $offset, json: $json")
      Try(read[Event](json)) match {
        case Success(event) => Vector(EventWithOffset(offset, event))
        // NOTE: if there's a JSON parsing error do nothing.  in production we would want to flag
        // this as an error in the DStream and record it somewhere.
        case Failure(e) => Nil
      }
    }

  /**
    * Persist Event's to Cassandra
    *
    * TODO: Use Cassandra UserDefinedType
    *
    * @param events The EventWithOffset's stream
    * @param keySpace The keyspace of the Event's table.
    * @param table The Event's table name.
    */
  def saveEvents(events: DStream[EventWithOffset], keySpace: String, table: String): Unit =
    events
      .map(e => (e.event.id, e.event.ts, e.offset.partition, e.offset.offset, e.event.source, e.event.client, e.event.health.code))
      .saveToCassandra(keySpace, table, SomeColumns("id", "ts", "partition", "offset", "source", "client", "health"))

  /**
    * Perform a sliding window operation over events and generate alerts when a certain number of Critical events
    * occur within the window.
    *
    * This operation can be modeled in SQL with something like:
    * SELECT SOURCE, SUM(*) FROM EVENTS WHERE EVENTHEALTH = CRITICAL AND TS BETWEEN TIME1 AND TIME2 GROUP BY SOURCE
    *
    * @param stream A stream of Event's
    * @return A stream of Alert's
    */
  def createAlertsBySource(stream: DStream[EventWithOffset]): DStream[Alert] = {
    val criticalEventsWindow = 5
    val criticalEventsThreshold = 10
    val reportingFrequency = 5
    stream
      // Filter out non-Critical events and map stream into a tuple of (source, 1) for following count reduction.
      .flatMap { e =>
      if (e.event.health.code >= Critical.code) Seq((e.event.source, 1))
      else Nil
    }
      // Sum all critical events by source within this windowDuration.
      .reduceByKeyAndWindow(
      reduceFunc = (a,b)=>a+b,
      windowDuration = Seconds(criticalEventsWindow),
      slideDuration = Seconds(reportingFrequency))
      // Filter counts that are greater than or equal to the threshold of events to cause an alert.
      .filter{ case (source, count) => count >= criticalEventsThreshold }
      // Create an Alert instance with the interval timestamp, source, and count of critical events in this window.
      .transform { (alerts, time) =>
      alerts.map {
        case (source, count) => Alert(time.milliseconds, source, count)
      }
    }
  }

  /**
    * Persist Alerts to Cassandra.
    *
    * @param alerts Alert stream.
    * @param keySpace The keyspace of the Alert's table.
    * @param table The Alert's table name.
    */
  def saveAlerts(alerts: DStream[Alert], keySpace: String, table: String): Unit =
    alerts.saveToCassandra(keySpace, table, SomeColumns("ts", "source", "event_count"))

  def produceAlerts(alerts: DStream[Alert], topic: String, brokers: String): Unit = {
    alerts.foreachRDD { alertsRdd =>
      alertsRdd.foreachPartition { alerts =>
        implicit val formats = org.json4s.native.Serialization.formats(NoTypeHints)

        val producer = new EventProducer(topic, brokers)

        alerts.foreach { alert =>
          val alertJson = swrite(alert)
          producer.send(alertJson, alert.source)
        }
        producer.close()
      }
    }
  }
}
