# Project Architecture Rules (Non-Obvious Only)

## Bundle Isolation Is Enforced at Build Time
- `*.internal.*` packages are blocked from cross-bundle Import-Package by the global `defaultPackageImport` filter in `cnf/resources/bnd/bundle.props`. Any design that requires importing an internal package across bundles is architecturally invalid — you must extract an SPI package or use an OSGi service instead.

## DS Component Lifecycle
- DS components are NOT registered unless explicitly listed in `-dsannotations:` (or `Service-Component:`) in the bundle's `bnd.bnd`. This is a deliberate global opt-in; the workspace default is blank. Plans that rely on annotation scanning without this explicit registration will silently fail at runtime.
- `osgi.ds.satisfying.condition.target=(osgi.condition.id=io.openliberty.process.running)` is used on some components to delay activation until Liberty is fully running — required for components that must not start during early boot.

## Jakarta EE Transformation (Twin Bundles)
- `generate.replacement: true` in `bnd.bnd` causes Bnd to auto-generate a Jakarta-namespaced (`*.jakarta`, EE 9) and optionally EE 10/11 variant of the bundle. Any plan adding a new API bundle that needs EE 9+ support must include this flag.
- The `cnf/build.bnd` `-buildpath+:` block maps `io.openliberty.*` bundle symbolic names back to their source `com.ibm.*` projects — this is how the workspace resolves transformed bundle names.

## FAT EE-Version Repeating
- FAT suites use `RepeatTests` + `FeatureReplacementAction` to run the entire test suite multiple times with different EE feature sets (EE9/EE10/EE11). Plans that add new FAT tests must consider whether tests need `@Mode(TestMode.LITE)` vs `FULL` to avoid bucket timeouts, especially on Windows where only one FULL-mode repeat is allowed.

## FFDC Compile-Time Instrumentation
- `instrument.ffdc: true` (global default) means the Bnd RAS plugin rewrites `.class` files at compile time to inject FFDC calls at exception catch sites. Plans that add new exception handling must account for this — unexpected FFDCs in test output indicate uncaught exception paths, not test infrastructure issues.

## NLS Globalization Pipeline
- The `globalize` Gradle task re-generates the Java `*Messages` class from `.nlsprops`. Any plan that adds or renames message keys must include a `globalize` step before the changes will compile, since the generated class is what Java source imports.

## Dependency on `build.sharedResources`
- Test JARs (JUnit, jmock, etc.) are sourced from `build.sharedResources/lib/`, not from Maven Central in the normal Gradle sense. Plans introducing new test libraries must check whether they are already available there before adding a Maven coordinate to `-testpath:`.
