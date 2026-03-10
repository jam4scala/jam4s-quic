package org.jam4s.quic.cli

import cats.effect.{ExitCode, IO}
import cats.syntax.all.*
import com.monovore.decline.*
import com.monovore.decline.effect.CommandIOApp

import java.nio.file.Path
import scala.concurrent.duration.*

object Main
    extends CommandIOApp(
      name = "jam4s-quic",
      header = "JAM4S QUIC CLI"
    ):

  override def main: Opts[IO[ExitCode]] =
    ???
