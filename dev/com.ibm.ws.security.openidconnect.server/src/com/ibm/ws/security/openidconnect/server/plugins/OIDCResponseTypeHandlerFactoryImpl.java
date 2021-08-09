/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.plugins;

import java.util.Map;

import org.osgi.service.component.ComponentContext;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.config.OAuthComponentConfigurationConstants;
import com.ibm.oauth.core.api.error.OAuthConfigurationException;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidResponseTypeException;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;
import com.ibm.oauth.core.internal.oauth20.responsetype.OAuth20ResponseTypeHandler;
import com.ibm.oauth.core.internal.oauth20.responsetype.OAuth20ResponseTypeHandlerFactory;
import com.ibm.oauth.core.internal.oauth20.responsetype.impl.OAuth20ResponseTypeHandlerCodeImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.openidconnect.server.internal.Utils;

public class OIDCResponseTypeHandlerFactoryImpl implements OAuth20ResponseTypeHandlerFactory {

    private static final TraceComponent tc = Tr.register(OIDCResponseTypeHandlerFactoryImpl.class, TraceConstants.TRACE_GROUP,
                                                         TraceConstants.MESSAGE_BUNDLE);

    OAuthComponentConfiguration _oldconfig = null; // this is not in use in tivoli code

    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        // do nothing for now
    }

    protected void deactivate(ComponentContext cc, Map<String, Object> properties) {
        // do nothing for now
    }

    protected void modified(ComponentContext cc, Map<String, Object> properties) {
        // do nothing for now
    }

    // oidc10 not needed in default OAuth20GrantTypeHandler
    public void init(OAuthComponentConfiguration oldconfig) {
        _oldconfig = oldconfig;
    };

    public synchronized OAuth20ResponseTypeHandler getHandler(String responseType, OAuth20ConfigProvider config) throws OAuthException {

        OAuth20ResponseTypeHandler result = null;

        /*
         * Just iterate through our known grant types
         */
        boolean foundCode = false;
        boolean foundToken = false;
        boolean allowed = false;
        if (responseType != null) {
            String[] responseTypes = responseType.split(" ");
            // right now, we only support "code", "token" and "id_token token"
            for (String rType : responseTypes) {
                if (OAuth20Constants.RESPONSE_TYPE_CODE.equals(rType)) {
                    foundCode = true;
                    if (responseTypes.length == 1) { // only "code"
                        allowed = config.isGrantTypeAllowed(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE);
                        if (!allowed) {
                            String allowedGrantTypesAsString = "";
                            if (config instanceof OAuth20Provider) {
                                allowedGrantTypesAsString = Utils.toString(((OAuth20Provider) config).getGrantTypesAllowed()); //TODO
                            }
                            Tr.error(tc, "OIDC_SERVER_GRANT_TYPE_NOT_ALLOWED_ERR", new Object[]
                            { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE, allowedGrantTypesAsString });
                            throw new OAuthConfigurationException(OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED, responseType, null);
                        }
                        result = new OAuth20ResponseTypeHandlerCodeImpl();
                    }
                } else if (OAuth20Constants.RESPONSE_TYPE_TOKEN.equals(rType) ||
                           OIDCConstants.RESPONSE_TYPE_ID_TOKEN.equals(rType)) {
                    // This allows both RESPONSE_TYPE_TOKEN and RESPONSE_TYPE_ID_TOKEN
                    // Further checks is inside OIDCResponseTypeHandlerImplicitImpl 
                    foundToken = true;
                    allowed = config.isGrantTypeAllowed(OAuth20Constants.GRANT_TYPE_IMPLICIT);
                    if (!allowed) {
                        String allowedGrantTypesAsString = "";
                        if (config instanceof OAuth20Provider) {
                            allowedGrantTypesAsString = Utils.toString(((OAuth20Provider) config).getGrantTypesAllowed()); //TODO
                        }
                        Tr.error(tc, "OIDC_SERVER_GRANT_TYPE_NOT_ALLOWED_ERR", new Object[]
                        { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE, allowedGrantTypesAsString });
                        throw new OAuthConfigurationException(OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED, responseType, null);
                    }
                    result = new OIDCResponseTypeHandlerImplicitImpl();
                } else {
                    Tr.error(tc, "OIDC_SERVER_INVALID_RESPONSE_TYPE_ERR", new Object[] { rType, "{'code', 'token', 'id_token token'}" });
                    throw new OAuth20InvalidResponseTypeException("security.oauth20.error.invalid.responsetype", responseType);
                }
            }
        }
        // check if only one of ResponseTypes is set.
        /*
         * false ^ false = false
         * false ^ true = true
         * true ^ false = true
         * true ^ true = false
         */
        if (!(foundToken ^ foundCode)) {
            // TODO: any exception thrown here isn't handled properly in the runtime
            if (foundToken) {
                Tr.error(tc, "OIDC_SERVER_MULTIPLE_RESPONSE_TYPE_ERR", new Object[] { "code", "token id_token" });
                throw new OAuth20InvalidResponseTypeException("security.oauth20.error.multiple.responsetype", responseType, "code", "token id_token");
            } else {
                Tr.error(tc, "OIDC_SERVER_INVALID_RESPONSE_TYPE_ERR", new Object[] { responseType, "{'code', 'token', 'id_token token'}" });
                throw new OAuth20InvalidResponseTypeException("security.oauth20.error.invalid.responsetype", responseType);
            }
        }

        return result;
    }
}
