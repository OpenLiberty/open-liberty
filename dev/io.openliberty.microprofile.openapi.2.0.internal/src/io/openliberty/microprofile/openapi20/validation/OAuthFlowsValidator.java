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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.models.security.OAuthFlow;
import org.eclipse.microprofile.openapi.models.security.OAuthFlows;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent;
import io.smallrye.openapi.runtime.io.securityscheme.SecuritySchemeConstant;

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
        if (t != null) {
            if (t.getImplicit() != null) {
                OAuthFlow implicit = t.getImplicit();;
                if (StringUtils.isNotBlank(implicit.getTokenUrl())) {
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.NON_APPLICABLE_FIELD_WITH_VALUE, SecuritySchemeConstant.PROP_TOKEN_URL, implicit.getTokenUrl(), ValidationMessageConstants.VARIABLE_OAUTH_FLOW_OBJECT, SecuritySchemeConstant.PROP_IMPLICIT);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
                }
                ValidatorUtils.validateRequiredField(implicit.getAuthorizationUrl(), context, SecuritySchemeConstant.PROP_AUTHORIZATION_URL).ifPresent(helper::addValidationEvent);
            }
            if (t.getPassword() != null) {
                OAuthFlow password = t.getPassword();
                if (StringUtils.isNotBlank(password.getAuthorizationUrl())) {
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.NON_APPLICABLE_FIELD_WITH_VALUE, SecuritySchemeConstant.PROP_AUTHORIZATION_URL, password.getAuthorizationUrl(), ValidationMessageConstants.VARIABLE_OAUTH_FLOW_OBJECT, SecuritySchemeConstant.PROP_PASSWORD);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
                }
                ValidatorUtils.validateRequiredField(password.getTokenUrl(), context, SecuritySchemeConstant.PROP_TOKEN_URL).ifPresent(helper::addValidationEvent);
            }
            if (t.getClientCredentials() != null) {
                OAuthFlow clientCred = t.getClientCredentials();
                if (StringUtils.isNotBlank(clientCred.getAuthorizationUrl())) {
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.NON_APPLICABLE_FIELD_WITH_VALUE, SecuritySchemeConstant.PROP_AUTHORIZATION_URL, clientCred.getAuthorizationUrl(), ValidationMessageConstants.VARIABLE_OAUTH_FLOW_OBJECT, SecuritySchemeConstant.PROP_CLIENT_CREDENTIALS);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
                }
                ValidatorUtils.validateRequiredField(clientCred.getTokenUrl(), context, SecuritySchemeConstant.PROP_TOKEN_URL).ifPresent(helper::addValidationEvent);
            }
            if (t.getAuthorizationCode() != null) {
                OAuthFlow authCode = t.getAuthorizationCode();
                ValidatorUtils.validateRequiredField(authCode.getTokenUrl(), context, SecuritySchemeConstant.PROP_TOKEN_URL).ifPresent(helper::addValidationEvent);
                ValidatorUtils.validateRequiredField(authCode.getAuthorizationUrl(), context, SecuritySchemeConstant.PROP_AUTHORIZATION_URL).ifPresent(helper::addValidationEvent);
            }
        }
    }
}
