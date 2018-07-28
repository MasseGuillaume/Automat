package automat

object Histogram {
  def apply[T](vs: (T, Int)*): Histogram[T] = Histogram(vs.toMap)
  def empty[T]: Histogram[T] = Histogram(Map.empty[T, Int])
  def apply[T](map: Map[T, Int]): Histogram[T] = new Histogram(new Map.WithDefault(map, _ => 0))
}

final case class Histogram[T](underlying: Map.WithDefault[T, Int]) {
  def +(value: T): Histogram[T] =
    new Histogram(underlying.updated(value, underlying(value) + 1))

  def ++(other: Histogram[T]): Histogram[T] = this.merge(other)

  def toList: List[(T, Int)] = underlying.toList

  def merge(other: Histogram[T]): Histogram[T] = {
    val m1 = underlying
    val m2 = other.underlying

    val km1 = m1.keySet
    val km2 = m2.keySet

    val extra = (km2 -- km1).map(k => k -> m2(k)).toMap
    val missing = (km1 -- km2).map(k => k -> m1(k)).toMap
    val changed = (km1.intersect(km2)).map(k => (k, (m1(k) + m2(k)) )).toMap

    Histogram(extra ++ missing ++ changed)
  }

  private def sortByCount: List[(T, Int)] = underlying.toList.sortBy(_._2)(Descending)
  def top(n: Int): List[(T, Int)] = sortByCount.take(n)

  def apply(k: T): Int = underlying(k)
}
