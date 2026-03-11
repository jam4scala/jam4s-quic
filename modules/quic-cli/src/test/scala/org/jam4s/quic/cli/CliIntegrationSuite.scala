package org.jam4s.quic.cli

import java.net.URI
import java.security.KeyStore

import cats.effect.IO
import cats.effect.std.Dispatcher
import cats.effect.unsafe.implicits.global
import cats.syntax.all.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.jam4s.quic4cats.*

class CliIntegrationSuite extends AnyFunSuite with Matchers:

  private given Logger[IO] = Slf4jLogger.getLogger[IO]

  private val protocol = "jam"
  private val alias    = "selfsigned"
  private val password = "password".toCharArray

  private lazy val keystore: KeyStore =
    val ks = KeyStore.getInstance("JKS")
    val is = getClass.getResourceAsStream("/keystore.jks")
    try ks.load(is, password)
    finally is.close()
    ks

  private val echoHandler: QStreamHandler[IO] = new QStreamHandler[IO]:
    def handle(stream: QStream[IO]): IO[Unit] =
      for
        bytes <- stream.read
        _     <- stream.write(bytes)
        _     <- stream.closeOutput
      yield ()

  test("client sends messages and receives echo responses") {
    val port     = 9010
    val params   = QServerParams(port, protocol, keystore, alias, password)
    val messages = List("UP-0", "CE-128")

    val result = (for
      dispatcher <- Dispatcher.parallel[IO]
      factory = QProtocolConnectionFactoryF[IO](echoHandler, dispatcher)
      _ <- MkQServer[IO].newServer(params, factory)
      c <- MkQClient[IO].newClient(URI(s"https://localhost:$port"), protocol)
    yield c)
      .use { conn =>
        messages.traverse { msg =>
          for
            stream   <- conn.stream()
            _        <- stream.write(msg.getBytes)
            _        <- stream.closeOutput
            response <- stream.read
          yield String(response)
        }
      }
      .unsafeRunSync()

    result shouldBe messages
  }

  test("client with custom messages echoes them back") {
    val port     = 9011
    val params   = QServerParams(port, protocol, keystore, alias, password)
    val messages = List("jam4s", "quic", "test")

    val result = (for
      dispatcher <- Dispatcher.parallel[IO]
      factory = QProtocolConnectionFactoryF[IO](echoHandler, dispatcher)
      _ <- MkQServer[IO].newServer(params, factory)
      c <- MkQClient[IO].newClient(URI(s"https://localhost:$port"), protocol)
    yield c)
      .use { conn =>
        messages.traverse { msg =>
          for
            stream   <- conn.stream()
            _        <- stream.write(msg.getBytes)
            _        <- stream.closeOutput
            response <- stream.read
          yield String(response)
        }
      }
      .unsafeRunSync()

    result shouldBe messages
  }
