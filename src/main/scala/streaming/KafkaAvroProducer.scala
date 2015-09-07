package streaming

import java.util.Properties

import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.avro.generic.GenericData
import org.apache.kafka.clients.producer.{ProducerConfig, ProducerRecord}

class KafkaAvroProducer {
  val props = new Properties()

  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, Config.get.kafkaBrokerList)
  props.put("schema.registry.url", Config.get.kafkaSchemaRegistry)
  props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[KafkaAvroSerializer].getName)
  props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[KafkaAvroSerializer].getName)

  val producer = new org.apache.kafka.clients.producer.KafkaProducer[Object, Object](props)


  def send(topic: String, record: GenericData.Record) = {
    val producerRecord = new ProducerRecord[Object, Object](topic, record)
    producer.send(producerRecord)
  }

  def close() = producer.close()
}
