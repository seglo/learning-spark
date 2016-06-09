package com.seglo.learningspark.exactlyonce

import java.util.Properties

import com.datastax.driver.core.ConsistencyLevel
import com.datastax.spark.connector.cql.{CassandraConnector, CassandraConnectorConf}
import com.datastax.spark.connector.writer.WriteConf
import com.seglo.learningspark.exactlyonce.ConfigHelper.RichConfig
import com.seglo.learningspark.exactlyonce.Defaults._
import com.typesafe.config.Config
import kafka.common.TopicAndPartition
import kafka.message.MessageAndMetadata
import kafka.serializer.StringDecoder
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka._
import org.apache.spark.streaming.{Seconds, StreamingContext}
import spark.jobserver.{SparkJobValid, SparkJobValidation, SparkStreamingJob}

import scala.collection.JavaConverters._

/**
  * The EventStreamingApp is a demonstration of a Spark Streaming Driver application with high availability
  * and fault tolerant characteristics when working with Kafka and Cassandra.
  *
  * - Use Direct kafka stream.
  * - Scale the number of Spark executors out to each Kafka topic partition.
  * - Manage our own partition offsets.
  * - Support "Exactly Once" message delivery semantics in the event of failure.
  */
object EventStreamingApp extends SparkStreamingJob {
  val consumerGroup, appName = "EventStreamingApp"

  /**
    * Main class is used when manually submitting job using spark-submit.  For Spark Job Server see `runJob`
    * @param args CLI params.
    */
  def main(args: Array[String]) {
    val brokers = args.headOption.getOrElse(defaultBrokers)
    val cassandra = args.lift(1).getOrElse(defaultCassandra)
    val topics = args.lift(2).getOrElse(defaultTopic)
    val topicPartitions = args.lift(3).map(_.toInt).getOrElse(defaultTopicPartitions)
    val keySpace = args.lift(4).getOrElse(defaultCassandraKeySpace)
    val eventsTable = args.lift(5).getOrElse(defaultCassandraEventsTable)
    val alertsTable = args.lift(6).getOrElse(defaultCassandraAlertsTable)

    val sparkConf = new SparkConf().setAppName(appName)
    val ssc = new StreamingContext(sparkConf, Seconds(1))
    start(ssc, topics, topicPartitions, cassandra, brokers, keySpace, eventsTable, alertsTable)
    ssc.awaitTermination()
  }

  /**
    * Validate the streaming context and config to give Spark Job Server a quick response if something is amiss.
    * @param ssc Streaming Context
    * @param config Spark Job Server config (typesafe library Config)
    * @return A SparkJobValid or error
    */
  def validate(ssc: StreamingContext, config: Config): SparkJobValidation = SparkJobValid

  /**
    * Run the Spark Streaming Job Server application using a shared streaming context.
    * @param ssc Streaming Context
    * @param config Spark Job Server config (typesafe library Config)
    * @return Streaming jobs never return.  Spark Job Server will gracefully attempt to shutdown streaming context
    *         when a DELETE request is sent to jobserver/contexts/[MY_STREAMING_CONTEXT].
    */
  def runJob(ssc: StreamingContext, config: Config): Any = {
    val brokers = config.getOptionalString("kafka.brokersList").getOrElse(defaultBrokers)
    val cassandra = config.getOptionalString("cassandra.host").getOrElse(defaultCassandra)
    val topics = config.getOptionalString("kafka.topics").getOrElse(defaultTopic)
    val topicPartitions = config.getOptionalInt("kafka.topicPartitions").getOrElse(defaultTopicPartitions)
    val keySpace = config.getOptionalString("cassandra.keyspace").getOrElse(defaultCassandraKeySpace)
    val eventsTable = config.getOptionalString("cassandra.eventsTable").getOrElse(defaultCassandraEventsTable)
    val alertsTable = config.getOptionalString("cassandra.alertsTable").getOrElse(defaultCassandraAlertsTable)

    start(ssc, topics, topicPartitions, cassandra, brokers, keySpace, eventsTable, alertsTable)
    ssc.awaitTermination()
  }

