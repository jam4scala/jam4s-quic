package org.jam4s.quic.core

import java.net.URI

import cats.effect.{ IO, IOApp }
import cats.syntax.all.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object EchoClientRun extends IOApp.Simple:

  private given Logger[IO] = Slf4jLogger.getLogger[IO]

  private val messages = List("UP-0", "CE-128")

  def run: IO[Unit] =
    MkQClient[IO]
      .newClient(URI("https://localhost:9000"), "jam")
      .use { conn =>
        messages.traverse_ { msg =>
          for
            stream   <- conn.stream()
            _        <- Logger[IO].info(s"Sending: $msg")
            _        <- stream.write(msg.getBytes)
            _        <- stream.closeOutput
            response <- stream.read
            _        <- Logger[IO].info(s"Received: ${String(response)}")
          yield ()
        }
      }
