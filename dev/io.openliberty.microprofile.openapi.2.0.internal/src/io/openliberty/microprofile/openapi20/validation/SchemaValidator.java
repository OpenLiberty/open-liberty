/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.validation;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.eclipse.microprofile.openapi.models.media.Schema;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.Constants;
import io.openliberty.microprofile.openapi20.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent;
import io.smallrye.openapi.runtime.io.schema.SchemaConstant;

/**
 *
 */
public class SchemaValidator extends TypeValidator<Schema> {

    private static final TraceComponent tc = Tr.register(SchemaValidator.class);

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
                ValidatorUtils.referenceValidatorHelper(reference, t, helper, context, key);
                return;
            }

            if (t.getType() != null && Constants.SCHEMA_TYPE_ARRAY.equals(t.getType().toString()) && t.getItems() == null) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_TYPE_ARRAY_NULL_ITEMS);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }

            if (t.getReadOnly() != null && t.getWriteOnly() != null && t.getReadOnly() && t.getWriteOnly()) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_READ_ONLY_OR_WRITE_ONLY);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }

            if (t.getMultipleOf() != null && (t.getMultipleOf().compareTo(BigDecimal.ONE) < 1)) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_MULTIPLE_OF_LESS_THAN_ONE);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }

            String type = (t.getType() != null) ? t.getType().toString() : Constants.SCHEMA_TYPE_NULL;
            ArrayList<String> propertiesInvalidValue = new ArrayList<String>();
            ArrayList<String> propertiesNotForSchemaType = new ArrayList<String>();
            if (t.getMaxLength() != null) {
                if (t.getMaxLength().intValue() < 0) {
                    propertiesInvalidValue.add(SchemaConstant.PROP_MAX_LENGTH);
                }
                if (!type.equals(Constants.SCHEMA_TYPE_STRING)) {
                    propertiesNotForSchemaType.add(SchemaConstant.PROP_MAX_LENGTH);
                }
            }

            if (t.getMinLength() != null) {
                if (t.getMinLength().intValue() < 0) {
                    propertiesInvalidValue.add(SchemaConstant.PROP_MIN_LENGTH);
                }
                if (!type.equals(Constants.SCHEMA_TYPE_STRING)) {
                    propertiesNotForSchemaType.add(SchemaConstant.PROP_MIN_LENGTH);
                }
            }

            if (t.getMinItems() != null) {
                if (t.getMinItems().intValue() < 0) {
                    propertiesInvalidValue.add(SchemaConstant.PROP_MIN_ITEMS);
                }
                if (!type.equals(Constants.SCHEMA_TYPE_ARRAY)) {
                    propertiesNotForSchemaType.add(SchemaConstant.PROP_MIN_ITEMS);
                }
            }

            if (t.getMaxItems() != null) {
                if (t.getMaxItems().intValue() < 0) {
                    propertiesInvalidValue.add(SchemaConstant.PROP_MAX_ITEMS);
                }
                if (!type.equals(Constants.SCHEMA_TYPE_ARRAY)) {
                    propertiesNotForSchemaType.add(SchemaConstant.PROP_MAX_ITEMS);
                }
            }

            if (t.getUniqueItems() != null && !type.equals(Constants.SCHEMA_TYPE_ARRAY)) {
                propertiesNotForSchemaType.add(SchemaConstant.PROP_UNIQUE_ITEMS);
            }

            if (t.getMinProperties() != null) {
                if (t.getMinProperties().intValue() < 0) {
                    propertiesInvalidValue.add(SchemaConstant.PROP_MIN_PROPERTIES);
                }
                if (!type.equals(Constants.SCHEMA_TYPE_OBJECT)) {
                    propertiesNotForSchemaType.add(SchemaConstant.PROP_MIN_PROPERTIES);
                }
            }

            if (t.getMaxProperties() != null) {
                if (t.getMaxProperties().intValue() < 0) {
                    propertiesInvalidValue.add(SchemaConstant.PROP_MAX_PROPERTIES);
                }
                if (!type.equals(Constants.SCHEMA_TYPE_OBJECT)) {
                    propertiesNotForSchemaType.add(SchemaConstant.PROP_MAX_PROPERTIES);
                }
            }

            if (!propertiesInvalidValue.isEmpty()) {
                for (String s : propertiesInvalidValue) {
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_MULTIPLE_OF_LESS_THAN_ZERO, s);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                }
            }

            if (!propertiesNotForSchemaType.isEmpty()) {
                for (String s : propertiesNotForSchemaType) {
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.SCHEMA_TYPE_DOES_NOT_MATCH_PROPERTY, s, type);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
                }
            }
        }
    }
}
