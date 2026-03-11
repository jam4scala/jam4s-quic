package org.jam4s.quic.core

import cats.effect.std.Dispatcher
import cats.effect.{ IO, IOApp }
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object EchoServerRun extends IOApp.Simple:

  private given Logger[IO] = Slf4jLogger.getLogger[IO]

  private val echoHandler: QStreamHandler[IO] = new QStreamHandler[IO]:
    def handle(stream: QStream[IO]): IO[Unit] =
      for
        bytes <- stream.read
        _     <- Logger[IO].info(s"Echo server received: ${String(bytes)}")
        _     <- stream.write(bytes)
        _     <- stream.closeOutput
      yield ()

  def run: IO[Unit] =
    val params = QServerParams(9000, "jam", "cert/keystore.jks", "selfsigned", "password")
    (for
      dispatcher <- Dispatcher.parallel[IO]
      factory = QProtocolConnectionFactoryF[IO](echoHandler, dispatcher)
      _ <- MkQServer[IO].newServer(params, factory)
    yield ()).use { _ =>
      Logger[IO].info("Echo server started on port 9000") *> IO.never
    }
