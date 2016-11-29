package ml.dmlc.xgboost4j.scala.example.conf

/**
 *
 * @author Yu Jing <yu@argcv.com> on 10/7/16
 */
object XGBoostConf {
  lazy val trainFilePath = "/Users/yu/Workspace/comp/iphigenia/data/xgboost/agaricus.txt.train"
  lazy val testFilePath = "/Users/yu/Workspace/comp/iphigenia/data/xgboost/agaricus.txt.test"
  lazy val modelFilePath = "/Users/yu/Workspace/comp/iphigenia/data/xgboost/agaricus.txt.model"
  lazy val outputFilePath = "/Users/yu/Workspace/comp/iphigenia/data/xgboost/agaricus.txt.output"
  lazy val numRound = 10
  lazy val nWorkers = 4
}
