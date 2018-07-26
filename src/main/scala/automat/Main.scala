import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.unmarshalling._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import org.json4s._

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.util.{Try, Failure, Success}


case class Story(title: String, kids: List[Long], descendants: Int)
case class Comment(by: String, kids: List[Long])

object Main {
  private implicit val formats = DefaultFormats
  private implicit val serialization = native.Serialization

  implicit val system = ActorSystem()
  import system.dispatcher
  implicit val materializer = ActorMaterializer()

  private val connectionPool
    : Flow[(HttpRequest, HttpRequest),
           (Try[HttpResponse], HttpRequest),
           Http.HostConnectionPool] = {

    Http().cachedHostConnectionPoolHttps[HttpRequest]("hacker-news.firebaseio.com")
  }

  private def parseJson[T](implicit un: Unmarshaller[ResponseEntity, T])
    : Flow[(Try[HttpResponse], _),
           Either[String, T],
           akka.NotUsed] = {
    Flow[(Try[HttpResponse], _)]
      .mapAsyncUnordered(parallelism = 1) {
        case (Success(res @ HttpResponse(StatusCodes.OK, _, entity, _)), ar) => {
          Unmarshal(entity).to[T].map(Right(_))
        }
        case (Success(x), ar) =>
          Future.successful(Left(s"Unexpected status code ${x.status} for $ar"))
        case (Failure(e), ar) =>
          Future.failed(new Exception(s"Failed to fetch $ar", e))
      }
  }

  def main(args: Array[String]): Unit = {
    val topStoriesRequest = 
      HttpRequest(
        uri = Uri("/v0/topstories.json"),
        headers = List(Accept(MediaTypes.`application/json`))
      )

    def item(n: Int): HttpRequest = 
      HttpRequest(
        uri = Uri(s"/v0/item/$n.json"),
        headers = List(Accept(MediaTypes.`application/json`))
      )
       
    def cacheByRequest(request: HttpRequest): (HttpRequest, HttpRequest) = (request, request)

    def showTop(top: Either[String, List[Int]]): Unit = {
      top match {
        case Right(list) => list.take(30).foreach(println)
        case Left(e)     => println(e)
      }
    }

    val topStories = 
      Source.single(topStoriesRequest)
       .map(cacheByRequest)
       .via(connectionPool)
       .via(parseJson[List[Int]])
       .runWith(Sink.foreach(showTop))

    Await.result(
      topStories,
      Duration.Inf
    )

    def showStory(story: Either[String, Story]): Unit = {
      story match {
        case Right(story) => println(story)
        case Left(e)     => println(e)
      }
    }


    val oneItem = item(17619352)

    val run =
      Source.single(oneItem)
        .map(cacheByRequest)
        .via(connectionPool)
        .via(parseJson[Story])
        .runWith(Sink.foreach(showStory))

    
    Await.result(
      run,
      Duration.Inf
    )

    system.terminate()
  }
}


