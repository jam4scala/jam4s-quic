# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

We're building the app described in @SPEC.md. Read that file for general architectural tasks or to double-check API specification, tech stack or application architecture.

Keep your replies extremely concise and focus on conveying the key information. No unnecessary fluff, no long code snippets.

Whenever working with any third-party library or something similar, you MUST look up the official documentation to ensure that you're working with up-to-date information. Use the DocsExplorer subagent for efficient documentation lookup.

## Project

jam4s-quic provides QUIC transport networking for the JAM4S project. It wraps the [KWIK](https://github.com/ptrd/kwik) library (v0.9) — a Java/Kotlin QUIC implementation — and exposes both a synchronous API and a functional API built on Cats Effect. Registers the `"jam"` application protocol via ALPN.

## Build Commands

Requires JDK 21.

```bash
sbt compile                 # Compile all modules
sbt test                    # Run all tests
sbt "testOnly *SuiteName"   # Run a single test suite
sbt fmt                     # Format all code (scalafmt)
sbt fmtCheck                # Check formatting without changing files
sbt lint                    # Format + organize imports (scalafmt + scalafix)
sbt assembly                # Build fat JAR for CLI module
sbt distTarGz               # Package fat JAR + scripts into .tar.gz
sbt coverage                # clean → coverage → test → coverageReport
```

Run echo demos (from quic-core test sources):
```bash
sbt "jam4s-quic-core/Test/runMain org.jam4s.quic4cats.EchoServerRun"
sbt "jam4s-quic-core/Test/runMain org.jam4s.quic4cats.EchoClientRun"
```

## Architecture

Multi-module sbt build:

- **`jam4s-quic-core`** (`modules/quic-core/`) — Two-layer design:
  - `org.jam4s.quic` — Synchronous core: direct wrappers around KWIK's Java API (`QClient`, `QServer`, `QProtocolConnection`)
  - `org.jam4s.quic4cats` — Functional API: Cats Effect wrappers with `Resource`-based lifecycle, typeclass factories (`MkQClient`, `MkQServer`), log4cats logging
- **`jam4s-quic-cli`** (`modules/quic-cli/`) — CLI application using Decline (entry point: `org.jam4s.cli.Main`). Packaged as a fat JAR via sbt-assembly, bundled with shell scripts into a `.tar.gz` via the `distTarGz` task.

Dependencies are defined in `project/Dependencies.scala`. Key deps: Cats Effect, FS2, KWIK, Decline.

## Code Style

- Scala 3.4.2 syntax (new control structures, optional braces, `*` for varargs)
- scalafmt: max 120 columns, `align.preset = more`, imports sorted ASCII, `removeOptionalBraces = yes`
- scalafix: OrganizeImports with groups `[javax/java, scala, jam4s, *]`
- Do not enable `scalafmtOnCompile` (known issues with Scala 3)

## TLS Certificates

Self-signed certs for development are in `cert/` (JKS keystore, PKCS12, PEM). Default keystore alias: `"selfsigned"`, password: `"password"`. See SPEC.md for regeneration steps.
