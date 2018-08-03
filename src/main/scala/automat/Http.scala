package automat

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.unmarshalling._

import org.json4s._

import scala.concurrent.Future
import scala.util.{Try, Failure, Success}

import java.io.{PrintWriter, StringWriter}

object Api {
  case class Story(title: String, kids: List[ItemId])
  case class Comment(by: Option[User], kids: List[ItemId])
}

class ViaHttp(topStoriesCount: Int)(implicit materializer: ActorMaterializer, system: ActorSystem) {
  import system.dispatcher
  import Json4sSupport.unmarshaller

  private implicit val formats = DefaultFormats
  private implicit val serialization = native.Serialization


  private type HttpFlow[T] = Flow[(Try[HttpResponse], _), T, akka.NotUsed]
  private type HttpUnmarshall[T] = Unmarshaller[ResponseEntity, Either[Throwable, Option[T]]]

  private def parseJson[T](implicit un: HttpUnmarshall[T]): HttpFlow[T] = {
    Flow[(Try[HttpResponse], _)]
      .mapAsyncUnordered(parallelism = 1) {
        case (Success(res @ HttpResponse(StatusCodes.OK, _, entity, _)), _) =>
          Unmarshal(entity).to[Either[Throwable, Option[T]]]
        case (Success(x), ar) =>
          Future.failed(new Exception(s"Unexpected status code ${x.status} for $ar"))
        case (Failure(e), ar) =>
          Future.failed(new Exception(s"Failed to fetch $ar", e))
      }
      .via(keepValidAndReportErrors)
  }

  private def keepValidAndReportErrors[T]: Flow[Either[Throwable, Option[T]], T, NotUsed] =
    Flow[Either[Throwable, Option[T]]]
      .alsoTo(Sink.foreach{
        case Left(e) =>
          val sw = new StringWriter()
          e.printStackTrace(new PrintWriter(sw))
          println(sw)
        case _ => ()
      })
      .collect{case Right(Some(v)) => v}

  private val parseTopStories = parseJson[List[ItemId]]
  private val parseStory      = parseJson[Api.Story]
  private val parseComment    = parseJson[Api.Comment]

  private val baseUrl = "hacker-news.firebaseio.com"
  private val connectionPool = Http().cachedHostConnectionPoolHttps[Option[ItemId]](baseUrl)
  
  private val topStoriesHttpRequest = 
    HttpRequest(
      uri = Uri("/v0/topstories.json"),
      headers = List(Accept(MediaTypes.`application/json`))
    )

  private def itemRequest(id: ItemId): HttpRequest = 
    HttpRequest(
      uri = Uri(s"/v0/item/$id.json"),
      headers = List(Accept(MediaTypes.`application/json`))
    )
  
  private val itemFlow: Flow[ItemId,(Try[HttpResponse], Option[ItemId]), NotUsed] =
    Flow[ItemId]
      .map(id => (itemRequest(id), Some(id)))
      .via(connectionPool)

  val stories: Flow[ItemId, Story, NotUsed] = 
    itemFlow.via(parseStory).map(s => Story(s.title, s.kids))

  val comments: Flow[ItemId, Comment, NotUsed] = 
    itemFlow.via(parseComment).collect {
      case Api.Comment(Some(user), kids) => 
        Comment(user, kids)
    }

  def topStoriesRequest: Future[List[ItemId]] = {
    Source.single(topStoriesHttpRequest)
      .map(r => (r, None))
      .via(connectionPool)
      .via(parseTopStories)
      .runWith(Sink.head)
      .map(_.take(topStoriesCount))
  }
}
