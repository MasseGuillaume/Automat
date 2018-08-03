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
    // Nodes to expand
    val frontier = mutable.Queue[S]()
    frontier ++= seeds

    // Nodes expanded
    val buffer = mutable.Queue[E]()

    // Using the flow to fetch more data
    var inFlight = false

    // Sink pulled but the buffer was empty
    var downstreamWaiting = false

    def isBufferFull() = buffer.size >= bufferSize

    def fillBuffer(): Unit = {
      val batchSize = Math.min(bufferSize - buffer.size, frontier.size)
      val batch = frontier.dequeueN(batchSize)
      inFlight = true
      debug(s"sent: $batchSize")

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
          inFlight = false
          es.foreach{ e =>
            buffer += e
            frontier ++= loop(e)
          }
          if (downstreamWaiting && buffer.nonEmpty) {
            val e = buffer.dequeue
            downstreamWaiting = false
            sendOne(e)
          } else {
            checkCompletion()
          }
          debug(s"got: $got")
          ()
        }
      }

      toProcess.onComplete(callback.invoke)
    }
    override def preStart(): Unit = {
      checkCompletion()
    }

    def checkCompletion(): Unit = {
      if (!inFlight && buffer.isEmpty && frontier.isEmpty) {
        completeStage()
      }
    } 

    def sendOne(e: E): Unit = {
      push(out, e)
      checkCompletion()
    }

    def onPull(): Unit = {
      if (buffer.nonEmpty) {
        sendOne(buffer.dequeue)
      } else {
        downstreamWaiting = true
      }

      if (!isBufferFull && frontier.nonEmpty) {
        fillBuffer()
      }
    }

    setHandler(out, this)


    def debug(msg: String): Unit = {
      println(
        s"""|
            |
            |$msg
            |inFlight: ${inFlight}
            |buffer:   ${buffer.size}
            |frontier: ${frontier.size}
            |
            |""".stripMargin
      )
    }
  }
}