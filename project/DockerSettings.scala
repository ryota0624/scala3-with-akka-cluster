import com.typesafe.sbt.packager.Keys.{daemonUser, daemonUserUid, packageName}
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import sbt.Def
import sbt.Keys.{name, version}

object DockerSettings {
  def apply(): Seq[Def.SettingsDefinition] = Seq(
    dockerBaseImage := "amazoncorretto:11.0.11",
    dockerExposedPorts ++= Seq(8080),
    // Docker publishing settings
    dockerRepository := sys.props.get("docker.repository"),
    dockerUsername := Some("user-sample"),
    Docker / daemonUserUid := None,
    Docker / daemonUser := "daemon",
    Docker / packageName := name.value,
    Docker / version := sys.props.getOrElse("docker.tag", version.value)
  )
}
