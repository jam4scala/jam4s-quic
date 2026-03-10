import sbt.*

object Dependencies {

  object V {
    val kwik         = "0.9"
    val cats         = "2.13.0"
    val catsEffect   = "3.6.3"
    val fs2          = "3.12.2"
    val decline      = "2.6.0"
    val slf4j        = "2.0.17"
    val logback      = "1.5.32"
    val scalaLogging = "3.9.6"
    val zerowaste    = "0.2.21"
    val scalacheck   = "1.19.0"
    val scalatest    = "3.2.19"
  }

  object Libraries {
    val kwik       = "tech.kwik"      % "kwik"        % V.kwik
    val cats       = "org.typelevel" %% "cats-core"   % V.cats
    val catsEffect = "org.typelevel" %% "cats-effect" % V.catsEffect
    val fs2Core    = "co.fs2"        %% "fs2-core"    % V.fs2
    val fs2io      = "co.fs2"        %% "fs2-io"      % V.fs2

    // Runtime
    val decline        = "com.monovore"               %% "decline"         % V.decline
    val declineEffect  = "com.monovore"               %% "decline-effect"  % V.decline
    val slf4j          = "org.slf4j"                   % "slf4j-api"       % V.slf4j
    val logbackClassic = "ch.qos.logback"              % "logback-classic" % V.logback
    val scalaLogging   = "com.typesafe.scala-logging" %% "scala-logging"   % V.scalaLogging

    // test
    val scalacheck = "org.scalacheck" %% "scalacheck" % V.scalacheck
    val scalatest  = "org.scalatest"  %% "scalatest"  % V.scalatest
  }

  object CompilerPlugins {
    val zerowaste = compilerPlugin("com.github.ghik" % "zerowaste" % V.zerowaste cross CrossVersion.full)
  }
}
