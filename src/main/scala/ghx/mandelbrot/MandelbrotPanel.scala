package ghx.mandelbrot

import ghx.mandelbrot._
import javafx.beans.Observable
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.control.{ProgressIndicator, Slider, TextField, Toggle, ToggleGroup}
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import kamon.Kamon

import scala.collection.parallel.CollectionConverters._
//import kamon.Kamon

import scala.collection.immutable

class MandelbrotPanel {

  @FXML
  var canvas: ResizableCanvas = _
  @FXML
  var xField: TextField = _
  @FXML
  var xSlider: Slider = _
  @FXML
  var yField: TextField = _
  @FXML
  var ySlider: Slider = _
  @FXML
  var scaleField: TextField = _
  @FXML
  var scaleSlider: Slider = _
  @FXML
  var iterationsField: TextField = _
  @FXML
  var iterationsSlider: Slider = _
  @FXML
  var comparisonField: TextField = _
  @FXML
  var comparisonSlider: Slider = _
  @FXML
  var colorScaleField: TextField = _
  @FXML
  var colorScaleSlider: Slider = _
  @FXML
  var progress: ProgressIndicator = _
  @FXML
  var mode: ToggleGroup = _

  val xProp = new SimpleDoubleProperty(10)
  var points: immutable.Seq[(Int, Int)] = _

  var draw: () => Any = () => ()

  def initialize() = {
    colorScaleSlider.valueProperty.setValue(1000)
    xSlider.valueProperty().bindBidirectional(MandelbrotSettings.x)
    ySlider.valueProperty().bindBidirectional(MandelbrotSettings.y)
    scaleSlider.valueProperty().bindBidirectional(MandelbrotSettings.scale)
    iterationsSlider.valueProperty().bindBidirectional(MandelbrotSettings.iterations)
    comparisonSlider.valueProperty().bindBidirectional(MandelbrotSettings.compValue)

//    xSlider.minProperty().bind(xSlider.valueProperty().subtract(scaleSlider.valueProperty()))
//    xSlider.maxProperty().bind(xSlider.valueProperty().add(scaleSlider.valueProperty()))
//    ySlider.minProperty().bind(ySlider.valueProperty().subtract(scaleSlider.valueProperty()))
//    ySlider.maxProperty().bind(ySlider.valueProperty().add(scaleSlider.valueProperty()))

    DoBindings.bind(xField.textProperty(), xSlider.valueProperty())
    DoBindings.bind(yField.textProperty(), ySlider.valueProperty())
    DoBindings.bind(scaleField.textProperty(), scaleSlider.valueProperty())
    DoBindings.bind(iterationsField.textProperty(), iterationsSlider.valueProperty())
    DoBindings.bind(comparisonField.textProperty(), comparisonSlider.valueProperty())
    DoBindings.bind(colorScaleField.textProperty(), colorScaleSlider.valueProperty())

    xSlider.valueProperty().addListener(new ChangeListener[Number] {
      def changed(observableValue: ObservableValue[_ <: Number], t: Number, t1: Number): Unit = {
        draw()
      }

    })
    ySlider.valueProperty().addListener(new ChangeListener[Number] {
      def changed(observableValue: ObservableValue[_ <: Number], t: Number, t1: Number): Unit = {
        draw()
      }

    })
    scaleSlider.valueProperty().addListener(new ChangeListener[Number] {
      def changed(observableValue: ObservableValue[_ <: Number], t: Number, t1: Number): Unit = {
        xSlider.minProperty().setValue(xSlider.getValue() - scaleSlider.getValue)
        xSlider.maxProperty().setValue(xSlider.getValue() + scaleSlider.getValue())
        ySlider.minProperty().setValue(ySlider.getValue() - scaleSlider.getValue())
        ySlider.maxProperty().setValue(ySlider.getValue() + scaleSlider.getValue())

        draw()
      }

    })
    iterationsSlider.valueProperty().addListener(new ChangeListener[Number] {
      def changed(observableValue: ObservableValue[_ <: Number], t: Number, t1: Number): Unit = {
        draw()
      }
    })
    colorScaleSlider.valueProperty().addListener(new ChangeListener[Number] {
      def changed(observableValue: ObservableValue[_ <: Number], t: Number, t1: Number): Unit = {
        draw()
      }
    })
    comparisonSlider.valueProperty().addListener(new ChangeListener[Number] {
      def changed(observableValue: ObservableValue[_ <: Number], t: Number, t1: Number): Unit = {
        draw()
      }
    })

    canvas.setOnScroll(new javafx.event.EventHandler[ScrollEvent] {
      override def handle(t: ScrollEvent): Unit = {
        println(t.getDeltaY)
        if (t.getDeltaY > 0) {
            MandelbrotSettings.scale.setValue(MandelbrotSettings.scale.get() * 0.7)
        } else {
          MandelbrotSettings.scale.setValue(MandelbrotSettings.scale.get() * (1.0 / 0.7))
        }
      }
    })

    mode.selectedToggleProperty().addListener(new ChangeListener[Toggle] {

      def changed(observableValue: ObservableValue[_ <: Toggle], t: Toggle, t1: Toggle): Unit = {
        draw = toggleToDrawFn(t1.getUserData)
        draw()
      }
    })

    def onResize = {
      remapPoints()
      draw()
    }
    canvas.widthProperty().addListener((evt: Observable) => onResize)
    canvas.heightProperty.addListener((evt: Observable) => onResize)

    draw = toggleToDrawFn(mode.selectedToggleProperty().get().getUserData)

    javafx.application.Platform.runLater(() => drawLoop)
  }

