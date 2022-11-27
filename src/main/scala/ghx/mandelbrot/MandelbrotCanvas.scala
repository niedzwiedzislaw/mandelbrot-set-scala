package ghx.mandelbrot

import ghx.mandelbrot.MandelbrotDepr.{ec, p, threads}
import ghx.mandelbrot.Mandelbrot.countIterationsToValue
import javafx.animation.AnimationTimer
import javafx.beans.Observable
import javafx.beans.property.SimpleLongProperty
import javafx.scene.input.ScrollEvent
import javafx.scene.paint.Color
import kamon.Kamon

import java.util.concurrent.{CountDownLatch, Executors}
import scala.concurrent.{ExecutionContext, Future}

class MandelbrotCanvas extends ResizableCanvas {

  import scala.collection.parallel.CollectionConverters._

  var points: Seq[(Int, Int)] = Seq.empty

  def remapPoints() = {
    p = Array.ofDim[((Int, Int), Int)](getWidth.toInt*getHeight.toInt)
    points =  for {
      x <- 0 to getWidth.toInt
      y <- 0 to getHeight.toInt
    } yield (x, y)
  }

  var draw: () => Unit = () => ()

  {
    widthProperty().addListener((evt: Observable) => remapPoints())
    heightProperty.addListener((evt: Observable) => remapPoints())
    remapPoints()

    setOnScroll(new javafx.event.EventHandler[ScrollEvent] {
      override def handle(t: ScrollEvent): Unit = {
        val prevPixelRatio = MandelbrotSettings.scale.get() / getWidth
        val prevEventXOnAxes = (t.getX - getWidth.toInt / 2) * prevPixelRatio
        val prevEventYOnAxes = (t.getY - getHeight.toInt / 2) * prevPixelRatio
        if (t.getDeltaY > 0) {
          MandelbrotSettings.scale.setValue(MandelbrotSettings.scale.get() * 0.7)
        } else {
          MandelbrotSettings.scale.setValue(MandelbrotSettings.scale.get() * (1.0 / 0.7))
        }
        val pixelRatio = MandelbrotSettings.scale.get() / getWidth
        val eventXOnAxes = (t.getX - getWidth.toInt / 2) * pixelRatio
        val eventYOnAxes = (t.getY - getHeight.toInt / 2) * pixelRatio

        // TODO [2022/11/27]: More algebra for faster results?
        MandelbrotSettings.x.setValue(MandelbrotSettings.x.get() + (prevEventXOnAxes - eventXOnAxes))
        MandelbrotSettings.y.setValue(MandelbrotSettings.y.get() + (prevEventYOnAxes - eventYOnAxes))
      }
    }
    )
  }

  def setDrawFn(id: AnyRef) = {
    clear()
    draw = toggleToDrawFn(id)
  }

  def startLoop() = {
    val lastUpdateTime = new SimpleLongProperty(0)
    val timer = new AnimationTimer() {
      override def handle(timestamp: Long): Unit = {
        if (lastUpdateTime.get > 0) {
          draw()
        }
        lastUpdateTime.set(timestamp)
      }
    }
    timer.start()
  }

  private def toggleToDrawFn(x: Any): () => Unit = x match {
    case "parArray" => drawWithParArray
    case "threads" => drawWithExecutionPool
    case "single thread" => drawSingleThreaded
  }

  def drawSingleThreaded() = {
    val halfWidth = getWidth / 2
    val halfHeight = getHeight / 2
    val maxIterations = MandelbrotSettings.iterations.get().toInt
    val canvasTranslation = (MandelbrotSettings.x.get(), MandelbrotSettings.y.get())
    val guardianValue = MandelbrotSettings.compValue.get()
    val pixelRatio = MandelbrotSettings.scale.get() / getWidth

    val t = Kamon.timer("created-sets").withTag("engine", "single thread").start()
    for {
      p <- points
      realPoint = canvasToPosition(p, (halfWidth, halfHeight), canvasTranslation, pixelRatio)
    } yield {
      val m = countIterationsToValue(maxIterations, guardianValue)(realPoint)
      val color = new Color(1.0 * m / maxIterations, 1.0 * m / maxIterations, 1.0 * m / maxIterations, 1)
      getGraphicsContext2D.getPixelWriter.setColor(p._1, p._2, color)
    }
    t.stop()
  }

