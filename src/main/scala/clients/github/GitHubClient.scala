package clients.github

import clients.Config
import dispatch.Defaults._
import dispatch._
import play.api.libs.json._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

case class GitHubEvent(id: BigInt, createdAt: String, eventType: String, raw: String)

class GitHubClient {
  val name = "GitHub"

  val accessToken = Config.get.githubAccessKey
  // 100 is the max page size this end point accepts as of 20150905
  val eventsPageSize = "100"
  val requestHeaders = Map(
    "Authorization" -> s"token $accessToken",
    "User-Agent" -> "Sean's Spark Streaming Demo"
  )
  val githubApi = host("api.github.com").secure <:< requestHeaders
  var lastETag: Option[String] = None
  var lastId: Option[BigInt] = None

  /**
   * Get a transformed list of Events from GitHub.
   * @return A future Either that represents Http or Parsing errors or
   *         the fully parsed list of Events.
   */
  def go = Future {
    // this sucks, but how else to yield a Future[Either[String, List[GitHubEvent]]]?
    val either = Await.result(events(lastETag), Duration.Inf)
    for {
      response <- either.right
      eventList <- createEvents(response.getResponseBody, lastId).right
    } yield {
      println("Headers" + response.getHeaders)
      println("Response body" + response.getResponseBody)

      lastETag = Some(response.getHeader("ETag"))
      if (eventList.length > 0) lastId = Some(eventList.maxBy(e => e.id).id)
      eventList
    }
  }

  /**
   * Get the latest GitHub events from their API.
   *
   * @param lastETag The ETag from the last events call.  Used to only get
   *                 new events.
   * @return Returns a dispatch.Future of Either.
   *         2XX responses are returned as Response all others are
   *         Throwables that are converted into simple string messages.
   */
  def events(lastETag: Option[String]) = {
    val eventEndpoint = githubApi / "events" <<? Map("per_page" -> eventsPageSize)
    val c =
      lastETag match {
        case Some(eTag) => eventEndpoint <:< Map("If-None-Match" -> eTag)
        case None => eventEndpoint
      }
    val future = Http(c OK as.Response(r => r)).either

    for (ex <- future.left)
      yield "An error happened on GitHub API: " + ex.getMessage
  }

  /**
   * Create a list of GitHubEvent's from the GitHub events JSON response.
   * @param responseBody The GitHub events JSON response.
   * @param lastId The ID of the most recent event from the last call.
   * @return An Either with an error message indicating all parsing failures
   *         or a list of successfully parsed GitHubEvent's.
   *
   *         A Left error message will be returned if there's at least one
   *         parsing failure.
   */
  def createEvents(responseBody: String, lastId: Option[BigInt]): Either[String, List[GitHubEvent]] = {
    val json = Json.parse(responseBody)

    val eventListResult = for {
      events <- json.validate[List[JsValue]]
    } yield events

    eventListResult match {
      case JsSuccess(eventList: List[JsValue], path) =>

        val githubEventResults: List[JsResult[GitHubEvent]] =
          for (e <- eventList)
            yield for {
                id <- (e \ "id").validate[String]
                createdAt <- (e \ "created_at").validate[String]
                eventType <- (e \ "type").validate[String]
              } yield GitHubEvent(BigInt(id), createdAt, eventType, e.toString())

        githubEventResults.partition(_.getClass == classOf[JsError]) match {
          case (errors, success) =>
            if (errors.length > 0)
              Left(s"There were ${errors.length} errors parsing ${eventList.length} events.\n" + errors.mkString("\n"))
            else
              Right(newEvents(success.map(_.get), lastId))
        }
      case e: JsError => Left("Could not parse events array: \n" + e.toString)
    }
  }

  /**
   * Only get new events based on consecutive GitHub event ID
   * @param eventList The list of GitHubEvent's
   * @param lastId The ID of the most recent event from the last call.
   * @return The list of new GitHubEvent's since the last call.
   */
  def newEvents(eventList: List[GitHubEvent], lastId: Option[BigInt]) =
    lastId match {
      case Some(id) =>
        val newEventList = eventList.filter(_.id > id)
        if (newEventList.length > 0) {
          val missedEventGap = newEventList.minBy(_.id).id - id
          if (missedEventGap > 1) println(s"Missed $missedEventGap events.")
        }
        newEventList
      case None => eventList
    }
}
