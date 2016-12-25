package com.argcv.iphigenia.biendata.smp2016.model

import java.io.File

import com.argcv.valhalla.utils.Awakable

import scala.io.Source

object SocialData extends Awakable {
  lazy val id2Name: Map[Long, String] = {
    val nameTrain = Source.fromFile(new File("data/smp2016/train/train_info.txt")).
      getLines().toList.flatMap(s => getIdAndName(s))
    val nameTest = Source.fromFile(new File("data/smp2016/test/test_info.txt")).
      getLines().toList.flatMap(s => getIdAndName(s))
    (nameTrain ::: nameTest).toMap
  }
  lazy val (trainFollowingInfo: Map[Long, Set[Long]], trainFollowerInfo) =
    followingInfoGetter("data/smp2016/train/train_links.txt")
  lazy val (testFollowingInfo, testFollowerInfo) =
    followingInfoGetter("data/smp2016/test/test_links.txt")
  lazy val trainLabels: Map[Long, Info] = {
    def parseInfo(s: String): Option[Info] = {
      val arrs = s.split("\\|\\|")
      if (arrs.length >= 4) {
        val id: Long = arrs(0).toLong
        val gender: Option[Gender] = Gender.fromString(arrs(1))
        val age: Option[Age] = Age.fromStrAge(arrs(2))
        val loc: Option[Loc] = Loc.reco(arrs(3))
        Some(Info(
          i = id,
          a = age,
          g = gender,
          l = loc
        ))
      } else {
        None
      }
    }
    Source.fromFile(new File("data/smp2016/train/train_labels.txt")).
      getLines().toList.
      flatMap(s => parseInfo(s)).
      map(e => e.i -> e).
      toMap
  }

  lazy val trainId2Off: Map[Long, Int] = trainLabels.
    keys.zipWithIndex.map(e => e._1 -> e._2).toMap

  def getIdAndName(s: String): Option[(Long, String)] = {
    val arrs = s.split("\\|\\|")
    if (arrs.length >= 2) {
      Some((arrs(0).toLong, arrs(1)))
    } else {
      None
    }
  }

  def followingInfoGetter(path: String) = {
    // [a,b] b is a's follower
    val rel: List[(Long, Long)] = Source.fromFile(new File(path)).
      getLines().toList.flatMap { s =>
        val arr = s.split("\\s").filter(_.nonEmpty)
        if (arr.length > 2) {
          val sp = arr.splitAt(1)
          val fe: Long = sp._1.head.toLong
          sp._2.map(s => (fe, s.toLong)).toList
        } else {
          List[(Long, Long)]()
        }
      }
    val following = rel.groupBy(_._2).
      map(r => r._1 -> r._2.map(_._1).toSet)
    val follower = rel.groupBy(_._1).
      map(r => r._1 -> r._2.map(_._2).toSet)
    (following, follower)
  }

  //  /**
  //   * 都是某人的粉丝
  //   *
  //   * @param id id
  //   * @return
  //   */
  //  def revCands(id: Long) = cands(id, testFollowingInfo, trainFollowerInfo)

  /**
   * 都被某人粉
   *
   * @param id        id
   * @param testInfo  testInfo
   * @param trainInfo trainInfo
   * @return
   */
  def cands(id: Long,
    testInfo: Map[Long, Set[Long]] = testFollowerInfo,
    trainInfo: Map[Long, Set[Long]] = trainFollowingInfo): Set[Long] = {
    testInfo.get(id) match {
      case Some(tr) =>
        tr.flatMap(sid => trainInfo.get(sid) match {
          case Some(s) => s.filterNot(l => l == id)
          case None => Set[Long]()
        })
      case None =>
        Set[Long]()
    }
  }

