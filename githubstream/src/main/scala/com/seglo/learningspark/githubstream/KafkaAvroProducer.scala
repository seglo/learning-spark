package com.seglo.learningspark.githubstream

import java.util.Properties

import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.avro.generic.GenericData
import org.apache.kafka.clients.producer.{Callback, ProducerConfig, ProducerRecord, RecordMetadata}

class KafkaAvroProducer {
  val props = new Properties()

  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, Config.get.kafkaBrokerList)
  // Confluent's [Avro] Schema Registry server, required by any producer/consumer using
  // their kafka-avro-serializer project
  props.put("schema.registry.url", Config.get.kafkaSchemaRegistry)
  props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[KafkaAvroSerializer].getName)
  props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[KafkaAvroSerializer].getName)
  // The most durable guarantee available by Kafka Producer's.  An acknowledgement will
  // only be sent once the configured # of replicas for a topic [partition] have
  // acknowledged the message.
  props.put(ProducerConfig.ACKS_CONFIG, "all")

  val producer = new org.apache.kafka.clients.producer.KafkaProducer[Object, Object](props)

  /**
   * Produce a batch of Avro records to Kafka.
   * Based on new Producer API.  More details:
   * http://kafka.apache.org/082/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html#send(org.apache.kafka.clients.producer.ProducerRecord,%20org.apache.kafka.clients.producer.Callback)
   * @param topic Kafka topic
   * @param records Avro records to produce
   * @return A Sequence of FutureRecordMetadata instances (based on Java Future)
   */
  def send(topic: String, records: Seq[GenericData.Record]/*, cb: (RecordMetadata, Exception) => Unit*/) = {
    records.map { r =>
      val producerRecord = new ProducerRecord[Object, Object](topic, r)
      producer.send(producerRecord, new Callback() {
        def onCompletion(metadata: RecordMetadata, e: Exception) {
          if(e != null)
            e.printStackTrace()
          //println("The offset of the record we just sent is: " + metadata.offset())
        }
      })
    }
  }

  def close() = producer.close()
}
