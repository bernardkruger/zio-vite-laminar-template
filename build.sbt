import sbtcrossproject.CrossPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

ThisBuild / scalaVersion := "3.8.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "io.micanic"

// shared
val zioVersion = "2.1.24"
val zioHttpVersion = "3.8.1"
val zioJsonVersion = "0.7.45"   // latest 0.9.0, so check the compatibility until matching
val zioInteropReactiveStreamsVersion = "2.0.2"    // latest 2.1.0

val upickleVersion = "4.4.2"
val scalaJavaTimeVersion = "2.6.0"

// jvm
val typesafeConfigVersion = "1.4.3"
val osLibVersion = "0.11.9-M6"
val poiVersion = "5.5.1"
val scalajsStubsVersion = "1.1.0"
val strataVersion = "2.12.66"


// js
val laminarVersion = "18.0.0-M2"
val scalaJsDomVersion = "2.8.1"

/* -------------------- ROOT -------------------- */
lazy val root = 
  project.in(file("."))
    .aggregate(shared.jvm, shared.js, server, client)
    .settings(
      name := "zio-vite-laminar-template",
      publish / skip := true
    )


/* -------------------- SHARED -------------------- */
lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .in(file("shared"))
  .settings(
    name := "shared",
    publish / skip := true,
  )
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio" % zioVersion,
      "dev.zio" %%% "zio-http" % zioHttpVersion,
      "dev.zio" %%% "zio-json" % zioJsonVersion,
      "dev.zio" %%% "zio-test-sbt" % zioVersion,

      "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion,
      "com.lihaoyi" %%% "upickle" % upickleVersion
    ),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "utf8",
      "-feature",
      "-unchecked",
    ),

    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %% "scalajs-stubs" % scalajsStubsVersion % "provided",
    )
  )
  


/* -------------------- SERVER -------------------- */
lazy val server = project
  .in(file("server"))
  .settings(
    name := "server",
    publish / skip := true,
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % typesafeConfigVersion,
      "org.apache.poi" % "poi" % poiVersion,
      "org.apache.poi" % "poi-ooxml" % poiVersion,
      "com.lihaoyi" %% "os-lib" % osLibVersion,
    ),
    assembly / mainClass := Some("template.Main"),
    assembly / assemblyJarName := "app.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "versions", "9", "module-info.class") => MergeStrategy.discard
      case PathList("META-INF", "versions", "11", "module-info.class") => MergeStrategy.discard
      case PathList("META-INF", "versions", "9", "OSGI-INF", "MANIFEST.MF") => MergeStrategy.first
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
      case PathList("scala", "annotation", "unroll.tasty") => MergeStrategy.first
      case PathList("scala", "annotation", "unroll.class") => MergeStrategy.first
      case x => (assembly / assemblyMergeStrategy).value.apply(x)
    }   // this works for MAC, for Windows you may need to adjust the strategy to avoid "Duplicate entry" errors
  )
  .dependsOn(shared.jvm)


/* -------------------- CLIENT -------------------- */
lazy val client = project
  .in(file("client"))
  .enablePlugins(org.scalajs.sbtplugin.ScalaJSPlugin)
  .settings(
    name := "client",
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { 
      _.withModuleKind(org.scalajs.linker.interface.ModuleKind.ESModule)
    },
    publish / skip := true,
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar" % laminarVersion,
      "org.scala-js" %%% "scalajs-dom" % scalaJsDomVersion,
    )
  )
  .dependsOn(shared.js)


/* -------------------- BUILDING AND PACKAGING -------------------- */
// This is not crucial for transferring/converting to a vite app
import scala.sys.process.*
import java.io.File

lazy val buildFrontend = taskKey[Unit]("Build the frontend using Vite")

buildFrontend := {
  // 1. scala.js optimised build
  (client / Compile / fullOptJS).value

  val clientDir = (client / baseDirectory).value

  // detect OS and set prefix for npm command
  val isWindows = System.getProperty("os.name").toLowerCase.contains("win")
  val cmdPrefix = if (isWindows) Seq("cmd", "/c") else Seq.empty

  // 2. npm install
  val npmCi = Process(cmdPrefix ++ Seq("npm", "ci"), clientDir).!
  if (npmCi != 0) sys.error("npm ci failed")

  // 3. npm run build
  val viteBuild = Process(cmdPrefix ++ Seq("npm", "run", "build"), clientDir).!
  if (viteBuild != 0) sys.error("npm run build failed")

  // 4. Copy to server static directory
  val distDir = new File(clientDir, "dist")
  val targetDir = (server / baseDirectory).value / "src" / "main" / "resources" / "static"

  IO.delete(targetDir)
  IO.copyDirectory(distDir, targetDir)
}

lazy val packageApp = taskKey[File]("Package the application into a fat JAR")

packageApp := {
  // Ensure the frontend is built and copied to the server's static directory
  buildFrontend.value
  val jar = (server / assembly).value

  val outputDir = baseDirectory.value / "dist"
  IO.createDirectory(outputDir)

  val target = outputDir / "app.jar"
  IO.copyFile(jar, target)

  target
}