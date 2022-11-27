package ghx.mandelbrot

import ghx.mandelbrot.Mandelbrot.T
import javafx.css.Size

object Mandelbrot2 {

  type T = Double

  def calculateValuesForPlane(
      canvasSize: (Int, Int),
      canvasTranslation: (Double, Double),
      pixelRatio: Double,
      iterations: Int,
      guardianValue: Double
  ): Seq[((Int, Int), Int)] = {
    import scala.collection.parallel.CollectionConverters._
    val halfWidth = canvasSize._1 / 2
    val halfHeight = canvasSize._2 / 2
    val points = for {
      x <- 0 to canvasSize._1
      y <- 0 to canvasSize._2
    } yield (x, y)
    val parResult = for {
      p <- points.par
      realPoint = canvasToPosition(p, (halfWidth, halfHeight), canvasTranslation, pixelRatio)
    } yield {
      (p, calculateIterations(iterations, guardianValue)(realPoint))
    }
    parResult.toArray.toSeq
  }

  private def canvasToPosition(
      canvasPos: (Int, Int),
      canvasOffset: (Double, Double),
      translate: (Double, Double),
      scale: Double
  ): (Double, Double) = (
        scale * (canvasPos._1 - canvasOffset._1) + translate._1,
        scale * (canvasPos._2 - canvasOffset._2) + translate._2
    )

  private def calculateIterations(iterations: Int, compValue: T)(p: (T, T)): Int = {
    var z = p.asComplex
    var i = 0
    val c = p.asComplex
    while (i < iterations && z.module < compValue) {
      z = z * z + c
      i = i + 1
    }
    i
  }

  implicit class TupleToComplex(p: (T, T)) {
    def asComplex = new Complex(p._1, p._2)
  }
}
