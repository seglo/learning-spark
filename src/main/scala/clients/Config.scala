package clients

import com.typesafe.config.ConfigFactory

case class Config(githubAccessKey: String, resolution: Int)

object Config {
  val conf = ConfigFactory.load()

  def get = Config(
    conf.getString("github-access-key"),
    conf.getInt("resolution"))
}
