package com.argcv.iphigenia.example.tf

import org.tensorflow._

//remove if not needed

/**
  *
  * @author Yu Jing <yu@argcv.com> on 2/17/17
  */
object MyTensorFlow {

  def main(args: Array[String]): Unit = {
    val graph = new Graph()
    val a: Output = graph.opBuilder("Const", "a").
      setAttr("dtype", DataType.INT32).
      setAttr("value", Tensor.create(Array(1, 2, 3))).
      build().
      output(0)

    val b: Output = graph.opBuilder("Const", "b").
      setAttr("dtype", DataType.INT32).
      setAttr("value", Tensor.create(Array(4, 5, 6))).
      build().
      output(0)

    val c: Output = graph.opBuilder("Mul", "c").
      addInput(a).
      addInput(b).
      build().
      output(0)

    val session = new Session(graph)
    val out = new Array[Int](3)
    session.runner().fetch("c").run().get(0).copyTo(out)

    println(out.mkString(", "))
  }

}
