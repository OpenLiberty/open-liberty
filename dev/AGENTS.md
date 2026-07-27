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

## bnd.bnd Is the Source of Truth

- Never add Java dependencies via `build.gradle` `dependencies {}` for bundle sources — use `-buildpath:` in `bnd.bnd` exclusively.
- `version=latest` is mandatory for workspace-local projects; numeric versions are only for external Maven artifacts.
- DS component registration requires explicit opt-in: add `-dsannotations: com.ibm.ws.mypkg.MyComponent` in `bnd.bnd` — the global default leaves it blank.
- Alternatively, DS components may be declared inline with `Service-Component:` in `bnd.bnd` (see `com.ibm.ws.security.token.ltpa/bnd.bnd` for an example).
- When creating a new bundle, `bnd.bnd` must start with `-include= ~../cnf/resources/bnd/bundle.props` and set `bVersion`.

## Logging Pattern (Mandatory)

```java
// In class:
private static final TraceComponent tc = Tr.register(MyClass.class);

// Always guard with double-check:
if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
    Tr.debug(tc, "message");
}
```

- Use `Tr.warning`, `Tr.error`, `Tr.audit` for messages that appear in `messages.log` — these require NLS keys.
- `@TraceOptions` goes on `package-info.java`, not on the class itself.
- Use `@FFDCIgnore(ExceptionType.class)` on methods where FFDC auto-injection should be suppressed.

## NLS Messages

- English source: `resources/com/ibm/ws/<pkg>/resources/MyMessages.nlsprops`
- Each key requires three entries: `KEY=CWWKEnnnnL: text`, `KEY.explanation=...`, `KEY.useraction=...`
- Run `./gradlew <project>:globalize` to regenerate Java message classes after editing `.nlsprops`.
- Variables use `{0}`, `{1}` — single quotes must be doubled (`''`) if `#NLS_MESSAGEFORMAT_VAR` is set.

## OSGi Gotchas

- `*.internal.*` packages are implicitly blocked from cross-bundle imports — never import them from another project.
- If a service is not found at runtime, check that `-dsannotations:` lists the component class AND `bnd.bnd` exports/provides the service interface.
- `generate.replacement: true` in `bnd.bnd` means a Jakarta-transformed twin bundle is generated — required for EE 9+ variants.

## FAT Test Apps

- WAR/EJB sources go in `test-applications/<name>/src/`, not `fat/src/`.
- Server XML configs belong in `publish/servers/<serverName>/server.xml`.
- Use `ShrinkHelper.buildDefaultApp(server, appName, "com.example.pkg")` — do not build WARs with plain ShrinkWrap without `ShrinkHelper`.
- `@MinimumJavaLevel(javaLevel = 11)` is available as an annotation to skip tests on older JVMs.
- FAT test classes use `@RunWith(FATRunner.class)`; suites use `@RunWith(Suite.class)` + `@SuiteClasses`.
- `AlwaysPassesTest.class` must be included in every `@SuiteClasses` so CI doesn't fail if all tests are filtered out.
- `RepeatTests` with `FeatureReplacementAction.EE9_FEATURES()` / `EE10_FEATURES()` / `EE11_FEATURES()` drives multi-EE repeat runs — add to `FATSuite` to test across Jakarta EE versions.

## Copyright Header

Every new Java file must begin with:
```
/*******************************************************************************
 * Copyright (c) <year> IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
```
