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
 * Represents an error that occurs when the redirect URI passed in the request
 * to the token endpoint does not match the redirect URI associated with the grant.
 */
public class OAuth20MismatchedRedirectUriException extends OAuth20Exception {

    private static final TraceComponent tc = Tr.register(OAuth20MismatchedRedirectUriException.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final long serialVersionUID = 1L;
    private String _receivedRedirectURI;
    private String _issuedToRedirectURI;

    /**
     * Create OAuth20MismatchedRedirectUriException
     * 
     * @param receivedRedirectURI a redirect URI passed in the request.
     * @param issuedToRedirectURI a redirect URI associated with the grant.
     */
    public OAuth20MismatchedRedirectUriException(String receivedRedirectURI, String issuedToRedirectURI) {
        super(INVALID_REQUEST, "The received redirect URI: "
                + receivedRedirectURI
                + " does not match the redirect URI the grant was issued to: "
                + issuedToRedirectURI, null);
        _receivedRedirectURI = receivedRedirectURI;
        _issuedToRedirectURI = issuedToRedirectURI;
    }

    // Liberty
    public OAuth20MismatchedRedirectUriException(String msgKey, String receivedRedirectURI, String issuedToRedirectURI) {
        super(INVALID_CLIENT, Tr.formatMessage(tc, msgKey, new Object[] { receivedRedirectURI, issuedToRedirectURI }), null);
        _msgKey = msgKey;
        _receivedRedirectURI = receivedRedirectURI;
        _issuedToRedirectURI = issuedToRedirectURI;
    }

    // Liberty
    @Override
    public String formatSelf(Locale locale, String encoding) {
        return MessageFormat.format(OAuth20ExceptionUtil.getResourceBundle(locale).getString(_msgKey),
                new Object[] { WebUtils.encode(_receivedRedirectURI, locale, encoding), _issuedToRedirectURI });
    }

    /**
     * @return the received redirect URI in a request.
     */
    public String getReceivedRedirectURI() {
        return _receivedRedirectURI;
    }

    /**
     * @return the redirect URI associated with the grant.
     */
    public String getIssuedToRedirectURI() {
        return _issuedToRedirectURI;
    }

}
