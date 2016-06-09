package com.seglo.learningspark.exactlyonce

import java.util.Properties

import com.datastax.driver.core.ConsistencyLevel
import com.datastax.spark.connector.cql.{CassandraConnector, CassandraConnectorConf}
import com.datastax.spark.connector.streaming._
import com.datastax.spark.connector.writer.WriteConf
import kafka.admin.AdminUtils
import kafka.utils.ZKStringSerializer
import org.I0Itec.zkclient.ZkClient
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.scalatest.concurrent._
import org.scalatest.exceptions.TestFailedException
import org.scalatest.{Matchers, Tag, _}

import scala.Console._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import Defaults._

// Define a ScalaTest tag to indicate which tests should be run against the Docker Compose instance
object DockerComposeTag extends Tag("DockerComposeTag")

class EventStreamingAppSpec extends fixture.FunSuite
  with fixture.ConfigMapFixture with Eventually with IntegrationPatience with Matchers
  with BeforeAndAfterEach {

  // The configMap passed to each test case will contain the connection information for the running Docker Compose
  // services. The key into the map is "serviceName:containerPort" and it will return "host:hostPort" which is the
  // Docker Compose generated endpoint that can be connected to at runtime. You can use this to endpoint connect to
  // for testing.
  val kafkaServiceKey = "kafka:9092"
  val kafkaTestTopic = Defaults.defaultTopic
  val kafkaTopicPartitions = Defaults.defaultTopicPartitions
  val zookeeperServiceKey = "kafka:2181"
  val cassandraServiceKey = "cassandra:9042"
  val cassandraKeySpace = "exactlyonce"
  val cassandraEventsTable = "events"
  val cassandraAlertsTable = "alerts"
  val sparkMaster = "local[*]"
  val servicesHost = "localhost"

  var ssc: StreamingContext = null

  override def beforeEach(): Unit = {}
  override def afterEach(): Unit = {
    Try(ssc.stop())
  }

  // TODO: remove me when sbt-docker-compose bug fixed
  val linux = false

  test("EventStreamingApp will persist at least 10 events and generate at least 1 CRITICAL alert in Cassandra", DockerComposeTag) {
    configMap =>{
      // Arrange
      println(configMap)
      // TODO: why does configMap contain no hosts?  only happens on linux, systems using docker-machine (i.e. Mac) provide docker-host ip
      // TODO: consult with sbt-docker-compose author

      val kafkaHostInfo = if (linux) s"$servicesHost${getHostInfo(kafkaServiceKey, configMap)}" else getHostInfo(kafkaServiceKey, configMap)
      val cassandraHostInfo = if (linux) servicesHost else getHostInfo(cassandraServiceKey, configMap).split(":").head
      val zookeeperHostInfo = if (linux) s"$servicesHost${getHostInfo(zookeeperServiceKey, configMap)}" else getHostInfo(zookeeperServiceKey, configMap)

      println(s"Resolved docker-compose service host info.")
      println(s"Kafka: $kafkaHostInfo, Cassandra: $cassandraHostInfo, Zookeeper: $zookeeperHostInfo")

      val sparkConf = new SparkConf()
        .setAppName(EventStreamingApp.appName)
        .setMaster(sparkMaster)
        .set(CassandraConnectorConf.ConnectionHostParam.name, cassandraHostInfo)
        .set(WriteConf.ConsistencyLevelParam.name, ConsistencyLevel.LOCAL_ONE.toString)

      // Create Cassandra schema
      eventually {
        CassandraConnector(sparkConf).withSessionDo { session =>
          session.execute(s"DROP KEYSPACE IF EXISTS $cassandraKeySpace;")
          session.execute(s"CREATE KEYSPACE IF NOT EXISTS $cassandraKeySpace WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 };")
          session.execute(
            s"""CREATE TABLE IF NOT EXISTS $cassandraKeySpace.$cassandraEventsTable (
               |id bigint,
               |ts timestamp,
               |partition int,
               |offset bigint,
               |source text,
               |client text,
               |health int,
               |PRIMARY KEY (partition, offset)
               |) WITH CLUSTERING ORDER BY (offset DESC);""".stripMargin)
          session.execute(
            s"""CREATE TABLE IF NOT EXISTS $cassandraKeySpace.$cassandraAlertsTable (
               |ts timestamp,
               |source text,
               |event_count int,
               |PRIMARY KEY (source, ts));""".stripMargin)
          session.execute(s"TRUNCATE $cassandraKeySpace.$cassandraEventsTable;")
          session.execute(s"TRUNCATE $cassandraKeySpace.$cassandraAlertsTable;")
        }
      }

      // Create Kafka topic
      createTopic(zookeeperHostInfo, kafkaTestTopic, kafkaTopicPartitions)

      val ssc = new StreamingContext(sparkConf, Seconds(1))

      // Act
      EventStreamingApp.start(ssc, kafkaTestTopic, kafkaTopicPartitions, cassandraHostInfo, kafkaHostInfo,
        defaultCassandraKeySpace, defaultCassandraEventsTable, defaultCassandraAlertsTable)

      import ExecutionContext.Implicits.global

      // awaitTermination is a blocking call, run in another thread and stop somewhere else
      Future {
        ssc.awaitTermination()
      }

      TestMessageGenerator.start(kafkaHostInfo, kafkaTestTopic, numEventsPerPartition = 10, delayEveryTen = false)

      // Assert
      eventually {
        ssc.cassandraTable(cassandraKeySpace, cassandraEventsTable).cassandraCount() should be >= 10.toLong
        ssc.cassandraTable(cassandraKeySpace, cassandraAlertsTable).cassandraCount() should be >= 1.toLong
      }
    }
  }

  def createTopic(zookeeperHostInfo: String, topic: String, numPartitions: Int) = {
    val timeoutMs = 10000
    val zkClient = new ZkClient(zookeeperHostInfo, timeoutMs, timeoutMs, ZKStringSerializer)

    val replicationFactor = 1
    val topicConfig = new Properties
    AdminUtils.createTopic(zkClient, topic, numPartitions, replicationFactor, topicConfig)
  }

  def getHostInfo(serviceKey: String, configMap: ConfigMap): String = {
    if (configMap.keySet.contains(serviceKey)) {
      configMap(serviceKey).toString
    } else {
      throw new TestFailedException(s"Cannot find the expected Docker Compose service '$serviceKey' in the configMap", 10)
    }
  }
}
