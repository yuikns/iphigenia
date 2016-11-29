import java.io.{ BufferedWriter, File, FileWriter }

import com.argcv.valhalla.exception.ExceptionHelper._
import de.bwaldvogel.liblinear._
import org.slf4j.LoggerFactory
import spire.math._

import scala.collection.mutable.{ ArrayBuffer, Map => MMap }
import scala.io.Source
import spire.implicits.cfor

/**
 *
 * @param train  -t
 * @param test   -p
 * @param model  -m
 * @param result -r
 * @param c      -c
 * @param eps    -e
 */
case class ArgsOpt(
  train: Option[String] = None,
  test: Option[String] = None,
  model: Option[String] = None,
  result: Option[String] = None,
  c: Double = 1.0,
  eps: Double = 0.00001,
  trainFlag: Boolean = true) {

  override def toString = ""

  def withArgs(args: Array[String], offset: Int = 0): ArgsOpt = {
    //lazy val logger = LoggerFactory.getLogger(this.getClass)
    def noEnoughParameterErrorLog(s: String): Unit = {
      println(s"no enough parameter for $s")
    }
    def unExpectParameterErrorLog(s: String, e: String): Unit = {
      println(s"unexpect parameter after: $s $e")
    }
    def unRecognizedParameterErrorLog(s: String): Unit = {
      println(s"unrecognized parameter : $s")
    }
    if (args.length > offset) {
      args(offset) match {
        case "-t" => // train
          if (args.length > offset) {
            val nxArgs: (ArgsOpt, Int) = try {
              (this.copy(train = Some(args(offset + 1))), offset + 2)
            } catch {
              case t: Exception =>
                unExpectParameterErrorLog(args(offset), t.getLocalizedMessage)
                (this, offset + 1)
            }
            nxArgs._1.withArgs(args, nxArgs._2)
          } else {
            noEnoughParameterErrorLog(args(offset))
            this
          }
        case "-p" => // test file (predict file)
          if (args.length > offset) {
            val nxArgs: (ArgsOpt, Int) = try {
              (this.copy(test = Some(args(offset + 1))), offset + 2)
            } catch {
              case t: Exception =>
                unExpectParameterErrorLog(args(offset), t.getLocalizedMessage)
                (this, offset + 1)
            }
            nxArgs._1.withArgs(args, nxArgs._2)
          } else {
            noEnoughParameterErrorLog(args(offset))
            this
          }
        case "-m" => // model file
          if (args.length > offset) {
            val nxArgs: (ArgsOpt, Int) = try {
              (this.copy(model = Some(args(offset + 1))), offset + 2)
            } catch {
              case t: Exception =>
                unExpectParameterErrorLog(args(offset), t.getLocalizedMessage)
                (this, offset + 1)
            }
            nxArgs._1.withArgs(args, nxArgs._2)
          } else {
            noEnoughParameterErrorLog(args(offset))
            this
          }
        case "-c" => // paramerter : C
          if (args.length > offset) {
            val nxArgs: (ArgsOpt, Int) = try {
              (this.copy(c = args(offset + 1).toDouble), offset + 2)
            } catch {
              case t: Exception =>
                unExpectParameterErrorLog(args(offset), t.getLocalizedMessage)
                (this, offset + 1)
            }
            nxArgs._1.withArgs(args, nxArgs._2)
          } else {
            noEnoughParameterErrorLog(args(offset))
            this
          }
        case "-e" => // eps
          if (args.length > offset) {
            val nxArgs: (ArgsOpt, Int) = try {
              (this.copy(eps = args(offset + 1).toDouble), offset + 2)
            } catch {
              case t: Exception =>
                unExpectParameterErrorLog(args(offset), t.getLocalizedMessage)
                (this, offset + 1)
            }
            nxArgs._1.withArgs(args, nxArgs._2)
          } else {
            noEnoughParameterErrorLog(args(offset))
            this
          }
        case "-ntf" =>
          this.copy(trainFlag = false).withArgs(args, offset + 1)
        case "-tf" =>
          this.copy(trainFlag = true).withArgs(args, offset + 1)
        case s: String =>
          unRecognizedParameterErrorLog(s)
          this
      }
    } else {
      this
    }
  }
}

/**
 * @author yu
 */
