package com.argcv.iphigenia.utils

import com.argcv.valhalla.fs.SingleMachineFileSystemHelper
import com.argcv.valhalla.utils.Awakable

/**
 *
 * @author Yu Jing <yu@argcv.com> on 12/24/16
 */
object CommonResources extends Awakable with SingleMachineFileSystemHelper {
  lazy val stopwordsZh: Set[String] = getLines("data/common/stopwords_zh.txt").map(_.trim).filter(_.nonEmpty).toSet

}
