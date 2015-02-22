import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import scala.xml.XML

object StackAnalysis {
  def main (args: Array[String]) {
    val conf = new SparkConf().setAppName("StackAnalysis")
    val sc = new SparkContext(conf)

    //val file = sc.textFile("/home/seglo/stackexchange/stackoverflow.com-Posts/Posts.xml")
    //val file = sc.textFile("/home/seglo/stackexchange/stackoverflow.com-Posts/Posts1m.xml")
    val file = sc.textFile("/home/seglo/stackexchange/stackoverflow.com-Posts/Posts100k.xml")

    // use random sampling for 10% of data
    //.sample(false, 0.1, System.currentTimeMillis().toInt)
    val tagCounts = file
      .filter(l ⇒ l.contains("row")) // skip XML lines without <row /> elements
      .flatMap( l ⇒ {
        try {
          val xml = XML.loadString(l)
          //val id = (xml \ "@Id").text.toLong
          val postTypeId = (xml \ "@PostTypeId").text.toInt
          val tags = (xml \ "@Tags").text
          List((postTypeId, tags))
        } catch {
          case ex: Exception ⇒ {
            println(s"failed to parse line: $l")
            List[(Int, String)]()
          }
        }
      })
      .filter { case (postTypeId, tags) ⇒ postTypeId == 1 } // post type id 1 is a question
      .flatMap { case (postTypeId, tags) ⇒ {
        val splitTags = if (tags.length == 0) Array[String]() else tags.substring(1,tags.length-1).split("><") 
        val otherTags = splitTags.diff(List("scala"))
        if (splitTags.length > otherTags.length)
          otherTags.map(tag ⇒ (tag, 1))
        else
          List[(String, Int)]() 
      }}
      .reduceByKey((a, b) ⇒ a + b)
      .collect.toList.sortBy{case (tag, count) ⇒ -count}//.take(10)
      //.saveAsTextFile("/home/seglo/stackexchange/stackoverflow.com-Posts/tag-counts.txt")
   
    //println(s"there are $lineCount lines") 
    tagCounts.foreach(println)

  }
}
