/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.internal.validation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult.ValidationEvent;
import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult.ValidationEvent.Severity;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.internal.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.internal.validation.TypeValidator;
import io.openliberty.microprofile.openapi20.internal.validation.ValidationHelper;
import io.smallrye.openapi.runtime.io.schema.SchemaConstant;

public class Schema31Validator extends TypeValidator<Schema> {
    private static final TraceComponent tc = Tr.register(Schema31Validator.class);

    private static final Set<String> VALID_TYPES = Set.of("null", "boolean", "object", "array", "number", "integer", "string");

    private static final Schema31Validator INSTANCE = new Schema31Validator();

    public static Schema31Validator getInstance() {
        return INSTANCE;
    }

    private Schema31Validator() {}

    @Override
    public void validate(ValidationHelper helper, Context context, String key, Schema schema) {

        if (schema == null) {
            return;
        }

        if (!isSupportedDialect(schema, context)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Schema uses an unsupported dialect, not validating.", schema.getSchemaDialect());
            }
            return;
        }

        if (schema.getBooleanSchema() != null) {
            // No validation required for a boolean schema
            return;
        }

        String ref = schema.getRef();
        if (ref != null) {
            helper.validateReference(context, key, schema.getRef(), Schema.class);
        }

        // JSON Schema Core
        validatePropType(helper, context, schema, SchemaConstant.PROP_COMMENT, PropertyType.STRING);
        validateArrayOfSchema(helper, context, schema, SchemaConstant.PROP_ALL_OF);
        validateArrayOfSchema(helper, context, schema, SchemaConstant.PROP_ANY_OF);
        validateArrayOfSchema(helper, context, schema, SchemaConstant.PROP_ONE_OF);
        validateIsSchema(helper, context, schema, SchemaConstant.PROP_NOT);
        validateIsSchema(helper, context, schema, SchemaConstant.PROP_IF);
        validateIsSchema(helper, context, schema, SchemaConstant.PROP_THEN);
        validateIsSchema(helper, context, schema, SchemaConstant.PROP_ELSE);
        validateIsMapOfNamedSchemas(helper, context, schema, SchemaConstant.PROP_DEPENDENT_SCHEMAS);
        validateArrayOfSchema(helper, context, schema, SchemaConstant.PROP_PREFIX_ITEMS);
        validateIsSchema(helper, context, schema, SchemaConstant.PROP_ITEMS);
        validateIsSchema(helper, context, schema, SchemaConstant.PROP_CONTAINS);
        validateIsMapOfNamedSchemas(helper, context, schema, SchemaConstant.PROP_PROPERTIES);
        validateIsMapOfNamedSchemas(helper, context, schema, SchemaConstant.PROP_PATTERN_PROPERTIES);
        validateIsSchema(helper, context, schema, SchemaConstant.PROP_ADDITIONAL_PROPERTIES);
        validateIsSchema(helper, context, schema, SchemaConstant.PROP_PROPERTY_NAMES);
        validateIsSchema(helper, context, schema, SchemaConstant.PROP_UNEVALUATED_ITEMS);
        validateIsSchema(helper, context, schema, SchemaConstant.PROP_UNEVALUATED_PROPERTIES);

