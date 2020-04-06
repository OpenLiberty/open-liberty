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

import com.ibm.oauth.core.api.error.OAuthException;

/**
 * Represents an exception while processing OAuth 2.0 request and response.
 * This class is the base class for all OAuth 2.0 component exceptions.
 */
public class OAuth20Exception extends OAuthException {

    private static final long serialVersionUID = 6451370128254992895L;

    public static final String INVALID_REQUEST = "invalid_request";

    public static final String UNAUTHORIZED_CLIENT = "unauthorized_client";

    public static final String ACCESS_DENIED = "access_denied";

    public static final String UNSUPPORTED_RESPONSE_TPE = "unsupported_response_type";

    public static final String INVALID_SCOPE = "invalid_scope";

    public static final String SERVER_ERROR = "server_error";

    public static final String TEMPORARILY_UNAVAILABLE = "temporarily_unavailable";

    public static final String INVALID_CLIENT = "invalid_client";

    public static final String INVALID_GRANT = "invalid_grant";

    public static final String UNSUPPORED_GRANT_TPE = "unsupported_grant_type";

    public static final String INVALID_TOKEN = "invalid_token";

    public static final String INSUFFICIENT_SCOPE = "insufficient_scope";

    protected String _error;

    /**
     * Creates a OAuth20Exception.
     * 
     * @param error A message for the OAuth 2.0 error.
     * @param message A message for the general OAuth error.
     * @param cause A root exception.
     */
    public OAuth20Exception(String error, String message, Throwable cause) {
        super(message, cause);
        _error = error;
    }

    /**
     * @return the error response associated with this OAuth 2.0 exception. These
     *         errors correspond to the mandated error field in OAuth 2.0 protocol.
     */
    public String getError() {
        return _error;
    }

    public String formatSelf(Locale locale, String encoding) {
        if (_msgKey != null) {
            return MessageFormat.format(OAuth20ExceptionUtil.getResourceBundle(locale).getString(_msgKey), new Object[] {});
        } else {
            return getMessage();
        }
    }

}
