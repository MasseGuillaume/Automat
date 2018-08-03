package automat

import utest._

import akka.actor.ActorSystem
import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object FlowTest extends TestSuite{
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val tests = Tests{
    't - { main() }
  }

  val storyA = "Story A"
  val storyB = "Story B"
  val storyC = "Story C"

  val storiesM = Map(
    1 -> Story(storyA, 1, 2),
    2 -> Story(storyB, 7, 8, 9),
    3 -> Story(storyC, 16, 17, 18)
  )
  def storiesF(id: ItemId): Story = storiesM(id.toInt)

  /*
  Story A
  ├── a-1
  │   └── b-3
  │       └── c-5
  └── c-2
      └── b-4
          └── c-6

  Story B
  ├── a-7
  ├── b-8
  │   └── a-10
  └── c-9
      ├── a-11
      │   ├── b-13
      │   │   └── a-15
      │   └── c-14
      └── b-12

  Story C
  ├── a-16
  │   └── b-19
  │       └── a-23
  ├── b-17
  │   └── a-20
  │       └── b-24
  └── c-18
      ├── a-21
      │   └── b-25
      └── b-22
          ├── c-26
          └── c-27
  */

  val a = "user-a"
  val b = "user-b"
  val c = "user-c"

  val commentsM = Map(

    // A
    1  -> Comment(a, 3),
    2  -> Comment(c, 4),
    3  -> Comment(b, 5),
    4  -> Comment(b, 6),
    5  -> Comment(c),
    6  -> Comment(c),

    // B
    7  -> Comment(a),
    8  -> Comment(b, 10),
    9  -> Comment(c, 11, 12),
    10 -> Comment(a),
    11 -> Comment(a, 13, 14),
    12 -> Comment(b),
    13 -> Comment(b, 15),
    14 -> Comment(c),
    15 -> Comment(a),


    // C
    16 -> Comment(a, 19),
    17 -> Comment(b, 20),
    18 -> Comment(c, 21, 22),
    19 -> Comment(b, 23),
    20 -> Comment(a, 24),
    21 -> Comment(a, 25),
    22 -> Comment(b, 26, 27),
    23 -> Comment(a),
    24 -> Comment(b),
    25 -> Comment(b),
    26 -> Comment(c),
    27 -> Comment(c)
  )

  def commentF(id: ItemId): Comment = {
    commentsM(id.toInt)
  }
  
  val topStoriesRequest: Future[List[ItemId]] = Future.successful(List(1, 2, 3))
  val stories: Flow[ItemId, Story,   NotUsed] = Flow.fromFunction(storiesF _).throttle(1, 1.second)
  val comments: Flow[ItemId, Comment, NotUsed] = Flow.fromFunction(commentF _).throttle(1, 1.second)


  def assertNoDiff(obtained: CommentsCount, expected: CommentsCount): Unit = {
    obtained.zip(expected).foreach{ case ((storyObtained, histoObtained), (storyExpected, histoExpected)) =>
      assert(storyObtained == storyExpected)
      if (histoObtained != histoExpected) {
        println(histoObtained.toList.sorted)
        println(histoExpected.toList.sorted)
        throw new Exception(storyObtained + " Histogram")
      }
    }

    assert(obtained.size == expected.size)
  }

  def main(): Unit = {
    
    val task = Main.run(topStoriesRequest, stories, comments)
    val obtainedCommentsCount = Await.result(task, Duration.Inf)

    val expectedCommentsCount = 
      List(
        (storyA, Histogram(Map(a -> 1, c -> 3, b -> 2))),
        (storyB, Histogram(Map(a -> 4, b -> 3, c -> 2))),
        (storyC, Histogram(Map(a -> 4, b -> 5, c -> 3)))
      )

    assertNoDiff(obtainedCommentsCount, expectedCommentsCount)

    val topCommenterCount = 2
    val obtainedSummary = Summary(obtainedCommentsCount, topCommenterCount)

    val expectedSummary = 
      Summary(
        List(
          StorySummary("Story A", List(UserSummary(c, 3,  8), UserSummary(b, 2, 10))),
          StorySummary("Story B", List(UserSummary(a, 4,  9), UserSummary(b, 3, 10))),
          StorySummary("Story C", List(UserSummary(b, 5, 10), UserSummary(a, 4,  9)))
        )
      )

    if (obtainedSummary != expectedSummary) {
      println(obtainedSummary)
      println(expectedSummary)
      assert(false)
    }
  }
}