  def socialInfo(id: Long,
    c: Set[Long],
    testInfo: Map[Long, Set[Long]] = testFollowerInfo,
    trainInfo: Map[Long, Set[Long]] = trainFollowerInfo): Array[Float] = {
    val scores = c.par.flatMap { cid =>
      trainLabels.get(cid) match {
        case Some(cInfo) =>
          val simScore = sim(id, cid, testInfo, trainInfo)
          val revSimScore = -simScore
          // age
          val ages: (Double, Double, Double) = cInfo.a match {
            case Some(a) =>
              a.v match {
                case 1 =>
                  (simScore, revSimScore, revSimScore)
                case 2 =>
                  (revSimScore, simScore, revSimScore)
                case 3 =>
                  (revSimScore, revSimScore, simScore)
                case _ =>
                  (0.0, 0.0, 0.0)
              }
            case None =>
              (0.0, 0.0, 0.0)
          }

          val gens: (Double, Double) = cInfo.g match {
            case Some(g) =>
              g.v match {
                case 1 =>
                  (simScore, revSimScore)
                case 2 =>
                  (revSimScore, simScore)
                case _ =>
                  (0.0, 0.0)
              }
            case None =>
              (0.0, 0.0)
          }

          val locs: (Double, Double, Double, Double, Double, Double, Double, Double) = cInfo.l match {
            case Some(l) =>
              l.v match {
                case 1 =>
                  (simScore, revSimScore, revSimScore, revSimScore, revSimScore, revSimScore, revSimScore, revSimScore)
                case 2 =>
                  (revSimScore, simScore, revSimScore, revSimScore, revSimScore, revSimScore, revSimScore, revSimScore)
                case 3 =>
                  (revSimScore, revSimScore, simScore, revSimScore, revSimScore, revSimScore, revSimScore, revSimScore)
                case 4 =>
                  (revSimScore, revSimScore, revSimScore, simScore, revSimScore, revSimScore, revSimScore, revSimScore)
                case 5 =>
                  (revSimScore, revSimScore, revSimScore, revSimScore, simScore, revSimScore, revSimScore, revSimScore)
                case 6 =>
                  (revSimScore, revSimScore, revSimScore, revSimScore, revSimScore, simScore, revSimScore, revSimScore)
                case 7 =>
                  (revSimScore, revSimScore, revSimScore, revSimScore, revSimScore, revSimScore, simScore, revSimScore)
                case 8 =>
                  (revSimScore, revSimScore, revSimScore, revSimScore, revSimScore, revSimScore, revSimScore, simScore)
                case _ =>
                  (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
              }
            case None =>
              (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
          }
          Some((ages._1, ages._2, ages._3,
            gens._1, gens._2,
            locs._1, locs._2, locs._3, locs._4,
            locs._5, locs._6, locs._7, locs._8))
        case None =>
          None
      }
    }.toArray
    val scoreSum = scores.foldLeft(List[Double](
      0.0, 0.0, 0.0,
      0.0, 0.0,
      0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
    )) { (l, c) =>
      List[Double](
        l.head + c._1, l(1) + c._2, l(2) + c._3,
        l(3) + c._4, l(4) + c._5,
        l(5) + c._6, l(6) + c._7, l(7) + c._8, l(8) + c._9,
        l(9) + c._10, l(10) + c._11, l(11) + c._12, l(12) + c._13
      )
    }.map(_.toFloat).toArray
    scoreSum
  }

  def sim(testId: Long,
    trainId: Long,
    testInfo: Map[Long, Set[Long]] = testFollowerInfo,
    trainInfo: Map[Long, Set[Long]] = trainFollowerInfo): Double = {
    val testSet: Set[Long] = testInfo.getOrElse(testId, Set[Long]())
    val trainSet: Set[Long] = trainInfo.getOrElse(trainId, Set[Long]())
    if (trainSet.isEmpty || testSet.isEmpty) {
      0.0
    } else {
      sim(testSet, trainSet)
    }
  }

  def sim(sl: Set[Long], sr: Set[Long]): Double = {
    val inter = (sl & sr).size.toDouble
    if (inter < 3.0) 0.0
    else inter / (Math.sqrt(sl.size.toDouble) * Math.sqrt(sr.size.toDouble))
  }

}
