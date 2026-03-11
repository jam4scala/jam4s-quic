package org.jam4s.quic4cats

import java.net.URI
import java.security.KeyStore

import cats.effect.IO
import cats.effect.std.Dispatcher
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

trait QuicTestSupport:

  protected given Logger[IO] = Slf4jLogger.getLogger[IO]

  protected val protocol: String      = "jam"
  protected val alias: String         = "selfsigned"
  protected val password: Array[Char] = "password".toCharArray

  protected lazy val keystore: KeyStore =
    val ks = KeyStore.getInstance("JKS")
    val is = getClass.getResourceAsStream("/keystore.jks")
    try ks.load(is, password)
    finally is.close()
    ks

  protected val echoHandler: QStreamHandler[IO] = new QStreamHandler[IO]:
    def handle(stream: QStream[IO]): IO[Unit] =
      for
        bytes <- stream.read
        _     <- stream.write(bytes)
        _     <- stream.closeOutput
      yield ()

  protected def withEchoServerAndClient[A](f: QConnectionF[IO] => IO[A]): IO[A] =
    val params = QServerParams(0, protocol, keystore, alias, password)
    (for
      dispatcher <- Dispatcher.parallel[IO]
      factory = QProtocolConnectionFactoryF[IO](echoHandler, dispatcher)
      server <- MkQServer[IO].newServer(params, factory)
      c      <- MkQClient[IO].newClient(URI(s"https://localhost:${server.port}"), protocol)
    yield c).use(f)
