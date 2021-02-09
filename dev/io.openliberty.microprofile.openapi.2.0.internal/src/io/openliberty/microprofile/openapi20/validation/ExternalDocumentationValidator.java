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

import org.eclipse.microprofile.openapi.models.ExternalDocumentation;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent;
import io.smallrye.openapi.runtime.io.externaldocs.ExternalDocsConstant;

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
        ValidatorUtils.validateRequiredField(t.getUrl(), context, ExternalDocsConstant.PROP_URL).ifPresent(helper::addValidationEvent);

        if (t.getUrl() != null) {
            if (!ValidatorUtils.isValidURI(t.getUrl())) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.EXTERNAL_DOCUMENTATION_INVALID_URL, t.getUrl());
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(ExternalDocsConstant.PROP_URL), message));
            }
        }
    }
}
