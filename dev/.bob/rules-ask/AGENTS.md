# Project Documentation Context (Non-Obvious Only)

## Codebase Layout Surprises
- `dev/` is the entire workspace root — all ~2000 subprojects live as flat peer directories directly under `dev/`. There is no `src/` at the workspace level.
- FAT projects (`*_fat`) contain test source under `fat/src/`, NOT `src/`. The `src/` directory in a FAT project does not exist or is unused.
- `test-applications/` inside a FAT project holds WAR/EJB source — it is NOT a test utility; it is the app-under-test source.
- `build.sharedResources/` holds shared JARs (e.g., JUnit) referenced via `../build.sharedResources/lib/...` in `-testpath:`.

## Build System
- `bnd.bnd` (not `build.gradle`) drives compilation, packaging, versioning, and OSGi metadata for bundle projects.
- `cnf/build.bnd` is workspace-wide bnd config; `cnf/resources/bnd/bundle.props` is the base template every bundle includes.
- `./gradlew <project>:release` copies the built JAR to `build.image/wlp` — this is what makes a change visible to a running Liberty server. `:build` alone does NOT deploy.

## NLS / Translations
- The English `.nlsprops` (no locale suffix) is the authoritative source. All other `*_fr.nlsprops`, `*_de.nlsprops`, etc. are generated/managed by IBM translation tooling — do not manually edit translated files.
- Two separate `.nlsprops` families often exist per project: `*Messages.nlsprops` (runtime messages with `CWWK` IDs) and `*Options.nlsprops` (CLI help text, no message IDs required).

## OSGi Service Visibility
- A service not appearing at runtime almost always means either: (a) `-dsannotations:` in `bnd.bnd` is missing the component class, or (b) the Export-Package / provide interface is wrong. The bundle will still start — OSGi just won't register the component silently.

## FFDC (First Failure Data Capture)
- FFDC is instrumented at compile time by `com.ibm.ws.ras.instrument` when `instrument.ffdc: true` (global default). The `.class` files in the repo already have FFDC calls injected — they are not visible in source.
