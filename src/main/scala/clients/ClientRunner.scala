package clients

import clients.github.{GitHubEvent, GitHubClient}
import com.ning.http.client.Response

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object ClientRunner {
  val clients = Seq(new GitHubClient)
  val resolution = Config.get.resolution

  def main (args: Array[String]) {
    run
  }

  def run = {
    var loop = true
    while(loop) {
      for (c <- clients) {
        val responseFuture = c.go

        //for (response <- responseFuture) { }
        val response = Await.result(responseFuture, Duration.Inf)

        response match {
          case Right(events) =>
            produce(c.name, events)
            Thread.sleep(resolution)
          case Left(error) =>
            println("An error occurred: " + error)
            // kill the loop when an error occurs so we don't make octocat mad
            loop = false
        }
      }
    }
  }

  def produce(clientName: String, events: List[GitHubEvent]) = {
    println("Producing a response for " + clientName)
    println("Events:\n" + events.mkString)
  }
}
