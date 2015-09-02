package clients.github

import com.ning.http.client.Response
import dispatch.Defaults._
import dispatch._

class GitHubClient {
  val accessToken = Config.get.githubAccessKey

  val githubApi = host("api.github.com").secure <:< Map("Authorization" -> s"token $accessToken")

  /**
   * Get the latest GitHub events.
   *
   * @param lastETag Used to determine if new events from last call are
   *                 available.  Won't increment hourly rate limit when
   *                 no new events are available.
   * @return Returns a dispatch Future of Either.
   *         2XX responses are returned as Response all others are
   *         Throwables that are converted into simple string messages.
   */
  def events(lastETag: Option[String]): dispatch.Future[Either[Throwable, Response]] = {
    val c =
      lastETag match {
        case Some(eTag) => githubApi / "events" <:< Map("If-None-Match" -> eTag)
        case None => githubApi / "events"
      }
    Http(c OK as.Response(r => r)).either
  }
}
