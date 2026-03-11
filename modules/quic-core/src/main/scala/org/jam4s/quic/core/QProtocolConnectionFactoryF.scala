package org.jam4s.quic.core

import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import net.luminis.quic.QuicConnection
import net.luminis.quic.server.{ ApplicationProtocolConnection, ApplicationProtocolConnectionFactory }
import org.typelevel.log4cats.Logger

final class QProtocolConnectionFactoryF[F[_]: Async: Logger](
    handler: QStreamHandler[F],
    dispatcher: Dispatcher[F]
) extends ApplicationProtocolConnectionFactory:

  override def maxTotalPeerInitiatedBidirectionalStreams(): Long = 12L

  override def maxConcurrentPeerInitiatedBidirectionalStreams(): Int = Int.MaxValue

  override def createConnection(
      protocol: String,
      quicConnection: QuicConnection
  ): ApplicationProtocolConnection =
    QProtocolConnectionF[F](handler, dispatcher)
