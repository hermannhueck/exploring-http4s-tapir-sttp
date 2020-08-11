val projectName        = "exploring-http4s-tapir-sttp"
val projectDescription = "Exploring http4s tapir and sttp"
val projectVersion     = "0.1.0"

val scala213 = "2.13.3"

lazy val commonSettings =
  Seq(
    version := projectVersion,
    scalaVersion := scala213,
    publish / skip := true,
    scalacOptions ++= ScalacOptions.defaultScalacOptions,
    Compile / console / scalacOptions := ScalacOptions.consoleScalacOptions,
    Test / console / scalacOptions := ScalacOptions.consoleScalacOptions,
    semanticdbEnabled := true,
    semanticdbVersion := "4.3.20",                                                // scalafixSemanticdb.revision,
    ThisBuild / scalafixDependencies ++= Seq("com.github.liancheng" %% "organize-imports" % "0.4.0"),
    Test / parallelExecution := false,
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-s", "100"), // -s = -minSuccessfulTests
    testFrameworks += new TestFramework("munit.Framework"),
    initialCommands :=
      s"""|
          |import scala.util.chaining._
          |import fs2._, cats.effect._, cats.effect.implicits._, cats.implicits._
          |import scala.concurrent.ExecutionContext.Implicits.global
          |import scala.concurrent.duration._
          |implicit val contextShiftIO: ContextShift[IO] = IO.contextShift(global)
          |implicit val timerIO: Timer[IO] = IO.timer(global)
          |println
          |""".stripMargin
  )

lazy val root = (project in file("."))
  .aggregate(`exploring-http4s`)
  .settings(commonSettings)
  .settings(
    name := "root",
    description := "root project",
    sourceDirectories := Seq.empty
  )

// lazy val core = (project in file("core"))
//   .dependsOn(hutil)
//   .settings(commonSettings)
//   .settings(
//     name := projectName,
//     description := projectDescription,
//     libraryDependencies ++= Dependencies.coreDependencies(scalaVersion.value)
//   )

lazy val `exploring-http4s` = (project in file("exploring-http4s"))
  .dependsOn(hutil)
  .settings(commonSettings)
  .settings(
    name := "exploring-http4s",
    description := "Exploring http4s",
    libraryDependencies ++= Dependencies.coreDependencies(scalaVersion.value)
  )

lazy val hutil = (project in file("hutil"))
  .settings(commonSettings)
  .settings(
    name := "hutil",
    description := "Hermann's Utilities",
    libraryDependencies ++= Dependencies.hutilDependencies(scalaVersion.value)
  )

// GraphBuddy support
// resolvers += Resolver.bintrayRepo("virtuslab", "graphbuddy")
// addCompilerPlugin("com.virtuslab.semanticgraphs" % "scalac-plugin" % "0.0.10" cross CrossVersion.full)
// scalacOptions += "-Yrangepos"
