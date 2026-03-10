import Dependencies.*
import sbt.Keys.name
import sbt.url
import sbtwelcome.*

ThisBuild / scalaVersion     := "3.4.2"
ThisBuild / organization     := "org.jam4s"
ThisBuild / organizationName := "JAM for Scala"

ThisBuild / evictionErrorLevel := Level.Warn

// Include Maven local repository
ThisBuild / resolvers ++= Seq(Resolver.mavenLocal)
ThisBuild / resolvers += Resolver.sonatypeCentralSnapshots

ThisBuild / pushRemoteCacheTo := Some(MavenCache("local-cache", file("tmp/remote-cache")))

ThisBuild / parallelExecution := false

// Publishing settings
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / homepage := Some(url("https://github.com/jam4scala/jam4s-quic"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/jam4scala/jam4s-quic"),
    "scm:git:git@github.com:jam4scala/jam4s-quic.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "sergeiastapov",
    name = "Sergei Astapov",
    email = "serg.astapov@gmail.com",
    url = url("https://github.com/sergey-astapov")
  )
)

// Coverage settings
coverageMinimumStmtTotal   := 70
coverageMinimumBranchTotal := 60
coverageFailOnMinimum      := false
coverageHighlighting       := true

Compile / run / fork := true

Global / onChangedBuildSource := ReloadOnSourceChanges
Global / semanticdbEnabled    := true // for metals

def jam4sLogo(scalaVersion: String, project: String) =
  s"""
     |${scala.Console.YELLOW}░▀▀█░█▀█░█▄█░█░█░█▀▀░░░█▀█░█░█░▀█▀░█▀▀░
     |${scala.Console.RED}░░░█░█▀█░█░█░░▀█░▀▀█░░░█░█░█░█░░█░░█░░░
     |${scala.Console.CYAN}░▀▀░░▀░▀░▀░▀░░░▀░▀▀▀░░░▀▀█░▀▀▀░▀▀▀░▀▀▀░
     |
     |Powered by ${scala.Console.YELLOW}Scala $scalaVersion${scala.Console.RESET}
     |
     |Visit: ${scala.Console.YELLOW} https://jam4scala.org${scala.Console.RESET}
     |Get the graypaper at: ${scala.Console.YELLOW} https://graypaper.com${scala.Console.RESET}
     |
     |Project: ${scala.Console.CYAN}$project ${scala.Console.RESET}
  """.stripMargin

logo := jam4sLogo(scalaVersion.value, "root")

usefulTasks := List(
  UsefulTask("lint", "Run scalafmtAll and scalafix OrganizeImports rule"),
  UsefulTask("fmt", "Run scalafmtSbt and scalafmtAll"),
  UsefulTask("fmtcheck", "Check if fmt is necessary")
)

excludeDependencies ++= Seq(
  ExclusionRule("ch.qos.logback", "logback-classic"),
  ExclusionRule("org.slf4j", "slf4j-api")
)

val minSettings = List(
  scalafmtOnCompile := false, // recommended in Scala 3
  logo              := jam4sLogo(scalaVersion.value, name.value),
  libraryDependencies ++= List(
    CompilerPlugins.zerowaste,
    Libraries.cats,
    Libraries.catsEffect,
    Libraries.fs2Core,
    Libraries.fs2io,
    Libraries.slf4j          % Test,
    Libraries.scalaLogging   % Test,
    Libraries.logbackClassic % Test,
    Libraries.scalacheck     % Test,
    Libraries.scalatest      % Test
  )
)

lazy val root = (project in file("."))
  .settings(
    name := "jam4s-quic"
  )
  .aggregate(
    `jam4s-quic-core`,
    `jam4s-quic-cli`
  )
  .settings(
    publish / skip := true
  )

lazy val `jam4s-quic-core` = (project in file("modules/quic-core"))
  .settings(minSettings *)

lazy val distTarGz = taskKey[File]("Package fat JAR and scripts into a tar.gz")