        // JSON Schema Validation
        validateType(helper, context, schema); // String or array, values unique, values from set
        validateEnum(helper, context, schema); // Array, should be non-empty, elements should be unique
        validateMultipleOf(helper, context, schema);
        validatePropType(helper, context, schema, SchemaConstant.PROP_MAXIMUM, PropertyType.NUMBER);
        validatePropType(helper, context, schema, SchemaConstant.PROP_EXCLUSIVE_MAXIMUM, PropertyType.NUMBER);
        validatePropType(helper, context, schema, SchemaConstant.PROP_MINIMUM, PropertyType.NUMBER);
        validatePropType(helper, context, schema, SchemaConstant.PROP_EXCLUSIVE_MINIMUM, PropertyType.NUMBER);
        validateIntZeroOrGreater(helper, context, schema, SchemaConstant.PROP_MAX_LENGTH);
        validateIntZeroOrGreater(helper, context, schema, SchemaConstant.PROP_MIN_LENGTH);
        validatePropType(helper, context, schema, SchemaConstant.PROP_PATTERN, PropertyType.STRING); // Should be regex
        validateIntZeroOrGreater(helper, context, schema, SchemaConstant.PROP_MAX_ITEMS);
        validateIntZeroOrGreater(helper, context, schema, SchemaConstant.PROP_MIN_ITEMS);
        validatePropType(helper, context, schema, SchemaConstant.PROP_UNIQUE_ITEMS, PropertyType.BOOLEAN);
        validateIntZeroOrGreater(helper, context, schema, SchemaConstant.PROP_MAX_CONTAINS);
        validateIntZeroOrGreater(helper, context, schema, SchemaConstant.PROP_MIN_CONTAINS);
        validateIntZeroOrGreater(helper, context, schema, SchemaConstant.PROP_MAX_PROPERTIES);
        validateIntZeroOrGreater(helper, context, schema, SchemaConstant.PROP_MIN_PROPERTIES);
        validateRequired(helper, context, schema); // Array of unique strings
        validateDependentRequred(helper, context, schema); // Map of Arrays of unique strings
        validatePropType(helper, context, schema, SchemaConstant.PROP_FORMAT, PropertyType.STRING);
        validatePropType(helper, context, schema, SchemaConstant.PROP_CONTENT_ENCODING, PropertyType.STRING);
        validatePropType(helper, context, schema, SchemaConstant.PROP_CONTENT_MEDIA_TYPE, PropertyType.STRING); // must be a media type
        validateIsSchema(helper, context, schema, SchemaConstant.PROP_CONTENT_SCHEMA); // Ignored if contentMediaType not present
        validatePropType(helper, context, schema, SchemaConstant.PROP_TITLE, PropertyType.STRING);
        validatePropType(helper, context, schema, SchemaConstant.PROP_DESCRIPTION, PropertyType.STRING);
        validatePropType(helper, context, schema, SchemaConstant.PROP_DEPRECATED, PropertyType.BOOLEAN);
        validatePropType(helper, context, schema, SchemaConstant.PROP_READ_ONLY, PropertyType.BOOLEAN);
        validatePropType(helper, context, schema, SchemaConstant.PROP_WRITE_ONLY, PropertyType.BOOLEAN); // Warning if both specified?
        validatePropType(helper, context, schema, SchemaConstant.PROP_EXAMPLES, PropertyType.ARRAY);
    }

    private void validateType(ValidationHelper helper, Context context, Schema schema) {
        Object o = schema.get(SchemaConstant.PROP_TYPE);
        if (o == null) {
            // no value to validate
            return;
        }

        boolean isValid;
        if (o instanceof List) {
            isValid = true;
            for (Object e : (List<?>) o) {
                if (!isValidTypeElement(e)) {
                    isValid = false;
                    break;
                }
            }
        } else {
            isValid = isValidTypeElement(o);
        }

        if (!isValid) {
            String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_INVALID_TYPE);
            helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
            return;
        }

        if (o instanceof List) {
            Set<Object> set = new HashSet<>();
            for (Object e : (List<?>) o) {
                if (!set.add(e)) {
                    String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_PROPERTY_NOT_UNIQUE, SchemaConstant.PROP_TYPE);
                    helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
                    return;
                }
            }
        }
    }

    private boolean isValidTypeElement(Object o) {
        if (o instanceof SchemaType) {
            return true;
        } else if (o instanceof String) {
            return VALID_TYPES.contains(o);
        } else {
            return false;
        }
    }

    private void validateEnum(ValidationHelper helper, Context context, Schema schema) {
        Object o = schema.get(SchemaConstant.PROP_ENUM);

        if (o == null) {
            // No value to validate
            return;
        }

        if (!(o instanceof List)) {
            String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_PROPERTY_WRONG_TYPE, SchemaConstant.PROP_ENUM, "array");
            helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
            return;
        }

        List<?> list = (List<?>) o;
        if (list.size() == 0) {
            String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_PROPERTY_EMPTY_ARRAY, SchemaConstant.PROP_ENUM);
            helper.addValidationEvent(new ValidationEvent(Severity.WARNING, context.getLocation(), message));
            return;
        }

        HashSet<Object> set = new HashSet<>();
        for (Object e : list) {
            if (!set.add(e)) {
                String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_PROPERTY_NOT_UNIQUE, SchemaConstant.PROP_ENUM);
                helper.addValidationEvent(new ValidationEvent(Severity.WARNING, context.getLocation(), message));
                return;
            }
        }
    }

    private void validateMultipleOf(ValidationHelper helper, Context context, Schema schema) {
        Object o = schema.get(SchemaConstant.PROP_MULTIPLE_OF);
        if (o == null) {
            // no value to validate
            return;
        }

        if (!(o instanceof Number)) {
            String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_PROPERTY_WRONG_TYPE, SchemaConstant.PROP_MULTIPLE_OF, "number");
            helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
            return;
        }

        boolean isGreaterThanZero;
        if (o instanceof BigDecimal) { // Expected case
            BigDecimal decimal = (BigDecimal) o;
            isGreaterThanZero = decimal.compareTo(BigDecimal.ZERO) > 0; // decimal > 0
        } else if (o instanceof Double) {
            Double d = (Double) o;
            isGreaterThanZero = d > 0;
        } else if (o instanceof Float) {
            Float f = (Float) o;
            isGreaterThanZero = f > 0;
        } else if (o instanceof BigInteger) {
            BigInteger bigInteger = (BigInteger) o;
            isGreaterThanZero = bigInteger.compareTo(BigInteger.ZERO) > 0; // bigInteger > 0
        } else {
            Number n = (Number) o;
            isGreaterThanZero = n.doubleValue() > 0;
        }

        if (!isGreaterThanZero) {
            String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_MULTIPLE_OF_NOT_GREATER_THAN_ZERO);
            helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
        }
    }

    private void validateRequired(ValidationHelper helper, Context context, Schema schema) {
        // Array of unique strings
        Object o = schema.get(SchemaConstant.PROP_REQUIRED);

        if (o == null) {
            // No value to validate
            return;
        }

        // Check is list
        if (!(o instanceof List)) {
            String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_PROPERTY_WRONG_TYPE, SchemaConstant.PROP_REQUIRED, "array");
            helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
            return;
        }

        // Check all strings
        List<?> list = (List<?>) o;
        for (Object e : list) {
            if (!(e instanceof String)) {
                String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_COLLECTION_ELEMENT_WRONG_TYPE, SchemaConstant.PROP_REQUIRED, "string");
                helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
                return;
            }
        }

        // Check all unique
        Set<Object> set = new HashSet<>();
        for (Object e : list) {
            if (!set.add(e)) {
                String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_PROPERTY_NOT_UNIQUE, SchemaConstant.PROP_REQUIRED);
                helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
                return;
            }
        }
    }

    private void validateDependentRequred(ValidationHelper helper, Context context, Schema schema) {
        // Map of Arrays of unique strings
        Object o = schema.get(SchemaConstant.PROP_DEPENDENT_REQUIRED);

        if (o == null) {
            // No value to validate
            return;
        }

        // Check is map
        if (!(o instanceof Map)) {
            String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_PROPERTY_WRONG_TYPE, SchemaConstant.PROP_DEPENDENT_REQUIRED, "object");
            helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
            return;
        }

        Map<?, ?> map = (Map<?, ?>) o;

        // Check each value is an array
        for (Object value : map.values()) {
            if (!(value instanceof List)) {
                String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_COLLECTION_ELEMENT_WRONG_TYPE, SchemaConstant.PROP_DEPENDENT_REQUIRED, "array");
                helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
                return;
            }
        }

        // Check each array consists of unique strings
        for (Object value : map.values()) {
            List<?> list = (List<?>) value; // Types checked above
            Set<Object> set = new HashSet<>();
            for (Object element : list) {
                if (!(element instanceof String) || !set.add(element)) {
                    String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_DEPENDENT_REQUIRED_UNIQUE_STRINGS);
                    helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
                    return;
                }
            }
        }
    }

    private void validateIntZeroOrGreater(ValidationHelper helper, Context context, Schema schema, String prop) {
        Object o = schema.get(prop);
        if (o == null) {
            // no value to validate
            return;
        }

        boolean isZeroOrGreater;
        if (o instanceof Integer) {
            Integer integer = (Integer) o;
            isZeroOrGreater = integer >= 0;
        } else if (o instanceof Long) {
            Long value = (Long) o;
            isZeroOrGreater = value >= 0L;
        } else if (o instanceof Short) {
            Short s = (Short) o;
            isZeroOrGreater = s >= 0;
        } else if (o instanceof Byte) {
            Byte value = (Byte) o;
            isZeroOrGreater = value >= 0;
        } else if (o instanceof BigInteger) {
            BigInteger value = (BigInteger) o;
            isZeroOrGreater = value.compareTo(BigInteger.ZERO) >= 0; // value >= 0
        } else {
            // invalid type
            String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_PROPERTY_WRONG_TYPE, prop, "integer");
            helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
            return;
        }

        if (!isZeroOrGreater) {
            String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_PROPERTY_LESS_THAN_ZERO, prop);
            helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
        }
    }

    private boolean isSchema(Object o) {
        if (o instanceof Schema) {
            return true;
        } else if (o instanceof Boolean) {
            // A boolean is a valid schema
            return true;
        } else if (o instanceof Map<?, ?>) {
            // A map _could_ be a valid schema, we won't validate it but don't want to report an error either
            return true;
        } else {
            return false;
        }
    }

    private void validateIsSchema(ValidationHelper helper, Context context, Schema schema, String prop) {
        Object o = schema.get(prop);
        if (o == null) {
            // No value to validate
            return;
        }

        if (!isSchema(o)) {
            String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_PROPERTY_WRONG_TYPE, prop, "schema");
            helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
        }
    }

    private void validateIsMapOfNamedSchemas(ValidationHelper helper, Context context, Schema schema, String prop) {
        Object o = schema.get(prop);
        if (o == null) {
            // No value to validate
            return;
        }

        if (!(o instanceof Map<?, ?>)) {
            String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_PROPERTY_WRONG_TYPE, prop, "object");
            helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
            return;
        }

        Map<?, ?> map = (Map<?, ?>) o;
        for (Object e : map.values()) {
            if (!isSchema(e)) {
                String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_COLLECTION_ELEMENT_WRONG_TYPE, prop, "schema");
                helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
                return;
            }
        }
    }

    private void validateArrayOfSchema(ValidationHelper helper, Context context, Schema schema, String prop) {
        Object o = schema.get(prop);
        if (o == null) {
            // No value to validate
            return;
        }

        if (!(o instanceof List<?>)) {
            String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_PROPERTY_WRONG_TYPE, prop, "array");
            helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
            return;
        }

        List<?> list = (List<?>) o;
        for (Object e : list) {
            if (!isSchema(e)) {
                String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_COLLECTION_ELEMENT_WRONG_TYPE, prop, "schema");
                helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
                return;
            }
        }
    }

    private void validatePropType(ValidationHelper helper, Context context, Schema schema, String prop, PropertyType type) {
        Object o = schema.get(prop);
        if (o == null) {
            // Nothing to validate
            return;
        }
        PropertyType actualType;
        if (o instanceof Number) {
            actualType = PropertyType.NUMBER;
        } else if (o instanceof String) {
            actualType = PropertyType.STRING;
        } else if (o instanceof List<?>) {
            actualType = PropertyType.ARRAY;
        } else if (o instanceof Map<?, ?>) {
            actualType = PropertyType.OBJECT;
        } else if (o instanceof Boolean) {
            actualType = PropertyType.BOOLEAN;
        } else if (o instanceof Schema) {
            actualType = ((Schema) o).getBooleanSchema() ? PropertyType.BOOLEAN : PropertyType.OBJECT;
        } else if (o instanceof Constructible) {
            actualType = PropertyType.OBJECT;
        } else {
            actualType = PropertyType.UNKNOWN;
        }

        if (actualType != type) {
            String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_PROPERTY_WRONG_TYPE, prop, type);
            helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
        }
    }

    private boolean isSupportedDialect(Schema schema, Context context) {
        String dialect = schema.getSchemaDialect();

        // NOTE: we should get the default dialect from the root OpenAPI here, but it's missing from the API

        if (dialect == null || dialect.equals(SchemaConstant.DIALECT_JSON_2020_12) || dialect.equals(SchemaConstant.DIALECT_OAS31)) {
            return true;
        } else {
            return false;
        }
    }

    private enum PropertyType {
        STRING("string"),
        NUMBER("number"),
        ARRAY("array"),
        OBJECT("object"),
        BOOLEAN("boolean"),
        UNKNOWN("unknown");

        private String value;

        private PropertyType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

    }

}
