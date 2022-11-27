package ghx.mandelbrot

import javafx.beans.binding.Bindings
import javafx.beans.property.{DoubleProperty, StringProperty}
import javafx.util.StringConverter

object DoBindings {
  def bind(s: StringProperty, v: DoubleProperty): Unit = {
    Bindings.bindBidirectional(s, v, new StringConverter[Number]() {
        override def fromString(s: String): java.lang.Double = {
          try return s.toDouble
          catch {
            case t: Throwable =>
              return 0.0
          }
        }

        override def toString(n: Number): String = {
          if ((n.asInstanceOf[java.lang.Double]).isInfinite) {
            return "Infinity"
          }
          else {
            return n.toString
          }
        }
      }
    )
  }
}
