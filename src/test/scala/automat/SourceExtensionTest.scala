package automat

import utest._

import akka.actor.ActorSystem
import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object SourceExtensionTest extends TestSuite{
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def collect(flow: Flow[Int, List[Int], NotUsed], seed: Int): List[Int] = {
    val result = 
      SourceExtension.unfoldSource(List(seed), flow)(x => x)
        .mapConcat(x => x)
        .runWith(Sink.seq)

    Await.result(result, Duration.Inf).toList    
  }

  val tests = Tests{
    "unfold chain" - {
      val start = 0
      val end = 10

      val sequence: Flow[Int, List[Int], NotUsed] = Flow.fromFunction(x =>
        if (x >= end) Nil
        else List(x + 1)
      )

      val obtained = collect(sequence, start)
      val expected = ((start + 1) to end).toList

      assert(obtained == expected)
    }

    // 0 => [1, 9]
    //   1 => [10, 19]
    //   2 => [20, 29]
    //   ...
    //   9 => [90, 99]
    // _ => []
    "unfold tree" - {
      val nested: Flow[Int, List[Int], NotUsed] = Flow.fromFunction(x =>
        if (x == 0) (1 to 9).toList
        else if (1 <= x && x <= 9) ((x * 10) to ((x + 1) * 10 - 1)).toList
        else Nil
      )

      val start = 0
      val end = 99

      val obtained = collect(nested, start)
      val expected = ((start + 1) to end).toList

      assert(obtained == expected)
    }
  }
}
