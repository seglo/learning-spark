import java.text.SimpleDateFormat
import java.util.Date

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD

import scala.xml.XML

object StackAnalysis {
  def main (args: Array[String]) {
    val options = CommandLineOptions.getMap(args)
    
    val conf = new SparkConf().setAppName("StackAnalysis")
    val sc = new SparkContext(conf)

    try {
      // Input dataset
      val inputFile = options.getOrElse('inputfile, "data/stackexchange/stackoverflow.com-Posts/Posts100k.xml").toString

      // Optional directory
      val outputDirOption = options.get('outputdir)

      try {
        val rows = sc.textFile(inputFile)
        val sqs = scalaQuestions(rows)
        
        println(s"Scala question count ${sqs.count()}")
        
        val tc = tagCounts(sqs)

        val sqsByMonth = scalaQuestionsByMonth(sqs)

        outputDirOption match {
          // NOTE: When writing to HDFS use `hdfs dfs -getmerge` to retrieve fully constructed file
          case Some(outputDir) ⇒
            println("Writing output files to disk")
            
            val outputDirStr = outputDir.toString
            val stcFile = s"$outputDirStr/ScalaTagCount.txt"
            val sqbmFile = s"$outputDirStr/ScalaQuestionsByMonth.txt" 
            
            tc.saveAsTextFile(stcFile)
            sqsByMonth.saveAsTextFile(sqbmFile)
          case None ⇒ println("No output file provided.")
        }
      } finally {
        sc.stop()
      }
    }
  }

  def scalaQuestions(rows: RDD[String]) = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
    rows  
      // Use random sampling for 10% of data
      //.sample(false, 0.1, System.currentTimeMillis().toInt)
      // Skip XML lines without <row /> elements
      .filter(l ⇒ l.contains("row"))
      // Extract PostTypeId and Tags
      // If XML deserialization successful then return data
      .flatMap(l ⇒ {
        try {
          val xml = XML.loadString(l)
          //val id = (xml \ "@Id").text.toLong
          val postTypeId = (xml \ "@PostTypeId").text.toInt
          val creationDate = (xml \ "@CreationDate").text
          val tags = (xml \ "@Tags").text
          List((postTypeId, creationDate, tags))
        } catch {
          case ex: Exception ⇒
            println(s"failed to parse line: $l")
            Nil
        }
      })
      // Format create date string
      .map { case (postTypeId, creationDateString, tagString) ⇒ (postTypeId, sdf.parse(creationDateString), tagString) }
      // Format tags into Array
      // i.e. <scala><java><potato> -> Array[String]("scala", "java", "potato")
      .map { case (postTypeId, creationDate, tagString) ⇒
        val splitTags = if (tagString.length == 0) Array[String]() else tagString.substring(1,tagString.length-1).split("><")
        (postTypeId, creationDate, splitTags.toList)
      }
      // Only get "Question" posts (PostTypeId == 1)
      .filter { case (postTypeId, creationDate, tags) ⇒ postTypeId == 1 }
      // Filter question posts by those that contain Scala tags
      .filter { case (id, creationDate, tags) ⇒ tags.contains("scala") }
      // Cache this dataflow for other uses
      .cache()
  }

  def tagCounts(qs: RDD[(Int, Date, List[String])]) = {
    qs
      // If this question contains a scala tag then return a collection of all other tags
      .flatMap { case (postTypeId, creationDate, tags) ⇒
        val otherTags = tags.diff(List("scala"))
        // Return key value pair (tuple) with tag name as the key and a base number
        // to sum in subsequent reduce step.
        if (tags.length > otherTags.length)
          otherTags.map(tag ⇒ (tag, 1))
        else
          Nil
      }
      // `reduceByKey` groups by key (tag name) and performs a reduction for all elements
      // that have the same key
      .reduceByKey((a, b) ⇒ a + b)
      // Swap tuple values for sortByKey
      .map { case (tag, count) ⇒ (count, tag) }
      // Sort by tag counts in descending order
      .sortByKey(ascending = false)
  }

  def scalaQuestionsByMonth(sqs: RDD[(Int, Date, List[String])]) = {
    // Date pattern to group by (Month)
    val sdfMonth = new SimpleDateFormat("yyyy-MM")
    
    sqs
      // Transform to key value pairs of Month and Seed count
      .map { case (id, creationDate, tags) ⇒ (sdfMonth.format(creationDate), 1)} 
      // Sum reduction by key
      .reduceByKey((a, b) ⇒ a + b) 
      // Sort by month
      .sortByKey(ascending = true)
  }
}
