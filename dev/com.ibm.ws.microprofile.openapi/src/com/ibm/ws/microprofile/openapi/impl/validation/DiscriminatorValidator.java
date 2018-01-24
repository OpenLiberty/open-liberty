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

import org.eclipse.microprofile.openapi.models.media.Discriminator;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class DiscriminatorValidator extends TypeValidator<Discriminator> {

    private static final TraceComponent tc = Tr.register(DiscriminatorValidator.class);

    private static final DiscriminatorValidator INSTANCE = new DiscriminatorValidator();

    public static DiscriminatorValidator getInstance() {
        return INSTANCE;
    }

    private DiscriminatorValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Discriminator t) {

        if (t != null) {

            if (t.getPropertyName() == null || t.getPropertyName().isEmpty()) {
                final String message = Tr.formatMessage(tc, "requiredFieldMissing", t, "propertyName");
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }
        }
    }
}