lazy val `jam4s-quic-cli` = (project in file("modules/quic-cli"))
  .settings(minSettings *)
  .settings(libraryDependencies ++= Seq(Libraries.decline, Libraries.declineEffect))
  // Assembly settings
  .settings(
    assembly / mainClass       := Some("org.jam4s.cli.Main"),
    assembly / assemblyJarName := s"${name.value}-${version.value}.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "MANIFEST.MF") =>
        MergeStrategy.discard

      case PathList("META-INF", "services", _ @_*) =>
        MergeStrategy.concat

      case PathList("META-INF", "native", _ @_*) =>
        MergeStrategy.first
      case PathList("META-INF", "maven", _ @_*) =>
        MergeStrategy.first

      case PathList("META-INF", "versions", _ @_*) =>
        MergeStrategy.first

      case PathList("META-INF", xs @ _*) if xs.exists { name =>
            val lower = name.toLowerCase
            lower.endsWith(".sf") ||
            lower.endsWith(".dsa") ||
            lower.endsWith(".rsa") ||
            lower == "index.list" ||
            lower == "dependencies" ||
            lower == "dependency" ||
            lower == "notice" || lower == "notice.txt" ||
            lower == "license" || lower == "license.txt"
          } =>
        MergeStrategy.discard

      case "reference.conf"   => MergeStrategy.concat
      case "application.conf" => MergeStrategy.concat

      case "logback.xml" => MergeStrategy.first

      case x if x.endsWith("io.netty.versions.properties") =>
        MergeStrategy.first

      case "module-info.class" =>
        MergeStrategy.discard

      case _ =>
        MergeStrategy.first
    },
    // Create TAR.GZ archive after assembly
    distTarGz := distTarGzTask("jam4s-quic-cli").value
  )
  // publishing settings
  .settings(
    name := "jam4s-quic-cli",

    // Replace default jar with assembly
    Compile / packageBin := (Compile / assembly).value,

    // Ensure distTarGz is built before publish
    publish := publish.dependsOn(distTarGz).value,

    // Publish distTarGz as an additional artifact (classifier "bin")
    addArtifact(Artifact("jam4s-quic-cli", "tar.gz", "tar.gz", "bin"), distTarGz)
  )
  .dependsOn(`jam4s-quic-core` % "compile->compile")

// Some legacy libaries requires deep reflective access to low-level API on JDK 21+
val unnamedJvmFlags = Seq(
  "--add-opens=java.base/java.nio.channels.spi=ALL-UNNAMED",
  "--add-opens=java.base/java.nio=ALL-UNNAMED",
  "--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED",
  "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED",
  "--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED",
  "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
  "--add-opens=jdk.compiler/com.sun.tools.javac=ALL-UNNAMED",
  "--add-opens=java.base/java.lang=ALL-UNNAMED",
  "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
  "--add-opens=java.base/java.io=ALL-UNNAMED",
  "--add-opens=java.base/java.util=ALL-UNNAMED"
)

ThisBuild / fork := true

ThisBuild / javaOptions ++= unnamedJvmFlags

Test / javaOptions ++= Seq(
  "-Xms1G",
  "-Xmx2G",
  "-XX:+UseG1GC"
)

Test / fork := true

addCommandAlias("lint", ";scalafmtAll ;scalafixAll --rules OrganizeImports")
addCommandAlias("fmt", "all root/scalafmtSbt root/scalafmtAll")
addCommandAlias("fmtCheck", "all root/scalafmtSbtCheck root/scalafmtCheckAll")
addCommandAlias("coverage", ";clean ;coverage ;test ;coverageReport")
addCommandAlias("coveragePvm", ";project pvm ;clean ;coverage ;test ;coverageReport")

// Tasks
import org.apache.commons.compress.archivers.tar.*

import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream

def distTarGzTask(projectName: String): Def.Initialize[Task[File]] = Def.task {
  val log        = streams.value.log
  val jarFile    = (Compile / assembly).value
  val baseDir    = baseDirectory.value
  val scriptsDir = baseDir / "scripts"
  val outputDir  = target.value / "dist"
  val tarGzFile  = outputDir / s"$projectName-${version.value}.tar.gz"

  IO.delete(outputDir)
  IO.createDirectory(outputDir)

  val filesToInclude: Seq[(File, String, Boolean)] = {
    val scriptFiles: Seq[File] = scriptsDir.listFiles().toSeq
    val scriptMappings = scriptFiles.flatMap { file =>
      if (file.isFile) {
        file.setExecutable(true, false)
      }
      file.relativeTo(scriptsDir).map { relative =>
        (file, s"$relative", file.canExecute)
      }
    }
    val jarMapping = Seq((jarFile, s"$projectName.jar", true))
    scriptMappings ++ jarMapping
  }

  val tarStream = new TarArchiveOutputStream(new GZIPOutputStream(new FileOutputStream(tarGzFile)))
  tarStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)

  filesToInclude.foreach { case (file, path, isExecutable) =>
    val entry = new TarArchiveEntry(file, path)
    entry.setSize(file.length())
    if (isExecutable) entry.setMode(0x755) else entry.setMode(0x644)
    tarStream.putArchiveEntry(entry)
    IO.transfer(file, tarStream)
    tarStream.closeArchiveEntry()
  }

  tarStream.finish()
  tarStream.close()

  log.info(s"Created TAR.GZ archive: ${tarGzFile.getAbsolutePath}")
  tarGzFile
}
