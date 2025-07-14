enablePlugins(JavaFxPlugin)
name := "mandelbrot-set"

version := "0.1"

scalaVersion := "3.6.4"

//scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit", "-encoding", "utf8", "-feature")
scalacOptions ++= Seq("-rewrite", "-source:3.4-migration")

javaFxMainClass := "ghx.mandelbrot.WApp"

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", ps @ _*) if ps.contains("MANIFEST.MF") => MergeStrategy.first
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.first
  case "module-info.class" => MergeStrategy.discard
  case _ => MergeStrategy.first
}

//assemblyMergeStrategy in assembly := {
//  case PathList("javax", "servlet", xs@_*) => MergeStrategy.first
//  case PathList(ps@_*) if ps.last endsWith ".html" => MergeStrategy.first
//  case "application.conf" => MergeStrategy.concat
//  case "unwanted.txt" => MergeStrategy.discard
//  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
//  case x =>
//    val oldStrategy = (assemblyMergeStrategy in assembly).value
//    oldStrategy(x)
//}

// Fork a new JVM for 'run' and 'test:run', to avoid JavaFX double initialization problems
//fork := true

// Determine OS version of JavaFX binaries
lazy val osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux") => "linux"
  case n if n.startsWith("Mac") => "mac"
  case n if n.startsWith("Windows") => "win"
  case _ => throw new Exception("Unknown platform!")
}
Global / serverConnectionType := ConnectionType.Tcp

// Add JavaFX dependencies
lazy val javaFXModules = Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
libraryDependencies ++= javaFXModules.map(m =>
  "org.openjfx" % s"javafx-$m" % "23.0.2" classifier osName
)

libraryDependencies += "io.kamon" %% "kamon-core" % "2.5.11"
libraryDependencies += "io.kamon" %% "kamon-influxdb" % "2.5.11"
libraryDependencies += "com.typesafe" % "config" % "1.4.2"
libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.14" % "test"
libraryDependencies += "ch.qos.logback" % "logback-core" % "1.4.5"
libraryDependencies += "org.slf4j" % "slf4j-api" % "2.0.4"
