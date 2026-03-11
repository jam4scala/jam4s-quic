package org.jam4s.quic4cats

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.*
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

class QuicEchoSuite extends AsyncFunSuite with AsyncIOSpec with Matchers with QuicTestSupport:

  test("echo single message") {
    withEchoServerAndClient { conn =>
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
    withEchoServerAndClient { conn =>
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
    withEchoServerAndClient { conn =>
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
    withEchoServerAndClient { conn =>
      for
        stream   <- conn.stream()
        _        <- stream.write(data)
        _        <- stream.closeOutput
        response <- stream.read
      yield response shouldBe data
    }
  }
