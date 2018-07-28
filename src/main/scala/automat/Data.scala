package automat

object Story {
  def apply(title: StoryTitle, kids: ItemId*): Story = Story(title, kids.toList)
}
case class Story(title: StoryTitle, kids: List[ItemId])

object Comment {
  def apply(user: User, kids: ItemId*): Comment = Comment(user, kids.toList)
}
case class Comment(user: User, kids: List[ItemId])

