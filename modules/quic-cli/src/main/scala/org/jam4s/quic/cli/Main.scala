package org.jam4s.quic.cli

import java.net.URI
import java.nio.file.Path

import cats.effect.{ ExitCode, IO }
import cats.effect.std.Dispatcher
import cats.syntax.all.*
import com.monovore.decline.*
import com.monovore.decline.effect.CommandIOApp
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.jam4s.quic4cats.*

object Main
    extends CommandIOApp(
      name = "jam4s-quic",
      header = "JAM4S QUIC CLI",
      version = "0.1.0"
    ):

  private given Logger[IO] = Slf4jLogger.getLogger[IO]

  private val portOpt =
    Opts.option[Int]("port", short = "p", help = "Port number.").withDefault(9000)

  private val protocolOpt =
    Opts.option[String]("protocol", help = "ALPN protocol name.").withDefault("jam")

  private val keystoreOpt =
    Opts.option[Path]("keystore", short = "k", help = "Path to JKS keystore.")

  private val aliasOpt =
    Opts.option[String]("alias", help = "Keystore certificate alias.").withDefault("selfsigned")

  private val passwordOpt =
    Opts.option[String]("password", help = "Keystore password.").withDefault("password")

  private val hostOpt =
    Opts.option[String]("host", short = "h", help = "Server host.").withDefault("localhost")

  private val messageOpt =
    Opts.options[String]("message", short = "m", help = "Message to send.").orEmpty

  private val serverCmd = Opts.subcommand("server", "Start a QUIC echo server.") {
    (portOpt, protocolOpt, keystoreOpt, aliasOpt, passwordOpt).mapN(ServerConfig.apply)
  }

  private val clientCmd = Opts.subcommand("client", "Connect to a QUIC server and send messages.") {
    (hostOpt, portOpt, protocolOpt, messageOpt).mapN(ClientConfig.apply)
  }

  private[cli] val opts: Opts[IO[ExitCode]] =
    (serverCmd orElse clientCmd).map {
      case cfg: ServerConfig => runServer(cfg)
      case cfg: ClientConfig => runClient(cfg)
    }

  override def main: Opts[IO[ExitCode]] = opts

  private def runServer(cfg: ServerConfig): IO[ExitCode] =
    IO.blocking(QServerParams(cfg.port, cfg.protocol, cfg.keystore.toString, cfg.alias, cfg.password)).flatMap {
      params =>
        val echoHandler: QStreamHandler[IO] = new QStreamHandler[IO]:
          def handle(stream: QStream[IO]): IO[Unit] =
            for
              bytes <- stream.read
              _     <- Logger[IO].info(s"Received ${bytes.length} bytes: ${String(bytes)}")
              _     <- stream.write(bytes)
              _     <- stream.closeOutput
            yield ()

        (for
          dispatcher <- Dispatcher.parallel[IO]
          factory = QProtocolConnectionFactoryF[IO](echoHandler, dispatcher)
          _ <- MkQServer[IO].newServer(params, factory)
        yield ())
          .use { _ =>
            Logger[IO].info(s"Echo server started on port ${cfg.port} (protocol: ${cfg.protocol})") *> IO.never
          }
          .as(ExitCode.Success)
    }

  private def runClient(cfg: ClientConfig): IO[ExitCode] =
    val uri      = URI(s"https://${cfg.host}:${cfg.port}")
    val messages = if cfg.messages.nonEmpty then cfg.messages else List("UP-0", "CE-128")
    MkQClient[IO]
      .newClient(uri, cfg.protocol)
      .use { conn =>
        messages.traverse_ { msg =>
          for
            stream   <- conn.stream()
            _        <- Logger[IO].info(s"Sending: $msg")
            _        <- stream.write(msg.getBytes)
            _        <- stream.closeOutput
            response <- stream.read
            _        <- Logger[IO].info(s"Received: ${String(response)}")
          yield ()
        }
      }
      .as(ExitCode.Success)

private case class ServerConfig(
    port: Int,
    protocol: String,
    keystore: Path,
    alias: String,
    password: String
)

private case class ClientConfig(
    host: String,
    port: Int,
    protocol: String,
    messages: List[String]
)
