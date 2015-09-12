package util

import com.gensler.scalavro.types.AvroType
import streaming.GitHubEvent

/**
 * I use this as a convenience call to generate Avro schema's from Scala case classes.
 * https://github.com/GenslerAppsPod/scalavro
 */
object AvroSchemaGenerator {
  def main (args: Array[String]) {
    val s = AvroType[GitHubEvent].schema()
    println(s)
  }
}