object Launcher extends App {
  //
  lazy val logger = LoggerFactory.getLogger(Launcher.getClass)
  val param = ArgsOpt().withArgs(args)
  //
  val base = "data/example"
  val pathTrain = s"$base/train.csv"
  val pathTest = s"$base/test.csv"
  val pathResult = s"$base/predict.csv"
  val timeStart = System.currentTimeMillis()
  logger.info("starting...")
  val pw = new BufferedWriter(new FileWriter(new File(pathResult)))

  val upperBound = MMap[Int, Double]()

  def updateUpperBound(i: Int, d: Double) = upperBound.synchronized {
    val absd = abs(d)
    val rabsd = if (absd > 10000000) 10000000 else absd
    upperBound.get(i) match {
      case Some(v) =>
        if (v < rabsd) {
          upperBound.put(i, rabsd)
        }
      case None =>
        upperBound.put(i, rabsd)
    }
  }

  def trainModel() = {
    logger.info("start training...")

    val ybuff = ArrayBuffer[Double]()
    val px: Array[Array[Feature]] = Source.fromFile(new File(pathTrain)).getLines().toList.drop(1).map { l =>
      val d: Array[Double] = l.split(",").drop(1).map(_.toDouble)
      ybuff.append(if (d.last == 1) 1 else -1)
      (1 until d.length).map(i => new FeatureNode(i, dataProcess(i - 1, d(i - 1)))).toArray.asInstanceOf[Array[Feature]]
    }.toArray
    val py = ybuff.toArray

    println(s"positive: ${py.count(_ == 1)}, negative: ${py.count(_ == -1)}")

    val p = new Problem
    p.x = px
    p.y = py
    p.l = py.length
    p.n = px.head.length

    logger.info(s"loaded, examples: ${p.l}, features: ${p.n}")

    // -s 0
    val solver: SolverType = SolverType.L2R_LR
    // cost of constraints violation
    val C: Double = param.c
    // stopping criteria
    val eps: Double = param.eps
    val parameter = new Parameter(solver, C, eps)

    logger.info("training model ... ")
    val model = Linear.train(p, parameter)
    logger.info("model trained ... ")

    if (param.model.isDefined) {
      logger.info(s"saving model: ${param.model.get}")
      model.save(new File(param.model.get))
      logger.info(s"model saved")
    } else {
      logger.info(s"[skip] model saving")
    }
    model
  }

  Source.fromFile(new File(pathTrain)).getLines().toList.drop(1).foreach { l =>
    val d: Array[Double] = l.split(",").drop(1).map(_.toDouble)
    cfor(1)(_ < d.length, _ + 1) { i =>
      (i, updateUpperBound(i - 1, d(i - 1)))
    }
  }
  Source.fromFile(new File(pathTest)).getLines().toList.drop(1).foreach { l =>
    val d: Array[Double] = l.split(",").drop(1).map(_.toDouble)
    (1 until d.length).foreach(i => (i, updateUpperBound(i - 1, d(i - 1))))
  }

  logger.info(s"all pairs: ${upperBound.mkString(",")}")
  logger.info(s"size: ${upperBound.size} , list equals 0 : ${upperBound.filter(kv => kv._2 == 0).mkString(",")}")

  def dataProcess(i: Int, d: Double): Double = {
    val r: Double = upperBound.getOrElse(i, 0.0)
    val rd = if (d > 10000000) 10000000 else if (d < -10000000) -10000000 else d
    if (r == 0.0) 0.0
    else {
      rd / r
    }
  }

  def loadModel() = {
    val f = if (param.model.isDefined) Some(new File(param.model.get)) else None
    if (f.isDefined && f.get.exists()) {
      (() => Linear.loadModel(new File(param.model.get))).safeExec
    } else {
      logger.error("model file not found")
      None
    }
  }

  (if (param.trainFlag) Some(trainModel()) else loadModel()) match {
    case Some(model) =>
      logger.info("writing ... ")
      pw.append("ID,TARGET\n")
      Source.fromFile(new File(pathTest)).getLines().toList.drop(1).foreach { l =>
        val elems = l.split(",")
        val d: Array[Double] = elems.drop(1).map(_.toDouble)
        val id = elems.head
        val features = (1 to d.length).map(i => new FeatureNode(i, dataProcess(i - 1, d(i - 1)))).toArray.asInstanceOf[Array[Feature]]
        val result: Double = Linear.predict(model, features)
        pw.append(id).append(",").append(if (result >= 0) "1" else "0").append("\n")
      }
      pw.flush()
      pw.close()
    case None =>
      logger.info("model not found")
  }
  logger.info(s"finished , all time cost : ${System.currentTimeMillis() - timeStart}")
}
