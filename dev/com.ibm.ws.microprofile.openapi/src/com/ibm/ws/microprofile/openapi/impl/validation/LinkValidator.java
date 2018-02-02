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

import org.eclipse.microprofile.openapi.models.links.Link;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class LinkValidator extends TypeValidator<Link> {

    private static final TraceComponent tc = Tr.register(LinkValidator.class);

    private static final LinkValidator INSTANCE = new LinkValidator();

    public static LinkValidator getInstance() {
        return INSTANCE;
    }

    private LinkValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Link t) {

        if (t != null) {

            String reference = t.getRef();

            if (reference != null && !reference.isEmpty()) {
                ValidatorUtils.referenceValidatorHelper(reference, t, helper, context, key);
                return;
            }

            if (t.getOperationRef() != null && !t.getOperationRef().isEmpty()) {

                if (t.getOperationId() != null && !t.getOperationId().isEmpty()) {
                    final String message = Tr.formatMessage(tc, "linkOperationRefOrId", t);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                }
            }
        }
    }
}
