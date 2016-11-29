/*
 Copyright (c) 2014 by Contributors

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package ml.dmlc.xgboost4j.scala.example.spark

import com.argcv.iphigenia.utils.SparkHelper
import com.argcv.valhalla.console.ColorForConsole._
import com.argcv.valhalla.fs.SingleMachineFileSystemHelper
import com.argcv.valhalla.utils.Awakable
import ml.dmlc.xgboost4j.scala.example.conf.XGBoostConf
import ml.dmlc.xgboost4j.scala.spark.DataUtils._
import ml.dmlc.xgboost4j.scala.spark.XGBoost
import ml.dmlc.xgboost4j.scala.{ Booster, DMatrix }
import org.apache.spark.ml.feature.{ LabeledPoint => MLLabeledPoint }
import org.apache.spark.ml.linalg.{ DenseVector => MLDenseVector }
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.rdd.RDD

object SparkWithRDD extends App with SingleMachineFileSystemHelper with Awakable {
  val label = s"[${"SparkWithRDD".withColor(BLUE)}]"
  //    if (args.length != 5) {
  //      logger.info(
  //        "usage: program num_of_rounds num_workers training_path test_path model_path")
  //      sys.exit(1)
  //    }
  //    val sparkConf = new SparkConf().setAppName("XGBoost-spark-example")
  //      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
  //sparkConf.registerKryoClasses(Array(classOf[Booster]))
  //    implicit val sc = new SparkContext(sparkConf)
  //    val inputTrainPath = args(2)
  //    val inputTestPath = args(3)
  //    val outputModelPath = args(4)
  implicit val sc = SparkHelper.createSparkContext(appName = "SparkWithRDD", cfg = { in =>
    in.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .registerKryoClasses(Array(classOf[Booster]))
  })
  val inputTrainPath = XGBoostConf.trainFilePath
  val inputTestPath = XGBoostConf.testFilePath
  val modelFilePath = XGBoostConf.modelFilePath
  val outputModelPath = XGBoostConf.outputFilePath
  val numRound = XGBoostConf.numRound
  // number of iterations
  //val numRound = args(0).toInt
  val trainRDD: RDD[MLLabeledPoint] = MLUtils.loadLibSVMFile(sc, inputTrainPath).map(lp => MLLabeledPoint(lp.label, new MLDenseVector(lp.features.toArray)))
  val testSet: List[MLDenseVector] = MLUtils.loadLibSVMFile(sc, inputTestPath).collect().map(lp => new MLDenseVector(lp.features.toArray)).toList
  val testLabels: List[Double] = MLUtils.loadLibSVMFile(sc, inputTestPath).collect().map(lp => lp.label).toList
  // training parameters
  val paramMap = List(
    "eta" -> 0.1f,
    "max_depth" -> 2,
    "objective" -> "binary:logistic").toMap
  val xgboostModel = XGBoost.trainWithRDD(trainRDD, paramMap, numRound, nWorkers = XGBoostConf.nWorkers, useExternalMemory = true)
  //xgboostModel.booster.predict(new DMatrix(testSet.iterator))
  // save model to HDFS path
  xgboostModel.saveModelAsHadoopFile(modelFilePath)
  val lxgboostModel = XGBoost.loadModelFromHadoopFile(modelFilePath)

  val predictResult: Array[Array[Float]] = lxgboostModel.booster.predict(new DMatrix(testSet.iterator))

  if (predictResult.length == testLabels.length) {
    val predictionAndLabels: RDD[(Double, Double)] = sc.parallelize(testLabels.indices.map { i =>
      predictResult(i).headOption match {
        case Some(v) =>
          ((if (v > 0.5) 1 else -1).toDouble, if (testLabels(i) > 0) 1.0 else -1.0)
        case None =>
          logger.error(s"$label no result")
          (0.toDouble, if (testLabels(i) > 0) 1.0 else -1.0)
      }
    })
    val metrics = new BinaryClassificationMetrics(predictionAndLabels)

    metrics.precisionByThreshold().foreach(e => logger.info(s"$label threshold: ${e._1} precision: ${e._2}"))

    metrics.recallByThreshold().foreach(e => logger.info(s"$label threshold: ${e._1} recall: ${e._2}"))

    metrics.fMeasureByThreshold().foreach(e => logger.info(s"$label threshold: ${e._1} FScore: ${e._2}, beta - 1 (aka f1 score)"))

    metrics.fMeasureByThreshold(0.5).foreach(e => logger.info(s"$label threshold: ${e._1} FScore: ${e._2}, beta - 0.5"))

    val auPRC = metrics.areaUnderPR
    logger.info(s"$label Area under precision-recall(AUPRC) curve = " + auPRC)

    val roc = metrics.roc
    logger.info(s"$label ROC curve = " + roc)

    val auROC = metrics.areaUnderROC
    logger.info(s"$label Area under ROC(AUROC) = " + auROC)

  } else {
    logger.error(s"$label label length: ${testLabels.length} , result length: ${predictResult.length}")
  }

  writeLines(outputModelPath) { bw =>
    lxgboostModel.booster.predict(new DMatrix(testSet.iterator)).foreach { l =>
      bw.append(l.mkString(",")).write("\n")
    }
  }
}
