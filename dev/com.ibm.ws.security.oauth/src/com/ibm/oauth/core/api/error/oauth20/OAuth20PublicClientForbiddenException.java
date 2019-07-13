/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
 * Represents an access denied exception for public client.
 */
public class OAuth20PublicClientForbiddenException extends OAuth20Exception {

    private static final TraceComponent tc = Tr.register(OAuth20PublicClientForbiddenException.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final long serialVersionUID = 1L;
    private String _clientId;

    /**
     * Create OAuth20PublicClientForbiddenException.
     * 
     * @param clientId
     */
    public OAuth20PublicClientForbiddenException(String clientId) {
        super(INVALID_CLIENT, "A public client attempted to access the token endpoint and public clients are forbidden in this component configuration. The client_id is: "
                + clientId, null);
        _clientId = clientId;
    }

    // Liberty
    public OAuth20PublicClientForbiddenException(String msgKey, String clientId) {
        super(INVALID_CLIENT, Tr.formatMessage(tc, msgKey, new Object[] { clientId }), null);
        _msgKey = msgKey;
        _clientId = clientId;
    }

    // Liberty
    @Override
    public String formatSelf(Locale locale, String encoding) {
        return MessageFormat.format(OAuth20ExceptionUtil.getResourceBundle(locale).getString(_msgKey),
                new Object[] { WebUtils.encode(_clientId, locale, encoding) });
    }

    /**
     * @return client ID.
     */
    public String getClientId() {
        return _clientId;
    }

}
