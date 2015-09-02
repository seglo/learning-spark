package clients.github

import com.typesafe.config.ConfigFactory

case class Config(githubAccessKey: String)

object Config {
  val conf = ConfigFactory.load()
  def get = Config(
    conf.getString("github-access-key"))
}
