ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.0"

lazy val root = (project in file("."))
  .settings(
    name := "sokoban",
    // JVM options for Java 17+.
    javaOptions += "--add-opens",
    javaOptions += "javafx.graphics/com.sun.javafx.application=ALL-UNNAMED",
    // Dependencies.
    libraryDependencies += "org.cosplayengine" % "cosplay" % "0.8.10"
  )
