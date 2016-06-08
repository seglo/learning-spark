package com.seglo.learningspark.githubstream

import java.util

import org.apache.avro.Schema
import org.apache.avro.generic.GenericData

import scala.collection.JavaConversions._

case class GitHubEvent(id: Long, createdAt: String, eventType: String,
                       login: String, userId: Long, avatar: String, repoId: Long,
                       repoName: String, commitMessages: List[String],
                       commentBody: String, prBody: String, language: String) {
  lazy val toAvro = GitHubEvent.toAvro(this)
}

object GitHubEvent {
  private val parser = new Schema.Parser()
  val avroSchema =
    """{
      |    "fields": [
      |        {
      |            "name": "id",
      |            "type": "long"
      |        },
      |        {
      |            "name": "createdAt",
      |            "type": "string"
      |        },
      |        {
      |            "name": "eventType",
      |            "type": "string"
      |        },
      |        {
      |            "name": "login",
      |            "type": "string"
      |        },
      |        {
      |            "name": "userId",
      |            "type": "long"
      |        },
      |        {
      |            "name": "avatar",
      |            "type": "string"
      |        },
      |        {
      |            "name": "repoId",
      |            "type": "long"
      |        },
      |        {
      |            "name": "repoName",
      |            "type": "string"
      |        },
      |        {
      |            "name": "commitMessages",
      |            "type": {
      |                "items": "string",
      |                "type": "array"
      |            }
      |        },
      |        {
      |            "name": "commentBody",
      |            "type": "string"
      |        },
      |        {
      |            "name": "prBody",
      |            "type": "string"
      |        },
      |        {
      |            "name": "language",
      |            "type": "string"
      |        }
      |    ],
      |    "name": "com.seglo.learningspark.githubstream.GitHubEvent",
      |    "type": "record"
      |}
    """.stripMargin
  val schema = parser.parse(avroSchema)

  def toAvro(c: GitHubEvent) = {
    val avroRecord = new GenericData.Record(GitHubEvent.schema)

    avroRecord.put("id", c.id)
    avroRecord.put("createdAt", c.createdAt)
    avroRecord.put("eventType", c.eventType)
    avroRecord.put("login", c.login)
    avroRecord.put("userId", c.userId)
    avroRecord.put("avatar", c.avatar)
    avroRecord.put("repoId", c.repoId)
    avroRecord.put("repoName", c.repoName)
    // convert to JavaCollection so Avro's GenericDatumWriter doesn't complain
    avroRecord.put("commitMessages", asJavaCollection(c.commitMessages))
    avroRecord.put("commentBody", c.commentBody)
    avroRecord.put("prBody", c.prBody)
    avroRecord.put("language", c.language)
    avroRecord
  }

  def toCaseClass(r: GenericData.Record) =
    GitHubEvent(r.get("id").asInstanceOf[Long],
      r.get("createdAt").toString,
      r.get("eventType").toString,
      r.get("login").toString,
      r.get("userId").asInstanceOf[Long],
      r.get("avatar").toString,
      r.get("repoId").asInstanceOf[Long],
      r.get("repoName").toString,
      // omg, seek help, find a scala/avro marshaling lib
      collectionAsScalaIterable(r.get("commitMessages")
        .asInstanceOf[util.Collection[AnyRef]])
        .map(_.toString).toList,
      r.get("commentBody").toString,
      r.get("prBody").toString,
      r.get("language").toString)
}
