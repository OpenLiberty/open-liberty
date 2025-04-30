/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.internal.services.validation.test;

import static io.openliberty.microprofile.openapi20.test.utils.ValidationResultMatcher.failedWithEvents;
import static io.openliberty.microprofile.openapi20.test.utils.ValidationResultMatcher.hasError;
import static io.openliberty.microprofile.openapi20.test.utils.ValidationResultMatcher.successful;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.openliberty.microprofile.openapi40.internal.validation.Schema31Validator;
import io.smallrye.openapi.api.models.ComponentsImpl;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.media.SchemaImpl;
import io.smallrye.openapi.runtime.io.schema.SchemaConstant;

/**
 *
 */
public class Schema31ValidatorTest {

    private String key;
    private OpenAPIImpl model;
    private Context context;
    private Schema31Validator validator;
    private TestValidationHelper vh;
    private SchemaImpl testSchema;

    @Before
    public void setup() {
        testSchema = new SchemaImpl();
        key = "testSchema";

        Components components = new ComponentsImpl();
        components.addSchema("testTarget", new SchemaImpl());
        components.addSchema(key, testSchema);

        model = new OpenAPIImpl();
        model.components(new ComponentsImpl());

        context = new TestValidationContextHelper(model);
        vh = new TestValidationHelper31();

        validator = Schema31Validator.getInstance();
    }

