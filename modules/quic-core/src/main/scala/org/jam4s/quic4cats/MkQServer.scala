package org.jam4s.quic4cats

import java.io.FileInputStream
import java.net.DatagramSocket
import java.security.KeyStore

import cats.effect.Resource
import cats.effect.kernel.Async
import net.luminis.quic.log.{ Logger as QLogger, SysOutLogger }
import net.luminis.quic.server.{ ApplicationProtocolConnectionFactory, ServerConnector }

case class QServerParams(
    port: Int,
    protocol: String,
    keystore: KeyStore,
    alias: String,
    password: Array[Char]
)

object QServerParams:
  def apply(
      port: Int,
      protocol: String,
      jksPath: String,
      alias: String,
      password: String
  ): QServerParams =
    val ks = KeyStore.getInstance("JKS")
    val is = FileInputStream(jksPath)
    try ks.load(is, password.toCharArray)
    finally is.close()
    QServerParams(port, protocol, ks, alias, password.toCharArray)

trait MkQServer[F[_]]:
  def newServer(
      params: QServerParams,
      factory: ApplicationProtocolConnectionFactory,
      log: QLogger = MkQServer.defaultLogger()
  ): Resource[F, QConnectorF[F]]

object MkQServer:

  def apply[F[_]](using ev: MkQServer[F]): MkQServer[F] = ev

  def defaultLogger(): QLogger =
    val logger = SysOutLogger()
    logger.timeFormat(QLogger.TimeFormat.Long)
    logger.logInfo(true)
    logger.logWarning(true)
    logger

  given [F[_]: Async]: MkQServer[F] with
    def newServer(
        params: QServerParams,
        factory: ApplicationProtocolConnectionFactory,
        log: QLogger = defaultLogger()
    ): Resource[F, QConnectorF[F]] =
      Resource.make(
        Async[F].blocking {
          val socket     = new DatagramSocket(params.port)
          val actualPort = socket.getLocalPort
          val connector = ServerConnector
            .builder()
            .withPort(actualPort)
            .withSocket(socket)
            .withKeyStore(params.keystore, params.alias, params.password)
            .withLogger(log)
            .build()
          connector.registerApplicationProtocol(params.protocol, factory)
          connector.start()
          QConnectorF[F](connector, actualPort)
        }
      )(_ => Async[F].unit) // ServerConnector has no close/stop API in KWIK v0.9
