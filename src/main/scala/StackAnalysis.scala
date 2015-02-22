//import org.apache.spark.SparkContext
//import org.apache.spark.SparkContext._
//import org.apache.spark.SparkConf
//
//object StackAnalysis {
//  def main (args: Array[String]) {
//    val conf = new SparkConf().setAppName("StackAnalysis").setMaster("local")
//    val sc = new SparkContext(conf)
//
//
//    val file = sc.textFile("/home/seglo/stackexchange/stackoverflow.com-Posts/Posts.xml")
//    
//    val lineCount = file.count()
//    println(s"there are $lineCount lines")
////    val lines = file.filter(line ⇒ line.contains("clone"))
////    // Count all instances of clone
////    val cloneCount = lines.filter(line ⇒ line.contains("clone")).count()
////    println(s"there are $cloneCount mentions of clone in this script")
////    // Fetch the MySQL errors as an array of strings
////    println("lines that contain clone:")
////    lines.collect().foreach(println)
//  }
//}
