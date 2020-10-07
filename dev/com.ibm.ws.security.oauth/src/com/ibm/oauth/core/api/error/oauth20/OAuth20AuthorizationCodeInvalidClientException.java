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
 * Represents an illegal authorization code exception presented by a client.
 */
public class OAuth20AuthorizationCodeInvalidClientException extends OAuth20Exception {

    private static final TraceComponent tc = Tr.register(OAuth20AuthorizationCodeInvalidClientException.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final long serialVersionUID = 1L;
    private String _code;
    private String _clientId;

    /**
     * Creates a OAuth20AuthorizationCodeInvalidClientException.
     * 
     * @param code An authorization code presented by a client.
     * @param clientId A client Id representing a client.
     */
    public OAuth20AuthorizationCodeInvalidClientException(String code, String clientId) {
        super(INVALID_GRANT, "The authorization code: " + code
                + " does not belong to the client attempting to use it: "
                + clientId, null);
        _code = code;
        _clientId = clientId;
    }

    // Liberty
    public OAuth20AuthorizationCodeInvalidClientException(String msgKey, String code, String clientId) {
        super(INVALID_GRANT, Tr.formatMessage(tc, msgKey, new Object[] { code, clientId }), null);
        _code = code;
        _clientId = clientId;
        _msgKey = msgKey;
    }

    // Liberty
    @Override
    public String formatSelf(Locale locale, String encoding) {
        return MessageFormat.format(OAuth20ExceptionUtil.getResourceBundle(locale).getString(_msgKey),
                new Object[] { WebUtils.encode(_clientId, locale, encoding) });
    }

    /**
     * @return the authorization code presented in the request.
     */
    String getCode() {
        return _code;
    }

    /**
     * @return the client ID presented in the request.
     */
    String getClientId() {
        return _clientId;
    }

}
