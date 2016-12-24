package com.argcv.iphigenia.biendata.smp2016.model

import com.argcv.valhalla.string.StringHelper._

import scala.collection.parallel.immutable.ParSeq

case class Info(i: Long,
  a: Option[Age],
  g: Option[Gender],
  l: Option[Loc]) {
  override def toString =
    s"$i,${a.getOrElse(Age(1))},${g.getOrElse(Gender(0))},${l.getOrElse(Loc(1))}"

  def toScores: List[Double] = Info.toScores(this)
}

object Info {
  def toScores(i: Info): List[Double] = {
    val ages: (Double, Double, Double) = i.a match {
      case Some(a) =>
        a.v match {
          case 1 =>
            (1.0, -1.0, -1.0)
          case 2 =>
            (-1.0, 1.0, -1.0)
          case 3 =>
            (-1.0, -1.0, 1.0)
          case _ =>
            (0.0, 0.0, 0.0)
        }
      case None =>
        (0.0, 0.0, 0.0)
    }

    val gens: (Double, Double) = i.g match {
      case Some(g) =>
        g.v match {
          case 1 =>
            (1.0, -1.0)
          case 2 =>
            (-1.0, 1.0)
          case _ =>
            (0.0, 0.0)
        }
      case None =>
        (0.0, 0.0)
    }

    val locs: (Double, Double, Double, Double, Double, Double, Double, Double) = i.l match {
      case Some(l) =>
        l.v match {
          case 1 =>
            (1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0)
          case 2 =>
            (-1.0, 1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0)
          case 3 =>
            (-1.0, -1.0, 1.0, -1.0, -1.0, -1.0, -1.0, -1.0)
          case 4 =>
            (-1.0, -1.0, -1.0, 1.0, -1.0, -1.0, -1.0, -1.0)
          case 5 =>
            (-1.0, -1.0, -1.0, -1.0, 1.0, -1.0, -1.0, -1.0)
          case 6 =>
            (-1.0, -1.0, -1.0, -1.0, -1.0, 1.0, -1.0, -1.0)
          case 7 =>
            (-1.0, -1.0, -1.0, -1.0, -1.0, -1.0, 1.0, -1.0)
          case 8 =>
            (-1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, 1.0)
          case _ =>
            (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }
      case None =>
        (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }
    List[Double](ages._1, ages._2, ages._3,
      gens._1, gens._2,
      locs._1, locs._2, locs._3, locs._4,
      locs._5, locs._6, locs._7, locs._8)
  }

  def result(i: Long, scores: List[Double]): Info = {
    def prob(off: Int, size: Int): Int = {
      scores.slice(off, off + size).
        zipWithIndex.maxBy(_._1)._2 + 1
    }
    Info(
      i,
      Some(Age(prob(0, 3))),
      Some(Gender(prob(3, 2))),
      Some(Loc(prob(5, 8)))
    )
  }

  def apply(i: Long): Info = Info(
    i,
    None,
    None,
    None)
}