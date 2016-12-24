package com.argcv.iphigenia.biendata.smp2016.agent

import java.util.concurrent.atomic.AtomicInteger

import com.argcv.iphigenia.biendata.smp2016.model.SocialData
import com.argcv.valhalla.client.SSDBClient
import com.argcv.valhalla.fs.SingleMachineFileSystemHelper

/**
 *
 * @author Yu Jing <yu@argcv.com> on 12/22/16
 */
object SSDBAgent extends SSDBClient {
  val pool = SSDBPool("127.0.0.1", 9528)

  /**
   *
   * @param target 星
   * @param from   粉
   * @return
   */
  def addFollowInfo(target: Long, from: Long, score: Long = 0L) = {
    zset(s">${from.toString}", target.toString, score, pool) &&
      zset(s"<${target.toString}", from.toString, score, pool)
  }

  /**
   * 取星
   *
   * @param from   src
   * @param offset offset
   * @param size   size
   */
  def getFollowing(from: Long, offset: Int = 0, size: Int = 2000): List[(String, Long)] = {
    zrange(s">${from.toString}", offset, size, pool)
  }

  /**
   * 取粉
   *
   * @param target src
   * @param offset offset
   * @param size   size
   */
  def getFollower(target: Long, offset: Int = 0, size: Int = 2000): List[(String, Long)] = {
    zrange(s"<${target.toString}", offset, size, pool)
  }

}

object SSDBDataInportor extends SingleMachineFileSystemHelper {
  lazy val spIds: Set[Long] = SocialData.trainLabels.keys.toSet

  def initDataFromPath(path: String): Unit = {
    val cnt = new AtomicInteger()
    val label = path.split("\\/").last
    getLines(path).foreach { s =>
      val arr = s.split("\\s").filter(_.nonEmpty)
      if (arr.length > 2) {
        val sp = arr.splitAt(1)
        val fe: Long = sp._1.head.toLong

        val score = if (spIds.contains(fe)) {
          1L
        } else {
          0L
        }
        sp._2.map { s =>
          val cid = s.toLong
          SSDBAgent.addFollowInfo(fe, cid,
            if (score > 0) {
              score
            } else if (spIds.contains(cid)) {
              1L
            } else {
              0L
            }
          )
        }.toList
        val ccnt = cnt.incrementAndGet()
        if (ccnt % 1000 == 0) {
          logger.info(s"$label ccnt: $ccnt")
        }
      }
    }
  }

  def doInitDataFromPath(): Unit = {
    initDataFromPath("data/smp2016/train/train_links.txt")
    initDataFromPath("data/smp2016/test/test_links.txt")
    initDataFromPath("/Users/yu/Downloads/smp2016/smp2016/other_links.txt")
    logger.info("all done..")
  }
}
