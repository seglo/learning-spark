package clients.github

import clients.Config
import com.ning.http.client.Response
import dispatch.Defaults._
import dispatch._

class GitHubClient(useLastETag: Boolean) {
  val name = "GitHub"

  val accessToken = Config.get.githubAccessKey

  val githubApi = host("api.github.com").secure <:< Map("Authorization" -> s"token $accessToken")

  var lastETag: Option[String] = None

  def go = events

  /**
   * Get the latest GitHub events.
   *
   * @return Returns a dispatch Future of Either.
   *         2XX responses are returned as Response all others are
   *         Throwables that are converted into simple string messages.
   */
  def events: dispatch.Future[Either[Throwable, Response]] = {
    val c =
      lastETag match {
        case Some(eTag) => githubApi / "events" <:< Map("If-None-Match" -> eTag)
        case None => githubApi / "events"
      }
    val future = Http(c OK as.Response(r => r)).either

    for (response <- future.right)
      yield {
        if (useLastETag) lastETag = getETag(response)
        response
      }
  }

  def getETag(response: Response) =
    Some(response.getHeader("ETag"))
}
