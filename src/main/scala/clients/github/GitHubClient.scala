package clients.github

import clients.Config
import com.ning.http.client.Response
import dispatch.Defaults._
import dispatch._
import play.api.libs.json._

case class GitHubEvent(id: String, createdAt: String, eventType: String)

class GitHubClient(useLastETag: Boolean = false) {
  val name = "GitHub"

  val accessToken = Config.get.githubAccessKey

  val githubApi = host("api.github.com").secure <:< Map("Authorization" -> s"token $accessToken")

  var lastETag: Option[String] = None

  def go = events

  /**
   * Get the latest GitHub events.
   *
   * @return Returns a dispatch.Future of Either.
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
        if (useLastETag) lastETag = Some(response.getHeader("ETag"))
        response
      }
  }

  /**
   * Create a list of GitHubEvent's from the GitHub events JSON response.
   * @param responseBody The GitHub events JSON response.
   * @return An Either with an error message indicating all parsing failures
   *         or a list of successfully parsed GitHubEvent's.
   *
   *         A Left error message will be returned if there's at least one
   *         parsing failure.
   */
  def createEvents(responseBody: String): Either[String, List[GitHubEvent]] = {
    val json = Json.parse(responseBody)

    val eventListJs = for {
      events <- json.validate[List[JsValue]]
    } yield events

    eventListJs match {
      case JsSuccess(events: List[JsValue], path) => {

        val eventList: List[JsResult[GitHubEvent]] =
          for (e <- events)
            yield for {
                id <- (e \ "id").validate[String]
                createdAt <- (e \ "created_at").validate[String]
                eventType <- (e \ "type").validate[String]
              } yield GitHubEvent(id, createdAt, eventType)

        eventList.partition(_.getClass == classOf[JsError]) match {
          case (errors, success) =>
            if (errors.length > 0)
              Left(s"There were errors parsing ${errors.length} events.\n" + errors.mkString("\n"))
            else
              Right(success.map(s => s.get))
        }
      }
      case e: JsError => Left("Could not parse events array: \n" + e.toString)
    }
  }
}
