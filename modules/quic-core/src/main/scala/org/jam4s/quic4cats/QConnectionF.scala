package org.jam4s.quic4cats

import cats.effect.kernel.Sync
import net.luminis.quic.QuicConnection

trait QConnection[F[_]]:
  def stream(bidirectional: Boolean = true): F[QStream[F]]

final class QConnectionF[F[_]: Sync] private[quic4cats] (val underlying: QuicConnection)
    extends QConnection[F]:

  def stream(bidirectional: Boolean = true): F[QStream[F]] =
    Sync[F].blocking {
      QStreamF[F](underlying.createStream(bidirectional))
    }
