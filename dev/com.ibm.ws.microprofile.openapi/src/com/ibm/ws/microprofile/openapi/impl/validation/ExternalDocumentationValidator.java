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

import org.eclipse.microprofile.openapi.models.ExternalDocumentation;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class ExternalDocumentationValidator extends TypeValidator<ExternalDocumentation> {

    private static final TraceComponent tc = Tr.register(ExternalDocumentationValidator.class);

    private static final ExternalDocumentationValidator INSTANCE = new ExternalDocumentationValidator();

    public static ExternalDocumentationValidator getInstance() {
        return INSTANCE;
    }

    private ExternalDocumentationValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, ExternalDocumentation t) {
        ValidatorUtils.validateRequiredField(t.getUrl(), context, "url").ifPresent(helper::addValidationEvent);

        if (t.getUrl() != null) {
            if (!ValidatorUtils.isValidURL(t.getUrl())) {
                final String message = Tr.formatMessage(tc, "externalDocumentationInvalidURL", t.getUrl());
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation("url"), message));
            }
        }
    }
}
