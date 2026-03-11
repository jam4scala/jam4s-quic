package org.jam4s.quic.cli

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.*
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import org.jam4s.quic4cats.*

class CliIntegrationSuite extends AsyncFunSuite with AsyncIOSpec with Matchers with QuicTestSupport:

  test("client sends messages and receives echo responses") {
    val messages = List("UP-0", "CE-128")
    withEchoServerAndClient { conn =>
      messages.traverse { msg =>
        for
          stream   <- conn.stream()
          _        <- stream.write(msg.getBytes)
          _        <- stream.closeOutput
          response <- stream.read
        yield String(response)
      }
    }.asserting(_ shouldBe List("UP-0", "CE-128"))
  }

  test("client with custom messages echoes them back") {
    val messages = List("jam4s", "quic", "test")
    withEchoServerAndClient { conn =>
      messages.traverse { msg =>
        for
          stream   <- conn.stream()
          _        <- stream.write(msg.getBytes)
          _        <- stream.closeOutput
          response <- stream.read
        yield String(response)
      }
    }.asserting(_ shouldBe messages)
  }
