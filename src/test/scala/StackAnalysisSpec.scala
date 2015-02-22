import org.specs2.mutable._
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.SparkContext._
import org.specs2.specification.Scope
import scala.xml.XML

/**
 * Created by seglo on 05/02/15.
 */
class StackAnalysisSpec extends Specification {
  "stackoverflow posts" should {
    "tag counts" in new sparkContext {
      //val file = sc.textFile("/home/seglo/stackexchange/stackoverflow.com-Posts/Posts.xml")
      //val file = sc.textFile("/home/seglo/stackexchange/stackoverflow.com-Posts/Posts1m.xml")
      val file = sc.textFile("/home/seglo/stackexchange/stackoverflow.com-Posts/Posts100k.xml")
      
      val tagCounts = file
        .filter(l ⇒ l.contains("row")) // skip XML lines without <row /> elements

        .sample(false, 0.1, System.currentTimeMillis().toInt)
        .map ( l ⇒ {
          //println(l)
          val xml = XML.loadString(l)

          val id = (xml \ "@Id").text.toLong
          val postTypeId = (xml \ "@PostTypeId").text.toInt
          val tags = (xml \ "@Tags").text

          val splitTags = if (tags.length == 0) Array[String]() else tags.substring(1,tags.length-1).split("><") 

          //println(splitTags)
          val stuff = (id, postTypeId, splitTags)
          //println(s"stuff: $stuff")
          stuff
        })
          .flatMap( s ⇒ s._3)
          .map( tag ⇒ (tag, 1))
          .reduceByKey(_ + _)
          .collect().toList.sortBy(_._2)
          //.saveAsTextFile("/home/seglo/stackexchange/stackoverflow.com-Posts/tag-counts.txt")
          //println(s"there are $lineCount lines") 
          tagCounts.foreach(println)
          success
          //-1 must beGreaterThan(0)
    }
  }
}
//
//trait sparkContext extends After {
//  lazy val conf = new SparkConf().setAppName("StackAnalysisSpec").setMaster("local")
//  lazy val sc = new SparkContext(conf)
//
//  def after = sc.stop
//} 
