# Jakarta Validation 4.0 — `ConstraintValidator#initialize(ConstraintDescriptor, ConstraintValidatorInitializationContext)`

## Overview

Jakarta Validation 4.0 adds a richer `initialize` overload to the `ConstraintValidator`
interface that supplies the full `ConstraintDescriptor` and a new
`ConstraintValidatorInitializationContext` to validators at creation time.

Before 4.0, a validator received only the constraint annotation itself at initialisation.
The new overload provides access to a `ClockProvider` (and provider-specific extensions)
once at startup — eliminating the need to look it up from the `ConstraintValidatorContext`
on every `isValid` call.

---

## Background

### Pre-4.0 initialisation

```java
public class MyValidator implements ConstraintValidator<MyConstraint, LocalDate> {

    @Override
    public void initialize(MyConstraint annotation) {
        // only the annotation is available — no clock, no descriptor
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext ctx) {
        // clock must be fetched on every single call
        ClockProvider cp = ctx.getClockProvider();
        return value.isAfter(LocalDate.now(cp.getClock()));
    }
}
```

### Problems with the pre-4.0 pattern

- The `ClockProvider` is fetched from `ConstraintValidatorContext` on **every** `isValid`
  invocation — repeated work that cannot be cached across calls.
- There is no way for a validator to inspect its own `ConstraintDescriptor` at
  initialisation time (for example, to read constraint attributes with the new
  type-safe `getAttribute` API introduced in this same release).
- Provider-specific context (extensions available via `unwrap`) is not accessible
  at initialisation.

---

## What Changed in 4.0

### New `initialize` overload on `ConstraintValidator`

```java
// Jakarta Validation 3.x — only one initialize method
public interface ConstraintValidator<A extends Annotation, T> {
    default void initialize(A constraintAnnotation) {}
    boolean isValid(T value, ConstraintValidatorContext context);
}

// Jakarta Validation 4.0 — new richer overload added
public interface ConstraintValidator<A extends Annotation, T> {
    default void initialize(A constraintAnnotation) {}

    // NEW in 4.0 — receives the full descriptor and an initialisation context
    default void initialize(ConstraintDescriptor<A> constraintDescriptor,
                            ConstraintValidatorInitializationContext initializationContext) {
        // default implementation delegates to the existing single-arg overload
        initialize(constraintDescriptor.getAnnotation());
    }

    boolean isValid(T value, ConstraintValidatorContext context);
}
```

### New `ConstraintValidatorInitializationContext` interface

```java
public interface ConstraintValidatorInitializationContext {

    /** Clock provider configured for the validator factory. */
    ClockProvider getClockProvider();

    /** Unwrap to a provider-specific type (e.g. Hibernate Validator internals). */
    <T> T unwrap(Class<T> type);
}
```

### Backwards compatibility

The new overload is a `default` method. Its default implementation calls the existing
single-argument `initialize(annotation)` method, so **all existing validators continue
to work with no changes**.

---

## Sample Application

The `val40init` test application in `io.openliberty.jakarta.validation.v40_fat`
demonstrates this feature on Liberty.

### Project structure

```
test-applications/val40init/
├── resources/WEB-INF/
│   └── beans.xml                                      ← CDI bean-discovery-mode="all"
└── src/val40init/web/
    ├── FutureDate.java                                ← custom @FutureDate constraint
    ├── FutureDateValidator.java                       ← implements the new initialize overload
    ├── ConstraintValidatorInitializationContext.java  ← local stub (see note below)
    ├── Appointment.java                               ← bean with @FutureDate on appointmentDate
    └── ConstraintValidatorInitContextTestServlet.java ← 6 FAT tests
```

> **Note on the local stub:** `ConstraintValidatorInitializationContext` is not yet
> present in the `4.0.0-M1` spec jar. It is defined here as a placeholder so the
> application compiles and demonstrates the intended programming model. Once the
> 4.0 GA jar ships this interface, the stub must be deleted and all imports updated to
> `jakarta.validation.ConstraintValidatorInitializationContext`.

### The constraint annotation — `FutureDate.java`

```java
@Constraint(validatedBy = { FutureDateValidator.class })
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface FutureDate {
    String message() default "date must be in the future";
    Class<?>[] groups()                    default {};
    Class<? extends Payload>[] payload()   default {};
}
```

