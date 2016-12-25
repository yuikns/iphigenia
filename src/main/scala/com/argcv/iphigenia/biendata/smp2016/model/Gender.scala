package com.argcv.iphigenia.biendata.smp2016.model

/**
 *
 * @author Yu Jing <yu@argcv.com> on 12/24/16
 */
case class Gender(v: Int) {
  override def toString = v match {
    case 1 => "m"
    case 2 => "f"
    case _ => ""
  }
}

object Gender {
  def fromString(s: String): Option[Gender] = {
    s match {
      case "男" => Some(Gender(1))
      case "m" => Some(Gender(1))
      case "女" => Some(Gender(2))
      case "f" => Some(Gender(2))
      case _ => None
    }
  }
}