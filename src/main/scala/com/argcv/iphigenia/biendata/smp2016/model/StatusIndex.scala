package com.argcv.iphigenia.biendata.smp2016.model

import java.util.concurrent.atomic.AtomicInteger

import com.argcv.valhalla.utils.Awakable

import scala.collection.mutable.{ Map => MMap }
import scala.collection.parallel.mutable.{ ParTrieMap => PTMap }

class StatusIndex {
  val maxDocId: AtomicInteger = new AtomicInteger()
  val doc: PTMap[Int, Long] = PTMap[Int, Long]()
  var meta: PTMap[Int, Int] = PTMap[Int, Int]()

  // term => id => count
  var index: PTMap[String, PTMap[Int, Int]] = PTMap[String, PTMap[Int, Int]]()

  def insert(tokens: List[String], id: Long) = {
    val cid = maxDocId.getAndIncrement()
    val gtokens = tokens.groupBy(identity).map(e => (e._1, e._2.length))
    doc.put(cid, id)
    gtokens.foreach { e =>
      index.synchronized {
        index.get(e._1) match {
          case Some(m) =>
            m
          case None =>
            val m = PTMap[Int, Int]()
            index.put(e._1, m)
            m
        }
      }.put(cid, e._2)
    }
    meta.put(cid, tokens.length)
    cid
  }

  def get(docId: Int): Option[Long] = doc.get(docId)

  def query(tokens: List[String]): List[(Int, Double)] = {
    val gtokens = tokens.groupBy(identity).map(e => (e._1, e._2.length)).toList

    val ads = size()
    gtokens.flatMap { t =>
      val cands: List[(Int, Int)] = index.get(t._1) match {
        case Some(m) =>
          m.toList
        case None =>
          List[(Int, Int)]()
      }
      val dscct = cands.length
      cands.map {
        case (docId, stid) =>
          val atsid = meta.getOrElse(docId, 0)
          (docId, tfidf(stid, atsid, ads, dscct))
      }
    }.groupBy(_._1).toList.
      map(v => (v._1, v._2.foldLeft(0.0)((l, c) => l + c._2))).
      sortWith(_._2 > _._2)
  }

  def size(): Int = maxDocId.get()

  // tf-idf k in document D
  // stid : size of term k in  D, 当前 term 数量
  // atsid :  all term size in D,  term 数量
  // ads : all document size, doc 总数
  // dscct : document size contains current term, 当前 doc 有多少这个 term
  def tfidf(stid: Int,
    atsid: Int,
    ads: Int,
    dscct: Int): Double = {
    if (ads == 0 || atsid == 0 || dscct == 0) return 0
    (stid.toDouble / atsid) * Math.log(ads.toDouble / dscct) / 2.302585
  }
}