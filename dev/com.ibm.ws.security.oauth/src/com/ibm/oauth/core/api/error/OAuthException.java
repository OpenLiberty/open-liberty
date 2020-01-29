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
package com.ibm.oauth.core.api.error;

import java.util.Locale;

/**
 * Represents an exception while processing OAuth request and response.
 * This class is the base class for all OAuth component exceptions.
 */
public abstract class OAuthException extends Exception {

    private static final long serialVersionUID = 1L;

    protected String _msgKey = null;
    protected Object[] _objs = null;

    /**
     * Creates a OAuthException.
     *
     * @param message A message for the error.
     * @param cause A root exception.
     */
    public OAuthException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Gets error type for this OAuth exception
     *
     * @return error type
     */
    public abstract String getError();

    public abstract String formatSelf(Locale locale, String encoding);

    public String getMsgKey() {
        return _msgKey;
    }

    public Object[] getObjects() {
        return _objs;
    }

}
