package automat

import utest._

object SummaryTest extends TestSuite{
  val userA = "user-a"
  val userB = "user-b"
  val userC = "user-c"

  val storyA = "Story A"
  val storyB = "Story B"
  val storyC = "Story C"

  val sample = 
    Summary(List(
      StorySummary(storyA, List(UserSummary(userC, 3,  8), UserSummary(userB, 2, 10))),
      StorySummary(storyB, List(UserSummary(userA, 4,  9), UserSummary(userB, 3, 10))),
      StorySummary(storyC, List(UserSummary(userB, 5, 10), UserSummary(userA, 4,  9)))
    ))

  val tests = Tests{
    "summary" - { 
      val obtained = Summary(
        commentsCount = List(
          (storyA, Histogram(userA -> 1, userB -> 2, userC -> 3)),
          (storyB, Histogram(userA -> 4, userB -> 3, userC -> 2)),
          (storyC, Histogram(userA -> 4, userB -> 5, userC -> 3))
        ),
        topCommentersCount = 2
      )

      val expected = sample

      assert(obtained == expected)
    }

    "toString" - {
      val obtained = sample.toString

      val expected = 
        """|
           || Story   | 1st Top Commenter               | 2nd Top Commenter               |
           || ------- | ------------------------------- | ------------------------------- |
           || Story A | user-c (3 for story - 8 total)  | user-b (2 for story - 10 total) |
           || Story B | user-a (4 for story - 9 total)  | user-b (3 for story - 10 total) |
           || Story C | user-b (5 for story - 10 total) | user-a (4 for story - 9 total)  |""".stripMargin

      assert(obtained == expected)
    }
  }
}