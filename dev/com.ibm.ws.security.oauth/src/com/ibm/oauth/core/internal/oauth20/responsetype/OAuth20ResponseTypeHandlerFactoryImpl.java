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
package com.ibm.oauth.core.internal.oauth20.responsetype;

import java.util.logging.Logger;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.config.OAuthComponentConfigurationConstants;
import com.ibm.oauth.core.api.error.OAuthConfigurationException;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidResponseTypeException;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;
import com.ibm.oauth.core.internal.oauth20.responsetype.impl.OAuth20ResponseTypeHandlerCodeImpl;
import com.ibm.oauth.core.internal.oauth20.responsetype.impl.OAuth20ResponseTypeHandlerTokenImpl;

public class OAuth20ResponseTypeHandlerFactoryImpl implements OAuth20ResponseTypeHandlerFactory {

    final static String CLASS = OAuth20ResponseTypeHandlerFactoryImpl.class
            .getName();
    final static Logger _log = Logger.getLogger(CLASS);

    OAuthComponentConfiguration _oldconfig = null; // this is not in use in tivoli code

    // oidc10 not needed in default OAuth20GrantTypeHandler
    public void init(OAuthComponentConfiguration oldconfig) {
        _oldconfig = oldconfig;
    };

    public synchronized OAuth20ResponseTypeHandler getHandler(
            String responseType, OAuth20ConfigProvider config)
            throws OAuthException {
        String methodName = "getHandler";
        _log.entering(CLASS, methodName, new Object[] { responseType });
        OAuth20ResponseTypeHandler result = null;
        try {
            /*
             * Just iterate through our known grant types
             */
            boolean found = false;
            boolean allowed = false;
            if (responseType != null) {
                if (responseType.equals(OAuth20Constants.RESPONSE_TYPE_CODE)) {
                    found = true;
                    allowed = config.isGrantTypeAllowed(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE);
                    result = new OAuth20ResponseTypeHandlerCodeImpl();
                } else if (responseType.equals(OAuth20Constants.RESPONSE_TYPE_TOKEN)) {
                    found = true;
                    allowed = config.isGrantTypeAllowed(OAuth20Constants.GRANT_TYPE_IMPLICIT);
                    result = new OAuth20ResponseTypeHandlerTokenImpl();
                }
            }
            if (!found) {
                throw new OAuth20InvalidResponseTypeException("security.oauth20.error.invalid.responsetype", responseType);
            }
            if (!allowed) {
                throw new OAuthConfigurationException("security.oauth.error.mismatch.responsetype.exception",
                        OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED,
                        responseType, null);
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }
        return result;
    }
}
