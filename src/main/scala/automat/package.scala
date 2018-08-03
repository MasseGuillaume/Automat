import scala.collection.mutable

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import scala.concurrent.ExecutionContext

package object automat {
  type ItemId = BigInt
  type StoryTitle = String
  type User = String
  type CommentsCount = List[(StoryTitle, Histogram[User])]

  def Descending[T : Ordering] = implicitly[Ordering[T]].reverse

  implicit class MutableQueueExtensions[A](private val self: mutable.Queue[A]) extends AnyVal {
    def dequeueN(n: Int): List[A] = {
      val b = List.newBuilder[A]
      var i = 0
      while (i < n) {
        val e = self.dequeue
        b += e
        i += 1
      }
      b.result()
    }
  }

  implicit class SourceExtensions(private val source: Source.type) extends AnyVal {
    def unfoldTree[S, E](seeds: List[S], 
                     flow: Flow[S, E, NotUsed],
                     loop: E => List[S],
                     bufferSize: Int)(implicit ec: ExecutionContext): Source[E, NotUsed] = {
      Source.fromGraph(new UnfoldSource(seeds, flow, loop, bufferSize))
    }
  }
}