package clients

import clients.github.GitHubClient
import com.ning.http.client.Response

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object ClientRunner {
  val clients = Seq(new GitHubClient(useLastETag = true))
  val resolution = Config.get.resolution

  def main (args: Array[String]) {
    run
  }

  def run = {
    var loop = true
    while(loop) {
      for (c <- clients) {
        val responseFuture = c.go

        val r = Await.result(responseFuture, Duration.Inf)

        for (response <- r.right)
          produce(c.name, response)

        for (error <- r.left) {
          println("An error occurred: " + error.getMessage)
          loop = false
        }
      }

      loop = false
      Thread.sleep(resolution)
    }
  }

  def produce(clientName: String, response: Response) = {
    println("Producing a response for " + clientName)
    println("Headers" + response.getHeaders)
    println("Response body" + response.getResponseBody)
  }
}
