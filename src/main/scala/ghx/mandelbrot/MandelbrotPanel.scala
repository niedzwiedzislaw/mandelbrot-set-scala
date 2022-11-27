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
  var canvas: MandelbrotCanvas = _
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
        canvas.draw()
      }

    })
    ySlider.valueProperty().addListener(new ChangeListener[Number] {
      def changed(observableValue: ObservableValue[_ <: Number], t: Number, t1: Number): Unit = {
        canvas.draw()
      }

    })
    scaleSlider.valueProperty().addListener(new ChangeListener[Number] {
      def changed(observableValue: ObservableValue[_ <: Number], t: Number, t1: Number): Unit = {
        xSlider.minProperty().setValue(xSlider.getValue() - scaleSlider.getValue)
        xSlider.maxProperty().setValue(xSlider.getValue() + scaleSlider.getValue())
        ySlider.minProperty().setValue(ySlider.getValue() - scaleSlider.getValue())
        ySlider.maxProperty().setValue(ySlider.getValue() + scaleSlider.getValue())

        canvas.draw()
      }

    })
    iterationsSlider.valueProperty().addListener(new ChangeListener[Number] {
      def changed(observableValue: ObservableValue[_ <: Number], t: Number, t1: Number): Unit = {
        canvas.draw()
      }
    })
    colorScaleSlider.valueProperty().addListener(new ChangeListener[Number] {
      def changed(observableValue: ObservableValue[_ <: Number], t: Number, t1: Number): Unit = {
        canvas.draw()
      }
    })
    comparisonSlider.valueProperty().addListener(new ChangeListener[Number] {
      def changed(observableValue: ObservableValue[_ <: Number], t: Number, t1: Number): Unit = {
        canvas.draw()
      }
    })

    mode.selectedToggleProperty().addListener(new ChangeListener[Toggle] {
      def changed(observableValue: ObservableValue[_ <: Toggle], t: Toggle, t1: Toggle): Unit = {
        canvas.setDrawFn(t1.getUserData)
        canvas.draw()
      }
    })

    canvas.setDrawFn(mode.selectedToggleProperty().get().getUserData)

    canvas.startLoop()
  }



}
