package io.picnicml.doddlemodel.preprocessing

import breeze.linalg.DenseMatrix
import io.picnicml.doddlemodel.TestingUtils
import io.picnicml.doddlemodel.data.Feature.{CategoricalFeature, FeatureIndex, NumericalFeature}
import io.picnicml.doddlemodel.syntax.TransformerSyntax._
import org.scalatest.{FlatSpec, Matchers}

class OneHotEncoderTest extends FlatSpec with Matchers with TestingUtils {

  "One hot encoder" should "encode all categorical features" in {
    val x = DenseMatrix(
      List(1.0, 1.0, 1.0),
      List(3.0, 0.0, 1.0),
      List(6.0, 2.0, 0.0)
    )
    val xEncodedExpected = DenseMatrix(
      List(1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0),
      List(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0),
      List(2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0)
    )

    val featureIndex = FeatureIndex(List(CategoricalFeature, NumericalFeature, CategoricalFeature))
    val oneHotEncoder = OneHotEncoder(featureIndex)

    val fittedOneHotEncoder = oneHotEncoder.fit(x)
    val xTransformed = fittedOneHotEncoder.transform(x)
    breezeEqual(xTransformed, xEncodedExpected) shouldBe true
  }

  it should "encode a subset of categorical features" in {
    val x = DenseMatrix(
      List(1.0, 1.0, 1.0),
      List(3.0, 0.0, 1.0),
      List(6.0, 2.0, 0.0)
    )
    val xEncodedExpected = DenseMatrix(
      List(1.0, 1.0, 0.0, 1.0),
      List(3.0, 0.0, 0.0, 1.0),
      List(6.0, 2.0, 1.0, 0.0)
    )

    val featureIndex = FeatureIndex(List(CategoricalFeature, NumericalFeature, CategoricalFeature))
    val oneHotEncoder = OneHotEncoder(featureIndex.subset(IndexedSeq(2)))

    val fittedOneHotEncoder = oneHotEncoder.fit(x)
    val xTransformed = fittedOneHotEncoder.transform(x)
    breezeEqual(xTransformed, xEncodedExpected) shouldBe true
  }
}
