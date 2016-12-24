package com.argcv.iphigenia.biendata.smp2016.model

import java.util.concurrent.atomic.AtomicInteger

import com.argcv.valhalla.fs.SingleMachineFileSystemHelper
import com.argcv.valhalla.utils.Awakable
import com.argcv.valhalla.string.StringHelper._

/**
 *
 * @author Yu Jing <yu@argcv.com> on 12/24/16
 */
case class Status(
  id: Long,
  numRetweet: Int,
  numReview: Int,
  uag: String,
  // yyyy-MM-dd HH:mm:ss or yyyy-MM-dd HH:mm
  ts: String,
  data: List[String],
  addr: List[Int]) {
  //def withAddr() = this.copy(addr = data.par.flatMap(Loc.reco).toList.map(_.v).filter(s => s > 0 && s != 8))
}

object Status extends Awakable with SingleMachineFileSystemHelper {
  lazy val trainStatusInfo: Map[Long, List[Status]] = loadStatusInfo("data/smp2016/train/train_status.txt")
  lazy val testStatusInfo: Map[Long, List[Status]] = loadStatusInfo("data/smp2016/test/test_status.txt")
  lazy val uagMap: Map[String, Int] = {
    logger.info("loading uag map")
    val m = getLines("data/smp2016/xg/uags.txt").map { l =>
      val arr = l.split("\t")
      arr(0) -> arr(1).toInt
    }.toMap
    logger.info("uag map: loaded...")
    m
  }

  def loadStatusInfo(path: String) = {
    logger.info(s"[status] loading ... -- $path")
    val cnt = new AtomicInteger()
    val statusMap = getLines(path).toList.par.flatMap { s =>
      val ccnt = cnt.incrementAndGet()
      if (ccnt % 100 == 0) {
        logger.info(s"[status] loading ... $ccnt  -- $path")
      }
      Status(s)
    }.toList.groupBy(_.id)
    logger.info(s"[status] loaded .. -- $path")
    statusMap
  }

  def apply(s: String): Option[Status] = {
    val arr = s.split(",")
    if (arr.length >= 6) {
      val id = arr.head.safeToLong.get
      val nrt = arr(1).safeToInt.get
      val nrv = arr(2).safeToInt.get
      val uag = arr(3)
      val ts = arr(4)
      var data = statusTokenFilter(arr.drop(5).flatMap(_.split("\\s")).toList)
      if (data.contains("签到")) {
        data = List[String]("#签到#")
      }
      val addr: List[Int] = data.par.flatMap(Loc.recoNonDefault).toList.map(_.v).filter(s => s > 0 && s != 8)
      //      val addr: List[Int] = List[Int]()
      Some(Status(
        id = id,
        numRetweet = nrt,
        numReview = nrv,
        uag = uag,
        ts = ts,
        data = data,
        addr = addr
      ))
    } else {
      None
    }
  }

  def statusTokenFilter(data: List[String]): List[String] = {
    val blackList = Set[String]()
    data.filter { s =>
      if (s.isEmpty) {
        false
      } else if (blackList.contains(s)) {
        false
      } else if (hasChinese(s)) {
        // accept chinese
        true
      } else if (s.toLowerCase.exists(c => c >= 'a' && c <= 'z')) {
        // accept contains english
        true
      } else {
        false
      }
    }.map { s =>
      if (s.startsWith("http") || s.startsWith("www")) {
        "site"
      } else if (s.startsWith("@")) {
        s.drop(1)
      } else {
        s
      }
    }
  }

  def saveUAGs() = {
    writeLines("data/smp2016/xg/uags.txt") { bw =>
      trainStatusInfo.flatMap(_._2).map(_.uag).toList.distinct.zipWithIndex.foreach { i =>
        bw.append(i._1).append("\t").append((i._2 + 1).toString).write("\n")
      }
    }
  }

  def trainUagFeats(id: Long) = uagFeats(id, trainStatusInfo)

  def testUagFeats(id: Long) = uagFeats(id, testStatusInfo)

  def uagFeats(id: Long, statusInfo: Map[Long, List[Status]]): Array[Float] = {
    val sz = uagMap.size
    val initArr = (0 until sz).map(_ => new AtomicInteger()).toArray
    statusInfo.get(id) match {
      case Some(l) =>
        l.par.foreach { (st: Status) =>
          val off: Int = uag2Off(st.uag)
          if (off > 0 && off <= sz) {
            initArr(off - 1).incrementAndGet()
          }
        }
      case None =>
    }
    initArr.map(_.get().toFloat)
  }

  def uag2Off(s: String): Int = {
    uagMap.getOrElse(s, 0)
  }

  def trainAddrFeats(id: Long) = addrFeats(id, trainStatusInfo)

  def testAddrFeats(id: Long) = addrFeats(id, testStatusInfo)

  def addrFeats(id: Long, statusInfo: Map[Long, List[Status]]): Array[Float] = {
    val sz = 7 // 0 - 6
    val initArr = (0 until sz).map(_ => new AtomicInteger()).toArray
    statusInfo.get(id) match {
      case Some(l) =>
        l.par.foreach { (st: Status) =>
          st.addr.foreach { off =>
            if (off > 0 && off <= sz) {
              initArr(off - 1).incrementAndGet()
            }
          }
        }
      case None =>
    }
    initArr.map(_.get().toFloat)
  }

}

