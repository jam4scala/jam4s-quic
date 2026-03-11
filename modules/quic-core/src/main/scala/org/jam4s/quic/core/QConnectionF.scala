package org.jam4s.quic.core

import cats.effect.kernel.Async
import net.luminis.quic.QuicConnection

trait QConnection[F[_]]:
  def stream(bidirectional: Boolean = true): F[QStream[F]]

final class QConnectionF[F[_]: Async] private[core] (val underlying: QuicConnection)
    extends QConnection[F]:

  def stream(bidirectional: Boolean = true): F[QStream[F]] =
    Async[F].blocking {
      QStreamF[F](underlying.createStream(bidirectional))
    }
