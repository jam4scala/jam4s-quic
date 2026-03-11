package org.jam4s.quic.core

trait QStreamHandler[F[_]]:
  def handle(stream: QStream[F]): F[Unit]
