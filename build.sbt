import sbt.Keys.libraryDependencies

val scala3Version = "3.0.0"
val AkkaVersion = "2.6.15"

val Libraries = Seq(
  "com.novocode" % "junit-interface" % "0.11" % "test",
  ("com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion)
    .cross(CrossVersion.for3Use2_13),
  ("com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion)
    .cross(CrossVersion.for3Use2_13),
  "ch.qos.logback" % "logback-classic" % "1.2.5"
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "scala3-akka-simple",
    version := "0.1.0",
    scalaVersion := scala3Version,
    libraryDependencies ++= Libraries
  )
