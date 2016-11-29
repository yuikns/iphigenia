package com.argcv.iphigenia.conf

import com.argcv.valhalla.console.ColorForConsole._
import com.argcv.valhalla.utils.Awakable
import com.typesafe.config.ConfigFactory

import scala.util.Properties

/**
 * @author yu
 */
trait IphigeniaDefaultSettings extends Awakable {
  lazy val mode = {
    // default/local
    val envMode = Properties.envOrElse("IPCONF", "default")
    logger.info(s"[${"IPCONF".withColor(BLUE)}] configure mode : ${envMode.withColor(CYAN)}") //  + "$\t$" + sys.env.toList.mkString("|")
    envMode
  }
  lazy val conf = ConfigFactory.load("iphigenia").getConfig(mode)

  //  lazy val MONGO_ADDR = conf.getConfigList("mongo.addr")
  //  lazy val MONGO_ADDR_ARRAY: Array[(String, Int)] = MONGO_ADDR.toArray collect {
  //    case item: Config => (item.getString("host"), item.getInt("port"))
  //  }
  //
  //  lazy val MONGO_DB = conf.getString("mongo.db")

}

object IphigeniaDefaultSettings extends IphigeniaDefaultSettings
