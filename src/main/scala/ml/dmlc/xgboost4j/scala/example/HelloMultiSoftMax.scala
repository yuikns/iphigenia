package ml.dmlc.xgboost4j.scala.example

import java.io.File
import java.util.concurrent.atomic.AtomicInteger

import com.argcv.valhalla.utils.Awakable
import ml.dmlc.xgboost4j.scala.{ Booster, DMatrix, XGBoost }

/**
 *
 * @author Yu Jing <yu@argcv.com> on 12/23/16
 */
object HelloMultiSoftMax extends Awakable {
  def main(args: Array[String]): Unit = {
    val trainMax = new DMatrix("data/smp2016/xg/svm_age.train")
    val testMax = new DMatrix("data/smp2016/xg/svm_age.test")

    //    val params = Map[String,Any](
    //      "eta" -> 1.0,
    //      "max_depth" -> 2,
    //      "silent" -> 1,
    //      "objective" -> "binary:logistic"
    //    )

    // ref: https://github.com/dmlc/xgboost/blob/master/doc/parameter.md
    val params = Map[String, Any](
      "objective" -> "multi:softmax",
      //      "objective" -> "multi:softprob",
      "eta" -> 0.3,
      "max_depth" -> 4,
      "silent" -> 1,
      "nthread" -> 1,
      "num_class" -> 3
    )

    val watches = Map[String, DMatrix](
      "train" -> trainMax,
      "test" -> testMax
    )

    val round = 32
    // train a model
    val booster: Booster = XGBoost.train(trainMax, params, round, watches)
    // predict
    // save model to model path
    saveModel(booster)

    val predicts = booster.predict(testMax)
    val corr = new AtomicInteger()
    val total = new AtomicInteger()
    predicts.zipWithIndex.foreach { e =>
      if (testMax.getLabel(e._2) == e._1.head) corr.incrementAndGet()
      total.incrementAndGet()
      println(s"${e._2} => ${e._1.mkString(",")}# ${testMax.getLabel(e._2) == e._1.head}")
    }
    println(s"acc: ${corr.get().toDouble / total.get()}")
  }

  def saveModel(booster: Booster): Unit = {
    val file = new File("data/smp2016/xg/")
    if (!file.exists()) {
      file.mkdirs()
    }
    booster.saveModel(file.getAbsolutePath + "/xgb.model")
  }
}
