package util

import org.apache.avro.generic.GenericData
import streaming.{GitHubEvent, ClientRunner, KafkaAvroProducer}

object GitHubEventGenerator {
  def main(args: Array[String]) {
    val p = new TestAvroProducer
    val r = new ClientRunner(p)
    r.run()
    p.close()
  }
}

class TestAvroProducer extends KafkaAvroProducer {
  val testDataGenerator = new TestAvroSerializer

  override def send(topic: String, records: Seq[GenericData.Record]) = {
    testDataGenerator.write(records, "src/test/resources/streaming/GitHubEvents.avro", GitHubEvent.schema)
    null
  }
}
