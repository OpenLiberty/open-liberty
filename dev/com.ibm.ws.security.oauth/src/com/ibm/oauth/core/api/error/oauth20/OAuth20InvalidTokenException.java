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
package com.ibm.oauth.core.api.error.oauth20;

import java.text.MessageFormat;
import java.util.Locale;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.web.WebUtils;

/**
 * Represents an invalid token exception in an OAuth request.
 */
public class OAuth20InvalidTokenException extends OAuth20Exception {

    private static final TraceComponent tc = Tr.register(OAuth20InvalidTokenException.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final long serialVersionUID = 1L;
    private String _key;
    private String _tokenType;
    private String _tokenSubType;

    /**
     * Creates a OAuth20InvalidTokenException.
     * 
     * @param key the token cache lookup key which is a unique identifier for a token.
     * @param tokenType the token type for an OAuth request.
     * @param tokenSubType the token sub-type for an OAuth request.
     * @param isTokenRequest a boolean to indicate if client is making a token request.
     */
    public OAuth20InvalidTokenException(String key, String tokenType, String tokenSubType, boolean isTokenRequest) {
        super(null, "The token with key: " + key + " type: " + tokenType
                + " subType: " + tokenSubType
                + " was not found in the token cache.", null);
        resolveErrorType(isTokenRequest);
        _key = key;
        _tokenType = tokenType;
        _tokenSubType = tokenSubType;
    }

    // Liberty
    public OAuth20InvalidTokenException(String msgKey, String key, String tokenType, String tokenSubType, boolean isTokenRequest) {
        super(null, Tr.formatMessage(tc, msgKey, new Object[] { key, tokenType, tokenSubType }), null);
        resolveErrorType(isTokenRequest);
        _msgKey = msgKey;
        _key = key;
        _tokenType = tokenType;
        _tokenSubType = tokenSubType;

    }

    // Liberty
    @Override
    public String formatSelf(Locale locale, String encoding) {
        return MessageFormat.format(OAuth20ExceptionUtil.getResourceBundle(locale).getString(_msgKey),
                new Object[] { WebUtils.encode(_key, locale, encoding),
                        WebUtils.encode(_tokenType, locale, encoding),
                        WebUtils.encode(_tokenSubType, locale, encoding) });
    }

    /**
     * @return the key
     */
    public String getKey() {
        return _key;
    }

    /**
     * @return the token type.
     */
    public String getTokenType() {
        return _tokenType;
    }

    /**
     * @return token sub-type.
     */
    public String getTokenSubType() {
        return _tokenSubType;
    }

    /**
     * @param isTokenRequest
     */
    private void resolveErrorType(boolean isTokenRequest) {
        if (isTokenRequest) {
            _error = INVALID_GRANT;
        } else {
            _error = INVALID_TOKEN;
        }
    }

}
