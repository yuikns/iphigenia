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
import ml.dmlc.xgboost4j.scala.Booster
import ml.dmlc.xgboost4j.scala.example.conf.XGBoostConf
import ml.dmlc.xgboost4j.scala.spark.XGBoost
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.sql.types._
import org.apache.spark.sql.{ Row, SparkSession }

object SparkWithDataFrame extends App with SingleMachineFileSystemHelper with Awakable {
  val label = s"[${"SparkWithRDD".withColor(BLUE)}]"
  //    if (args.length != 5) {
  //      println(
  //        "usage: program num_of_rounds num_workers training_path test_path model_path")
  //      sys.exit(1)
  //    }
  // create SparkSession
  //  val sparkConf: SparkConf = new SparkConf().setAppName("XGBoost-spark-example")
  //    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
  //    sparkConf.registerKryoClasses(Array(classOf[Booster]))
  val sqlContext: SparkSession = SparkHelper.createSparkSession(appName = "XGBoost-spark-example", cfg = { in =>
    in.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .registerKryoClasses(Array(classOf[Booster]))
  })
  //val sqlContext: SQLContext = new SQLContext(new SparkContext(sparkConf))
  // create training and testing dataframes
  val inputTrainPath = XGBoostConf.trainFilePath
  val inputTestPath = XGBoostConf.testFilePath
  val outputModelPath = XGBoostConf.outputFilePath
  //  // number of iterations
  val numRound = XGBoostConf.numRound
  val nWorkers = XGBoostConf.nWorkers
  val trainRDDOfRows = MLUtils.loadLibSVMFile(sqlContext.sparkContext, inputTrainPath).
    map { labeledPoint => Row(labeledPoint.features, labeledPoint.label) }
  val trainDF = sqlContext.createDataFrame(trainRDDOfRows, StructType(
    Array(StructField("features", ArrayType(FloatType)), StructField("label", IntegerType))))
  val testRDDOfRows = MLUtils.loadLibSVMFile(sqlContext.sparkContext, inputTestPath).
    zipWithIndex().map {
      case (labeledPoint, id) =>
        Row(id, labeledPoint.features, labeledPoint.label)
    }
  val testDF = sqlContext.createDataFrame(testRDDOfRows, StructType(
    Array(StructField("id", LongType),
      StructField("features", ArrayType(FloatType)), StructField("label", IntegerType))))
  // training parameters
  val paramMap = List(
    "eta" -> 0.1f,
    "max_depth" -> 2,
    "objective" -> "binary:logistic").toMap
  val xgboostModel = XGBoost.trainWithDataFrame(
    trainDF, paramMap, numRound, nWorkers = nWorkers, useExternalMemory = true)
  // xgboost-spark appends the column containing prediction results
  //  xgboostModel.transform(testDF).show()
}
