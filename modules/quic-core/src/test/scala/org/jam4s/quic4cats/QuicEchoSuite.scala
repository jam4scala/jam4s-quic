package org.jam4s.quic4cats

import java.net.URI
import java.security.KeyStore

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.all.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class QuicEchoSuite extends AnyFunSuite with Matchers:

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

  private def withEchoServerAndClient[A](port: Int)(f: QConnectionF[IO] => IO[A]): A =
    val params  = QServerParams(port, protocol, keystore, alias, password)
    val factory = QProtocolConnectionFactoryF(echoHandler)
    (for
      _ <- MkQServer[IO].newServer(params, factory)
      c <- MkQClient[IO].newClient(URI(s"https://localhost:$port"), protocol)
    yield c)
      .use(f)
      .unsafeRunSync()

  test("echo single message") {
    withEchoServerAndClient(9001) { conn =>
      for
        stream   <- conn.stream()
        _        <- stream.write("hello".getBytes)
        _        <- stream.closeOutput
        response <- stream.read
      yield String(response) shouldBe "hello"
    }
  }

  test("echo multiple messages on separate streams") {
    val messages = List("UP-0", "CE-128", "jam4s-quic")
    withEchoServerAndClient(9002) { conn =>
      messages.traverse_ { msg =>
        for
          stream   <- conn.stream()
          _        <- stream.write(msg.getBytes)
          _        <- stream.closeOutput
          response <- stream.read
        yield String(response) shouldBe msg
      }
    }
  }

  test("echo large payload") {
    val data = Array.tabulate[Byte](8192)(i => (i % 127).toByte)
    withEchoServerAndClient(9003) { conn =>
      for
        stream   <- conn.stream()
        _        <- stream.write(data)
        _        <- stream.closeOutput
        response <- stream.read
      yield response shouldBe data
    }
  }

  test("echo binary data") {
    val data = Array.tabulate[Byte](256)(_.toByte)
    withEchoServerAndClient(9004) { conn =>
      for
        stream   <- conn.stream()
        _        <- stream.write(data)
        _        <- stream.closeOutput
        response <- stream.read
      yield response shouldBe data
    }
  }
