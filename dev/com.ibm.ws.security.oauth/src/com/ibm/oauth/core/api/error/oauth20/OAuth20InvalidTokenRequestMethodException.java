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
 * Represents an invalid token request method exception in an OAuth request.
 */
public class OAuth20InvalidTokenRequestMethodException extends OAuth20Exception {

    private static final TraceComponent tc = Tr.register(OAuth20InvalidTokenRequestMethodException.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final long serialVersionUID = 1L;
    private String _method;

    /**
     * Creates a OAuth20InvalidTokenRequestMethodException.
     * 
     * @param method the token request method.
     */
    public OAuth20InvalidTokenRequestMethodException(String method) {
        super(INVALID_REQUEST, "An invalid HTTP method was used at the token endpoint: " + method, null);
        _method = method;
    }

    // Liberty
    public OAuth20InvalidTokenRequestMethodException(String msgKey, String method) {
        super(INVALID_REQUEST, Tr.formatMessage(tc, msgKey, new Object[] { method }), null);
        _msgKey = msgKey;
        _method = method;
    }

    // Liberty
    @Override
    public String formatSelf(Locale locale, String encoding) {
        return MessageFormat.format(OAuth20ExceptionUtil.getResourceBundle(locale).getString(_msgKey),
                new Object[] { WebUtils.encode(_method, locale, encoding) });
    }

    /**
     * @return the token request method.
     */
    public String getMethod() {
        return _method;
    }

}
