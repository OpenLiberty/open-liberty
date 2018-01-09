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

import org.eclipse.microprofile.openapi.models.security.OAuthFlow;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class OAuthFlowValidator extends TypeValidator<OAuthFlow> {

    private static final TraceComponent tc = Tr.register(OAuthFlowValidator.class);

    private static final OAuthFlowValidator INSTANCE = new OAuthFlowValidator();

    public static OAuthFlowValidator getInstance() {
        return INSTANCE;
    }

    private OAuthFlowValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, OAuthFlow t) {
        //TODO - Need to validate 'REQUIRED' fields.
        if (t.getAuthorizationUrl() != null) {
            if (!ValidatorUtils.isValidURL(t.getAuthorizationUrl())) {
                final String message = Tr.formatMessage(tc, "oAuthFlowInvalidURL", t.getAuthorizationUrl());
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
            }
        }
        if (t.getTokenUrl() != null) {
            if (!ValidatorUtils.isValidURL(t.getTokenUrl())) {
                final String message = Tr.formatMessage(tc, "oAuthFlowInvalidURL", t.getTokenUrl());
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
            }
        }
        if (t.getRefreshUrl() != null) {
            if (!ValidatorUtils.isValidURL(t.getRefreshUrl())) {
                final String message = Tr.formatMessage(tc, "oAuthFlowInvalidURL", t.getRefreshUrl());
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
            }
        }
    }
}