  /**
    * Start the Spark Streaming Job
    * @param ssc Spark Streaming Context
    * @param topic Kafka topic to consume
    * @param topicPartitions The number of partitions this kafka topic has
    * @param cassandra The Cassandra hostname
    * @param brokers The Kafka brokers to connect to
    * @param keySpace The Cassandra keyspace
    * @param eventsTable The events table in Cassandra
    * @param alertsTable The alerts table in Cassandra
    */
  def start(ssc: StreamingContext, topic: String, topicPartitions: Int, cassandra: String, brokers: String,
            keySpace: String, eventsTable: String, alertsTable: String): Unit = {
    val sparkConf = ssc.sparkContext.getConf
    sparkConf.setAppName(appName)

    // The Cassandra connection host defaults to localhost.  When submitting a job this config is generally set from the
    // CLI with spark-submit:
    // i.e. `--conf spark.cassandra.connection.host=10.147.0.21`
    // This is just a helper parameter in case we want to execute this app without the aid of spark-submit
    if (sparkConf.getOption(CassandraConnectorConf.ConnectionHostParam.name).isEmpty)
      sparkConf.set(CassandraConnectorConf.ConnectionHostParam.name, cassandra)

    // Consistency level for writes is LOCAL_ONE (one C* node in the local DC)
    sparkConf.set(WriteConf.ConsistencyLevelParam.name, ConsistencyLevel.LOCAL_ONE.toString)

    // TODO: set checkpoint directory for StreamingContext to FT storage (HDFS)

    // Consumer Configuration options (new, 0.9.0.0+)
    // http://kafka.apache.org/documentation.html#newconsumerconfigs
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
    // Only used in 0.9.0.0+ consumers.  We're forced to use 0.8.2.1 because of spark-streaming-kafka
    //props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer])
    //props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer])
    props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup)
    val kafkaConsumerParams = props.asScala.toMap[String, String]

    // Retrieve the last kafka offset for each Kafka partition to support exactly-once delivery semantics.
    val eventsTableFullName = s"$keySpace.$eventsTable"
    val offsets = partitionOffsets(ssc.sparkContext.getConf, topicPartitions, eventsTableFullName, topic)

    println(s"kafka offsets offsets: $offsets")

    // TODO: evaluate Kafka back pressure options describe in this answer: http://stackoverflow.com/a/36930270/895309
    val messages: InputDStream[(PartitionAndOffset, String)] = KafkaUtils.
      createDirectStream[String, String, StringDecoder, StringDecoder, (PartitionAndOffset, String)](
      ssc, kafkaConsumerParams, fromOffsets = offsets,
      messageHandler = (m: (MessageAndMetadata[String, String])) => {
        (PartitionAndOffset(m.partition, m.offset), m.message())
      })

    val app = new EventStreaming

    val events: DStream[EventWithOffset] = app.makeEvents(messages)
    events.cache()
    app.saveEvents(events, keySpace, eventsTable)

    val alerts: DStream[Alert] = app.createAlertsBySource(events)
    app.saveAlerts(alerts, keySpace, alertsTable)
    //app.produceAlerts(alerts, "foobar", brokers)

    ssc.start()
  }

  /**
    * Retrieve the last kafka offset for each Kafka partition to support exactly-once delivery semantics.  Partitions
    * begin from 0.  If an offset cannot be found then we have persisted no data for this partition and we will start
    * from offset 0.
    *
    * NOTE: It's important to include the right number of partitions so that we get offsets for all the partitions,
    * otherwise we will only consume a subset of partitions from Kafka.
    *
    * @param sparkConf Spark Conf for CassandraConnector
    * @param numOfPartitions The number of topic partitions.
    * @param eventsTable The fully qualified name for the events table (keyspace, table)
    * @return A map of TopicAndPartition's to each offset.
    */
  def partitionOffsets(sparkConf: SparkConf, numOfPartitions: Int, eventsTable: String, eventsTopic: String): Map[TopicAndPartition, Long] = {
    CassandraConnector(sparkConf).withSessionDo { session =>
      (0 until numOfPartitions).flatMap { partition =>
        val topicPartition = TopicAndPartition(eventsTopic, partition)
        val rs = session.execute(s"select partition, offset from $eventsTable where partition = $partition limit 1;").one()
        Option(rs)
          .map(r => Vector(topicPartition -> (r.getLong("offset") + 1)))
          .getOrElse(Vector(topicPartition -> 0.toLong))
      }.toMap
    }
  }
}


