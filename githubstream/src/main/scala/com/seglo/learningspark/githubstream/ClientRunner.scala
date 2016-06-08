package com.seglo.learningspark.githubstream

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object ClientRunner {
  val runner = new ClientRunner(new KafkaAvroProducer)
  def main(args: Array[String]) = runner.run()
}

class ClientRunner(producer: KafkaAvroProducer) {
  val client = new GitHubClient
  val interval = Config.get.interval

  val GitHubEventStreamTopic = "GitHubEventStream"
  def run() = {
    var loop = true
    try {
      while (loop) {
        val response = Await.result(client.go, Duration.Inf)

        response match {
          case Right(parsedResponse) =>
            produce(client.name, parsedResponse)
            Thread.sleep(interval)
          case Left(error) =>
            println("An error occurred: " + error)
            // kill the loop when an error occurs so we don't make octocat mad
            loop = false
        }
      }
    } finally {
      producer.close()
    }
  }

  def produce(clientName: String, response: GitHubEventResponse) = {
//    println("Producing a response for " + clientName)
//    println("Events:\n" + response.events.mkString)
    producer.send(GitHubEventStreamTopic, response.events.map(_.toAvro))
  }
}
