import java.io.File
import org.apache.commons.io.FileUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import scala.collection.JavaConversions._
import scala.xml.XML
import CommandLineOptions._

object StackAnalysis {
  def main (args: Array[String]) {
    val options = CommandLineOptions.getMap(args)
    
    val conf = new SparkConf().setAppName("StackAnalysis")
    val sc = new SparkContext(conf)

    try {
      // Input dataset
      val inputFile = options.get('inputfile)
        .getOrElse("data/stackexchange/stackoverflow.com-Posts/Posts100k.xml").toString

      // Optional Output file
      val outputFile = options.get('outputfile)

      tagCounts(sc, inputFile, outputFile)
    } finally {
      sc.stop()
    }
  }

  def tagCounts(sc: SparkContext, inputFile: String, outputFile: Option[Any]) = {
    val file = sc.textFile(inputFile)

    // Use random sampling for 10% of data
    //.sample(false, 0.1, System.currentTimeMillis().toInt)
    val tagCounts = file
      // Skip XML lines without <row /> elements
      .filter(l ⇒ l.contains("row"))
      // Extract PostTypeId and Tags
      // If XML deserialization successful then return data
      .flatMap(l ⇒ {
        try {
          val xml = XML.loadString(l)
          //val id = (xml \ "@Id").text.toLong
          val postTypeId = (xml \ "@PostTypeId").text.toInt
          val tags = (xml \ "@Tags").text
          List((postTypeId, tags))
        } catch {
          case ex: Exception ⇒ {
            println(s"failed to parse line: $l")
            Nil
          }
        }
      })
      // Format tags into Array
      // i.e. <scala><java><potato> -> Array[String]("scala", "java", "potato")
      .map { case (postTypeId, tagString) ⇒
        val splitTags = if (tagString.length == 0) Array[String]() else tagString.substring(1,tagString.length-1).split("><")
        (postTypeId, splitTags.toList)
      }
      // Only get "Question" posts (PostTypeId == 1)
      .filter { case (postTypeId, tags) ⇒ postTypeId == 1 }
      // If this question contains a scala tag then return a collection of all other tags
      .flatMap { case (postTypeId, tags) ⇒ {
        val otherTags = tags.diff(List("scala"))
        // Return key value pair (tuple) with tag name as the key and a base number
        // to sum in subsequent reduce step.
        if (tags.length > otherTags.length)
          otherTags.map(tag ⇒ (tag, 1))
        else
          Nil
      }}
      // `reduceByKey` groups by key (tag name) and performs a reduction for all elements
      // that have the same key
      .reduceByKey((a, b) ⇒ a + b)
      // Swap tuple values for sortByKey
      .map { case (tag, count) ⇒ (count, tag) }
      // Sort by tag counts in descending order
      .sortByKey(false)
      // Flush datapipeline to memory
      .collect
      //.saveAsTextFile("/home/seglo/stackexchange/stackoverflow.com-Posts/tag-counts.txt")
   
    outputFile match {
      // TODO: write non-collected stream to HDFS and use `hdfs dfs -getmerge`
      case Some(outputFile) ⇒ {
        println(s"Writing output file ${outputFile.toString} to disk")
        FileUtils.writeLines(new File(outputFile.toString), tagCounts.toSeq)
      }
      case None ⇒ println("No output file provided.")
    }
  }
}
