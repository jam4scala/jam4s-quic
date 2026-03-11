package org.jam4s.quic4cats

import java.net.URI
import java.security.KeyStore

import cats.effect.Resource
import cats.effect.kernel.Async
import net.luminis.quic.QuicClientConnection
import net.luminis.quic.log.Logger as QLogger

trait MkQClient[F[_]]:
  def newClient(
      uri: URI,
      protocol: String,
      trustStore: Option[KeyStore] = None,
      log: QLogger = QDefaults.logger()
  ): Resource[F, QConnectionF[F]]

object MkQClient:

  def apply[F[_]](using ev: MkQClient[F]): MkQClient[F] = ev

  given [F[_]: Async]: MkQClient[F] with
    def newClient(
        uri: URI,
        protocol: String,
        trustStore: Option[KeyStore] = None,
        log: QLogger = QDefaults.logger()
    ): Resource[F, QConnectionF[F]] =
      Resource.make(
        Async[F].blocking {
          val builder = QuicClientConnection
            .newBuilder()
            .uri(uri)
            .applicationProtocol(protocol)
            .logger(log)
          trustStore match
            case Some(ts) => builder.customTrustStore(ts)
            case None     => builder.noServerCertificateCheck()
          val conn = builder.build()
          conn.connect()
          QConnectionF[F](conn)
        }
      )(connF =>
        Async[F].blocking {
          connF.underlying.closeAndWait()
        }
      )
