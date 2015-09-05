package clients

import clients.github._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object ClientRunner {
  val clients = Seq(new GitHubClient)
  val resolution = Config.get.resolution

  def main (args: Array[String]) = run()

  def run() = {
    var loop = true
    while(loop) {
      for (c <- clients) {
        val response = Await.result(c.go, Duration.Inf)

        response match {
          case Right(parsedResponse) =>
            produce(c.name, parsedResponse)
            Thread.sleep(resolution)
          case Left(error) =>
            println("An error occurred: " + error)
            // kill the loop when an error occurs so we don't make octocat mad
            loop = false
        }
      }
    }
  }

  def produce(clientName: String, response: GitHubEventResponse) = {
    println("Producing a response for " + clientName)
    println("Events:\n" + response.events.mkString)
  }
}
