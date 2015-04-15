package quickcheck

import common._

import org.scalacheck._
import Arbitrary._
import Gen._
import Prop._

abstract class QuickCheckHeap extends Properties("Heap") with IntHeap {

  property("min1") = forAll { a: Int =>
    val h = insert(a, empty)
    findMin(h) == a
  }

//  If you insert any two elements into an empty heap, finding the minimum of the resulting heap should get the smallest of the two elements back.
  property("min1") = forAll { (a: Int, b: Int) =>
    val h = insert(a,insert(b, empty))
    findMin(h) == a.min(b)
  }

//  If you insert an element into an empty heap, then delete the minimum, the resulting heap should be empty.
  property("empty1") = forAll { a: Int =>
    deleteMin(insert(a, empty)) == empty
  }


  lazy val genHeap: Gen[H] = for {
    k <- arbitrary[Int]
    h <- oneOf(const(empty), genHeap)
  } yield insert(k, h)

  implicit lazy val arbHeap: Arbitrary[H] = Arbitrary(genHeap)


  property("gen1") = forAll { (h: H) =>
    val m = if (isEmpty(h)) 0 else findMin(h)
    findMin(insert(m, h))==m
  }


  // Given any heap, you should get a sorted sequence of elements when continually finding and deleting minima.
  // (Hint: recursion and helper functions are your friends.)

  property("gen2") = forAll { (h: H) =>

    def assertSorted(prev: Int, h: H): Boolean = {
      if (isEmpty(h)) true
      else {
        val min = findMin(h)
        if (prev > min) false
        else  assertSorted(min, deleteMin(h))
      }
    }
    (!isEmpty(h)) ==> (assertSorted(findMin(h), deleteMin(h)) == true)
  }

//  Finding a minimum of the melding of any two heaps should return a minimum of one or the other.

  property("gen3") = forAll { (h1: H, h2 :H) =>
    val min1 = findMin(h1)
    val min2 = findMin(h2)
    findMin(meld(h1,h2)) == min1.min(min2)
  }

  property("compare list vs heap") = forAll { l: List[Int] =>
    def assertEquals(h: H, l: List[Int]): Boolean = {
      if (isEmpty(h)) {
        l.isEmpty
      } else {
        !l.isEmpty && findMin(h) == l.head && assertEquals(deleteMin(h), l.tail)
      }
    }
    val sl = l.sorted
    val h = l.foldLeft(empty)((he, a) => insert(a, he))
    assertEquals(h, sl)
  }

}
