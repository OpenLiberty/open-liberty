# Project Coding Rules (Non-Obvious Only)

## Dependency Declaration
- **Never** use `build.gradle` `dependencies {}` for bundle source code — ALL compile deps go in `bnd.bnd` under `-buildpath:`.
- Workspace-local projects: `;version=latest`. External Maven: `;version=x.y.z` or `;strategy=exact;version=x.y.z`.
- Test-only deps go under `-testpath:` in `bnd.bnd`, not `-buildpath:`.

## DS / OSGi Component Registration
- `-dsannotations:` is **blank by default** (`cnf/resources/bnd/bundle.props`). You must explicitly list every `@Component`-annotated class in the bundle's `bnd.bnd` or the component will silently never register.
- Alternative: declare `Service-Component:` inline in `bnd.bnd` (used by some older bundles like `com.ibm.ws.security.token.ltpa`).
- `*.internal.*` packages are auto-blocked from cross-bundle Import-Package — never reference them from another project.

## Logging
- Always guard trace calls: `if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())`.
- `@TraceOptions(traceGroup=..., messageBundle=...)` goes on `package-info.java`, NOT on the class.
- `instrument.ffdc: true` (global default in `cnf/build.bnd`) auto-injects FFDC at compile time. Suppress with `@FFDCIgnore` on the method.
- `instrument.disabled: true` in a project's `bnd.bnd` disables FFDC injection entirely (used for utility/tool bundles).

## NLS
- Only edit the no-suffix English `.nlsprops` file; translated variants are managed externally.
- After editing `.nlsprops`, run `./gradlew <project>:globalize` to regenerate the Java message class.
- Single quotes in message strings must be doubled (`''`) when `#NLS_MESSAGEFORMAT_VAR` is active.

## FAT Projects
- FAT source lives in `fat/src/`, NOT `src/`. Test app (WAR/EJB) sources go in `test-applications/<app>/src/`.
- Include `AlwaysPassesTest.class` in every `@SuiteClasses` — CI fails if 0 tests run.
- Use `LibertyServerFactory.getLibertyServer("serverName")` to obtain server instances; server name must match a directory under `publish/servers/`.
- `@AllowedFFDC("com.example.SomeException")` on a test class/method suppresses unexpected FFDC failures.

## Jakarta EE Multi-Version Testing
- Wrap repeats in `FATSuite` via `RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT()).andWith(EE9_FEATURES()).andWith(EE10_FEATURES()).andWith(EE11_FEATURES())`.
- Use `JakartaEEAction.isEE9Active()` / `isEE10Active()` inside tests to branch on current EE level.

## Code Style
- Target Java 8 (`javac.source: 1.8`) unless project `bnd.bnd` overrides — do not use lambdas requiring Java 11+ APIs unless the project opts up.
- `-Xdoclint:none` is globally set; Javadoc HTML errors are intentionally ignored.