  private def toggleToDrawFn(x: Any): () => Unit = x  match {
    case "parArray" => drawWithParArray
    case "threads" => drawWithExecutionPool
    case "ng" => drawNg
  }

  def drawLoop(): Unit = {
    javafx.application.Platform.runLater{
      () =>
        draw()
        drawLoop()
    }
  }

  val black = new Color(0, 0, 0, 1)

  def remapPoints() = {
    Mandelbrot.p = Array.ofDim[((Int, Int), Int)](canvas.getWidth.intValue() * canvas.getHeight.intValue())
    points =
      for {
        x <- 0 to canvas.getWidth.intValue()
        y <- 0 to canvas.getHeight.intValue()
      } yield {
        (x, y)
      }
  }

  def drawNg() = {
    javafx.application.Platform.runLater(() => {
      val i = MandelbrotSettings.iterations.get().toInt
      val c = MandelbrotSettings.compValue.get()
      val axisRatio = MandelbrotSettings.scale.get() / canvas.getWidth

      val t = Kamon.timer("created-sets").withTag("engine", "ng").start()
      val valuesAtPoints = Mandelbrot2.calculateValuesForPlane(
        canvasSize = (canvas.getWidth.toInt, canvas.getHeight.toInt),
        canvasTranslation = (MandelbrotSettings.x.get(), MandelbrotSettings.y.get()),
        pixelRatio = MandelbrotSettings.scale.get() / canvas.getWidth,
        iterations = i,
        c
      )
      valuesAtPoints.foreach {
        case ((x0, y0), m) =>
          val color = new Color(1.0 * m / i, 1.0 * m / i, 1.0 * m / i, 1)
          canvas.getGraphicsContext2D.getPixelWriter.setColor(x0, y0, color)
      }
      t.stop()

      progress.setVisible(false)

    })
  }

