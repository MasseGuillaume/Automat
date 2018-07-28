
package object automat {
  type ItemId = BigInt
  type StoryTitle = String
  type User = String
  type CommentsCount = List[(StoryTitle, Histogram[User])]

  def Descending[T : Ordering] = implicitly[Ordering[T]].reverse
}