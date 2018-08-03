package automat

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}

import scala.concurrent.ExecutionContext

import scala.collection.mutable
import scala.util.{Success, Failure, Try}



class UnfoldSource[S, E](seeds: List[S],
                         flow: Flow[S, E, NotUsed],
                         loop: E => List[S],
                         bufferSize: Int)(implicit ec: ExecutionContext) extends GraphStage[SourceShape[E]] {

  val out: Outlet[E] = Outlet("UnfoldSource.out")
  override val shape: SourceShape[E] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with OutHandler {  
    val frontier = mutable.Queue[S]()
    val buffer = mutable.Queue[E]()
    var inFlight = 0

    def isBufferFull() = buffer.size >= bufferSize
    frontier ++= seeds
    var downstreamWaiting = false

    def fillBuffer(): Unit = {
      val batchSize = Math.min(bufferSize - buffer.size, frontier.size)
      val batch = frontier.dequeueN(batchSize)
      inFlight += batchSize
      println("sent batchSize: " + batchSize)

      val toProcess =
        Source(batch)
          .via(flow)
          .runWith(Sink.seq)(materializer)

      val callback = getAsyncCallback[Try[Seq[E]]]{
        case Failure(ex) => {
          fail(out, ex)
        }
        case Success(es) => {
          val got = es.size
          println("got: " + got)
          inFlight -= got
          es.foreach{ e =>
            buffer += e
            frontier ++= loop(e)
          }
          if (downstreamWaiting && buffer.nonEmpty) {
            val e = buffer.dequeue
            downstreamWaiting = false
            sendOne(e)
          }
          ()
        }
      }

      toProcess.onComplete(callback.invoke)
    }
    override def preStart(): Unit = fillBuffer()

    def sendOne(e: E): Unit = {
      push(out, e)
      println(inFlight, buffer.size, frontier.size)
      if (inFlight == 0 && buffer.size == 0 && frontier.size == 0) {
        completeStage()
      }
    }

    def onPull(): Unit = {
      if (buffer.nonEmpty) {
        sendOne(buffer.dequeue)
      } else {
        downstreamWaiting = true
      }

      if (!isBufferFull) {
        fillBuffer()
      }
    }

    setHandler(out, this)
  }
}