  def drawWithParArray(): Any = {
    progress.setVisible(true)
    javafx.application.Platform.runLater(() => {
      val i = MandelbrotSettings.iterations.get().toInt
      val c = MandelbrotSettings.compValue.get()
      val _tx = MandelbrotSettings.x.get()
      val _ty = MandelbrotSettings.y.get()
      val axisRatio = MandelbrotSettings.scale.get() / canvas.getWidth
      val dw = canvas.getWidth / 2
      val dh = canvas.getHeight / 2
      val t = Kamon.timer("created-sets").withTag("engine", "parallel array").start()
      val matchingPoints = points.par.map { p =>
        val (x0, y0) = p
        val (x, y) = Mandelbrot.canvasToPosition((x0, y0), (dw, dh), (_tx, _ty), axisRatio)
        val m = Mandelbrot.calculateIterations(i, c)((x, y))
        ((x0, y0), m)
      }.toArray
      matchingPoints.foreach {
        case ((x0, y0), m) =>
          val color = new Color(1.0 * m / i, 1.0 * m / i, 1.0 * m / i, 1)
          canvas.getGraphicsContext2D.getPixelWriter.setColor(x0, y0, color)
      }
      t.stop()

      progress.setVisible(false)
    })
  }

  def drawWithExecutionPool(): Any = {
//    println("draw4")
    progress.setVisible(true)
    javafx.application.Platform.runLater(() => {
      val t = Kamon.timer("created-sets").withTag("engine", "execution pool").start()
      val i = MandelbrotSettings.iterations.get().toInt
      Mandelbrot.calculateParalelly(canvas.getWidth.intValue(), canvas.getHeight.intValue()).foreach {
        case ((x0, y0), m) =>
          //        canvas.getGraphicsContext2D.strokeRect(x0, y0, 1, 1)
          val color = new Color(1.0 * m / i, 1.0 * m / i, 1.0 * m / i, 1)
          canvas.getGraphicsContext2D.getPixelWriter.setColor(x0, y0, color)
      }
     t.stop()

      progress.setVisible(false)
    })
  }

  def draw2() = {
    progress.setVisible(true)
    javafx.application.Platform.runLater(() => {
      val i = MandelbrotSettings.iterations.get().toInt
      val c = MandelbrotSettings.compValue.get()
      val _tx = MandelbrotSettings.x.get()
      val _ty = MandelbrotSettings.y.get()
      val s = MandelbrotSettings.scale.get().floatValue()
      val axisRatio = MandelbrotSettings.scale.get() / canvas.getWidth
      val dw = canvas.getWidth / 2
      val dh = canvas.getHeight / 2
//      println(s"Translating by (-$dw, -$dh), scaling by $axisRatio")
      var max = 0.0
      val matchingPoints = points.par.map { p =>
        val (x0, y0) = p
        val (x, y) = Mandelbrot.translate(((x0 - dw) * axisRatio, (y0 - dh) * axisRatio), (_tx, _ty))
        val m = Mandelbrot.calculateValues(i, c)((x, y)).module
        if (m > max) max = Math.min(m, c)
        ((x0, y0), m)
      }.toArray
      matchingPoints.foreach {
        case ((x0, y0), m) =>
          //        canvas.getGraphicsContext2D.strokeRect(x0, y0, 1, 1)
          val color = new Color(Math.min(1.0, 1.0 * m / max), Math.min(1.0, 1.0 * m / max), Math.min(1.0, 1.0 * m / max), 1)
          canvas.getGraphicsContext2D.getPixelWriter.setColor(x0, y0, color)
      }

      progress.setVisible(false)
    })
  }

  def clear() = {
    canvas.getGraphicsContext2D.clearRect(0, 0, canvas.getWidth.intValue(), canvas.getHeight.intValue())
    canvas.getGraphicsContext2D.setStroke(black)
    canvas.getGraphicsContext2D.setLineWidth(2)
    canvas.getGraphicsContext2D.strokeRect(1, 1, canvas.getWidth.intValue() - 1, canvas.getHeight.intValue() - 1)
    canvas.getGraphicsContext2D.setStroke(new Color(0.4, 0.3, 0.3, 1))
    canvas.getGraphicsContext2D.setLineWidth(1)
    canvas.getGraphicsContext2D.strokeLine(0, canvas.getHeight.intValue() / 2, canvas.getWidth.intValue(), canvas.getHeight.intValue() / 2)
    canvas.getGraphicsContext2D.setStroke(black)
    canvas.getGraphicsContext2D.setLineWidth(1)
//    println("Clearing")
  }


}
