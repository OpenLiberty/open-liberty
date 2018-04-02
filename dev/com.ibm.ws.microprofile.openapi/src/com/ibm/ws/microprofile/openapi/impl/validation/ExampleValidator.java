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

import org.eclipse.microprofile.openapi.models.examples.Example;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class ExampleValidator extends TypeValidator<Example> {

    private static final TraceComponent tc = Tr.register(ContactValidator.class);

    private static final ExampleValidator INSTANCE = new ExampleValidator();

    public static ExampleValidator getInstance() {
        return INSTANCE;
    }

    private ExampleValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Example t) {

        if (t != null) {
            String reference = t.getRef();

            if (reference != null && !reference.isEmpty()) {
                ValidatorUtils.referenceValidatorHelper(reference, t, helper, context, key);
                return;
            }

            // The value field and externalValue fields are mutually exclusive.
            if (t.getValue() != null && (t.getExternalValue() != null && !t.getExternalValue().isEmpty())) {
                final String message = Tr.formatMessage(tc, "exampleOnlyValueOrExternalValue", key);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
            }
        }
    }
}
