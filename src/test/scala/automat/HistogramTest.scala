package automat

import utest._

object HistogramTest extends TestSuite{
  val tests = Tests{
    "merge disjoint" - { 
      val obtained = Histogram('a -> 1) ++ Histogram('b -> 2)
      val expected =  Histogram('a -> 1, 'b -> 2)
      assert(obtained == expected)
    }

    "merge overlap" - {
      val obtained = Histogram('a -> 1, 'b -> 1) ++ Histogram('b -> 1, 'c -> 1)
      val expected =  Histogram('a -> 1, 'b -> 2, 'c -> 1)
      assert(obtained == expected) 
    }

    "add present" - {
      val obtained = Histogram('a -> 1) + 'a
      val expected = Histogram('a -> 2)
      assert(obtained == expected)
    }

    "add present" - {
      val obtained = Histogram('b -> 1) + 'a
      val expected = Histogram('a -> 1, 'b -> 1)
      assert(obtained == expected)
    }

    'top - {
      val obtained = Histogram(('a' to 'z').zipWithIndex: _*).top(3)
      val expected = List(('z', 25), ('y', 24), ('x', 23))
      assert(obtained == expected)
    }
  }
}