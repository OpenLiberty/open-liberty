/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

import org.eclipse.microprofile.openapi.models.examples.Example;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.internal.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.internal.validation.OASValidationResult.ValidationEvent;

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
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.EXAMPLE_ONLY_VALUE_OR_EXTERNAL_VALUE, key);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
            }
        }
    }
}
