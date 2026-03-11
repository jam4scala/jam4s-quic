package org.jam4s.quic4cats

import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import org.typelevel.log4cats.Logger
import net.luminis.quic.QuicConnection
import net.luminis.quic.server.{ ApplicationProtocolConnection, ApplicationProtocolConnectionFactory }

final class QProtocolConnectionFactoryF[F[_]: Async: Logger](
    handler: QStreamHandler[F],
    dispatcher: Dispatcher[F]
) extends ApplicationProtocolConnectionFactory:

  override def maxTotalPeerInitiatedBidirectionalStreams(): Long = 0L

  override def maxConcurrentPeerInitiatedBidirectionalStreams(): Int = Int.MaxValue

  override def createConnection(
      protocol: String,
      quicConnection: QuicConnection
  ): ApplicationProtocolConnection =
    QProtocolConnectionF[F](handler, dispatcher)
