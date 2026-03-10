# modules/quic - Specification

## Overview

The `quic` module provides QUIC transport networking for the JAM4S project. It wraps the [KWIK](https://github.com/ptrd/kwik) library (v0.9) — a Java/Kotlin QUIC implementation — and exposes both a low-level synchronous API and a high-level functional API built on Cats Effect.

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
├── README.md
├── cert/                              # TLS certificates and keystores
│   ├── keystore.jks
│   ├── keystore.p12
│   ├── cert.pem
│   ├── key.pem
│   └── X509_certificate.cer
├──modules/quic-core/
│  └── src/
│      ├── main/scala/org/jam4s/
│      │   ├── quic/                      # Synchronous core layer
│      │   │   ├── QClient.scala
│      │   │   ├── QServer.scala
│      │   │   ├── QServerOps.scala
│      │   │   ├── QProtocolConnection.scala
│      │   │   └── QProtocolConnectionFactory.scala
│      │   └── quic4cats/                 # Cats Effect functional layer
│      │       ├── QStreamF.scala
│      │       ├── QConnectionF.scala
│      │       ├── QConnectorF.scala
│      │       ├── QProtocolConnectionF.scala
│      │       ├── QProtocolConnectionFactoryF.scala
│      │       ├── MkQClient.scala
│      │       └── MkQServer.scala
│      └── test/scala/org/jam4s/quic4cats/
│          ├── EchoServerRun.scala        # Echo server demo
│          └── EchoClientRun.scala        # Echo client demo
├──modules/quic-cli/
   └── src/
       ├── main/scala/org/jam4s/quic
       │   ├── cli/                       # CLI
       │   │   ├── Main.scala
       └── test/scala/org/jam4s/quic/cli
```

## Architecture

The module is organized into two layers:

### Layer 1: Synchronous Core (`org.jam4s.quic`)

Direct wrappers around KWIK's Java API using `scala.concurrent.Future` for async stream handling. Suitable for standalone testing and debugging.

### Layer 2: Functional API (`org.jam4s.quic4cats`)

Effect-based wrappers using Cats Effect (`F[_]: Async`), providing:
- Resource-safe lifecycle management via `Resource[F, _]`
- Typeclass-based factories (`MkQClient`, `MkQServer`)
- Blocking I/O properly wrapped with `Sync[F].blocking`
- Integration with log4cats for structured logging

## Abstract Interfaces

Defined in `modules/types` (`org.jam4s.types.quic`):

```scala
trait QStream[F[_]]:
  def read: F[Array[Byte]]
  def write(bytes: Array[Byte]): F[Unit]

trait QConnection[F[_]]:
  def stream(bidirectional: Boolean = true): F[QStream[F]]

trait QConnector[F[_]]:
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
| `write(bytes)` | Blocking write to the stream's `OutputStream`, then closes it |

### QConnectionF

Wraps `net.luminis.quic.QuicConnection`. Implements `QConnection[F]`.

| Method | Behavior |
|--------|----------|
| `stream(bidirectional)` | Creates a new QUIC stream (blocking), returns `QStreamF[F]` |

### QConnectorF

Wraps `net.luminis.quic.server.ServerConnector`. Implements `QConnector[F]`.

| Method | Behavior |
|--------|----------|
| `start` | Blocking call to start the QUIC server connector |

### QProtocolConnectionF

Implements KWIK's `ApplicationProtocolConnection`. Bridges the callback-based KWIK API to the Cats Effect world.

| Method | Behavior |
|--------|----------|
| `acceptPeerInitiatedStream(stream)` | Wraps incoming `QuicStream` in `QStreamF[IO]`, passes to `QStreamHandler[IO]`, runs via `unsafeRunAsync` |

Error handling: logs warnings via `Logger[IO]` on failure.

### QProtocolConnectionFactoryF

Implements KWIK's `ApplicationProtocolConnectionFactory`. Creates `QProtocolConnectionF` instances.

| Config | Value |
|--------|-------|
| `maxTotalPeerInitiatedBidirectionalStreams` | `0` (unlimited) |
| `maxConcurrentPeerInitiatedBidirectionalStreams` | `Int.MaxValue` |

### MkQClient

Typeclass factory for QUIC client connections.

```scala
trait MkQClient[F[_]]:
  def newClient(
    uri: URI,
    protocol: String,
    log: QLogger = defaultLogger()
  ): Resource[F, QConnectionF[F]]
```

- **Acquire:** Creates `QuicClientConnection` via builder (blocking), connects to server
- **Release:** Calls `closeAndWait()` on connection
- **TLS:** No server certificate validation (development mode)
- **Implicit instance:** Available for any `F[_]: Async`

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
    log: QLogger = defaultLogger()
  ): Resource[F, QConnectorF[F]]
```

- **Acquire:** Creates `ServerConnector`, registers protocol factory, starts server (blocking)
- **Release:** No-op
- **Helper:** `QServerParams.apply(port, protocol, jksPath, alias, password)` loads a JKS keystore from file
- **Implicit instance:** Available for any `F[_]: Async`

## Configuration

### Default Server Configuration

| Parameter | Default Value |
|-----------|---------------|
| Port | `9000` |
| Protocol (ALPN) | `"jam"` |
| Keystore format | JKS |
| Keystore alias | `"selfsigned"` |
| Keystore password | `"password"` |
| Max open peer-initiated bidirectional streams | `12` |
| Max concurrent peer-initiated bidirectional streams | `Int.MaxValue` |

### Logging

- KWIK logger: `SysOutLogger` with long time format, info + warning enabled
- Application logger: log4cats `Slf4jLogger` (in quic4cats layer)
- Scala logging: `StrictLogging` (in synchronous layer)

## TLS Certificate Setup

Self-signed certificates are used for development. The `cert/` directory contains pre-generated files. To regenerate:

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
| Server certificate validation | Disabled (development mode) |
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
| Synchronous core | `Try`/`catch` with `println`, `Future.onComplete` callbacks |
| Functional API | Effect error propagation, `onError` with `Logger[IO].warn`, `unsafeRunAsync` Left/Right handling |
| Resource cleanup | `Resource` acquire/release pattern, `Using` for synchronous code |

## Test / Demo Applications

### EchoServerRun

`IOApp.Simple` that starts a QUIC echo server on port 9000. Reads bytes from each incoming stream and echoes them back.

### EchoClientRun

`IOApp.Simple` that connects to `localhost:9000`, sends test messages (`"UP-0"`, `"CE-128"`), and logs responses.

Run with:
```
sbt "quic/Test/runMain org.jam4s.quic4cats.EchoServerRun"
sbt "quic/Test/runMain org.jam4s.quic4cats.EchoClientRun"
```
