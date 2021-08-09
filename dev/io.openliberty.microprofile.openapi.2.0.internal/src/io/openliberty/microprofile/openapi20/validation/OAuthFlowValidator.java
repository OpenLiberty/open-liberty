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

import org.eclipse.microprofile.openapi.models.security.OAuthFlow;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent;
import io.smallrye.openapi.runtime.io.securityscheme.SecuritySchemeConstant;

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

        if (t != null) {

            if (t.getAuthorizationUrl() != null) {
                if (!ValidatorUtils.isValidURI(t.getAuthorizationUrl())) {
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.OAUTH_FLOW_INVALID_URL, t.getAuthorizationUrl());
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(SecuritySchemeConstant.PROP_AUTHORIZATION_URL), message));
                }
            }
            if (t.getTokenUrl() != null) {
                if (!ValidatorUtils.isValidURI(t.getTokenUrl())) {
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.OAUTH_FLOW_INVALID_URL, t.getTokenUrl());
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(SecuritySchemeConstant.PROP_TOKEN_URL), message));
                }
            }
            if (t.getRefreshUrl() != null) {
                if (!ValidatorUtils.isValidURI(t.getRefreshUrl())) {
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.OAUTH_FLOW_INVALID_URL, t.getRefreshUrl());
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(SecuritySchemeConstant.PROP_REFRESH_URL), message));
                }
            }
            ValidatorUtils.validateRequiredField(t.getScopes(), context, SecuritySchemeConstant.PROP_SCOPES).ifPresent(helper::addValidationEvent);
        }
    }
}
