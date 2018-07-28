package automat

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

import java.nio.file._

object Main {
  implicit val system = ActorSystem()
  import system.dispatcher
  implicit val materializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {
    val topStoriesCount = 30
    val topCommentersCount = 10

    val http = new ViaHttp(topStoriesCount)

    val commentsAggregation = 
      Await.result(run(http.topStoriesRequest, http.stories, http.comments), Duration.Inf)

    val summary = Summary(commentsAggregation, topCommentersCount)
    val output = summary.toString

    val report = Paths.get("summary.md")
    Files.write(report, output.getBytes)
    margin()
    println("Wrote report to" + report.toAbsolutePath.toString)
    margin()
    
    system.terminate()
  }

  private def margin(): Unit = (1 to 10).foreach(_ => println())

  def run(topStoriesRequest: => Future[List[ItemId]],
          stories: Flow[ItemId, Story, NotUsed],
          comments: Flow[ItemId, Comment, NotUsed]
  ): Future[CommentsCount] = {
    val accumulateComments: Sink[Comment, Future[Histogram[User]]] =
      Sink.fold(Histogram.empty[User])(
        (histogram, comment) => histogram + comment.user
      )

    val parallelism = 1

    topStoriesRequest.flatMap( topStories =>
      Source(topStories)
        .via(stories)
        .mapAsync(parallelism)(story => {
          
          println("Running: " + story.title)

          SourceExtension.unfoldSource(story.kids, comments)(_.kids)
            .runWith(accumulateComments)
            .map(histogram => {
              println("Done: " + story.title)
              (story.title, histogram)
            })
        })
        .runWith(Sink.seq)
        .map(_.toList)
    )
  }
}