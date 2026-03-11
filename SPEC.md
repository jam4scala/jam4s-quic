# modules/quic - Specification

## Overview

The `quic` module provides QUIC transport networking for the JAM4S project. It wraps the [KWIK](https://github.com/ptrd/kwik) library (v0.9) — a Java/Kotlin QUIC implementation — and exposes a functional API built on Cats Effect.

The module registers a custom application protocol (`"jam"`) via ALPN and handles bidirectional streams for JAM node-to-node and client-to-node communication.

## Module Dependencies

```
kwik ──► jam4s-quic-core ──► jam4s-quic-cli
```

- **External dependency:** `tech.kwik:kwik:0.9`

## Directory Structure

```
jam4s-quic
├── SPEC.md
├── CLAUDE.md
├── cert/                              # TLS certificates and keystores
│   └── keystore.jks
├── modules/quic-core/
│   └── src/
│       ├── main/scala/org/jam4s/quic/core/
│       │   ├── QStreamF.scala
│       │   ├── QConnectionF.scala
│       │   ├── QConnectorF.scala
│       │   ├── QDefaults.scala
│       │   ├── QStreamHandler.scala
│       │   ├── QProtocolConnectionF.scala
│       │   ├── QProtocolConnectionFactoryF.scala
│       │   ├── MkQClient.scala
│       │   └── MkQServer.scala
│       └── test/
│           ├── scala/org/jam4s/quic/core/
│           │   ├── EchoServerRun.scala
│           │   ├── EchoClientRun.scala
│           │   ├── QuicEchoSuite.scala
│           │   └── QuicTestSupport.scala
│           └── resources/keystore.jks
└── modules/quic-cli/
    └── src/
        ├── main/scala/org/jam4s/quic/cli/
        │   └── Main.scala
        ├── test/
        │   ├── scala/org/jam4s/quic/cli/
        │   │   ├── CliIntegrationSuite.scala
        │   │   └── CliParseSuite.scala
        │   └── resources/keystore.jks
        └── scripts/                   # Shell scripts for distribution
```

## Architecture

The module provides a functional API (`org.jam4s.quic.core`) built on Cats Effect (`F[_]: Async`), providing:
- Resource-safe lifecycle management via `Resource[F, _]`
- Typeclass-based factories (`MkQClient`, `MkQServer`)
- Blocking I/O properly wrapped with `Async[F].blocking`
- Callback-to-effect bridging via `Dispatcher[F]`
- Integration with log4cats for structured logging

## Traits

Defined in `org.jam4s.quic.core`:

```scala
trait QStream[F[_]]:
  def read: F[Array[Byte]]
  def readTimeout(timeout: FiniteDuration): F[Array[Byte]]
  def write(bytes: Array[Byte]): F[Unit]
  def closeOutput: F[Unit]

trait QConnection[F[_]]:
  def stream(bidirectional: Boolean = true): F[QStream[F]]

trait QConnector[F[_]]:
  def port: Int
  def start: F[Unit]

trait QStreamHandler[F[_]]:
  def handle(stream: QStream[F]): F[Unit]
```

## Component Specifications

### QStreamF

Wraps `net.luminis.quic.QuicStream`. Implements `QStream[F]`.

| Method | Behavior |
|--------|----------|
| `read` | Blocking read of all bytes from the stream's `InputStream` |
| `readTimeout(timeout)` | `read` with a `FiniteDuration` timeout |
| `write(bytes)` | Blocking write to the stream's `OutputStream` |
| `closeOutput` | Blocking close of the stream's `OutputStream` |

### QConnectionF

Wraps `net.luminis.quic.QuicConnection`. Implements `QConnection[F]`.

| Method | Behavior |
|--------|----------|
| `stream(bidirectional)` | Creates a new QUIC stream (blocking), returns `QStreamF[F]` |

### QConnectorF

Wraps `net.luminis.quic.server.ServerConnector`. Implements `QConnector[F]`.

| Method | Behavior |
|--------|----------|
| `port` | Returns the actual bound port (may differ from requested port when using port 0) |
| `start` | Blocking call to start the QUIC server connector |

### QProtocolConnectionF

Implements KWIK's `ApplicationProtocolConnection`. Bridges the callback-based KWIK API to the Cats Effect world using `Dispatcher[F]`.

| Method | Behavior |
|--------|----------|
| `acceptPeerInitiatedStream(stream)` | Wraps incoming `QuicStream` in `QStreamF[F]`, dispatches to `QStreamHandler[F]` via `dispatcher.unsafeRunAndForget` |

Error handling: logs warnings via `Logger[F].warn` on failure.

### QProtocolConnectionFactoryF

Implements KWIK's `ApplicationProtocolConnectionFactory`. Creates `QProtocolConnectionF` instances.

| Config | Value |
|--------|-------|
| `maxTotalPeerInitiatedBidirectionalStreams` | `12` |
| `maxConcurrentPeerInitiatedBidirectionalStreams` | `Int.MaxValue` |

### QDefaults

Provides default KWIK logger configuration.

```scala
object QDefaults:
  def logger(): QLogger
```

Returns a `SysOutLogger` with long time format, info + warning enabled.

### MkQClient

Typeclass factory for QUIC client connections.

```scala
trait MkQClient[F[_]]:
  def newClient(
    uri: URI,
    protocol: String,
    trustStore: Option[KeyStore] = None,
    log: QLogger = QDefaults.logger()
  ): Resource[F, QConnectionF[F]]
```

- **Acquire:** Creates `QuicClientConnection` via builder (blocking), connects to server
- **Release:** Calls `closeAndWait()` on connection
- **TLS:** When `trustStore` is `None`, server certificate validation is disabled (development mode). When provided, uses the given `KeyStore` for validation.
- **Given instance:** Available for any `F[_]: Async`

### MkQServer

Typeclass factory for QUIC servers.

```scala
case class QServerParams(
  port: Int,
  protocol: String,
  keystore: KeyStore,
  alias: String,
  password: Array[Char]
)

trait MkQServer[F[_]]:
  def newServer(
    params: QServerParams,
    factory: ApplicationProtocolConnectionFactory,
    log: QLogger = QDefaults.logger()
  ): Resource[F, QConnectorF[F]]
```

- **Acquire:** Creates `DatagramSocket(port)` to bind port (port `0` lets the OS assign a random available port), builds `ServerConnector`, registers protocol factory, starts server
- **Release:** No-op (KWIK v0.9 lacks close API)
- **Helper:** `QServerParams.apply(port, protocol, jksPath, alias, password)` loads a JKS keystore from file
- **Given instance:** Available for any `F[_]: Async`

## CLI Application

Entry point: `org.jam4s.quic.cli.Main` (Decline `CommandApp`). Packaged as a fat JAR via sbt-assembly, bundled with shell scripts into a `.tar.gz` via the `distTarGz` task.

### Server Subcommand

| Option | Short | Default | Description |
|--------|-------|---------|-------------|
| `--keystore` | `-k` | (required) | Path to JKS keystore |
| `--port` | `-p` | `9000` | Port number |
| `--protocol` | | `"jam"` | ALPN protocol |
| `--alias` | | `"selfsigned"` | Keystore alias |
| `--password` | | `"password"` | Keystore password |

Starts an echo server that reads bytes from each incoming stream and echoes them back.

### Client Subcommand

| Option | Short | Default | Description |
|--------|-------|---------|-------------|
| `--host` | `-h` | `"localhost"` | Server host |
| `--port` | `-p` | `9000` | Port number |
| `--protocol` | | `"jam"` | ALPN protocol |
| `--message` | `-m` | `"UP-0"`, `"CE-128"` | Messages to send (repeatable) |

Connects to the server, sends each message on a separate stream, and logs responses.

## Configuration

### Default Server Configuration

| Parameter | Default Value |
|-----------|---------------|
| Port | `9000` |
| Protocol (ALPN) | `"jam"` |
| Keystore format | JKS |
| Keystore alias | `"selfsigned"` |
| Keystore password | `"password"` |
| Max total peer-initiated bidirectional streams | `12` |
| Max concurrent peer-initiated bidirectional streams | `Int.MaxValue` |

### Logging

- KWIK logger: `SysOutLogger` with long time format, info + warning enabled (via `QDefaults.logger()`)
- Application logger: log4cats `Slf4jLogger`

## TLS Certificate Setup

Self-signed certificates are used for development. The `cert/` directory contains a pre-generated JKS keystore. Test copies exist in `modules/quic-core/src/test/resources/` and `modules/quic-cli/src/test/resources/`. To regenerate:

1. Generate JKS keystore: `keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass password -validity 360 -keysize 2048`
2. Export certificate: `keytool -export -alias selfsigned -keystore keystore.jks -rfc -file X509_certificate.cer`
3. Convert to PKCS12: `keytool -importkeystore -srckeystore keystore.jks -destkeystore keystore.p12 -deststoretype PKCS12 -srcalias selfsigned -deststorepass password -destkeypass password`
4. Extract PEM cert: `openssl pkcs12 -in keystore.p12 -passin pass:password -nokeys -out cert.pem`
5. Extract PEM key: `openssl pkcs12 -in keystore.p12 -passin pass:password -nodes -nocerts -out key.pem`

## QUIC Protocol Features

| Feature | Status |
|---------|--------|
| Bidirectional streams | Supported |
| Unidirectional streams | Supported (via boolean flag) |
| Multiple concurrent streams | Supported (configurable limit) |
| ALPN (Application-Layer Protocol Negotiation) | Supported (protocol: `"jam"`) |
| TLS 1.3 encryption | Supported (via JKS keystore) |
| Server certificate validation | Optional (via `trustStore` parameter) |
| Connection keep-alive | Supported (via KWIK) |
| Connection close with wait | Supported (client-side) |

## Integration Points

### Node Module

The `node` module uses this module to accept incoming QUIC connections:

```
MkQServer[IO] → QProtocolConnectionFactoryF → QProtocolConnectionF → QStreamHandler[IO] → QJamnpRoutes
```

### JAMNP Client Module

The `jamnp_client` module uses this module to connect to JAM nodes:

```
MkQClient[IO] → QConnectionF[IO] → QStreamF[IO] → JamnpClient
```

## Error Handling

| Layer | Strategy |
|-------|----------|
| Functional API | Effect error propagation, `onError` with `Logger[F].warn`, `Dispatcher` for callback bridging |
| Resource cleanup | `Resource` acquire/release pattern |

## Test Suite

### QuicEchoSuite

Integration tests using `AsyncFunSuite` with `QuicTestSupport`:
- Single message echo
- Multiple messages on separate streams
- Large payload (8192 bytes)
- Binary data echo (all 256 byte values)

### CliParseSuite

Parser validation:
- Server subcommand requires `--keystore`, accepts all options
- Client subcommand accepts all options including multiple `-m` flags
- Top-level requires subcommand

### CliIntegrationSuite

End-to-end CLI command tests via echo server.

### Demo Applications

```bash
sbt "jam4s-quic-core/Test/runMain org.jam4s.quic.core.EchoServerRun"
sbt "jam4s-quic-core/Test/runMain org.jam4s.quic.core.EchoClientRun"
```

- **EchoServerRun:** `IOApp.Simple` starting echo server on port 9000
- **EchoClientRun:** `IOApp.Simple` connecting to `localhost:9000`, sending `"UP-0"` and `"CE-128"`
