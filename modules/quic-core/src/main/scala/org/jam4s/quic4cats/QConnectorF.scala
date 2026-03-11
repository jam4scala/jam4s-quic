package org.jam4s.quic4cats

import cats.effect.kernel.Sync
import net.luminis.quic.server.ServerConnector

trait QConnector[F[_]]:
  def port: Int
  def start: F[Unit]

final class QConnectorF[F[_]: Sync](underlying: ServerConnector, val port: Int) extends QConnector[F]:

  def start: F[Unit] =
    Sync[F].blocking {
      underlying.start()
    }
