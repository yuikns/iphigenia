package com.argcv.iphigenia.biendata.smp2016.model

import com.argcv.valhalla.string.StringHelper._

import scala.collection.parallel.immutable.ParSeq

/**
 *
 * @author Yu Jing <yu@argcv.com> on 12/24/16
 */
case class Age(v: Int) {
  override def toString = v match {
    case 1 => "-1979"
    case 2 => "1980-1989"
    case 3 => "1990+"
    case _ => ""
  }
}

object Age {
  def fromStrAge(a: String): Option[Age] = {
    a.safeToInt match {
      case Some(v) =>
        Some(fromIntAge(v))
      case None =>
        None
    }
  }

  def fromIntAge(a: Int): Age = {
    if (a <= 1979) Age(1)
    else if (a < 1990) Age(2)
    else Age(3)
  }
}