package com.seglo.learningspark.exactlyonce

import java.util.Properties

import org.apache.kafka.clients.producer._
import org.apache.kafka.common.serialization.StringSerializer

class EventProducer(topic: String, bootstrapServers: String) {
  // Producer Configuration options:
  // http://kafka.apache.org/documentation.html#producerconfigs
  val props = new Properties()
  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
  props.put(ProducerConfig.ACKS_CONFIG, "1")
  props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
  props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
  props.put(ProducerConfig.CLIENT_ID_CONFIG, "exactlyonce")
//  props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[KafkaAvroSerializer].getName)
//  props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[KafkaAvroSerializer].getName)

  val producer = new KafkaProducer[String, String](props)

  def send(message: String, key: String = null) = {
    val record = new ProducerRecord(topic, key, message)
    producer.send(record, new Callback() {
      def onCompletion(metadata: RecordMetadata, e: Exception) {
        if(e != null)
          e.printStackTrace()
        println("The offset of the record we just sent is: " + metadata.offset())
      }
    })
  }

  def close() = producer.close()
}
