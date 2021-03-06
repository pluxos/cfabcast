package cfabcast

import scala.util.control.Exception
import scala.annotation.tailrec

case class ElementAlreadyExistsException(e: String) extends Exception(e)

/*
 * Interval of instances
 */
class IRange(self: List[Interval]) extends Iterable[Interval] 
  with Serializable {
  override def toString = self.toString
  
  override def iterator = self.iterator

  override def isEmpty = self.isEmpty

  override def nonEmpty = !self.isEmpty
  
  //FIXME: improve this!
  def insert(elem: Int): IRange = {
    @tailrec
    def tryInsert(elem: Int, in: List[Interval], out: List[Interval]): List[Interval] = in match {
      case x::xs =>
        if(x contains elem) out ++ in 
        else if(elem < x.from) {
          x.from - elem match {
            case 1 => out ::: Interval(elem, x.to) :: xs
            case _ => out ::: List(Interval(elem)) ::: x :: xs
          }
        }
        else if(elem > x.to) {
          elem - x.to match {
            case 1 => 
              if (xs.nonEmpty) {
                xs.head.from - elem match {
                  case 1 => out ::: Interval(x.from, xs.head.to) :: xs.tail
                  case _ => out ::: Interval(x.from, elem) :: xs
                }
              } else {
                out ::: Interval(x.from, elem) :: xs
              }
            case _ => tryInsert(elem, xs, out :+ x)
          }
        }
        else tryInsert(elem, xs, out)
      case List() => out ++ List(Interval(elem))
    }
    new IRange(tryInsert(elem, self, List()))
  }

  def contains(elem: Int): Boolean =
    self.exists(interval => interval contains elem)

  def iterateOverAll[U](f: Int => U) = 
    self.foreach(interval => interval.foreach(elem => f(elem)))

  def append(interval: Interval): IRange = {
    var a = this
    interval.foreach(elem => a = a insert elem)
    a
  }

  def dropLast: IRange = new IRange(self.dropRight(1))

  def complement(start: Int = 0): IRange = {
    var result = List.empty[Interval]
    def makeComplement(first: Interval, second: List[Interval]): List[Interval] = {
      if (second.nonEmpty) {
        if (second.head.from - first.to > 0)
          result = result :+ Interval(first.to + 1, second.head.from - 1)
        makeComplement(second.head, second.tail)
      }
      else
        result = result :+ Interval(first.to + 1, -1)
      result
    }

    if(self.nonEmpty) {
      if (self.head.from - start > 0)
        result = result :+ Interval(start, self.head.from - 1)
      new IRange(makeComplement(self.head, self.tail))
    }
    else
      new IRange(List(Interval(start, -1)))
  }
  
  def next: Int = 
    if (self.nonEmpty) {
      if(self.last.to != -1) self.last.to + 1
      else self.last.from + 1
    }
    else 0
}

object IRange {
  def apply() = new IRange(List())
  
  def fromMap[T](map: Map[Int, T]) : IRange = {
    var r: IRange = IRange()
    for ((k, v) <- map)
      r = r insert k
    r
  }
}

class Interval(val from: Int, val to: Int) extends Ordered[Interval] 
  with Serializable {
 // The lowest interval in this context is one that has the lowest element
  def compare(that: Interval) = this.from - that.from

  def contains(elem: Int) = (from <= elem && elem <= to)

  def foreach[U](f: Int => U) = {
    for(i <- from to to by 1) yield f(i)
    if(to == -1 && from != -1) f(from)
  }

  override def toString: String = s"[${from}, ${to}]"

  def max = if(from >= to) from else to
  def first = from
  def last = to
  def next: Int = if(to != -1) to + 1 else from + 1
}

object Interval {
  def apply(elem: Int) = new Interval(elem, elem)
  def apply(from: Int, to: Int) = new Interval(from, to)
}
