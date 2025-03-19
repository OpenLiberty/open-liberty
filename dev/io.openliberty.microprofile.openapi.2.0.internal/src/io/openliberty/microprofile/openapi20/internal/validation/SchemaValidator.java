/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal.validation;

import static java.util.stream.Collectors.joining;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.models.media.Schema;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.service.util.ServiceCaller;

import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult.ValidationEvent;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIModelOperations;
import io.openliberty.microprofile.openapi20.internal.utils.Constants;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.internal.utils.ValidationMessageConstants;
import io.smallrye.openapi.runtime.io.schema.SchemaConstant;

/**
 *
 */
public class SchemaValidator extends TypeValidator<Schema> {

    private static final TraceComponent tc = Tr.register(SchemaValidator.class);

    // Non-final for unit testing
    private static ServiceCaller<OpenAPIModelOperations> MODEL_OPS = new ServiceCaller<>(SchemaValidator.class, OpenAPIModelOperations.class);

    private static final SchemaValidator INSTANCE = new SchemaValidator();

    public static SchemaValidator getInstance() {
        return INSTANCE;
    }

    private SchemaValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Schema t) {

        if (t != null) {

            String reference = t.getRef();
            if (reference != null && !reference.isEmpty()) {
                helper.validateReference(context, key, reference, Schema.class);
                return;
            }

            Set<String> types = MODEL_OPS.run(s -> s.getTypes(t))
                                         .map(typeList -> typeList.stream()
                                                                  .map(Object::toString)
                                                                  .collect(Collectors.toSet()))
                                         .orElse(null);

            if (types != null && types.contains(Constants.SCHEMA_TYPE_ARRAY) && t.getItems() == null) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_TYPE_ARRAY_NULL_ITEMS);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }

            if (t.getReadOnly() != null && t.getWriteOnly() != null && t.getReadOnly() && t.getWriteOnly()) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_READ_ONLY_OR_WRITE_ONLY);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }

            if (t.getMultipleOf() != null && (t.getMultipleOf().compareTo(BigDecimal.ZERO) <= 0)) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_MULTIPLE_OF_NOT_GREATER_THAN_ZERO);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }

            ArrayList<String> propertiesInvalidValue = new ArrayList<>();
            ArrayList<String> propertiesNotForSchemaType = new ArrayList<>();
            if (t.getMaxLength() != null) {
                if (t.getMaxLength().intValue() < 0) {
                    propertiesInvalidValue.add(SchemaConstant.PROP_MAX_LENGTH);
                }
                if (types != null && !types.contains(Constants.SCHEMA_TYPE_STRING)) {
                    propertiesNotForSchemaType.add(SchemaConstant.PROP_MAX_LENGTH);
                }
            }

            if (t.getMinLength() != null) {
                if (t.getMinLength().intValue() < 0) {
                    propertiesInvalidValue.add(SchemaConstant.PROP_MIN_LENGTH);
                }
                if (types != null && !types.contains(Constants.SCHEMA_TYPE_STRING)) {
                    propertiesNotForSchemaType.add(SchemaConstant.PROP_MIN_LENGTH);
                }
            }

            if (t.getMinItems() != null) {
                if (t.getMinItems().intValue() < 0) {
                    propertiesInvalidValue.add(SchemaConstant.PROP_MIN_ITEMS);
                }
                if (types != null && !types.contains(Constants.SCHEMA_TYPE_ARRAY)) {
                    propertiesNotForSchemaType.add(SchemaConstant.PROP_MIN_ITEMS);
                }
            }

            if (t.getMaxItems() != null) {
                if (t.getMaxItems().intValue() < 0) {
                    propertiesInvalidValue.add(SchemaConstant.PROP_MAX_ITEMS);
                }
                if (types != null && !types.contains(Constants.SCHEMA_TYPE_ARRAY)) {
                    propertiesNotForSchemaType.add(SchemaConstant.PROP_MAX_ITEMS);
                }
            }

            if (t.getUniqueItems() != null && types != null && !types.contains(Constants.SCHEMA_TYPE_ARRAY)) {
                propertiesNotForSchemaType.add(SchemaConstant.PROP_UNIQUE_ITEMS);
            }

            if (t.getMinProperties() != null) {
                if (t.getMinProperties().intValue() < 0) {
                    propertiesInvalidValue.add(SchemaConstant.PROP_MIN_PROPERTIES);
                }
                if (types != null && !types.contains(Constants.SCHEMA_TYPE_OBJECT)) {
                    propertiesNotForSchemaType.add(SchemaConstant.PROP_MIN_PROPERTIES);
                }
            }

            if (t.getMaxProperties() != null) {
                if (t.getMaxProperties().intValue() < 0) {
                    propertiesInvalidValue.add(SchemaConstant.PROP_MAX_PROPERTIES);
                }
                if (types != null && !types.contains(Constants.SCHEMA_TYPE_OBJECT)) {
                    propertiesNotForSchemaType.add(SchemaConstant.PROP_MAX_PROPERTIES);
                }
            }

            if (!propertiesInvalidValue.isEmpty()) {
                for (String s : propertiesInvalidValue) {
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_PROPERTY_LESS_THAN_ZERO, s);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                }
            }

            if (!propertiesNotForSchemaType.isEmpty()) {
                for (String s : propertiesNotForSchemaType) {
                    String typeString = types.size() == 1 ? types.iterator().next() : types.stream().collect(joining(",", "[", "]"));
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_TYPE_DOES_NOT_MATCH_PROPERTY, s, typeString);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
                }
            }
        }
    }
}