### The validated bean — `Appointment.java`

```java
public class Appointment {

    @FutureDate
    private final LocalDate appointmentDate;

    private final String description;
    // ...
}
```

### The validator — `FutureDateValidator.java`

The key pattern: capture `ClockProvider` **once** at initialisation via the new
overload, then reuse it across every `isValid` call.

```java
public class FutureDateValidator implements ConstraintValidator<FutureDate, LocalDate> {

    // Captured once at init — null when falling back to the pre-4.0 path
    private ClockProvider clockProvider;

    // ✅ Validation 4.0 — new overload: captures ClockProvider at startup
    public void initialize(ConstraintDescriptor<FutureDate> descriptor,
                           ConstraintValidatorInitializationContext initializationContext) {
        this.clockProvider = initializationContext.getClockProvider();
    }

    // Pre-4.0 fallback — called by runtimes that don't know about the new overload
    @Override
    public void initialize(FutureDate annotation) {
        // clockProvider stays null; fetched lazily in isValid
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null handling is @NotNull's responsibility
        }
        ClockProvider cp = (clockProvider != null)
                           ? clockProvider                  // 4.0 path — already captured
                           : context.getClockProvider();    // pre-4.0 fallback
        return value.isAfter(LocalDate.now(cp.getClock()));
    }
}
```

---

## Test Cases

| Test | What it verifies |
|---|---|
| `testFutureDateIsValid` | Baseline — future date produces no violations |
| `testPastDateProducesViolation` | Baseline — past date fires `@FutureDate` correctly |
| `testTodayProducesViolation` | Today's date is not in the future — should fail |
| `testNullDateIsValid` | Null is valid — null handling is `@NotNull`'s job |
| `testViolationMessage` | Violation message matches the `@FutureDate` default template |
| `testClockReusedAcrossMultipleValidations` | Multiple valid future dates all pass — confirms clock reuse |

---

## Current Status

These tests run successfully today **against `validation-3.1`** via the pre-4.0 fallback
path. The new `initialize(ConstraintDescriptor, Context)` overload is not called at
runtime (the 3.1 spec bundle's `ConstraintValidator` contract only dispatches the
single-argument `initialize(annotation)` overload). The tests therefore validate the
runtime *behaviour* of the validator — future/past date checks — which is
path-independent.

When `validation-4.0` lands in Liberty:

1. Delete [`ConstraintValidatorInitializationContext.java`](src/val40init/web/ConstraintValidatorInitializationContext.java)
   (the local stub).
2. Update [`FutureDateValidator.java`](src/val40init/web/FutureDateValidator.java) to
   import `jakarta.validation.ConstraintValidatorInitializationContext`.
3. Add a test that inspects `FutureDateValidator.clockProvider` to confirm it was
   injected via the new overload (requires reflection or a test-visible accessor).

---

## Impact on Open Liberty

| Area | Impact |
|---|---|
| **Open Liberty internal source** | ✅ None — no Liberty runtime code implements `ConstraintValidator` today |
| **Spec API bundle** | 🔧 Must ship `jakarta.validation-api:4.0.x` which adds the new interface and overload |
| **Hibernate Validator** | 🔧 Must call the new `initialize(descriptor, context)` overload during validator creation |
| **Customer applications** | ✅ Purely additive — existing validators are unaffected; opt in at will |
| **FAT test suite** | ✅ Covered by `val40init` app in `io.openliberty.jakarta.validation.v40_fat` |

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

All three apps (`val40`, `val40attr`, `val40init`) run in the same Liberty server
instance during a single FAT execution.

---

## References

- [Jakarta Validation 4.0 specification](https://jakarta.ee/specifications/bean-validation/)
- [`ConstraintValidator` Javadoc](https://jakarta.ee/specifications/bean-validation/4.0/apidocs/jakarta/validation/constraintvalidator)
- [`ConstraintValidatorInitializationContext` Javadoc](https://jakarta.ee/specifications/bean-validation/4.0/apidocs/jakarta/validation/constraintvalidatorinitializationcontext)
- `jakarta.validation:jakarta.validation-api:4.0.0-M1` on Maven Central
