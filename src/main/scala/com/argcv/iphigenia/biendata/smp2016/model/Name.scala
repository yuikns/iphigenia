package com.argcv.iphigenia.biendata.smp2016.model

import java.io.File
import java.util.concurrent.atomic.AtomicInteger

import com.argcv.valhalla.utils.Awakable
import com.argcv.valhalla.string.StringHelper._

import scala.io.Source
/**
 *
 * @author Yu Jing <yu@argcv.com> on 12/24/16
 */
object Name extends Awakable {
  def nameTokenLizer(s: String) = {
    def typeFlag(c: Char) = {
      if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
        1
      } else if (isChineseChar(c)) {
        2
      } else {
        0
      }
    }
    val rest = s.foldLeft(Array[String](), new StringBuffer(), 0) { (l, c) =>
      val cflg = typeFlag(c)
      if (cflg == 2) {
        val l2 = l._2.toString
        if (l2.nonEmpty) {
          (l._1 :+ l2 :+ c.toString, new StringBuffer(), cflg)
        } else {
          (l._1 :+ c.toString, l._2, cflg)
        }
      } else if (cflg == 1) {
        if (l._3 != 1) {
          (l._1, new StringBuffer().append(c), cflg)
        } else {
          (l._1, l._2.append(c), cflg)
        }
      } else { // cflg == 0
        if (l._3 == 1) {
          (l._1 :+ l._2.toString, new StringBuffer(), cflg)
        } else {
          (l._1, new StringBuffer(), cflg)
        }
      }
    }
    if (rest._3 != 1) {
      rest._1
    } else {
      rest._1 :+ rest._2.toString
    }
  }

  lazy val nfMap: Map[String, Int] = {
    logger.info("loading nfMap")
    val m = Source.fromFile(new File("data/smp2016/train/train_info.txt")).
      getLines().toList.flatMap(SocialData.getIdAndName).
      flatMap(e => nameTokenLizer(e._2)).
      groupBy(identity).
      map(e => (e._1, e._2.length)).
      filter(_._2 > 10).
      toList.sortWith(_._2 > _._2).
      take(100).
      map(_._1).
      zipWithIndex.toMap
    logger.info("nfMap loaded...")
    m
  }

  def nameFeats(id: Long): Array[Float] = {
    val sz = nfMap.size
    val initArr = (0 until sz).map(_ => new AtomicInteger()).toArray
    SocialData.id2Name.get(id) match {
      case Some(name) =>
        val nameTokens = nameTokenLizer(name)
        nameTokens.foreach { e =>
          nfMap.get(e) match {
            case Some(score) =>
              initArr(score).incrementAndGet()
            case None =>
          }
        }
      case None =>
    }
    initArr.map(_.get().toFloat)
  }

  lazy val stfMap: Map[String, Int] = {
    logger.info("loading stfMap")
    val m = Status.trainStatusInfo.values.flatten.
      flatMap(e => e.data).
      groupBy(identity).
      map(e => (e._1, e._2.size)).
      filter(_._2 > 10).
      toList.sortWith(_._2 > _._2).
      map(_._1).
      zipWithIndex.toMap
    logger.info("stfMap loaded...")
    m
  }

  def statusFeats(id: Long, statusInfo: Map[Long, List[Status]]): Array[Float] = {
    val sz = stfMap.size
    val initArr = (0 until sz).map(_ => new AtomicInteger()).toArray
    statusInfo.get(id) match {
      case Some(tokens) =>
        tokens.foreach { st =>
          st.data.foreach { e =>
            stfMap.get(e) match {
              case Some(score) =>
                initArr(score).incrementAndGet()
              case None =>
            }
          }
        }
      case None =>
    }
    initArr.map(_.get().toFloat)
  }

  def trainStatusFeats(id: Long) = statusFeats(id, Status.trainStatusInfo)

  def testStatusFeats(id: Long) = statusFeats(id, Status.testStatusInfo)

}
