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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.models.security.OAuthFlow;
import org.eclipse.microprofile.openapi.models.security.OAuthFlows;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class OAuthFlowsValidator extends TypeValidator<OAuthFlows> {

    private static final TraceComponent tc = Tr.register(OAuthFlowsValidator.class);

    private static final OAuthFlowsValidator INSTANCE = new OAuthFlowsValidator();

    public static OAuthFlowsValidator getInstance() {
        return INSTANCE;
    }

    private OAuthFlowsValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, OAuthFlows t) {
        if (t.getImplicit() != null) {
            OAuthFlow implicit = t.getImplicit();
            String type = "implicit OAuthFlow";
            if (StringUtils.isNotBlank(implicit.getTokenUrl())) {
                final String message = Tr.formatMessage(tc, "nonApplicableField", "tokenUrl", implicit.getTokenUrl(), type);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, null, message));
            } else {
                ValidatorUtils.validateRequiredField(implicit.getAuthorizationUrl(), type, "authorizationUrl").ifPresent(helper::addValidationEvent);
            }
        }
        if (t.getPassword() != null) {
            OAuthFlow password = t.getPassword();
            String type = "password OAuthFlow";
            if (StringUtils.isNotBlank(password.getAuthorizationUrl())) {
                final String message = Tr.formatMessage(tc, "nonApplicableField", "authorizationUrl", password.getAuthorizationUrl(), type);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, null, message));
            } else {
                ValidatorUtils.validateRequiredField(password.getTokenUrl(), type, "tokenUrl").ifPresent(helper::addValidationEvent);
            }
        }
        if (t.getClientCredentials() != null) {
            OAuthFlow clientCred = t.getClientCredentials();
            String type = "clientCredentials OAuthFlow";
            if (StringUtils.isNotBlank(clientCred.getAuthorizationUrl())) {
                final String message = Tr.formatMessage(tc, "nonApplicableField", "authorizationUrl", clientCred.getAuthorizationUrl(), type);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, null, message));
            } else {
                ValidatorUtils.validateRequiredField(clientCred.getTokenUrl(), type, "tokenUrl").ifPresent(helper::addValidationEvent);
            }
        }
        if (t.getAuthorizationCode() != null) {
            OAuthFlow authCode = t.getAuthorizationCode();
            String type = "authorizationCode OAuthFlow";
            if (StringUtils.isBlank(authCode.getTokenUrl())) {
                ValidatorUtils.validateRequiredField(authCode.getTokenUrl(), type, "tokenUrl").ifPresent(helper::addValidationEvent);
            } else {
                ValidatorUtils.validateRequiredField(authCode.getAuthorizationUrl(), type, "authorizationUrl").ifPresent(helper::addValidationEvent);
            }
        }

    }
}
