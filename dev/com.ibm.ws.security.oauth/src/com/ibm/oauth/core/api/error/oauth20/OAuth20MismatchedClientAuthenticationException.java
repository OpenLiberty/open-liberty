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
 * Represents a mismatch exception that the client ID does not match the authenticated client.
 */
public class OAuth20MismatchedClientAuthenticationException extends OAuth20Exception {

    private static final TraceComponent tc = Tr.register(OAuth20MismatchedClientAuthenticationException.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final long serialVersionUID = 1L;
    private String _clientId;
    private String _authenticatedClient;

    /**
     * Create OAuth20MismatchedClientAuthenticationException
     * 
     * @param clientId from request
     * @param authenticatedClient representing the authenticated client from caller.
     */
    public OAuth20MismatchedClientAuthenticationException(String clientId, String authenticatedClient) {
        super(INVALID_REQUEST, "The client_id passed in the request to the token endpoint: "
                + clientId + " did not match the authenticated client provided in the API call: "
                + authenticatedClient, null);
        _clientId = clientId;
        _authenticatedClient = authenticatedClient;
    }

    // Liberty
    public OAuth20MismatchedClientAuthenticationException(String msgKey, String clientId, String authenticatedClient) {
        super(INVALID_REQUEST, Tr.formatMessage(tc, msgKey, new Object[] { clientId, authenticatedClient }), null);
        _msgKey = msgKey;
        _clientId = clientId;
        _authenticatedClient = authenticatedClient;
    }

    // Liberty
    @Override
    public String formatSelf(Locale locale, String encoding) {
        return MessageFormat.format(OAuth20ExceptionUtil.getResourceBundle(locale).getString(_msgKey),
                new Object[] { WebUtils.encode(_clientId, locale, encoding), _authenticatedClient });
    }

    /**
     * @return client ID.
     */
    public String getClientId() {
        return _clientId;
    }

    /**
     * @return the authenticated client.
     */
    public String getAuthenticatedClient() {
        return _authenticatedClient;
    }

}
