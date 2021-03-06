package io.picnicml.doddlemodel.linear

import breeze.linalg._
import breeze.numerics.{exp, log, pow}
import cats.syntax.option._
import io.picnicml.doddlemodel.data.{Features, RealVector, Simplex, Target}
import io.picnicml.doddlemodel.linear.typeclasses.LinearClassifier
import io.picnicml.doddlemodel.syntax.OptionSyntax._

/** An immutable multiple multinomial regression model with ridge regularization.
  *
  * @param lambda L2 regularization strength, must be positive, 0 means no regularization
  *
  * Examples:
  * val model = SoftmaxClassifier()
  * val model = SoftmaxClassifier(lambda = 1.5)
  */
case class SoftmaxClassifier private(lambda: Double, numClasses: Option[Int], private val w: Option[RealVector]) {
  private var yPredProbaCache: Simplex = _
}

object SoftmaxClassifier {

  def apply(): SoftmaxClassifier = SoftmaxClassifier(0, none, none)

  def apply(lambda: Double): SoftmaxClassifier = {
    require(lambda > 0, "L2 regularization strength must be positive")
    SoftmaxClassifier(lambda, none, none)
  }

  private val wSlice: Range.Inclusive = 1 to -1

  implicit lazy val ev: LinearClassifier[SoftmaxClassifier] = new LinearClassifier[SoftmaxClassifier] {

    override def numClasses(model: SoftmaxClassifier): Option[Int] = model.numClasses

    override protected def w(model: SoftmaxClassifier): Option[RealVector] = model.w

    override protected[doddlemodel] def copy(model: SoftmaxClassifier, numClasses: Int): SoftmaxClassifier =
      model.copy(numClasses = numClasses.some)

    override protected def copy(model: SoftmaxClassifier, w: RealVector): SoftmaxClassifier =
      model.copy(w = w.some)

    override protected def predictStateless(model: SoftmaxClassifier, w: RealVector, x: Features): Target =
      convert(argmax(predictProbaStateless(model, w, x)(*, ::)), Double)

    override protected def predictProbaStateless(model: SoftmaxClassifier, w: RealVector, x: Features): Simplex = {
      val z = x * w.asDenseMatrix.reshape(x.cols, model.numClasses.getOrBreak - 1, View.Require)
      val maxZ = max(z)
      val zExpPivot = DenseMatrix.horzcat(exp(z - maxZ), DenseMatrix.fill[Double](x.rows, 1)(exp(-maxZ)))
      zExpPivot(::, *) /:/ sum(zExpPivot(*, ::))
    }

    override protected[linear] def lossStateless(model: SoftmaxClassifier,
                                                 w: RealVector, x: Features, y: Target): Double = {
      model.yPredProbaCache = predictProbaStateless(model, w, x)
      val yPredProbaOfTrueClass = 0 until x.rows map { rowIndex =>
        val targetClass = y(rowIndex).toInt
        model.yPredProbaCache(rowIndex, targetClass)
      }

      val wMatrix = w.asDenseMatrix.reshape(x.cols, model.numClasses.getOrBreak - 1, View.Require)
      sum(log(DenseMatrix(yPredProbaOfTrueClass))) / (-x.rows.toDouble) +
        .5 * model.lambda * sum(pow(wMatrix(wSlice, ::), 2))
    }

    override protected[linear] def lossGradStateless(model: SoftmaxClassifier,
                                                     w: RealVector, x: Features, y: Target): RealVector = {
      val yPredProba = model.yPredProbaCache(::, 0 to -2)

      val indicator = DenseMatrix.zeros[Double](yPredProba.rows, yPredProba.cols)
      0 until indicator.rows foreach { rowIndex =>
        val targetClass = y(rowIndex).toInt
        if (targetClass < model.numClasses.getOrBreak - 1) indicator(rowIndex, targetClass) = 1.0
      }

      val grad = (x.t * (indicator - yPredProba)) / (-x.rows.toDouble)
      val wMatrix = w.asDenseMatrix.reshape(x.cols, model.numClasses.getOrBreak - 1, View.Require)
      grad(wSlice, ::) += model.lambda * wMatrix(wSlice, ::)
      grad.toDenseVector
    }
  }
}
