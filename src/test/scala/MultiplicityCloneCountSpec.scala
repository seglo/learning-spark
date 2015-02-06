import org.specs2.mutable._
import org.apache.spark.{SparkContext, SparkConf}
import org.specs2.specification.Scope

/**
 * Created by seglo on 05/02/15.
 */
class MultiplicityCloneCountSpec extends Specification {
  "the multiplicity script" should {
    "contain 6 mentions of 'clone'" in new sparkContext {
      val file = sc.textFile("./data/multiplicity_script.txt")
      val lines = file.filter(line => line.contains("clone"))
      // Count all instances of clone
      val cloneCount = lines.filter(line => line.contains("clone")).count()

      println(s"there are $cloneCount mentions of clone in this script")
      lines.collect().foreach(println)

      cloneCount must beEqualTo(6)
    }
  }
}

trait sparkContext extends After {
  lazy val conf = new SparkConf().setAppName("MultiplicityCloneCountSpec").setMaster("local")
  lazy val sc = new SparkContext(conf)

  def after = sc.stop
}
