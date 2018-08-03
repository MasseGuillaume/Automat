package automat

import net.steppschuh.markdowngenerator.table.Table

case class UserSummary(name: String, storyCount: Int, topStoriesCount: Int) {
  override def toString: String = s"$name ($storyCount for story - $topStoriesCount total)"
}
case class StorySummary(title: StoryTitle, topUsers: List[UserSummary])
case class Summary(stories: List[StorySummary]) {
  override def toString: String = {
    val table = new Table.Builder()

    val n = stories.map(_.topUsers.size).reduceOption(_ max _).getOrElse(0)
    def toCardinal(i: Int): String = {
      val s = i.toString
      val card =
        s.last match {
          case '1' => "st"
          case '2' => "nd"
          case '3' => "rd"
          case _   => "th"
        }
      s + card
    }
      
    val header = "Story" +: (1 to n).map(i => s"${toCardinal(i)} Top Commenter").toSeq

    table.addRow(header: _*)

    stories.foreach{story =>
      val row = (story.title +: story.topUsers.map(_.toString)).padTo(n + 1, "")
      table.addRow(row: _*)
    }

    System.lineSeparator + table.build().toString
  }
}

object Summary {
  def apply(commentsCount: CommentsCount, topCommentersCount: Int): Summary = {
    val global = commentsCount
      .map{ case (_, h) => h }
      .foldLeft(Histogram.empty[User])( (acc, x) => acc.merge(x))

    val stories = 
      commentsCount.map{ case (title, histogram) =>
        val topUsers =
          histogram.top(topCommentersCount).map{ case (user, storyCount) => 
            val globalCount = global(user)
            UserSummary(user, storyCount, globalCount)
          }

        StorySummary(title, topUsers)
      }

    Summary(stories)
  }
}