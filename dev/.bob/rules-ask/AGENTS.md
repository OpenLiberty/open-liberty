# Ask Mode Documentation Rules

Non-obvious context for reading and explaining this Open Liberty codebase.

## Counterintuitive Structure

- **`fat/src/`** (not `src/`) holds FAT test source code. `src/` in a FAT project typically doesn't exist.
- **`test/`** in a bundle project is unit tests; FAT integration tests are in a separate `*_fat` sibling project.
- **`publish/`** in a FAT project contains server configs (`servers/`, `files/`, `shared/`) — not publication metadata.
- **`resources/`** in a bundle project is NLS source (`.nlsprops` files) compiled to `.class` by the `globalize` task — not static resources bundled into the JAR as-is.
- `wlp-gradle/subprojects/` contains Gradle scripts applied to all bnd projects — not subproject source code.

## Two Parallel Namespaces

- `com.ibm.ws.*` = older IBM namespace; `io.openliberty.*` = newer open namespace.
- Many `io.openliberty.*` bundles are **Jakarta EE 9+ transformations** of `com.ibm.ws.*` originals, generated at build time by the Eclipse Transformer. The source of truth is the `com.ibm.ws.*` project — look there first.

## build.image/wlp Is the Runtime

- `build.image/wlp/` is the assembled Liberty runtime image. `./gradlew <project>:release` installs a bundle's JAR there.
- FAT tests need a built `build.image/wlp` before they can run — `buildfat` depends on this image.

## cnf/ Is the Workspace Configuration

- `cnf/build.bnd` = workspace-wide defaults for all bnd projects (Java version, instrumentation, etc.)
- `cnf/resources/bnd/bundle.props` = common bnd properties included by every bundle.
- `cnf/oss_dependencies.maven` and `cnf/oss_ibm.maven` = the Maven index files that control external dependency resolution (no internet access at build time by default).

## Logging Output Location

- `Tr.warning/error/audit` → `<server>/logs/messages.log`
- `Tr.debug/entry/exit/event` → `<server>/logs/trace.log` (only when trace is enabled)
- FFDC incident files → `<server>/logs/ffdc/`
