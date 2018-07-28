package automat

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl._

import java.util.concurrent.atomic.AtomicInteger

object SourceExtension {
  def unfoldSource[I, C](seeds: List[I], flow: Flow[I, C, NotUsed])
                        (feedback: C => List[I])
                        (implicit mat: Materializer): Source[C, NotUsed] = {


    val bufferSize = 1000

    val remaining = new AtomicInteger(seeds.size)

    val (ref, publisher) =
      Source.actorRef[I](bufferSize, OverflowStrategy.fail)
        .toMat(Sink.asPublisher(true))(Keep.both)
        .run()

    seeds.foreach(ref ! _)

    Source.fromPublisher(publisher)
      .via(flow)
      .map{x =>
        feedback(x).foreach{ s =>
          remaining.incrementAndGet()
          ref ! s
        }
        x
      }
      .takeWhile(_ => {
        remaining.decrementAndGet() > 0
      }, inclusive = true)

  }
}