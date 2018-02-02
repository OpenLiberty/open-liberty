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

import org.eclipse.microprofile.openapi.models.info.Contact;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class ContactValidator extends TypeValidator<Contact> {

    private static final TraceComponent tc = Tr.register(ContactValidator.class);

    private static final ContactValidator INSTANCE = new ContactValidator();

    public static ContactValidator getInstance() {
        return INSTANCE;
    }

    private ContactValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Contact t) {
        if (t != null) {
            if (t.getUrl() != null) {
                if (!ValidatorUtils.isValidURL(t.getUrl())) {
                    final String message = Tr.formatMessage(tc, "contactInvalidURL", t.getUrl());
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation("url"), message));
                }
            }

            if (t.getEmail() != null) {
                if (!ValidatorUtils.isValidEmailAddress(t.getEmail())) {
                    final String message = Tr.formatMessage(tc, "contactInvalidEmail", t.getEmail());
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation("email"), message));
                }
            }
        }
    }
}
