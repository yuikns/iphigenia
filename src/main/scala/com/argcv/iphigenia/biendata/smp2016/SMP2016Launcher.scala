package com.argcv.iphigenia.biendata.smp2016

import java.io.{ File, PrintWriter }
import java.util.concurrent.atomic.AtomicInteger

import com.argcv.iphigenia.biendata.smp2016.model.SocialData._
import com.argcv.iphigenia.biendata.smp2016.model._
import com.argcv.valhalla.utils.Awakable
import com.argcv.valhalla.console.ColorForConsole._
import com.argcv.valhalla.fs.SingleMachineFileSystemHelper

import scala.collection.parallel.immutable.{ ParMap, ParSet }
import scala.io.Source
import de.bwaldvogel.liblinear._
import ml.dmlc.xgboost4j.scala.DMatrix

import scala.collection.mutable.{ ArrayBuffer, ListBuffer }

object MLModels extends Awakable with SingleMachineFileSystemHelper {

  import com.argcv.iphigenia.biendata.smp2016.model.SocialData._

  lazy val models: List[Model] = loadModels()
  val c: Double = 10000.0
  val eps: Double = 0.000001

  def trainLL(): Unit = {
    //liblinear
    val features = ArrayBuffer[Array[Feature]]()
    val ybuff: List[ArrayBuffer[Double]] = (0 until 13).toList.map(_ => new ArrayBuffer[Double]())
    getLines("data/smp2016/train/train_labels.txt").toList.foreach { s =>
      val arrs = s.split("\\|\\|")
      if (arrs.length >= 4) {
        val id = arrs(0).toLong
        val ft: Array[Feature] = scoresToFeatures(trainScores(id))
        features.append(ft)
        val values = trainLabels(id).toScores
        values.indices.foreach { i =>
          ybuff(i).append(values(i))
        }
      }
    }
    val px = features.toArray
    val pn = features.head.length
    val pl = features.length
    (0 until 13).map { i =>
      val p = new Problem
      p.x = px
      p.y = ybuff(i).toArray
      p.l = pl
      p.n = pn
      (p, i)
    }.par.foreach {
      case (p, i) =>
        // -s 0
        val solver: SolverType = SolverType.L2R_LR
        // cost of constraints violation
        val C: Double = this.c
        // stopping criteria
        val eps: Double = this.eps
        val parameter = new Parameter(solver, C, eps)

        logger.info(s"training model ... $i")
        val model = Linear.train(p, parameter)
        logger.info(s"model trained ... $i")
        model.save(new File(s"data/smp2016/ll/model_$i"))
    }
  }

  def loadModels(): List[Model] = {
    (0 until 13).map { i =>
      Linear.loadModel(new File(s"/Users/yu/Workspace/comp/iphigenia/data/smp2016/ll/model_$i"))
    }.toList
  }

  def eval(id: Long): Info = {
    val scores = trainScores(id)
    val ft: Array[Feature] = scoresToFeatures(scores)
    Info.result(
      id,
      models.map(Linear.predict(_, ft))
    )
  }

  def trainScores(id: Long): List[Double] = {
    val c: Set[Long] = trainCands(id)
    socialInfo(id, c,
      testInfo = trainFollowerInfo,
      trainInfo = trainFollowerInfo)
  }

  def trainCands(id: Long): Set[Long] = {
    cands(
      id,
      testInfo = trainFollowerInfo,
      trainInfo = trainFollowingInfo)
  }

  def scoresToFeatures(s: List[Double]): Array[Feature] =
    s.zipWithIndex.map { e =>
      new FeatureNode(e._2 + 1, e._1).asInstanceOf[Feature]
    }.toArray

  def predict(id: Long) = {
    val scores = testScores(id)
    val ft: Array[Feature] = scoresToFeatures(scores)
    Info.result(
      id,
      models.map(Linear.predict(_, ft))
    )
  }

  def testScores(id: Long): List[Double] = {
    val c: Set[Long] = testCands(id)
    socialInfo(id, c,
      testInfo = testFollowerInfo,
      trainInfo = trainFollowerInfo)
  }

  def testCands(id: Long): Set[Long] = {
    cands(
      id,
      testInfo = testFollowerInfo,
      trainInfo = trainFollowingInfo)
  }

