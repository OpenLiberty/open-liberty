# Plan Mode Architecture Rules

Non-obvious architectural constraints for Open Liberty.

## OSGi Bundle Boundaries Are Hard

- Internal packages (`*.internal.*`) are invisible across bundle boundaries at runtime — cross-bundle coupling must go through exported API or SPI packages.
- Services are wired via Declarative Services (DS); there is **no Spring/CDI DI** in the kernel. DS component lifecycle methods are `activate()`/`deactivate()` / `modified()`.
- A bundle change requires `./gradlew <project>:release` to push to `build.image/wlp` — code changes alone do nothing until released.

## Build Parallelism Requires Explicit `-dependson`

- Bnd resolves inter-project build order from `-buildpath:` entries in `bnd.bnd`, but if a project is needed at build-time without being on the classpath, it must be listed in `-dependson:`.
- Gradle parallel builds pre-initialize indexed Maven repos at settings evaluation time (see `wlp-gradle/bndSettings.gradle`) to avoid race conditions — do not add new indexed repos without pre-init.

## FAT Architecture

- FAT buckets (`*_fat`) are completely independent — they package themselves as `autoFVT.zip` and run via Ant against a full Liberty image.
- FATs discover the features they test by scanning `publish/servers/**/server.xml` for `<feature>` elements — the `tested.features` property in `bnd.bnd` supplements but does not replace this scan.
- The `trySkipFat` Gradle task (enabled with `-Dgit_diff=<diff-file>`) can skip unchanged FAT buckets using the change detector tool in `build.changeDetector/`.
- Multi-suite FATs use sub-bnd files with `suite.name` property to produce multiple `autoFVT*.zip` artifacts from one project.

## Jakarta EE Transformation Pipeline

- EE 9+ support is achieved by running the Eclipse Transformer on existing EE 8 bundles at build time.
- To add EE 9+ support to a bundle: create a `transformed.bnd` in the project and add `generate.replacement: true`.
- Rules files for the transformer live in `wlp-jakartaee-transform/rules/`.
- `cnf/build.bnd` lists the `io.openliberty.*` → `com.ibm.ws.*` project mappings for bundles that changed namespace.

## Version Numbering

- Bundle version = `bVersion` (major.minor) + `libertyBundleMicroVersion` (micro) + build qualifier.
- `bVersion` in `bnd.bnd` must be bumped when adding new exported API (breaking = major, additive = minor).
- `-baselinerepo: Release` means bnd checks new exports against the last released version and will error on illegal API changes.
