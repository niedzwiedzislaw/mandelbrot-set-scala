package ghx.mandelbrot

import ghx.mandelbrot.MandelbrotDepr.T
import javafx.css.Size

object Mandelbrot {

  type T = Double

  def countIterationsToValue(iterations: Int, compValue: T)(p: (T, T)): Int = {
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
