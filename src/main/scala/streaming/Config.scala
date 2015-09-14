package streaming

import com.typesafe.config.ConfigFactory

case class Config(githubAccessKey: String, resolution: Int,
                  kafkaBrokerList: String, kafkaSchemaRegistry: String,
                  mongoHost: String)

object Config {
  val conf = ConfigFactory.load()

  val get = Config(
    conf.getString("github-access-key"),
    conf.getInt("resolution"),
    conf.getString("kafka.brokerList"),
    conf.getString("kafka.schemaRegistry"),
    conf.getString("mongoHost"))
}
