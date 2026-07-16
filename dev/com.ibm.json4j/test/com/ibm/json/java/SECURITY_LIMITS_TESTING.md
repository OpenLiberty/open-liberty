# Security Limits Testing Documentation

## Overview

The JSON4J security limits are tested through two separate test classes that run in isolation from each other:

1. **SecurityLimitsDefaultTest** - Tests with default configuration values
2. **SecurityLimitsCustomPropertiesTest** - Tests with custom (small) configuration values

## Test Structure

### SecurityLimitsDefaultTest

This test class verifies that the parser correctly enforces security limits when using the default configuration (no system properties set).

**Key characteristics:**
- Uses default values from `ParserConfig` static initialization
- Tests boundary conditions at default limits
- Verifies backward compatibility with normal JSON documents
- Runs in the standard test suite without special configuration

**Default limits tested:**
- Max array size: 100,000 elements
- Max object members: 10,000 members
- Max nesting depth: 500 levels
- Max string length: 1,048,576 characters (1 MB)
- Max number length: 100 characters
- Max total size: 104,857,600 bytes (100 MB)
- Duplicate key behavior: SILENT (default)

### SecurityLimitsCustomPropertiesTest

This test class verifies that the parser correctly honors custom system property values by setting small limits and testing that they are enforced.

**Key characteristics:**
- Uses a custom JUnit `@Rule` to set system properties before each test
- Uses reflection to reset `ParserConfig` static fields with custom values
- Tests boundary conditions at custom (small) limits
- Verifies that system properties are correctly read and applied

**Custom limits tested:**
- Max array size: 5 elements
- Max object members: 3 members
- Max nesting depth: 2 levels
- Max string length: 20 characters
- Max number length: 10 characters
- Max total size: 1,024 bytes (1 KB)
- Duplicate key behavior: ERROR

## Test Isolation Strategy

### Why Isolation is Important

The `ParserConfig` class uses static initialization to load configuration from system properties. Once the class is loaded, the static fields are set and cannot be changed without reflection. This creates a challenge for testing:

1. If both test classes run in the same JVM, whichever runs first will initialize `ParserConfig`
2. The second test class will see the already-initialized values
3. System properties set after class initialization have no effect

### How Isolation is Achieved

**SecurityLimitsDefaultTest:**
- Runs with no special configuration
- Relies on `ParserConfig` default static initialization
- Can run in any order relative to other tests

**SecurityLimitsCustomPropertiesTest:**
- Uses a JUnit `@Rule` that runs before each test method
- The rule sets system properties to custom values
- The rule uses reflection to reset `ParserConfig` static fields
- The rule restores original system properties after each test
- Each test method verifies that custom limits are active before testing

### Reflection-Based Reset

The `SecurityLimitsCustomPropertiesTest` uses reflection to modify the final static fields in `ParserConfig`:

```java
private void resetParserConfigField(String fieldName, Object value) throws Exception {
    Field field = ParserConfig.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    
    // Remove final modifier
    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    
    // Set the new value
    field.set(null, value);
}
```

This approach allows each test method to run with fresh custom configuration values, ensuring proper isolation between tests within the same class.

## Running the Tests

### Run All Tests

```bash
./gradlew com.ibm.json4j:test
```

This will run both test classes in the standard test suite.

### Run Only Default Tests

```bash
./gradlew com.ibm.json4j:test --tests SecurityLimitsDefaultTest
```

### Run Only Custom Properties Tests

```bash
./gradlew com.ibm.json4j:test --tests SecurityLimitsCustomPropertiesTest
```

### Run a Specific Test Method

```bash
./gradlew com.ibm.json4j:test --tests SecurityLimitsDefaultTest.testArraySizeAtBoundary
./gradlew com.ibm.json4j:test --tests SecurityLimitsCustomPropertiesTest.testCustomArraySizeAtBoundary
```

## Test Coverage

Both test classes cover the same attack vectors with different limit values:

1. **Array Size Limits** - Prevents memory exhaustion from large arrays
2. **Object Member Limits** - Prevents memory exhaustion from objects with many keys
3. **String Length Limits** - Prevents memory exhaustion from extremely long strings
4. **Number Length Limits** - Prevents parser slowdown from very long number tokens
5. **Nesting Depth Limits** - Prevents stack overflow from deeply nested structures
6. **Total Size Limits** - Prevents memory exhaustion from many moderately-sized values
7. **Duplicate Key Behavior** - Tests SILENT mode (default) and ERROR mode (custom)

Each test class includes:
- Boundary tests (at the limit)
- Over-limit tests (exceeding the limit)
- Backward compatibility tests (normal JSON documents)
- Combined limit tests (multiple limits interacting)

## Maintenance Notes

### Adding New Security Limits

When adding a new security limit to `ParserConfig`:

1. Add the system property constant and default value to `ParserConfig`
2. Add the static field and getter method to `ParserConfig`
3. Load the property in the static initializer
4. Add tests to `SecurityLimitsDefaultTest` using the default value
5. Add a custom value constant to `SecurityLimitsCustomPropertiesTest`
6. Set the custom property in the `@Rule` setup
7. Reset the field using reflection in the `@Rule` setup
8. Add tests to `SecurityLimitsCustomPropertiesTest` using the custom value

### Modifying Existing Limits

When changing default limit values:

1. Update the default constant in `ParserConfig`
2. Update test expectations in `SecurityLimitsDefaultTest` if needed
3. Custom property tests should not need changes (they use fixed small values)

### Debugging Test Failures

If tests fail:

1. Check that `ParserConfig` is correctly loading the expected values
2. Verify that reflection is successfully resetting fields in custom property tests
3. Ensure that test JSON documents are correctly sized relative to limits
4. Check for off-by-one errors in boundary tests (some limits use `>`, others use `>=`)
5. Verify that error messages are informative and mention the relevant limit

## Related Files

- `ParserConfig.java` - Configuration class with security limits
- `Parser.java` - Main parser implementation that enforces limits
- `Tokenizer.java` - Tokenizer that enforces string and number length limits
- `ParserConfigTest.java` - Unit tests for ParserConfig property loading