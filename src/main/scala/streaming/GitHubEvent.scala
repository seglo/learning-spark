package streaming

import org.apache.avro.Schema
import org.apache.avro.generic.GenericData

case class GitHubEvent(id: Long, createdAt: String, eventType: String,
                       login: String, userId: Long, avatar: String, repoId: Long,
                       repoName: String) {
  val toAvro = {
    val avroRecord = new GenericData.Record(GitHubEvent.schema)

    avroRecord.put("id", id)
    avroRecord.put("createdAt", createdAt)
    avroRecord.put("eventType", eventType)
    avroRecord.put("login", login)
    avroRecord.put("userId", userId)
    avroRecord.put("avatar", avatar)
    avroRecord.put("repoId", repoId)
    avroRecord.put("repoName", repoName)
    avroRecord
  }
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
}
