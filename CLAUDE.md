# CLAUDE.md


We're building the app described in @SPEC.md. Read that file for general architectural tasks or to double-check API specification, tech stack or application architecture.

Keep your replies extremely concise and focus on conveying the key information. No unnecessary fluff, no long code snippets.

Whenever working with any third-party library or something similar, you MUST look up the official documentation to ensure that you're working with up-to-date information. Use the DocsExplorer subagent for efficient documentation lookup.

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

jam4s-quic is the JAM4S QUIC project.

## Build Commands

```bash
sbt test                    # Run all tests
sbt "testOnly *SuiteName"   # Run a single test suite
sbt compile                 # Compile all modules
sbt fmt                     # Format all code (scalafmt)
sbt fmtCheck                # Check formatting without changing files
sbt lint                    # Format + organize imports (scalafmt + scalafix)
sbt assembly                # Build fat JAR for CLI module
```

Requires JDK 21.

## Architecture

Multi-module sbt build with two modules:

- **`jam4s-quic-core`** (`modules/quic-core/`) — Core with Cats Effect for effects and FS2 for streaming.
- **`jam4s-quic-cli`** (`modules/quic-cli/`) — CLI application (entry point: `org.jam4s.cli.Main`). Depends on core. Packaged as a fat JAR via sbt-assembly, bundled with shell scripts into a `.tar.gz` via the `distTarGz` task.

Dependencies are defined in `project/Dependencies.scala`.

## Code Style

- Scala 3 syntax (new control structures, optional braces, `*` for varargs)
- scalafmt: max 120 columns, `align.preset = more`, imports sorted ASCII
- scalafix: OrganizeImports with groups `[javax/java, scala, jam4s, *]`
- Do not enable `scalafmtOnCompile` (known issues with Scala 3)
