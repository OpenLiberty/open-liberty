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
 * Represents an invalid client exception in an OAuth request.
 */
public class OAuth20InvalidClientException extends OAuth20Exception {

    private static final TraceComponent tc = Tr.register(OAuth20InvalidClientException.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final long serialVersionUID = 1L;
    private static final String END_USER_MSG_KEY = "security.oauth20.error.invalid.client.enduser";
    private String _clientId;

    /**
     * Creates a OAuth20InvalidClientException.
     * 
     * @param clientId A client's ID.
     * @param isTokenRequest A boolean to indicate if client is making token request.
     */
    public OAuth20InvalidClientException(String clientId, boolean isTokenRequest) {
        super(null, "The client could not be found: " + clientId, null);
        resolveErrorType(isTokenRequest);
        _clientId = clientId;
    }

    // Liberty
    public OAuth20InvalidClientException(String msgKey, String clientId, boolean isTokenRequest) {
        super(null, Tr.formatMessage(tc, msgKey, clientId), null);
        resolveErrorType(isTokenRequest);
        _msgKey = msgKey;
        _clientId = clientId;
    }

    // Liberty
    @Override
    public String formatSelf(Locale locale, String encoding) {
        if (isMessageForEndUser()) {
            return getFormattedMsg(locale, END_USER_MSG_KEY, (Object[]) null);
        } else if (_msgKey != null) {
            return getFormattedMsg(locale, _msgKey, new Object[] { WebUtils.encode(_clientId, locale, encoding) });
        }
        return getMessage();
    }

    private boolean isMessageForEndUser() {
        return UNAUTHORIZED_CLIENT.equals(_error);
    }

    private String getFormattedMsg(Locale locale, String key, Object[] arguments) {
        return MessageFormat.format(OAuth20ExceptionUtil.getResourceBundle(locale).getString(key), arguments);
    }

    /**
     * @return the client ID in the request.
     */
    public String getClientId() {
        return _clientId;
    }

    private void resolveErrorType(boolean isTokenRequest) {
        if (isTokenRequest) {
            _error = INVALID_CLIENT;
        } else {
            _error = UNAUTHORIZED_CLIENT;
        }
    }

}
