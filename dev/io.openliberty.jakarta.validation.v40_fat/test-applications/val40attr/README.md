# Jakarta Validation 4.0 — `ConstraintDescriptor#getAttribute(String, Class<V>)`

## Overview

Jakarta Validation 4.0 adds a new typed method to the `ConstraintDescriptor` interface:

```java
<V> V getAttribute(String attributeName, Class<V> type);
```

This eliminates the need for unchecked casts when reading constraint attributes
programmatically at runtime. It is a purely additive API change — no existing code
is broken.

---

## Background

Every constraint annotation has **attributes** — the values declared on the annotation
itself. For example, `@MinStock(min = 5)` has attributes `min`, `message`, `groups`, and
`payload`. The `ConstraintDescriptor` interface exposes these through:

```java
Map<String, Object> getAttributes();   // available since Validation 1.0
```

Accessing a specific attribute required a lookup plus an **unchecked cast**:

```java
// Pre-4.0 — raw map, unchecked cast, ClassCastException risk
int min = (int) descriptor.getAttributes().get("min");
```

If the attribute type doesn't match the cast, you get a `ClassCastException` at
runtime with no compiler or IDE help. Validation 4.0 closes this gap.

---

## What Changed in 4.0

### New method on `ConstraintDescriptor`

```java
// Jakarta Validation 3.x interface (no getAttribute)
public interface ConstraintDescriptor<T extends Annotation> {
    Map<String, Object> getAttributes();
    // ... other methods
}

// Jakarta Validation 4.0 — new typed accessor added
public interface ConstraintDescriptor<T extends Annotation> {
    Map<String, Object> getAttributes();             // still present
    <V> V getAttribute(String attributeName, Class<V> type);  // NEW in 4.0
    // ... other methods
}
```

### Usage comparison

```java
// ❌ Pre-4.0 — verbose, type-unsafe
int min    = (int)   descriptor.getAttributes().get("min");
String msg = (String) descriptor.getAttributes().get("message");

// ✅ Validation 4.0 — concise, type-safe, no cast
int min    = descriptor.getAttribute("min",     int.class);
String msg = descriptor.getAttribute("message", String.class);
```

---

## Sample Application

The `val40attr` test application in `io.openliberty.jakarta.validation.v40_fat`
demonstrates this feature end-to-end on Liberty.

### Project structure

```
test-applications/val40attr/
├── resources/WEB-INF/
│   └── beans.xml                          ← CDI bean-discovery-mode="all"
└── src/val40attr/web/
    ├── MinStock.java                      ← custom @MinStock(min=5) constraint
    ├── MinStockValidator.java             ← enforces the min threshold
    ├── Product.java                       ← bean with @MinStock(min=5) on stock field
    └── ConstraintDescriptorTestServlet.java  ← 6 FAT tests
```

### The constraint annotation — `MinStock.java`

```java
@Constraint(validatedBy = { MinStockValidator.class })
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface MinStock {

    int min() default 1;                        // ← the attribute we read back

    String message() default "stock must be at least {min}";

    Class<?>[] groups()                default {};
    Class<? extends Payload>[] payload() default {};
}
```

### The validated bean — `Product.java`

```java
public class Product {

    @MinStock(min = 5)   // ← min attribute declared as 5
    private final int stock;

    // ...
}
```

### Reading the attribute — `ConstraintDescriptorTestServlet.java`

```java
BeanDescriptor      beanDesc       = validator.getConstraintsForClass(Product.class);
PropertyDescriptor  propDesc       = beanDesc.getConstraintsForProperty("stock");
ConstraintDescriptor<?> descriptor = propDesc.getConstraintDescriptors().iterator().next();

// ✅ Validation 4.0 — type-safe, no cast
int min    = descriptor.getAttribute("min",     int.class);   // returns 5
String msg = descriptor.getAttribute("message", String.class); // returns "stock must be at least {min}"

// Pre-4.0 style — still works, but requires unchecked cast
int minLegacy = (int) descriptor.getAttributes().get("min");  // also returns 5
```

---

## Test Cases

| Test | What it verifies |
|---|---|
| `testValidProductProducesNoViolations` | Baseline — stock ≥ min produces no violations |
| `testLowStockProducesViolation` | Baseline — stock < min fires `@MinStock` correctly |
| `testGetAttributeReturnsTypedMinValue` | Core feature — `getAttribute("min", int.class)` returns `5` |
| `testGetAttributeReturnsMessageTemplate` | `getAttribute("message", String.class)` returns the template |
| `testGetAttributeMatchesGetAttributesMap` | Old and new API return identical values |
| `testGetAttributeWrongTypeThrows` | Wrong type → exception (fail-fast, no silent corruption) |

---

## Impact on Open Liberty

| Area | Impact |
|---|---|
| **Open Liberty internal source** | ✅ None — no Liberty runtime code calls `getAttribute` today |
| **Spec API bundle** | 🔧 Must ship `jakarta.validation-api:4.0.x` which adds the new method |
| **Hibernate Validator** | 🔧 Bundled provider must implement `getAttribute` on its `ConstraintDescriptorImpl` |
| **Customer applications** | ✅ Purely additive — existing code is unaffected; opt in at will |
| **FAT test suite** | ✅ Covered by `val40attr` app in `io.openliberty.jakarta.validation.v40_fat` |

---

## Build and Run

### Compile check

```bash
cd dev
./gradlew io.openliberty.jakarta.validation.v40_fat:compileJava --rerun-tasks
```

### Run the full FAT suite

```bash
./gradlew io.openliberty.jakarta.validation.v40_fat:buildandrun
```

Both the `val40` app (deprecation feature) and the `val40attr` app (`getAttribute`
feature) run in the same Liberty server instance during a single FAT execution.

---

## References

- [Jakarta Validation 4.0 specification](https://jakarta.ee/specifications/bean-validation/)
- [`ConstraintDescriptor` Javadoc](https://jakarta.ee/specifications/bean-validation/4.0/apidocs/jakarta/validation/metadata/constraintdescriptor)
- `jakarta.validation:jakarta.validation-api:4.0.0-M1` on Maven Central
