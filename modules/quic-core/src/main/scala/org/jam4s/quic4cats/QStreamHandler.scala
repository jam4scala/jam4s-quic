package org.jam4s.quic4cats

trait QStreamHandler[F[_]]:
  def handle(stream: QStream[F]): F[Unit]
