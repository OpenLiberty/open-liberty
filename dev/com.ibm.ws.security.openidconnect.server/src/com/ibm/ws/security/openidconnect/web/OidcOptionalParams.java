/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.web;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.oauth20.OAuth20BadParameterFormatException;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.util.OIDCConstants;

public class OidcOptionalParams
{
    private static TraceComponent tc = Tr.register(OidcOptionalParams.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    private static final String SPACE = " ";
    private volatile BrowserState browserState = null;
    private static final String[] OIDC_ALL_OPTIONAL_PARAMS = {
                                                              OIDCConstants.OIDC_AUTHZ_PARAM_NONCE,
                                                              OIDCConstants.OIDC_AUTHZ_PARAM_DISPLAY,
                                                              OIDCConstants.OIDC_AUTHZ_PARAM_PROMPT,
                                                              OIDCConstants.OIDC_AUTHZ_PARAM_MAX_AGE,
                                                              OIDCConstants.OIDC_AUTHZ_PARAM_UI_LOCALES,
                                                              OIDCConstants.OIDC_AUTHZ_PARAM_CLAIMS_LOCALES,
                                                              OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT,
                                                              OIDCConstants.OIDC_AUTHZ_PARAM_LOGIN_HINT,
                                                              OIDCConstants.OIDC_AUTHZ_PARAM_ACR_VALUES,
                                                              OIDCConstants.OIDC_AUTHZ_PARAM_RESPONSE_MODE
    };

    private static final List<String> OIDC_OPTIONAL_PARAMS_TO_BE_HANDLED = Collections.unmodifiableList(Arrays.asList(new String[] {
                                                                                                                                    OIDCConstants.OIDC_AUTHZ_PARAM_NONCE }));

    private static final List<String> MULTIPLE_VALUE_PARAMS = Collections.unmodifiableList(Arrays.asList(new String[] {
                                                                                                                       OIDCConstants.OIDC_AUTHZ_PARAM_UI_LOCALES,
                                                                                                                       OIDCConstants.OIDC_AUTHZ_PARAM_CLAIMS_LOCALES,
                                                                                                                       OIDCConstants.OIDC_AUTHZ_PARAM_ACR_VALUES }));

    OidcOptionalParams() {};

    public AttributeList getParameters(HttpServletRequest request) throws OAuth20BadParameterFormatException {
        AttributeList options = new AttributeList();
        for (String key : OIDC_ALL_OPTIONAL_PARAMS) {
            String value = request.getParameter(key);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "key:" + key + " value:" + value);
            }
            if (value != null && value.trim().length() > 0) {
                String[] values = new String[1];

                // multiple values 
                if (OIDCConstants.OIDC_AUTHZ_PARAM_PROMPT.equals(key)) {
                    value = value.trim();
                    // if there is "none" and there are more than one values, then exception.
                    if (value.contains(OIDCConstants.OIDC_AUTHZ_PARAM_PROMPT_NONE) && value.contains(SPACE)) {
                        // "none" should not be one of multiple values.
                        throw new OAuth20BadParameterFormatException("security.oauth20.error.invalid.authorization.prompt.none.value", OIDCConstants.OIDC_AUTHZ_PARAM_PROMPT, value);
                    }
                    values = value.split(SPACE);
                } else if (MULTIPLE_VALUE_PARAMS.contains(key)) {
                    values = value.trim().split(SPACE);
                } else {
                    values[0] = value.trim();
                }

                options.setAttribute(key, OAuth20Constants.ATTRTYPE_REQUEST, values);
                if (OIDC_OPTIONAL_PARAMS_TO_BE_HANDLED.contains(key)) { // add optional parameters, such as: nonce, to the Extension
                    options.setAttribute(OIDCConstants.EXTERNAL_CLAIMS_PREFIX + key, OAuth20Constants.EXTERNAL_CLAIMS, values);
                }
            }
        }
        return options;
    }
}
