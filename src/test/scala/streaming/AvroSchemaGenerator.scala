package streaming

import com.gensler.scalavro.types.AvroType
import org.specs2.mutable.Specification

/**
 * I use this spec as a convenience call to generate Avro schema's from Scala case classes.
 * https://github.com/GenslerAppsPod/scalavro
 */
class AvroSchemaGenerator extends Specification {
  "AvroSchemaGenerator" should {
    "Generate GitHubEvent Schema" in {
      val s = AvroType[GitHubEvent].schema()
      println(s)
      success
    }
  }
}