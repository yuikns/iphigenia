package com.argcv.iphigenia.example.linear

import java.io.File

import com.argcv.valhalla.utils.Awakable
import de.bwaldvogel.liblinear._

/**
 *
 * @author Yu Jing <yu@argcv.com> on 10/7/16
 */
object HelloLinear extends Awakable {
  def run(): Unit = {
    val timeStart = System.currentTimeMillis()
    logger.info("all starting ...")
    val p = new Problem
    p.x = Array[Array[Feature]](
      Array[Feature](new FeatureNode(1, 0), new FeatureNode(2, 0)),
      Array[Feature](new FeatureNode(1, 1), new FeatureNode(2, 1)),
      Array[Feature](new FeatureNode(1, -1), new FeatureNode(2, 1))
    )

    p.y = Array[Double](-1, 1, 1)

    // number of training examples
    p.l = p.x.length
    // number of features
    p.n = 3

    // -s 0
    val solver: SolverType = SolverType.L2R_LR
    // cost of constraints violation
    val C: Double = 1.0
    // stopping criteria
    val eps: Double = 0.01
    val parameter = new Parameter(solver, C, eps)

    val modelFile = new File("target/model")

    val model = Linear.train(p, parameter)

    model.save(modelFile)
    // load model or use it directly
    val model2 = Model.load(modelFile)

    println(s"(0,0) : ${Linear.predict(model2, Array[Feature](new FeatureNode(1, 0), new FeatureNode(2, 0)))}")
    println(s"(1,1) : ${Linear.predict(model2, Array[Feature](new FeatureNode(1, 1), new FeatureNode(2, 1)))}")
    println(s"(-1,1) : ${Linear.predict(model2, Array[Feature](new FeatureNode(1, -1), new FeatureNode(2, 1)))}")
    println(s"(0.5,0.4) : ${Linear.predict(model2, Array[Feature](new FeatureNode(1, 0.5), new FeatureNode(2, 0.4)))}")
    println(s"(0.5,0.6) : ${Linear.predict(model2, Array[Feature](new FeatureNode(1, 0.5), new FeatureNode(2, 0.6)))}")

    logger.info(s"all done , all time cost: ${System.currentTimeMillis() - timeStart} ms")
  }

}
