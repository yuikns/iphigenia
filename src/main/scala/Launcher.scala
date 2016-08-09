import org.slf4j.LoggerFactory

import scala.collection.mutable.{Map => MMap}

/**
  *
  * @param tag tag
  */
case class ArgsOpt(
                    tag: Option[String] = None) {

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
              (this.copy(tag = Some(args(offset + 1))), offset + 2)
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
  val timeStart = System.currentTimeMillis()
  val param = ArgsOpt().withArgs(args)
  //

  logger.info(s"finished , all time cost : ${System.currentTimeMillis() - timeStart}")
}