  //  def saveSVMFile(): Unit = {
  //    //liblinear
  //    val features = ArrayBuffer[Array[Feature]]()
  //    //val ybuff: List[ArrayBuffer[Double]] = (0 until 13).toList.map(_ => new ArrayBuffer[Double]())
  //    val valueBuff = ListBuffer[(Int,Int,Int)]()
  //    getLines("data/smp2016/train/train_labels.txt").toList.foreach { s =>
  //      val arrs = s.split("\\|\\|")
  //      if (arrs.length >= 4) {
  //        val id = arrs(0).toLong
  //        val ft: Array[Feature] = scoresToFeatures(trainScores(id))
  //        features.append(ft)
  //        val values = trainLabels(id)
  //        valueBuff.append(
  //          (values.a.get.v - 1,
  //          values.g.get.v - 1,
  //          values.l.get.v - 1)
  //        )
  //      }
  //    }
  //    val px = features.toArray
  //    val pn = features.head.length
  //    val pl = features.length
  //    writeLines(s"data/smp2016/xg/svm_age.train") { pw =>
  //      px.zipWithIndex.foreach{
  //        case (x, idx) =>
  //          pw.append(
  //            valueBuff(idx).toString
  //          ).append(" ")
  //          val cpx: Array[Feature] = px(idx)
  //          cpx.foreach{ cpf =>
  //            pw.append(s"${cpf.getIndex}:${cpf.getValue} ")
  //          }
  //          pw.write("\n")
  //      }
  //    }
  //  }
}

object XGBoostTrain extends Awakable with SingleMachineFileSystemHelper {
  var CONF_N_THREAD = 64

  import java.io.File
  import java.util.concurrent.atomic.AtomicInteger

  import com.argcv.valhalla.utils.Awakable
  import ml.dmlc.xgboost4j.LabeledPoint
  import ml.dmlc.xgboost4j.scala.{ Booster, DMatrix, XGBoost }

  lazy val (ab, gb, lb) = {
    logger.info("train starting ...")
    //liblinear
    val alps = ListBuffer[LabeledPoint]()
    val glps = ListBuffer[LabeledPoint]()
    val llps = ListBuffer[LabeledPoint]()
    getLines("data/smp2016/train/train_labels.txt").toList.foreach { s =>
      val arrs = s.split("\\|\\|")
      if (arrs.length >= 4) {
        val id = arrs(0).toLong
        val scoreSocial = MLModels.trainScores(id).map(_.toFloat).toArray
        val scoreUag = Status.trainUagFeats(id)
        val scoreAddr = Status.trainAddrFeats(id)
        val scores = scoreSocial ++ scoreUag ++ scoreAddr
        val values = trainLabels(id)
        val alp: LabeledPoint = LabeledPoint.fromDenseVector((values.a.get.v - 1).toFloat, scores)
        val glp: LabeledPoint = LabeledPoint.fromDenseVector((values.g.get.v - 1).toFloat, scores)
        val llp: LabeledPoint = LabeledPoint.fromDenseVector((values.l.get.v - 1).toFloat, scores)
        alps.append(alp)
        glps.append(glp)
        llps.append(llp)
      }
    }
    val aTrainMax: DMatrix = new DMatrix(alps.toIterator)
    val gTrainMax: DMatrix = new DMatrix(glps.toIterator)
    val lTrainMax: DMatrix = new DMatrix(llps.toIterator)
    val resp = (
      doTrain(aTrainMax, 3, "data/smp2016/xg/model_a", "age", nthread = CONF_N_THREAD),
      doTrain(gTrainMax, 2, "data/smp2016/xg/model_g", "gender", nthread = CONF_N_THREAD),
      doTrain(lTrainMax, 8, "data/smp2016/xg/model_l", "loc", maxDepth = 10, nthread = CONF_N_THREAD))
    logger.info("train finished")
    resp
  }

  def doTrain(mtx: DMatrix, nClass: Int, path: String, label: String = "#", round: Int = 256, eta: Double = 0.3, maxDepth: Int = 10, nthread: Int = 8): Booster = {
    val params = Map[String, Any](
      "objective" -> "multi:softmax",
      //      "objective" -> "multi:softprob",
      "eta" -> eta,
      "max_depth" -> maxDepth,
      "silent" -> 1,
      "nthread" -> nthread,
      "num_class" -> nClass
    )
    val watches = Map[String, DMatrix](
      s"train-$label" -> mtx //trainMax,
    //      "test" -> testMax
    )
    val booster: Booster = XGBoost.train(mtx, params, round, watches)
    booster.saveModel(path)
    booster
  }

