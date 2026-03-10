package org.jam4s.quic4cats

import cats.effect.IO
import net.luminis.quic.QuicConnection
import net.luminis.quic.server.{ ApplicationProtocolConnection, ApplicationProtocolConnectionFactory }

final class QProtocolConnectionFactoryF(handler: QStreamHandler[IO])
    extends ApplicationProtocolConnectionFactory:

  override def maxTotalPeerInitiatedBidirectionalStreams(): Long = 0L

  override def maxConcurrentPeerInitiatedBidirectionalStreams(): Int = Int.MaxValue

  override def createConnection(
      protocol: String,
      quicConnection: QuicConnection
  ): ApplicationProtocolConnection =
    QProtocolConnectionF(handler)
