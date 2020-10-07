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
 * Represents a refresh token exception due to mismatched client.
 */
public class OAuth20RefreshTokenInvalidClientException extends OAuth20Exception {

    private static final TraceComponent tc = Tr.register(OAuth20RefreshTokenInvalidClientException.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final long serialVersionUID = 1L;
    private String _refresh;
    private String _clientId;

    /**
     * Create OAuth20RefreshTokenInvalidClientException
     * 
     * @param refresh the token to be refreshed
     * @param clientId the request's client ID.
     */
    public OAuth20RefreshTokenInvalidClientException(String refresh, String clientId) {
        super(INVALID_GRANT, "The refresh token: " + refresh
                + " does not belong to the client attempting to use it: "
                + clientId, null);
        _refresh = refresh;
        _clientId = clientId;
    }

    // Liberty
    public OAuth20RefreshTokenInvalidClientException(String msgKey, String refresh, String clientId) {
        super(INVALID_GRANT, Tr.formatMessage(tc, msgKey, new Object[] { refresh, clientId }), null);
        _msgKey = msgKey;
        _refresh = refresh;
        _clientId = clientId;
    }

    // Liberty
    @Override
    public String formatSelf(Locale locale, String encoding) {
        return MessageFormat.format(OAuth20ExceptionUtil.getResourceBundle(locale).getString(_msgKey),
                new Object[] { WebUtils.encode(_refresh, locale, encoding),
                        WebUtils.encode(_clientId, locale, encoding) });
    }

    /**
     * @return the refresh token sent in the request.
     */
    String getRefreshToken() {
        return _refresh;
    }

    /**
     * @return client ID.
     */
    String getClientId() {
        return _clientId;
    }

}
