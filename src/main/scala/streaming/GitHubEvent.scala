package streaming

import org.apache.avro.Schema
import org.apache.avro.generic.GenericData

case class GitHubEvent(id: Long, createdAt: String, eventType: String,
                       login: String, userId: Long, avatar: String, repoId: Long,
                       repoName: String) {
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
      |        }
      |    ],
      |    "name": "clients.github.GitHubEvent",
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
    avroRecord
  }

  def toCaseClass(r: GenericData.Record) =
    GitHubEvent(r.get("id").toString.toLong,
      r.get("createdAt").toString,
      r.get("eventType").toString,
      r.get("login").toString,
      r.get("userId").toString.toLong,
      r.get("avatar").toString,
      r.get("repoId").toString.toInt,
      r.get("repoName").toString)
}
