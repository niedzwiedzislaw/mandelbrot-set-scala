package ghx.mandelbrot

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import kamon.Kamon

class WApp extends Application {

  override def start(stage: Stage): Unit = {
    val root: AnchorPane = FXMLLoader.load(getClass.getResource("MandelbrotPanel.fxml"))
    val scene = new Scene(root)
    stage.setTitle("Mandelbrot Set")
    stage.setScene(scene)
    stage.show()
  }


  def __launch(args: Array[String]) = {
    Application.launch(args: _*)
  }
}

object WApp2 extends App {
  var stage: Stage = null
  Kamon.init()
  Application.launch(classOf[WApp], args: _*)

//  new WApp().__launch(args: Array[String])
}
