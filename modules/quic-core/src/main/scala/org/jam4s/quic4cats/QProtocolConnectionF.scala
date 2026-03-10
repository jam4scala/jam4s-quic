package org.jam4s.quic4cats

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import net.luminis.quic.QuicStream
import net.luminis.quic.server.ApplicationProtocolConnection

final class QProtocolConnectionF(handler: QStreamHandler[IO]) extends ApplicationProtocolConnection:

  private given Logger[IO] = Slf4jLogger.getLogger[IO]

  override def acceptPeerInitiatedStream(stream: QuicStream): Unit =
    val qStream = QStreamF[IO](stream)
    handler
      .handle(qStream)
      .onError { e =>
        Logger[IO].warn(e)(s"Error handling stream: ${e.getMessage}")
      }
      .unsafeRunAsync {
        case Left(e)  => Logger[IO].warn(e)(s"Async error: ${e.getMessage}").unsafeRunAndForget()
        case Right(_) => ()
      }
