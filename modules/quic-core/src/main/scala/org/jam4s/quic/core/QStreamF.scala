package org.jam4s.quic.core

import java.util.concurrent.TimeoutException

import scala.concurrent.duration.FiniteDuration

import cats.effect.kernel.Async
import net.luminis.quic.QuicStream

trait QStream[F[_]]:
  def read: F[Array[Byte]]
  def readTimeout(timeout: FiniteDuration): F[Array[Byte]]
  def write(bytes: Array[Byte]): F[Unit]
  def closeOutput: F[Unit]

final class QStreamF[F[_]: Async](underlying: QuicStream) extends QStream[F]:

  def read: F[Array[Byte]] =
    Async[F].blocking {
      underlying.getInputStream.readAllBytes()
    }

  def readTimeout(timeout: FiniteDuration): F[Array[Byte]] =
    Async[F].timeoutTo(
      read,
      timeout,
      Async[F].raiseError(new TimeoutException(s"Read timed out after $timeout"))
    )

  def write(bytes: Array[Byte]): F[Unit] =
    Async[F].blocking {
      underlying.getOutputStream.write(bytes)
    }

  def closeOutput: F[Unit] =
    Async[F].blocking {
      underlying.getOutputStream.close()
    }
