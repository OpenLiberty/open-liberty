/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
            }

            if (t.getType().toString().equals("array") && t.getItems() == null) {
                final String message = Tr.formatMessage(tc, "schemaTypeArrayNullItems", t.getTitle());
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }

            if (t.getReadOnly() && t.getWriteOnly()) {
                final String message = Tr.formatMessage(tc, "schemaReadOnlyOrWriteOnly", t.getTitle());
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }
            if (t.getMultipleOf() != null && (t.getMultipleOf().compareTo(BigDecimal.ONE) < 1)) {
                final String message = Tr.formatMessage(tc, "schemaMultipleOfLessThanZero", t.getTitle());
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }
            if ((t.getMaxLength() != null && (t.getMaxLength().intValue() < 0)) ||
                (t.getMinLength() != null && (t.getMinLength().intValue() < 0)) ||
                (t.getMinItems() != null && (t.getMinItems().intValue() < 0)) ||
                (t.getMaxItems() != null && (t.getMaxItems().intValue() < 0)) ||
                (t.getMinProperties() != null && (t.getMinProperties().intValue() < 0)) ||
                (t.getMaxProperties() != null && (t.getMaxProperties().intValue() < 0))) {
                final String message = Tr.formatMessage(tc, "schemaPropertyLessThanZero", t.getTitle());
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }

        }
    }
}
