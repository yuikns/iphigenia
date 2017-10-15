import org.slf4j.LoggerFactory

import scala.collection.mutable.{Map => MMap}
import com.argcv.iphigenia.utils.SparkHelper._
import com.argcv.valhalla.string.StringHelper._
import org.apache.spark.rdd.RDD

/**
  * @author yu
  */
object Launcher extends App {
  //
  lazy val logger = LoggerFactory.getLogger(Launcher.getClass)
  val timeStart = System.currentTimeMillis()
  val sc = createSparkContext
  val in: RDD[((Int, Int, Int), Int)] = sc.textFile("./data/related-resources/topics/pid_to_topics.txt").flatMap { l =>
    val elems = l.split("\t")
    if (elems.length > 3) {
      val year = elems(1).safeToIntOrElse(0)
      val tids = elems.drop(2).map(_.safeToIntOrElse(0)).filter(_ > 0).toList
      tids.distinct.combinations(2).map{ p =>
        val pl: Seq[Int] = p.sortWith(_ < _)
        ((year, pl.head, pl.last), 1)
      }
    } else {
      Iterator[((Int, Int, Int), Int)]()
    }
  }.reduceByKey(_ + _)
  in.map{ e =>
    f"${e._1._1}%d\t${e._1._2}%08X\t${e._1._3}%08X\t${e._2}"
  }.saveAsTextFile("./data/related-resources/topics/topics_year_to_co_occ.txt")
  logger.info(s"finished , all time cost : ${System.currentTimeMillis() - timeStart}")
}
