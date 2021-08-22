import sbt.Keys.libraryDependencies

val scala3Version = "3.0.0"
val AkkaVersion = "2.6.15"
val AkkaManagementVersion = "1.1.1"

val akkaCluster = Seq(
  "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion
)

val akkaManagement = Seq(
  "com.lightbend.akka.management" %% "akka-management" % AkkaManagementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion,
  "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
  "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % AkkaManagementVersion,
  "com.lightbend.akka.discovery" %% "akka-discovery-aws-api-async" % AkkaManagementVersion
)

val akka = (Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion
) ++ akkaCluster ++ akkaManagement).map(_.cross(CrossVersion.for3Use2_13))

val Libraries = Seq(
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.5"
) ++ akka

lazy val root = project
  .in(file("."))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    name := "scala3-akka-simple",
    version := "0.1.0",
    scalaVersion := scala3Version,
    libraryDependencies ++= Libraries,
    fork := true
  )
  .settings(AssemblyMergeStrategy.apply())
  .settings(
    DockerSettings.apply(): _*
  )
  .settings(AssemblyMergeStrategy.apply())
