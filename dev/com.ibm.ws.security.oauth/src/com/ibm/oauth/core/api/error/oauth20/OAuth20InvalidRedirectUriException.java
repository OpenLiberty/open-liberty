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

/**
 * Represents an invalid redirect URI exception in an OAuth request.
 */
public class OAuth20InvalidRedirectUriException extends OAuth20Exception {

    private static final TraceComponent tc = Tr.register(OAuth20InvalidRedirectUriException.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final long serialVersionUID = 1L;
    private static final String END_USER_MSG_KEY = "security.oauth20.error.invalid.redirecturi.enduser";
    private String _redirectURI;
    private String _registeredRedirectURI;

    /**
     * Creates a OAuth20InvalidRedirectUriException.
     * 
     * @param redirectURI A redirect URI in a request.
     * @param cause A root exception.
     */

    public OAuth20InvalidRedirectUriException(String redirectURI, Throwable cause) {
        super(INVALID_REQUEST, "The redirect URI parameter was invalid: " + redirectURI, cause);
        _redirectURI = redirectURI;
    }

    // Liberty
    public OAuth20InvalidRedirectUriException(String msgKey, String redirectURI, Throwable cause) {
        this(msgKey, redirectURI, null, cause);
    }

    // Liberty
    public OAuth20InvalidRedirectUriException(String msgKey, String redirectUri, String registeredRedirectUri, Throwable cause) {
        super(INVALID_REQUEST, Tr.formatMessage(tc, msgKey, new Object[] { redirectUri, registeredRedirectUri }), cause);
        _registeredRedirectURI = registeredRedirectUri;
        _redirectURI = redirectUri;
        _msgKey = msgKey;
    }

    // Liberty
    @Override
    public String formatSelf(Locale locale, String encoding) {
        return MessageFormat.format(OAuth20ExceptionUtil.getResourceBundle(locale).getString(END_USER_MSG_KEY), (Object[]) null);
    }

    /**
     * @return redirect URI.
     */
    public String getRedirectURI() {
        return _redirectURI;
    }

    /**
     * @return registered redirect URI
     */
    public String getRegisteredRedirectURI() {
        return _registeredRedirectURI;
    }
}
