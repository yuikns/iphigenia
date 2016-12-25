package com.argcv.iphigenia.biendata.smp2016.model

import java.util.concurrent.atomic.AtomicInteger

import com.argcv.valhalla.fs.SingleMachineFileSystemHelper
import com.argcv.valhalla.utils.Awakable
import com.argcv.valhalla.string.StringHelper._

import scala.collection.parallel.immutable.ParMap

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
  lazy val trainIndex = {
    val idx = new StatusIndex
    logger.info("building index .... ")
    val cnt = new AtomicInteger()
    trainStatusInfo.par.foreach { elem =>
      val id: Long = elem._1
      val tokens: List[String] = elem._2.flatMap(_.data)
      idx.insert(tokens, id)
      val ccnt = cnt.incrementAndGet()
      if (ccnt % 5000 == 0) {
        logger.info(s"index building .... $ccnt")
      }
    }
    logger.info("index built...")
    idx
  }

  def loadStatusInfo(path: String) = {
    logger.info(s"[status] loading ... -- $path")
    val cnt = new AtomicInteger()
    val statusMap = getLines(path).toList.par.flatMap { s =>
      val ccnt = cnt.incrementAndGet()
      if (ccnt % 5000 == 0) {
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
    //    writeLines("data/smp2016/xg/uags.txt") { bw =>
    //      trainStatusInfo.flatMap(_._2).map(_.uag).toList.distinct.zipWithIndex.foreach { i =>
    //        bw.append(i._1).append("\t").append((i._2 + 1).toString).write("\n")
    //      }
    //    }
    writeLines("data/smp2016/xg/uags.txt") { bw =>
      trainStatusInfo.flatMap(_._2).map(_.uag).
        toList.groupBy(identity).
        map(e => e._1 -> e._2.length).toList.
        filter(_._2 > 10).
        sortWith(_._2 > _._2).
        map(_._1).
        take(100).
        zipWithIndex.foreach { i =>
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

  def trainAddrFeats(id: Long): Array[Float] = addrFeats(id, trainStatusInfo)

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

  def testAddrFeats(id: Long) = addrFeats(id, testStatusInfo)

  def queryIndex(id: Long, statusInfo: Map[Long, List[Status]] = trainStatusInfo): List[(Int, Double)] = {
    statusInfo.get(id) match {
      case Some(stl) =>
        val tokens: List[String] = stl.flatMap(_.data)
        val resp: List[(Int, Double)] = trainIndex.query(tokens).take(20)
        resp
      case None =>
        List[(Int, Double)]()
    }
  }

  def indexTest(id: Long) = {
    val ids = queryIndexExplain(id).take(100).filter(_._3 != id)
    val minfo = SocialData.trainLabels(id)
    val av = minfo.a.get.v
    val gv = minfo.g.get.v
    val lv = minfo.l.get.v
    val resp = ids.foldLeft(0.0, 0.0, 0.0, 0.0) { (curr, cid) =>
      val factor = cid._2
      val cinfo = SocialData.trainLabels(cid._3)
      (curr._1 + factor * (cinfo.a match {
        case Some(a) =>
          if (a.v == av) 1 else 0
        case None =>
          0
      }),
        curr._2 + factor * (cinfo.g match {
          case Some(g) =>
            if (g.v == gv) 1 else 0
          case None =>
            0
        }),
        curr._3 + factor * (cinfo.l match {
          case Some(l) =>
            if (l.v == lv) 1 else 0
          case None =>
            0
        }),
        curr._4 + factor)
    }
    (resp,
      (resp._1 / resp._4, resp._2 / resp._4, resp._2 / resp._4),
      (resp._1 / resp._4 * 3, resp._2 / resp._4 * 2, resp._3 / resp._4 * 8),
      resp._4)
  }

  def trainIndexFeats(id: Long): Array[Float] = {
    indexFeats(id, trainStatusInfo)
  }

  def indexFeats(id: Long, statusInfo: Map[Long, List[Status]] = trainStatusInfo): Array[Float] = {
    val ids: List[(Int, Double)] = queryIndex(id, statusInfo).take(20)
    // 3 + 2 + 8
    val initArr: Array[Float] = (0 until trainIndex.size()).map(_ => 0.0F).toArray
    ids.foreach { e =>
      initArr(e._1) += e._2.toFloat
    }
    initArr
  }

  def indexFeatsWithPP(id: Long, statusInfo: Map[Long, List[Status]] = trainStatusInfo): Array[Float] = {
    val ids: List[(Int, Double, Long)] = queryIndexExplain(id, statusInfo).take(100).filter(_._3 != id)
    // 3 + 2 + 8
    val initArr: Array[Float] = (0 until 13).map(_ => 0.0F).toArray
    var factAll = 0.0F
    ids.foreach { cid =>
      val factor = cid._2.toFloat
      factAll += factor
      val cinfo = SocialData.trainLabels(cid._3)
      cinfo.a match {
        case Some(a) =>
          val av = a.v
          if (av >= 1 && av <= 3) {
            initArr(av - 1) += factor
          }
        case None =>
      }
      cinfo.g match {
        case Some(g) =>
          val gv = g.v
          if (gv == 1 || gv == 2) {
            initArr(gv + 2) += factor
          }
        case None =>
      }
      cinfo.l match {
        case Some(l) =>
          val lv = l.v
          if (lv >= 1 && lv <= 8) {
            initArr(lv + 4) += factor
          }
        case None =>
      }
    }
    if (factAll <= 0.0F) {
      initArr
    } else {
      initArr.map(_ / factAll)
    }
  }

  def queryIndexExplain(id: Long, statusInfo: Map[Long, List[Status]] = trainStatusInfo): List[(Int, Double, Long)] = {
    statusInfo.get(id) match {
      case Some(stl) =>
        val tokens: List[String] = stl.flatMap(_.data)
        val resp: List[(Int, Double, Long)] = trainIndex.query(tokens).take(20).
          map(e => (e._1, e._2, trainIndex.get(e._1).getOrElse(-1L)))
        resp
      case None =>
        List[(Int, Double, Long)]()
    }
  }

  def testIndexFeats(id: Long): Array[Float] = {
    indexFeats(id, testStatusInfo)
  }

  lazy val cachedTrainIndexFeats: ParMap[Long, Array[Float]] = {
    val cnt = new AtomicInteger()
    logger.info("generating ... cachedTrainIndexFeats")
    val cache = getLines("data/smp2016/train/train_labels.txt").toList.par.flatMap { s =>
      val arrs = s.split("\\|\\|")
      val resp = if (arrs.length >= 4) {
        val id = arrs(0).toLong
        val scoreIndex = Status.trainIndexFeats(id)
        Some(id -> scoreIndex)
      } else {
        None
      }
      val ccnt = cnt.incrementAndGet()
      if (ccnt % 300 == 0) {
        logger.info(s"cache for train index feats $ccnt")
      }
      resp
    }.toMap
    logger.info("cache for train index feats is generated ...")
    cache
  }

  lazy val cachedTestIndexFeats: ParMap[Long, Array[Float]] = {
    val cnt = new AtomicInteger()
    logger.info("generating ... cachedTestIndexFeats")
    val cache = getLines("data/smp2016/test/test_nolabels.txt").toList.par.flatMap { s =>
      val id = s.toLong
      val scoreIndex = Status.testIndexFeats(id)
      val resp = Some(id -> scoreIndex)
      val ccnt = cnt.incrementAndGet()
      if (ccnt % 300 == 0) {
        logger.info(s"cache for test index feats $ccnt")
      }
      resp
    }.toMap
    logger.info("cache for test index feats is generated ...")
    cache
  }

}