    @Test
    public void validateComment() {
        testSchema.setComment("test");
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_COMMENT, 3); // Number is not valid
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_COMMENT, "string")));
    }

    @Test
    public void validateAllOf() {
        testSchema.addAllOf(new SchemaImpl());
        testSchema.addAllOf(new SchemaImpl());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_ALL_OF, List.of(new SchemaImpl(), "invalid"));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(collectionNeedsType(SchemaConstant.PROP_ALL_OF, "schema")));
    }

    @Test
    public void validateAnyOf() {
        testSchema.addAnyOf(new SchemaImpl());
        testSchema.addAnyOf(new SchemaImpl());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_ANY_OF, List.of(new SchemaImpl(), "invalid"));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(collectionNeedsType(SchemaConstant.PROP_ANY_OF, "schema")));
    }

    @Test
    public void validateOneOf() {
        testSchema.addOneOf(new SchemaImpl());
        testSchema.addOneOf(new SchemaImpl());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_ONE_OF, List.of(new SchemaImpl(), "invalid"));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(collectionNeedsType(SchemaConstant.PROP_ONE_OF, "schema")));
    }

    @Test
    public void validateNot() {
        testSchema.not(new SchemaImpl());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_NOT, true); // A boolean is a valid schema
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_NOT, new HashMap<>()); // A map can be a valid schema
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_NOT, 7); // A number is not a valid schema
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_NOT, "schema")));
    }

    @Test
    public void validateIf() {
        testSchema.ifSchema(new SchemaImpl());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_IF, 7); // A number is not a valid schema
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_IF, "schema")));
    }

    @Test
    public void validateThen() {
        testSchema.thenSchema(new SchemaImpl());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_THEN, 7); // A number is not a valid schema
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_THEN, "schema")));
    }

    @Test
    public void validateElse() {
        testSchema.elseSchema(new SchemaImpl());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_ELSE, 7); // A number is not a valid schema
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_ELSE, "schema")));
    }

    @Test
    public void validateDependentSchemas() {
        testSchema.addDependentSchema("test1", new SchemaImpl());
        testSchema.addDependentSchema("test2", new SchemaImpl());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_DEPENDENT_SCHEMAS, Map.of("test1", new SchemaImpl(), "test2", 5));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(collectionNeedsType(SchemaConstant.PROP_DEPENDENT_SCHEMAS, "schema")));

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_DEPENDENT_SCHEMAS, List.of(new SchemaImpl(), new SchemaImpl()));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_DEPENDENT_SCHEMAS, "object")));
    }

    @Test
    public void validatePrefixItems() {
        testSchema.addPrefixItem(new SchemaImpl().addType(SchemaType.INTEGER));
        testSchema.addPrefixItem(new SchemaImpl().addType(SchemaType.STRING));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_PREFIX_ITEMS, new SchemaImpl().addType(SchemaType.INTEGER)); // Not a list
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_PREFIX_ITEMS, "array")));

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_PREFIX_ITEMS, List.of(new SchemaImpl(), 7)); // Number is not a schema
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(collectionNeedsType(SchemaConstant.PROP_PREFIX_ITEMS, "schema")));
    }

    @Test
    public void validateItems() {
        testSchema.items(new SchemaImpl());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_ITEMS, 7); // A number is not a valid schema
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_ITEMS, "schema")));
    }

    @Test
    public void validateContains() {
        testSchema.contains(new SchemaImpl());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_CONTAINS, 7); // A number is not a valid schema
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_CONTAINS, "schema")));
    }

    @Test
    public void validateProperties() {
        testSchema.addProperty("test1", new SchemaImpl());
        testSchema.addProperty("test2", new SchemaImpl());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_PROPERTIES, Map.of("test1", new SchemaImpl(), "test2", 5));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(collectionNeedsType(SchemaConstant.PROP_PROPERTIES, "schema")));

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_PROPERTIES, List.of(new SchemaImpl(), new SchemaImpl()));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_PROPERTIES, "object")));
    }

    @Test
    public void validatePatternProperties() {
        testSchema.addPatternProperty("test1", new SchemaImpl());
        testSchema.addPatternProperty("test2", new SchemaImpl());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_PATTERN_PROPERTIES, Map.of("test1", new SchemaImpl(), "test2", 5));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(collectionNeedsType(SchemaConstant.PROP_PATTERN_PROPERTIES, "schema")));

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_PATTERN_PROPERTIES, List.of(new SchemaImpl(), new SchemaImpl()));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_PATTERN_PROPERTIES, "object")));

        // TODO: validate keys are regexes?
    }

    @SuppressWarnings("deprecation")
    @Test
    public void validationAdditionalProperties() {
        testSchema.additionalPropertiesSchema(new SchemaImpl());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.additionalPropertiesBoolean(false);
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        testSchema.set(SchemaConstant.PROP_ADDITIONAL_PROPERTIES, 7); // A number is not a valid schema
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_ADDITIONAL_PROPERTIES, "schema")));
    }

    @Test
    public void validatePropertyNames() {
        testSchema.propertyNames(new SchemaImpl());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_PROPERTY_NAMES, 7); // A number is not a valid schema
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_PROPERTY_NAMES, "schema")));
    }

    @Test
    public void validateUnevaluatedItems() {
        testSchema.unevaluatedItems(new SchemaImpl());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_UNEVALUATED_ITEMS, 7); // A number is not a valid schema
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_UNEVALUATED_ITEMS, "schema")));
    }

    @Test
    public void validateUnevaluatedProperties() {
        testSchema.unevaluatedProperties(new SchemaImpl());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_UNEVALUATED_PROPERTIES, 7); // A number is not a valid schema
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_UNEVALUATED_PROPERTIES, "schema")));
    }

    @Test
    public void validateType() {
        testSchema.type(List.of(SchemaType.NUMBER));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        testSchema.type(List.of(SchemaType.INTEGER));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.type(List.of());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.type(List.of(SchemaType.OBJECT, SchemaType.ARRAY, SchemaType.NULL));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.type(List.of(SchemaType.NULL, SchemaType.BOOLEAN, SchemaType.OBJECT, SchemaType.ARRAY, SchemaType.NUMBER, SchemaType.STRING, SchemaType.INTEGER));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_TYPE, "object");
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_TYPE, new HashMap<>());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(schemaInvalidType()));

        vh.resetResults();
        testSchema.type(List.of(SchemaType.OBJECT, SchemaType.ARRAY, SchemaType.OBJECT));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(elementsNotUnique(SchemaConstant.PROP_TYPE)));

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_TYPE, List.of(SchemaType.OBJECT, 3));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(schemaInvalidType()));
    }

    @Test
    public void validateEnum() {
        ArrayList<Object> enumeration = new ArrayList<>();
        enumeration.add("test");
        enumeration.add(3);
        enumeration.add(null);
        enumeration.add(List.of());
        enumeration.add(Map.of("key", "value"));
        testSchema.enumeration(enumeration);
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_ENUM, 3);
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_ENUM, "array")));

        vh.resetResults();
        testSchema.enumeration(List.of());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), failedWithEvents().warning(propArrayIsEmpty(SchemaConstant.PROP_ENUM)));

        vh.resetResults();
        testSchema.enumeration(List.of("test", "test"));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), failedWithEvents().warning(elementsNotUnique(SchemaConstant.PROP_ENUM)));
    }

    @Test
    public void validateConst() {
        // Any value is valid in const
        testSchema.constValue("test");
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.constValue(3);
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.constValue(List.of(1, 2, "three", List.of(4), 5.0D));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());
    }

    @Test
    public void validateMultipleOf() {
        testSchema.multipleOf(new BigDecimal("4.5"));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.multipleOf(new BigDecimal("0"));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propLessThanEqualZero(SchemaConstant.PROP_MULTIPLE_OF)));

        vh.resetResults();
        testSchema.multipleOf(new BigDecimal("-1"));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propLessThanEqualZero(SchemaConstant.PROP_MULTIPLE_OF)));

        vh.resetResults();
        testSchema.multipleOf(new BigDecimal("0.2"));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        List<Object> validValues = List.of(new BigDecimal("4.5"),
                                           new BigDecimal("0.2"),
                                           5, 5L, 5.5f, 5.5d, new BigInteger("5"));
        for (Object value : validValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_MULTIPLE_OF, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), successful());
        }

        List<Object> lteZeroValues = List.of(BigDecimal.ZERO, 0, 0L, 0.0f, 0.0d, BigInteger.ZERO,
                                             new BigDecimal("-1"), -1, -1L, -1.5f, -1.5d, new BigInteger("-5"));

        for (Object value : lteZeroValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_MULTIPLE_OF, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(propLessThanEqualZero(SchemaConstant.PROP_MULTIPLE_OF)));
        }

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_MULTIPLE_OF, "five");
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_MULTIPLE_OF, "number")));

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_MULTIPLE_OF, List.of(3, 5));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_MULTIPLE_OF, "number")));
    }

    @Test
    public void validateMaximum() {
        testSchema.maximum(new BigDecimal("99999999999999999999999999")); // Large number to ensure we're not casting to int anywhere
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.maximum(new BigDecimal("-99999999999999999999999999"));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.maximum(BigDecimal.ZERO); // Large number to ensure we're not casting to int anywhere
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        List<Object> validValues = List.of(new BigDecimal("4.5"),
                                           new BigDecimal("0.2"),
                                           5, 5L, 5.5f, 5.5d, new BigInteger("5"));
        for (Object value : validValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_MAXIMUM, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), successful());
        }

        List<Object> badTypeValues = List.of("test", Map.of(), List.of(), true);
        for (Object value : badTypeValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_MAXIMUM, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_MAXIMUM, "number")));
        }
    }

    @Test
    public void validateExclusiveMaximum() {
        testSchema.exclusiveMaximum(new BigDecimal("99999999999999999999999999")); // Large number to ensure we're not casting to int anywhere
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.exclusiveMaximum(new BigDecimal("-99999999999999999999999999"));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.exclusiveMaximum(BigDecimal.ZERO); // Large number to ensure we're not casting to int anywhere
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        List<Object> validValues = List.of(new BigDecimal("4.5"),
                                           new BigDecimal("0.2"),
                                           5, 5L, 5.5f, 5.5d, new BigInteger("5"));
        for (Object value : validValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_EXCLUSIVE_MAXIMUM, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), successful());
        }

        List<Object> badTypeValues = List.of("test", Map.of(), List.of(), true);
        for (Object value : badTypeValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_EXCLUSIVE_MAXIMUM, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_EXCLUSIVE_MAXIMUM, "number")));
        }
    }

    @Test
    public void validateMinimum() {
        testSchema.minimum(new BigDecimal("99999999999999999999999999")); // Large number to ensure we're not casting to int anywhere
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.minimum(new BigDecimal("-99999999999999999999999999"));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.minimum(BigDecimal.ZERO); // Large number to ensure we're not casting to int anywhere
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        List<Object> validValues = List.of(new BigDecimal("4.5"),
                                           new BigDecimal("0.2"),
                                           5, 5L, 5.5f, 5.5d, new BigInteger("5"));
        for (Object value : validValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_MINIMUM, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), successful());
        }

        List<Object> badTypeValues = List.of("test", Map.of(), List.of(), true);
        for (Object value : badTypeValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_MINIMUM, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_MINIMUM, "number")));
        }
    }

    @Test
    public void validateExclusiveMinimum() {
        testSchema.exclusiveMinimum(new BigDecimal("99999999999999999999999999")); // Large number to ensure we're not casting to int anywhere
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.exclusiveMinimum(new BigDecimal("-99999999999999999999999999"));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.exclusiveMinimum(BigDecimal.ZERO); // Large number to ensure we're not casting to int anywhere
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        List<Object> validValues = List.of(new BigDecimal("4.5"),
                                           new BigDecimal("0.2"),
                                           5, 5L, 5.5f, 5.5d, new BigInteger("5"));
        for (Object value : validValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_EXCLUSIVE_MINIMUM, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), successful());
        }

        List<Object> badTypeValues = List.of("test", Map.of(), List.of(), true);
        for (Object value : badTypeValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_EXCLUSIVE_MINIMUM, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_EXCLUSIVE_MINIMUM, "number")));
        }
    }

    @Test
    public void validateMaxLength() {
        testSchema.maxLength(99999);
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.maxLength(0);
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.maxLength(-1);
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propLessThanZero(SchemaConstant.PROP_MAX_LENGTH)));

        List<Object> validValues = List.of(0, 0L, (short) 0, BigInteger.ZERO,
                                           20, 20L, (short) 20, new BigInteger("20"));
        for (Object value : validValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_MAX_LENGTH, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), successful());
        }

        List<Object> negativeValues = List.of(-1, -1L, (short) -1, new BigInteger("-1"));
        for (Object value : negativeValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_MAX_LENGTH, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(propLessThanZero(SchemaConstant.PROP_MAX_LENGTH)));
        }

        List<Object> badTypeValues = List.of(0.5f, 0.5d, "test", Map.of(), List.of(), true, new BigDecimal("0.5"));
        for (Object value : badTypeValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_MAX_LENGTH, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_MAX_LENGTH, "integer")));
        }
    }

    @Test
    public void validateMinLength() {
        testSchema.minLength(99999);
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.minLength(0);
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.minLength(-1);
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propLessThanZero(SchemaConstant.PROP_MIN_LENGTH)));

        List<Object> validValues = List.of(0, 0L, (short) 0, BigInteger.ZERO,
                                           20, 20L, (short) 20, new BigInteger("20"));
        for (Object value : validValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_MIN_LENGTH, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), successful());
        }

        List<Object> negativeValues = List.of(-1, -1L, (short) -1, new BigInteger("-1"));
        for (Object value : negativeValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_MIN_LENGTH, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(propLessThanZero(SchemaConstant.PROP_MIN_LENGTH)));
        }

        List<Object> badTypeValues = List.of(0.5f, 0.5d, "test", Map.of(), List.of(), true, new BigDecimal("0.5"));
        for (Object value : badTypeValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_MIN_LENGTH, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_MIN_LENGTH, "integer")));
        }
    }

    @Test
    public void validateRequired() {
        testSchema.required(List.of());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.required(List.of("test"));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.required(List.of("test1", "test2"));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_REQUIRED, List.of("test1", "test2"));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.required(List.of("test1", "test1"));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(elementsNotUnique(SchemaConstant.PROP_REQUIRED)));

        List<Object> badTypeValues = List.of(3, "test", true, Map.of());
        for (Object value : badTypeValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_REQUIRED, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_REQUIRED, "array")));
        }

        List<Object> badElementValues = List.of(3, true, Map.of(), List.of());
        for (Object value : badElementValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_REQUIRED, List.of("test", value));
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(collectionNeedsType(SchemaConstant.PROP_REQUIRED, "string")));
        }
    }

    @Test
    public void validateDependentRequired() {
        testSchema.dependentRequired(Map.of());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.dependentRequired(Map.of("test1", List.of("test2", "test3"),
                                            "test4", List.of("test5"),
                                            "test6", List.of()));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.dependentRequired(Map.of("test1", List.of("test2", "test2")));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(dependentRequiredNotUniqueStrings()));

        List<Object> badTypeValues = List.of(3, "test", true, List.of());
        for (Object value : badTypeValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_DEPENDENT_REQUIRED, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_DEPENDENT_REQUIRED, "object")));
        }

        List<Object> badElementValues = List.of("test", 3, true, Map.of());
        for (Object value : badElementValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_DEPENDENT_REQUIRED, Map.of("test", value));
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(collectionNeedsType(SchemaConstant.PROP_DEPENDENT_REQUIRED, "array")));
        }

        List<Object> badListValues = List.of(3, true, Map.of(), List.of());
        for (Object value : badListValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_DEPENDENT_REQUIRED, Map.of("test", List.of(value)));
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(dependentRequiredNotUniqueStrings()));
        }
    }

    @Test
    public void validateFormat() {
        testSchema.format("test");
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        List<Object> badTypeValues = List.of(3, 7.0d, true, List.of(), Map.of());
        for (Object value : badTypeValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_FORMAT, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_FORMAT, "string")));
        }
    }

    @Test
    public void validateContentEncoding() {
        testSchema.contentEncoding("test");
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        List<Object> badTypeValues = List.of(3, 7.0d, true, List.of(), Map.of());
        for (Object value : badTypeValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_CONTENT_ENCODING, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_CONTENT_ENCODING, "string")));
        }
    }

    @Test
    public void validateContentMediaType() {
        testSchema.contentMediaType("test");
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        List<Object> badTypeValues = List.of(3, 7.0d, true, List.of(), Map.of());
        for (Object value : badTypeValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_CONTENT_MEDIA_TYPE, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_CONTENT_MEDIA_TYPE, "string")));
        }
    }

    @Test
    public void validateContentSchema() {
        testSchema.contentSchema(new SchemaImpl());
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_CONTENT_SCHEMA, true); // A boolean is a valid schema
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_CONTENT_SCHEMA, new HashMap<>()); // A map can be a valid schema
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        vh.resetResults();
        testSchema.set(SchemaConstant.PROP_CONTENT_SCHEMA, 7); // A number is not a valid schema
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_CONTENT_SCHEMA, "schema")));
    }

    @Test
    public void validateTitle() {
        testSchema.title("test");
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        List<Object> badTypeValues = List.of(3, 7.0d, true, List.of(), Map.of());
        for (Object value : badTypeValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_TITLE, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_TITLE, "string")));
        }
    }

    @Test
    public void validateDescription() {
        testSchema.description("test");
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        List<Object> badTypeValues = List.of(3, 7.0d, true, List.of(), Map.of());
        for (Object value : badTypeValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_DESCRIPTION, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_DESCRIPTION, "string")));
        }
    }

    @Test
    public void validateDeprecated() {
        testSchema.deprecated(true);
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        List<Object> badTypeValues = List.of(3, 7.0d, "test", List.of(), Map.of());
        for (Object value : badTypeValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_DEPRECATED, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_DEPRECATED, "boolean")));
        }
    }

    @Test
    public void validateReadOnly() {
        testSchema.readOnly(true);
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        List<Object> badTypeValues = List.of(3, 7.0d, "test", List.of(), Map.of());
        for (Object value : badTypeValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_READ_ONLY, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_READ_ONLY, "boolean")));
        }
    }

    @Test
    public void validateWriteOnly() {
        testSchema.writeOnly(true);
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        List<Object> badTypeValues = List.of(3, 7.0d, "test", List.of(), Map.of());
        for (Object value : badTypeValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_WRITE_ONLY, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_WRITE_ONLY, "boolean")));
        }
    }

    @Test
    public void validateExamples() {
        testSchema.examples(List.of("test", 3, Map.of("abc", "xyz")));
        validator.validate(vh, context, testSchema);
        assertThat(vh.getResult(), successful());

        List<Object> badTypeValues = List.of(3, 7.0d, "test", false, Map.of());
        for (Object value : badTypeValues) {
            vh.resetResults();
            testSchema.set(SchemaConstant.PROP_EXAMPLES, value);
            validator.validate(vh, context, testSchema);
            assertThat("For value: " + value, vh.getResult(), hasError(propNeedsType(SchemaConstant.PROP_EXAMPLES, "array")));
        }

    }

    private Matcher<String> propNeedsType(String prop, String type) {
        return equalTo("The type of the \"" + prop + "\" property of the Schema Object must be \"" + type + "\"");
    }

    private Matcher<String> collectionNeedsType(String propName, String type) {
        return equalTo("The \"" + propName + "\" property of the Schema Object contains a value whose type is not \"" + type + "\"");
    }

    private Matcher<String> elementsNotUnique(String propName) {
        return equalTo("The elements of the \"" + propName + "\" property of the Schema Object are not unique");
    }

    private Matcher<String> schemaInvalidType() {
        return equalTo("The \"type\" property of the Schema Object must contain values that are one of (\"null\", \"boolean\", \"object\", \"array\", \"number\", \"integer\", or \"string\")");
    }

    private Matcher<String> propArrayIsEmpty(String propName) {
        return equalTo("The \"" + propName + "\" property of the Schema Object is an empty array");
    }

    private Matcher<String> propLessThanEqualZero(String propName) {
        return equalTo("The Schema Object must have the \"" + propName + "\" property set to a number strictly greater than zero");
    }

    private Matcher<String> propLessThanZero(String propName) {
        return equalTo("The \"" + propName + "\" property of the Schema Object must be greater than or equal to zero");
    }

    private Matcher<String> dependentRequiredNotUniqueStrings() {
        return equalTo("Each element of the \"dependentRequired\" property must be an array of unique strings");
    }

}
