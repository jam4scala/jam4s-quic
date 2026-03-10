package org.jam4s.quic4cats

import java.net.URI

import cats.effect.Resource
import cats.effect.kernel.Async
import net.luminis.quic.QuicClientConnection
import net.luminis.quic.log.{ Logger as QLogger, SysOutLogger }

trait MkQClient[F[_]]:
  def newClient(
      uri: URI,
      protocol: String,
      log: QLogger = MkQClient.defaultLogger()
  ): Resource[F, QConnectionF[F]]

object MkQClient:

  def apply[F[_]](using ev: MkQClient[F]): MkQClient[F] = ev

  def defaultLogger(): QLogger =
    val logger = SysOutLogger()
    logger.timeFormat(QLogger.TimeFormat.Long)
    logger.logInfo(true)
    logger.logWarning(true)
    logger

  given [F[_]: Async]: MkQClient[F] with
    def newClient(
        uri: URI,
        protocol: String,
        log: QLogger = defaultLogger()
    ): Resource[F, QConnectionF[F]] =
      Resource.make(
        Async[F].blocking {
          val conn = QuicClientConnection
            .newBuilder()
            .uri(uri)
            .applicationProtocol(protocol)
            .noServerCertificateCheck()
            .logger(log)
            .build()
          conn.connect()
          QConnectionF[F](conn)
        }
      )(connF =>
        Async[F].blocking {
          connF.underlying.closeAndWait()
        }
      )
