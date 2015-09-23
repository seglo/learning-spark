package streaming

import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import reactivemongo.api.{ReadPreference, MongoDriver}
import reactivemongo.bson.{BSONDocument, BSONArray}
import reactivemongo.api.collections.bson.BSONCollection

import scala.util.{Failure, Success}

class MongoConnection {
  val driver = new MongoDriver
  val conn = driver.connection(List(Config.get.mongoHost))
  val db = conn.db("github")
  val repoColl: BSONCollection = db.collection("repo")
  val langColl: BSONCollection = db.collection("language-stream")


  def languageLookup(repoUrls: Seq[String]): Future[List[(String, Option[String])]] = {
    var bsArray = BSONArray()
    repoUrls.foreach(r => bsArray = bsArray.add(r))

    val listFuture = repoColl
      .find(BSONDocument("repository_url" -> BSONDocument("$in" -> bsArray)))
      .cursor[BSONDocument](ReadPreference.Primary)
      .collect[List]()

    listFuture.map { list =>
      list.map(doc => (doc.getAs[String]("repository_url").get, doc.getAs[String]("repository_language")))
    }
  }

  def insert(c: (String, Int), date: Long) = {
    val document = BSONDocument(
      "date" -> date,
      "language" -> c._1,
      "count" -> c._2)

    val future1: Future[WriteResult] = langColl.insert(document)

    future1.onComplete {
      case Failure(e) => throw e
      case Success(writeResult) =>
        //println(s"successfully inserted document with result: $writeResult")
    }
  }
}

object MongoConnection {
  lazy val connection = new MongoConnection
}
