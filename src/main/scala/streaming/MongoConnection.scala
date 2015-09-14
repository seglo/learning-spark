package streaming

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import reactivemongo.api.{ReadPreference, MongoDriver}
import reactivemongo.bson.{BSONDocument, BSONArray}
import reactivemongo.api.collections.bson.BSONCollection

class MongoConnection {
  val driver = new MongoDriver
  val conn = driver.connection(List(Config.get.mongoHost))
  val db = conn.db("github")
  val coll: BSONCollection = db.collection("repo")

  def languageLookup(repoUrls: Seq[String]): Future[List[(Option[String], Option[String])]] = {
    var bsArray = BSONArray()
    repoUrls.foreach(r => bsArray = bsArray.add(r))

    val listFuture = coll
      .find(BSONDocument("repository_url" -> BSONDocument("$in" -> bsArray)))
      .cursor[BSONDocument](ReadPreference.Primary)
      .collect[List]()

    listFuture.map { list =>
      list.map(doc => (doc.getAs[String]("repository_url"), doc.getAs[String]("repository_language")))
    }
  }
}
