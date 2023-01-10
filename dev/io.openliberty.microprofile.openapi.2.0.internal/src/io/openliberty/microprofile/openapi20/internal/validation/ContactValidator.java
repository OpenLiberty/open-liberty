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

import org.eclipse.microprofile.openapi.models.info.Contact;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.internal.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.internal.validation.OASValidationResult.ValidationEvent;
import io.smallrye.openapi.runtime.io.contact.ContactConstant;

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
            String url = t.getUrl();
            if (url != null) {
                if (!ValidatorUtils.isValidURI(url)) {
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.CONTACT_INVALID_URL, url);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(ContactConstant.PROP_URL), message));
                }
            }

            String email = t.getEmail();
            if (email != null) {
                if (!ValidatorUtils.isValidEmailAddress(email)) {
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.CONTACT_INVALID_EMAIL, email);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(ContactConstant.PROP_EMAIL), message));
                }
            }
        }
    }
}
