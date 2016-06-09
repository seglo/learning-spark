package com.seglo.learningspark.exactlyonce

object Defaults {
  val defaultTopic = "exactlyonce.events"
  val defaultTopicPartitions = 3
  val defaultBrokers = "localhost:9092"
  val defaultCassandra = "localhost"
  val defaultCassandraKeySpace = "exactlyonce"
  val defaultCassandraEventsTable = "events"
  val defaultCassandraAlertsTable = "alerts"
}
