package org.jam4s.quic.core

import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.syntax.all.*
import net.luminis.quic.QuicStream
import net.luminis.quic.server.ApplicationProtocolConnection
import org.typelevel.log4cats.Logger

final class QProtocolConnectionF[F[_]: Async: Logger](
    handler: QStreamHandler[F],
    dispatcher: Dispatcher[F]
) extends ApplicationProtocolConnection:

  override def acceptPeerInitiatedStream(stream: QuicStream): Unit =
    val qStream = QStreamF[F](stream)
    dispatcher.unsafeRunAndForget(
      handler
        .handle(qStream)
        .onError { e =>
          Logger[F].warn(e)(s"Error handling stream: ${e.getMessage}")
        }
    )
