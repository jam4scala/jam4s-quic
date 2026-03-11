![logo](doc/img/jam4s-logo.png)

# jam4s-quic

QUIC transport networking for the [JAM4S](https://jam4scala.org) project. Wraps the [KWIK](https://github.com/ptrd/kwik) library (v0.9) and exposes a functional API built on [Cats Effect](https://typelevel.org/cats-effect/).

Registers the `"jam"` application protocol via ALPN for JAM node-to-node and client-to-node communication.

## Requirements

- JDK 21+
- sbt

## Modules

```
kwik ──► jam4s-quic-core ──► jam4s-quic-cli
```

- **jam4s-quic-core** — Functional QUIC API with `Resource`-based lifecycle, typeclass factories (`MkQClient`, `MkQServer`), and log4cats logging.
- **jam4s-quic-cli** — CLI application with `server` and `client` subcommands. Packaged as a fat JAR via sbt-assembly.

## Build

```bash
sbt compile                 # Compile all modules
sbt test                    # Run all tests
sbt fmt                     # Format code (scalafmt)
sbt lint                    # Format + organize imports
sbt assembly                # Build fat JAR for CLI
sbt distTarGz               # Package fat JAR + scripts into .tar.gz
```

## CLI Usage

```bash
# Start an echo server
jam4s-quic server --keystore cert/keystore.jks

# Start with custom port
jam4s-quic server -k cert/keystore.jks -p 8443

# Connect and send default messages (UP-0, CE-128)
jam4s-quic client

# Send custom messages
jam4s-quic client -m "hello" -m "world"

# Connect to custom host/port
jam4s-quic client -h 10.0.0.1 -p 8443
```

### Server Options

| Option | Short | Default | Description |
|---|---|---|---|
| `--keystore` | `-k` | (required) | Path to JKS keystore |
| `--port` | `-p` | `9000` | Port number |
| `--protocol` | | `jam` | ALPN protocol name |
| `--alias` | | `selfsigned` | Keystore certificate alias |
| `--password` | | `password` | Keystore password |

### Client Options

| Option | Short | Default | Description |
|---|---|---|---|
| `--host` | `-h` | `localhost` | Server host |
| `--port` | `-p` | `9000` | Port number |
| `--protocol` | | `jam` | ALPN protocol name |
| `--message` | `-m` | `UP-0, CE-128` | Messages to send (repeatable) |

## TLS Certificates

Self-signed certificates for development are in `cert/`. To regenerate:

```bash
keytool -genkey -keyalg RSA -alias selfsigned \
  -keystore cert/keystore.jks -storepass password \
  -validity 360 -keysize 2048 -dname "CN=localhost" -noprompt
```

## Known Limitations

- **Server shutdown:** KWIK v0.9's `ServerConnector` interface does not expose a `close()` or `stop()` method, so the `MkQServer` `Resource` release is a no-op. Server sockets are not explicitly cleaned up on shutdown.

## License

[Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0)
