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
package com.ibm.ws.security.openidconnect.server.plugins;

import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;

/**
 * Represents an exception while processing OAuth 2.0 request and response.
 * This class is the base class for all OAuth 2.0 component exceptions.
 */
public class OIDCMissingScopeException extends OAuth20Exception {

    private static final long serialVersionUID = -1;

    /**
     * Creates a OAuth20Exception.
     * 
     * @param error A message for the OAuth 2.0 error.
     * @param message A message for the general OAuth error.
     * @param cause A root exception.
     */

    public OIDCMissingScopeException(String error, String message, Throwable cause) {
        super(error, message, cause);
        _error = error;
    }

    /**
     * @return the error response associated with this OAuth 2.0 exception. These
     *         errors correspond to the mandated error field in OAuth 2.0 protocol.
     */
    public String getError() {
        return _error;
    }

}