  def drawWithParArray() = {
    val halfWidth = getWidth / 2
    val halfHeight = getHeight / 2
    val maxIterations = MandelbrotSettings.iterations.get().toInt
    val canvasTranslation = (MandelbrotSettings.x.get(), MandelbrotSettings.y.get())
    val guardianValue = MandelbrotSettings.compValue.get()
    val pixelRatio = MandelbrotSettings.scale.get() / getWidth

    val t = Kamon.timer("created-sets").withTag("engine", "parallel array").start()
    val parResult = for {
      p <- points.par
      realPoint = canvasToPosition(p, (halfWidth, halfHeight), canvasTranslation, pixelRatio)
    } yield {
      (p, countIterationsToValue(maxIterations, guardianValue)(realPoint))
    }
    parResult.toArray.foreach { case ((x, y), m) =>
      val color = new Color(0.5 * m / maxIterations, 1.0 * m / maxIterations, 0.5 * m / maxIterations, 1)
      getGraphicsContext2D.getPixelWriter.setColor(x, y, color)
    }
    t.stop()
  }

  val threads = 8
  val ec = ExecutionContext.fromExecutor(Executors.newWorkStealingPool(threads))
  var p = Array.ofDim[((Int, Int), Int)](900*900)
  def drawWithExecutionPool(): Any = {
    val i = MandelbrotSettings.iterations.get().toInt
    val c = MandelbrotSettings.compValue.get()
    val w = getWidth().toInt
    val h = getHeight().toInt
    val dw = w / 2
    val dh = h / 2
    val _tx = MandelbrotSettings.x.get()
    val _ty = MandelbrotSettings.y.get()
    val pixelRatio = MandelbrotSettings.scale.get() / getWidth
    val l = new CountDownLatch(threads)

    val t = Kamon.timer("created-sets").withTag("engine", "execution pool").start()
    val cThreads = (0 to threads).map { j =>
      Future{
        try {
          (0 until w).foreach { x0 =>
            (j until h by threads).foreach { y0 =>
              if (y0 < h) {
                val (x, y) = canvasToPosition((x0, y0), (dw, dh), (_tx, _ty), pixelRatio)
//                val (x, y) = Mandelbrot.translate(((x0 - dw) * pixelRatio, (y0 - dh) * pixelRatio), (_tx, _ty))
                val m = countIterationsToValue(i, c)((x, y))
                p(x0 + y0 * w) = (((x0, y0), m))
              }
            }
          }
        } catch {
          case t: Throwable =>
            t.printStackTrace()
        }
        l.countDown()
      }(ec)
    }
    l.await()
    p.foreach {
      case ((x0, y0), m) =>
        val color = new Color(1.0 * m / i, 0.5 * m / i, 0.5 * m / i, 1)
        getGraphicsContext2D.getPixelWriter.setColor(x0, y0, color)
      case null =>
    }
    t.stop()
  }


  def clear() = {
    getGraphicsContext2D.clearRect(0, 0, getWidth.intValue(), getHeight.intValue())
    getGraphicsContext2D.setStroke(Color.BLACK)
    getGraphicsContext2D.setLineWidth(2)
    getGraphicsContext2D.strokeRect(1, 1, getWidth.intValue() - 1, getHeight.intValue() - 1)
    getGraphicsContext2D.setStroke(Color.RED)
    getGraphicsContext2D.setLineWidth(1)
    getGraphicsContext2D.strokeLine(0, getHeight.intValue() / 2, getWidth.intValue(), getHeight.intValue() / 2)
    getGraphicsContext2D.setStroke(Color.BLACK)
    getGraphicsContext2D.setLineWidth(1)
  }

  def canvasToPosition(
      canvasPos: (Int, Int),
      canvasOffset: (Double, Double),
      translate: (Double, Double),
      scale: Double
  ): (Double, Double) = (
      scale * (canvasPos._1 - canvasOffset._1) + translate._1,
      scale * (canvasPos._2 - canvasOffset._2) + translate._2
  )
}
