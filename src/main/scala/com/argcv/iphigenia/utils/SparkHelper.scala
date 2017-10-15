package com.argcv.iphigenia.utils

import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}

/**
  *
  * @author Yu Jing <yu@argcv.com> on 10/6/16
  */
object SparkHelper {

  def createSparkContext: SparkContext = createSparkContext("Iphigenia", "local[*]")

  def createSparkContext(appName: String,
                         masterUrl: String = "local[*]",
                         cfg: (SparkConf) => SparkConf = { in => in }): SparkContext = {
    new SparkContext(sparkConf(appName, masterUrl, cfg))
  }

  def sparkConf(appName: String, masterUrl: String, cfg: (SparkConf) => SparkConf): SparkConf = {
    cfg(new SparkConf()
      .setAppName(appName)
      .setMaster(masterUrl)
      .set("spark.executor.memory", "2G")
      .set("spark.driver.memory", "1G")
      //      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      //            .set("spark.kryoserializer.buffer","24")
      //    conf.registerKryoClasses(Array(classOf[Event]))
      //    conf.registerKryoClasses(Array(classOf[Booster]))
    )
  }

  def createSparkSession: SparkSession = createSparkSession("Iphigenia", "local[*]")

  def createSparkSession(appName: String,
                         masterUrl: String = "local[*]",
                         cfg: (SparkConf) => SparkConf = { in => in }): SparkSession = {
    SparkSession.builder().config(sparkConf(appName, masterUrl, cfg)).getOrCreate()
  }
}
