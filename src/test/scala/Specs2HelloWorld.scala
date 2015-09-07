import streaming.{GitHubClientSpecs, KafkaAvroProducer, GitHubEvent}
import com.ning.http.client.Response
import dispatch._, Defaults._
import org.specs2.mutable._
import scala.concurrent._
import scala.concurrent.duration._
import play.api.libs.json._


case class Location(city: String, state: String)

class Specs2HelloWorld extends Specification {

  //  "The 'Hello world' string" should {
  //    "contain 11 characters" in {
  //      "Hello world" must have size(11)
  //    }
  //    "start with 'Hello'" in {
  //      "Hello world" must startWith("Hello")
  //    }
  //    "end with 'world'" in {
  //      "Hello world" must endWith("world")
  //    }
  //
  //  }
  def foo() = {
    val svc = url("http://api.hostip.info/country.php")
    val country = Http(svc OK as.String)

    for (c <- country)
      println(c)
  }

  def weatherSvc(loc: Location) = {
    host("api.wunderground.com") / "api" / "5a7c66db0ba0323a" /
      "conditions" / "q" / loc.state / (loc.city + ".xml")
  }

  //  "Learning dispatch" should {
  //    "Get latest events" in {
  //      foo()
  //      success
  //    }
  //    "Get weather" in {
  //      val nyc = Location("New York", "NY")
  //
  //      println(Await.result(Http(weatherSvc(nyc) OK as.String), Duration.Inf))
  //      success
  //    }
  //
  //  }

  //  "clients.github.GitHubClient" should {
  //    "Get latest events" in {
  //      val g = new GitHubClient(useLastETag = false)
  //
  //      val f = for (ex <- g.events.left)
  //        yield "An error happened on GitHub API: " + ex.getMessage
  //
  //      val responseFuture = for (res <- f.right) yield res
  //
  //      val realResponse = Await.result(responseFuture, Duration.Inf)
  //
  //      for (response <- realResponse.right) {
  //        println("Headers" + response.getHeaders)
  //        println("Response body" + response.getResponseBody)
  //      }
  //
  //      for (error <- realResponse.left) {
  //        println(error)
  //      }
  //
  //      success
  //    }
  //  }
  "kafka" should {
    "produce" in {
      val e = GitHubEvent(123L, "createdAt", "eventType", "login", 123L, "avatar", 123L, "repoName")
      val p = new KafkaAvroProducer
      try {
        p.send("GitHubEventStream", e.toAvro).get()
      } finally {
        p.close()
      }

      success
    }
  }
}
