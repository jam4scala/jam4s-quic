package org.jam4s.quic.cli

import com.monovore.decline.Command
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CliParseSuite extends AnyFunSuite with Matchers:

  private val cmd = Command("jam4s-quic", "test")(Main.opts)

  private def parses(args: String*): Any =
    cmd.parse(args) match
      case Left(help) => fail(s"Expected parse success but got:\n$help")
      case Right(_)   => succeed

  private def fails(args: String*): Any =
    cmd.parse(args) match
      case Left(_)  => succeed
      case Right(_) => fail(s"Expected parse failure for args: ${args.mkString(" ")}")

  // --- server subcommand ---

  test("server: requires --keystore") {
    fails("server")
  }

  test("server: parses with required keystore") {
    parses("server", "--keystore", "/tmp/ks.jks")
  }

  test("server: parses with short -k") {
    parses("server", "-k", "/tmp/ks.jks")
  }

  test("server: parses with all options") {
    parses(
      "server",
      "-k",
      "/tmp/ks.jks",
      "-p",
      "8443",
      "--protocol",
      "custom",
      "--alias",
      "myalias",
      "--password",
      "secret"
    )
  }

  test("server: rejects unknown option") {
    fails("server", "-k", "/tmp/ks.jks", "--unknown", "value")
  }

  // --- client subcommand ---

  test("client: parses with no args (all defaults)") {
    parses("client")
  }

  test("client: parses with host and port") {
    parses("client", "-h", "example.com", "-p", "8443")
  }

  test("client: parses with multiple messages") {
    parses("client", "-m", "hello", "-m", "world")
  }

  test("client: parses with all options") {
    parses("client", "-h", "10.0.0.1", "-p", "9999", "--protocol", "custom", "-m", "test")
  }

  test("client: rejects unknown option") {
    fails("client", "--unknown", "value")
  }

  // --- top-level ---

  test("no subcommand fails") {
    fails()
  }

  test("unknown subcommand fails") {
    fails("unknown")
  }
