# Jakarta Validation 4.0 — ConstraintViolationBuilder Deprecation

## Overview

Jakarta Validation 4.0 (Jakarta EE 12) formally deprecates the `addNode(String name)` method
in `ConstraintViolationBuilder` and its nested interfaces by adding the Java annotation:

```java
@Deprecated(since = "1.1", forRemoval = true)
```

Prior to 4.0, the method carried only a Javadoc `@deprecated` text comment since
Bean Validation 1.1 (~2013). That comment was invisible to compilers and IDEs.
The 4.0 change makes the deprecation **machine-enforceable**.

---

## Background

### The old API (deprecated since 1.1, but only in Javadoc)

```java
// Bean Validation 1.0 — original, now deprecated
context.buildConstraintViolationWithTemplate("invalid")
       .addNode("fieldName")           // ← only Javadoc @deprecated, no compiler signal
       .addConstraintViolation();
```

### The modern replacements (introduced in 1.1)

| Old method | Modern replacement | Use case |
|---|---|---|
| `addNode(String name)` | `addPropertyNode(String name)` | Named property on a bean |
| `addNode(null)` / class-level | `addBeanNode()` | Class-level (bean) node |
| — | `addParameterNode(int index)` | Cross-parameter method argument |

---

## What Changed in 4.0

### Before (Validation 3.x) — Javadoc only, no enforcement

```java
/**
 * @deprecated since 1.1 - replaced by {@link #addPropertyNode(String)},
 *             {@link #addBeanNode()} and {@link #addParameterNode(int)}
 */
NodeBuilderDefinedContext addNode(String name);
```

- Compilers: **silent** — no warning emitted
- IDEs: **no strikethrough** (or inconsistent, depending on IDE settings)
- Tooling: **nothing** enforced

### After (Validation 4.0) — Machine-enforceable

```java
/**
 * @deprecated since 1.1 - replaced by {@link #addPropertyNode(String)},
 *             {@link #addBeanNode()} and {@link #addParameterNode(int)}
 */
@Deprecated(since = "1.1", forRemoval = true)
NodeBuilderDefinedContext addNode(String name);
```

- Compilers: emit **`-Xlint:removal` warning** at every call site
- IDEs: render the call with a **strikethrough** as soon as the 4.0 jar is indexed
- `forRemoval = true`: signals the method **will be removed** in a future version

### Affected interfaces

The annotation is applied to `addNode(String name)` in all four builder interfaces:

1. `ConstraintValidatorContext.ConstraintViolationBuilder`
2. `ConstraintViolationBuilder.NodeBuilderDefinedContext`
3. `ConstraintViolationBuilder.NodeBuilderCustomizableContext`
4. `ConstraintViolationBuilder.NodeContextBuilder`

---

## Key Distinction

> **Deprecated ≠ Removed.**
>
> The method still executes correctly at runtime in Validation 4.0.
> `forRemoval = true` is a warning that removal is planned in a **future** version.
> Application developers should migrate now; their existing deployed apps keep working.

---

## Impact on Open Liberty

| Area | Impact |
|---|---|
| **Open Liberty internal source** | ✅ None — no Liberty runtime code calls `addNode()` |
| **New API bundle** | 🔧 A `validation.4.0.bnd` must reference `jakarta.validation-api:4.0.x` |
| **Hibernate Validator** | 🔧 The bundled provider must be updated to a 4.0-compatible version |
| **`validation-4.0` feature** | 🔧 Full feature wiring needed once the above are in place |
| **Customer applications** | ⚠️ Build-time `[removal]` warnings if they call `addNode()` — runtime unaffected |
| **FAT test suite** | ✅ Created — `io.openliberty.jakarta.validation.v40_fat` |

---

## Sample Application

The FAT project `io.openliberty.jakarta.validation.v40_fat` demonstrates the feature.

### Key file: `OrderFormValidator.java`

```java
// ❌ DEPRECATED — addNode(String)
// @Deprecated(since = "1.1", forRemoval = true) in Validation 4.0
// IDE shows strikethrough. Compiler emits [removal] warning.
context.buildConstraintViolationWithTemplate("username must not be blank")
       .addNode("username")           // ← strikethrough in IDE, [removal] warning in compiler
       .addConstraintViolation();

// ✅ MODERN REPLACEMENT — addPropertyNode(String)  [since 1.1]
// No warning, no strikethrough.
context.buildConstraintViolationWithTemplate("quantity must be greater than zero")
       .addPropertyNode("quantity")   // ← clean, no warning
       .addConstraintViolation();
```

### Project structure

```
io.openliberty.jakarta.validation.v40_fat/
├── bnd.bnd                               ← pulls jakarta.validation-api:4.0.0-M1
├── build.gradle                          ← enables -Xlint:removal compiler flag
├── fat/src/.../v40/fat/
│   ├── FATSuite.java
│   └── Validation40Test.java             ← boots Liberty, runs servlet tests
├── publish/servers/validation.v40.fat/
│   ├── server.xml                        ← validation-4.0 feature
│   └── bootstrap.properties
└── test-applications/val40/src/val40/web/
    ├── OrderForm.java                    ← bean being validated
    ├── ValidOrder.java                   ← @ValidOrder constraint annotation
    ├── OrderFormValidator.java           ← validator with deprecated + modern API
    └── Validation40TestServlet.java      ← 4 runtime tests
```

### Test cases

| Test | What it verifies |
|---|---|
| `testValidOrder` | Valid input → no violations |
| `testDeprecatedAddNodeStillProducesViolation` | `addNode()` still works at runtime in 4.0 |
| `testModernAddPropertyNodeProducesViolation` | `addPropertyNode()` works — the correct replacement |
| `testBothViolationsReported` | Both code paths triggered together |

---

## Build and Run

### Prerequisites

```bash
export JAVA_HOME=/path/to/java21
export JAVA_21_HOME=/path/to/java21
```

### One-time setup

```bash
cd dev

# Seed the 4.0.0-M1 jar into the bnd release repo
mkdir -p cnf/release/jakarta/validation/jakarta.validation-api/4.0.0-M1
cp ~/.m2/repository/jakarta/validation/jakarta.validation-api/4.0.0-M1/jakarta.validation-api-4.0.0-M1.jar \
   cnf/release/jakarta/validation/jakarta.validation-api/4.0.0-M1/

# Initialize the workspace
./gradlew cnf:initialize
```

### Compile and see the deprecation warning

```bash
./gradlew io.openliberty.jakarta.validation.v40_fat:compileJava --rerun-tasks
```

Expected output:

```
warning: [removal] addNode(String) in ConstraintViolationBuilder has been
deprecated and marked for removal
               .addNode("username")          // <-- deprecated in Validation 4.0
               ^
1 warning
```

### Run the FAT tests (requires a full Liberty build)

```bash
./gradlew io.openliberty.jakarta.validation.v40_fat:buildandrun
```

### IDE strikethrough (no build needed)

Open `OrderFormValidator.java` in IntelliJ IDEA or Eclipse after the workspace is
initialized. The `.addNode("username")` call renders with a **strikethrough** immediately
because the 4.0.0-M1 jar on the bnd classpath carries `@Deprecated(since="1.1", forRemoval=true)`.

In IntelliJ, hovering over the call shows:

> `addNode(String name)` is deprecated and marked for removal since version 1.1

---

## References

- [Jakarta Validation 4.0 issue tracker](https://github.com/jakartaee/validation/issues)
- [JEP 277 — Enhanced Deprecation (Java 9)](https://openjdk.org/jeps/277)
- `jakarta.validation:jakarta.validation-api:4.0.0-M1` on Maven Central
