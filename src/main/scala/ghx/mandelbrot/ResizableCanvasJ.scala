package ghx.mandelbrot

import javafx.beans.Observable
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color


class ResizableCanvasJ extends Canvas {
  // Redraw canvas when size changes.
//    widthProperty.addListener(new ChangeListener[Observable] {
//      override def changed(observableValue: ObservableValue[_ <: Observable], t: Observable, t1: Observable): Unit =
//        draw()
//    })
  widthProperty.addListener((evt: Observable) => draw())
  heightProperty.addListener((evt: Observable) => draw())

  private def draw(): Unit = {
    val width = getWidth
    val height = getHeight
    val gc = getGraphicsContext2D
    gc.clearRect(0, 0, width, height)
    gc.setStroke(Color.RED)
    gc.strokeLine(0, 0, width, height)
    gc.strokeLine(0, height, width, 0)
  }

  override def isResizable = true

  override def prefWidth(height: Double): Double = getWidth

  override def prefHeight(width: Double): Double = getHeight
}
