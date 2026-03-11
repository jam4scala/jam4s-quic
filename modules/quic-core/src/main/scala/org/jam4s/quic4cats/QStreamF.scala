package org.jam4s.quic4cats

import cats.effect.kernel.Sync
import net.luminis.quic.QuicStream

trait QStream[F[_]]:
  def read: F[Array[Byte]]
  def write(bytes: Array[Byte]): F[Unit]
  def closeOutput: F[Unit]

final class QStreamF[F[_]: Sync](underlying: QuicStream) extends QStream[F]:

  def read: F[Array[Byte]] =
    Sync[F].blocking {
      underlying.getInputStream.readAllBytes()
    }

  def write(bytes: Array[Byte]): F[Unit] =
    Sync[F].blocking {
      underlying.getOutputStream.write(bytes)
    }

  def closeOutput: F[Unit] =
    Sync[F].blocking {
      underlying.getOutputStream.close()
    }
