import sbtassembly.AssemblyKeys.assemblyMergeStrategy
import sbtassembly.AssemblyPlugin.autoImport.{MergeStrategy, assembly}
import sbtassembly.PathList

object AssemblyMergeStrategy {
  def apply() = assembly / assemblyMergeStrategy := {
    case PathList(ps @ _*) if ps.last == "mime.types" => MergeStrategy.first
    case PathList(ps @ _*) if ps.last == "module-info.class" => MergeStrategy.first
    case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
    case PathList("codegen-resources", xs @ _*) => MergeStrategy.first
    case "META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat" =>
      new MergeLog4j2PluginCachesStrategy
    case x => (assembly / assemblyMergeStrategy).value(x)
  }
}
