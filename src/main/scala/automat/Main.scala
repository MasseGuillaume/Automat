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
    val req = 
      HttpRequest(
        uri = Uri("/v0/topstories.json"),
        headers = List(Accept(MediaTypes.`application/json`))
      )

    def cacheByRequest(request: HttpRequest): (HttpRequest, HttpRequest) = (request, request)

    def show(top: Either[String, List[Int]]): Unit = {
      top match {
        case Right(list) => list.take(10).foreach(println)
        case Left(e)     => println(e)
      }
    }
    
    val topStories = 
      Source.single(req)
       .map(cacheByRequest)
       .via(connectionPool)
       .via(parseJson[List[Int]])
       .runWith(Sink.foreach(show))

    Await.result(
      topStories,
      Duration.Inf
    )

    system.terminate()
  }
}