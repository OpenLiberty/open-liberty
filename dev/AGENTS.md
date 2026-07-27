# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Project Overview

Open Liberty is a large OSGi-based Java application server. The `dev/` directory contains ~2000+ subprojects, each an independent OSGi bundle or FAT (Functional Acceptance Test) bucket. Build is driven by **Gradle + Bnd** (biz.aQute.bnd). All commands below are run from `dev/`.

## Build Commands

```bash
# Build a single bundle project (run from dev/)
./gradlew <project-name>:build          # e.g. ./gradlew com.ibm.ws.kernel.boot:build

# Release a bundle to build.image/wlp (what most developers want)
./gradlew <project-name>:release

# Run unit tests for a bundle
./gradlew <project-name>:test

# Build & run a FAT bucket locally
./gradlew <fat-project>:buildandrun     # e.g. ./gradlew com.ibm.ws.kernel.boot_fat:buildandrun

# Build all (slow – resolves the entire workspace)
./gradlew :build
```

## Running a Single FAT Test or Test Method

FAT tests run via **Ant** against a pre-built `autoFVT` directory. After `buildfat`, run:

```bash
# Run an entire test class
ant -f <fat-project>/build/autoFVT/TestBuild.xml -Dtest=com.ibm.ws.kernel.boot.ServerStartTest

# Run a single test method
ant -f <fat-project>/build/autoFVT/TestBuild.xml \
    -Dtest=com.ibm.ws.kernel.boot.ServerStartTest -Dmethod=testServerStart
```

For unit tests in a regular bundle project, use Gradle's standard `--tests` flag:
```bash
./gradlew <project-name>:test --tests "com.ibm.ws.kernel.boot.SomeTest.methodName"
```

## Project Structure Rules

- **Bundle projects** (`com.ibm.ws.*`, `io.openliberty.*`): contain `bnd.bnd`, `src/`, `test/`, `resources/`
- **FAT projects** (`*_fat`): contain `fat/src/` (tests), `publish/servers/` (server configs), `test-applications/` (WARs/EJBs). FAT source lives in `fat/src`, NOT `src/`.
- **`bnd.bnd`** drives everything: `fat.project: true` marks a FAT; `-buildpath` replaces Maven deps; `version=latest` resolves from workspace.
- Every `bnd.bnd` starts with `-include= ~../cnf/resources/bnd/bundle.props` (common props).
- `cnf/build.bnd` is the workspace-wide bnd config — edit carefully as it affects all projects.

## OSGi / DS Patterns (Non-Obvious)

- **`-dsannotations:` and `-metatypeannotations:` are BLANK by default** (`cnf/resources/bnd/bundle.props`): you must explicitly list annotated classes in each `bnd.bnd` or DS components won't register.
- **`defaultPackageImport`** in `bundle.props` blocks `*.internal.*` imports automatically — internal packages must never be imported across bundles.
- Import-Package uses `!*.internal.*, !*.internal, !com.ibm.ws.kernel.boot.cmdline, *` as default.
- OSGi bundle is released with `./gradlew <project>:release`, which copies JARs to `build.image/wlp`.

## Logging & NLS

- **Trace logging**: `private static final TraceComponent tc = Tr.register(MyClass.class);` — always guard calls with `TraceComponent.isAnyTracingEnabled() && tc.isXxxEnabled()`.
- **Trace group / message bundle** are declared via `@TraceOptions` on `package-info.java`, not on individual classes.
- **NLS messages** live in `resources/**/*.nlsprops` (English, no suffix). Translated variants are `*_fr.nlsprops` etc. The `globalize` Gradle task compiles them into Java classes. Each message key gets `.explanation` and `.useraction` suffixes — all three are required.
- Message IDs use the format `CWWKEnnnnL` where L is `I/W/E` (info/warning/error).
- **RAS instrumentation** (`instrument.ffdc: true` in `build.bnd`) auto-injects FFDC at compile time. Use `@FFDCIgnore` to suppress on expected exceptions.

## Code Style

- Target source/binary compatibility: **Java 8** (`javac.source: 1.8`, `javac.target: 1.8` in `cnf/build.bnd`). Some newer bundles use Java 11 or 21 — check the project's `bnd.bnd`.
- Every source file must have the EPL-2.0 copyright header (see any existing file for the exact format).
- `options.release` is set automatically by Gradle to match `javac.source` — do not set it manually in `bnd.bnd`.
- `-Xdoclint:none` is set globally; Javadoc HTML errors are intentionally ignored.
- Internal packages (`*.internal.*`) are not exported and cannot be imported across bundles.

## FAT Test Conventions

- FAT test classes use `@RunWith(FATRunner.class)` (not plain JUnit4 runner).
- Each FAT has a `FATSuite.java` with `@RunWith(Suite.class)` + `@SuiteClasses({...})`.
- Liberty servers for tests are configured in `publish/servers/<serverName>/server.xml`.
- Test applications (WARs/EJBs) source is in `test-applications/<appName>/src/`.
- `LibertyServer` and `LibertyServerFactory` (from `fattest.simplicity`) are the API for server lifecycle in tests.
- `ShrinkHelper` (from `com.ibm.websphere.simplicity`) builds WARs programmatically.
- `fat.test.localrun=true` is set automatically during `runfat` — FATs detect this to behave differently than CI.

## Dependency Management

- Dependencies are declared in `bnd.bnd` under `-buildpath:`, always with `;version=latest` for workspace projects.
- Maven/external deps use `group:artifact;version=x.y.z` syntax (resolved via `cnf/oss_*.maven` index files).
- No `pom.xml` or `package.json` — Bnd's Maven BndRepository handles external resolution.
