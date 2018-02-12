/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.impl.validation;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.eclipse.microprofile.openapi.models.media.Schema;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

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

            if (t.getType() != null && "array".equals(t.getType().toString()) && t.getItems() == null) {
                final String message = Tr.formatMessage(tc, "schemaTypeArrayNullItems");
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }

            if (t.getReadOnly() != null && t.getWriteOnly() != null && t.getReadOnly() && t.getWriteOnly()) {
                final String message = Tr.formatMessage(tc, "schemaReadOnlyOrWriteOnly");
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }

            if (t.getMultipleOf() != null && (t.getMultipleOf().compareTo(BigDecimal.ONE) < 1)) {
                final String message = Tr.formatMessage(tc, "schemaMultipleOfLessThanOne");
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }

            String type = (t.getType() != null) ? t.getType().toString() : "null";
            ArrayList<String> propertiesInvalidValue = new ArrayList<String>();
            ArrayList<String> propertiesNotForSchemaType = new ArrayList<String>();
            if (t.getMaxLength() != null) {
                if (t.getMaxLength().intValue() < 0) {
                    propertiesInvalidValue.add("maxLength");
                }
                if (!type.equals("string")) {
                    propertiesNotForSchemaType.add("maxLength");
                }
            }

            if (t.getMinLength() != null) {
                if (t.getMinLength().intValue() < 0) {
                    propertiesInvalidValue.add("minLength");
                }
                if (!type.equals("string")) {
                    propertiesNotForSchemaType.add("minLength");
                }
            }

            if (t.getMinItems() != null) {
                if (t.getMinItems().intValue() < 0) {
                    propertiesInvalidValue.add("minItems");
                }
                if (!type.equals("array")) {
                    propertiesNotForSchemaType.add("minItems");
                }
            }

            if (t.getMaxItems() != null) {
                if (t.getMaxItems().intValue() < 0) {
                    propertiesInvalidValue.add("maxItems");
                }
                if (!type.equals("array")) {
                    propertiesNotForSchemaType.add("maxItems");
                }
            }

            if (t.getUniqueItems() != null && !type.equals("array")) {
                propertiesNotForSchemaType.add("uniqueItems");
            }

            if (t.getMinProperties() != null) {
                if (t.getMinProperties().intValue() < 0) {
                    propertiesInvalidValue.add("minProperties");
                }
                if (!type.equals("object")) {
                    propertiesNotForSchemaType.add("minProperties");
                }
            }

            if (t.getMaxProperties() != null) {
                if (t.getMaxProperties().intValue() < 0) {
                    propertiesInvalidValue.add("maxProperties");
                }
                if (!type.equals("object")) {
                    propertiesNotForSchemaType.add("maxProperties");
                }
            }

            if (!propertiesInvalidValue.isEmpty()) {
                for (String s : propertiesInvalidValue) {
                    final String message = Tr.formatMessage(tc, "schemaPropertyLessThanZero", s);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                }
            }

            if (!propertiesNotForSchemaType.isEmpty()) {
                for (String s : propertiesNotForSchemaType) {
                    final String message = Tr.formatMessage(tc, "schemaTypeDoesNotMatchProperty", s, type);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
                }
            }
        }
    }
}