  //  def predict(ids: Array[Array[Float]]): (Array[Int], Array[Int], Array[Int]) = {
  //    val alps = ListBuffer[LabeledPoint]()
  //    val glps = ListBuffer[LabeledPoint]()
  //    val llps = ListBuffer[LabeledPoint]()
  //    ids.foreach { scores =>
  //      //val scores = MLModels.trainScores(id).map(_.toFloat).toArray
  //      val value: Float = 0.0.toFloat
  //      val alp: LabeledPoint = LabeledPoint.fromDenseVector(value, scores)
  //      val glp: LabeledPoint = LabeledPoint.fromDenseVector(value, scores)
  //      val llp: LabeledPoint = LabeledPoint.fromDenseVector(value, scores)
  //      alps.append(alp)
  //      glps.append(glp)
  //      llps.append(llp)
  //    }
  //    val ar: Array[Int] = predict(new DMatrix(alps.toIterator), ab)
  //    val gr: Array[Int] = predict(new DMatrix(glps.toIterator), gb)
  //    val lr: Array[Int] = predict(new DMatrix(llps.toIterator), lb)
  //    (ar, gr, lr)
  //  }

  def predict(id: Long): (Int, Int, Int) = {
    val alps = ListBuffer[LabeledPoint]()
    val glps = ListBuffer[LabeledPoint]()
    val llps = ListBuffer[LabeledPoint]()
    //val scores = MLModels.testScores(id).map(_.toFloat).toArray
    val scoreSocial = MLModels.testScores(id).map(_.toFloat).toArray
    val scoreUag = Status.testUagFeats(id)
    val scoreAddr = Status.testAddrFeats(id)
    val scores = scoreSocial ++ scoreUag ++ scoreAddr
    val value: Float = 0.0.toFloat
    val alp: LabeledPoint = LabeledPoint.fromDenseVector(value, scores)
    val glp: LabeledPoint = LabeledPoint.fromDenseVector(value, scores)
    val llp: LabeledPoint = LabeledPoint.fromDenseVector(value, scores)
    alps.append(alp)
    glps.append(glp)
    llps.append(llp)
    val ar = predict(new DMatrix(alps.toIterator), ab).head
    val gr = predict(new DMatrix(glps.toIterator), gb).head
    val lr = predict(new DMatrix(llps.toIterator), lb).head
    (ar, gr, lr)
  }

  def predict(mtx: DMatrix, b: Booster): Array[Int] = {
    b.predict(mtx).map(_.head.toInt)
  }

}

/**
 *
 * @author Yu Jing <yu@argcv.com> on 12/21/16
 */
object SMP2016Launcher extends Awakable with SingleMachineFileSystemHelper {
  import com.argcv.iphigenia.biendata.smp2016.model.SocialData._

  val timeIn = System.currentTimeMillis()
  val tasks: List[Long] = getLines("data/smp2016/test/test_nolabels.txt").toList.map(_.toLong)

  def saveAll(args: Array[String] = Array[String]()): Unit = {
    logger.info(s"start... args ${args.mkString(",")}")
    def getTimeCostStr = (System.currentTimeMillis() - timeIn).toString.withColor(CYAN)

    logger.info(s"all predicted .. size ${predMap.size}")

    writeLines("/Users/yu/Workspace/comp/iphigenia/data/smp2016/ll/temp.csv") { pw =>
      pw.append("uid,age,gender,province\n")
      tasks.foreach { id =>
        pw.append(predMap.getOrElse(id, Info(id)).toString).write("\n")
      }
    }
    logger.info(s"end ... time cost $getTimeCostStr ms")
  }

  def main(args: Array[String]): Unit = {
    saveAll(args)
  }

  logger.info(s"size of id 2 name: ${id2Name.size}")
  val cnt = new AtomicInteger()
  logger.info("loading ...")
  val predMap: Map[Long, Info] = tasks.map { id =>
    //var rel = MLModels.predict(id) //Info(id) //SocialData.predict(id, c)
    val pred: (Int, Int, Int) = XGBoostTrain.predict(id)
    var rel = Info(
      id,
      Some(Age(pred._1 + 1)),
      Some(Gender(pred._2 + 1)),
      Some(Loc(pred._3 + 1))
    )
    id2Name.get(id) match {
      case Some(name) =>
        val m = List[String]("斌", "牛仔", "峰", "强")
        val f = List[String]("喵", "珊", "雪", "筱", "兰", "柠", "娜", "婷", "咩", "妹", "梦")
        if (m.exists(name.contains(_))) {
          rel = rel.copy(
            g = Some(Gender(1))
          )
        } else if (f.exists(name.contains(_))) {
          rel = rel.copy(
            g = Some(Gender(2))
          )
        }
      case None =>
    }
    val ccnt = cnt.incrementAndGet()
    if (ccnt % 100 == 0) {
      println(s"$ccnt ...")
    }
    id -> rel
  }.toMap

}
