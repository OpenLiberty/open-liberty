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
package com.ibm.oauth.core.api.error;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;

/**
 *
 */
public class OidcServerException extends OAuth20Exception {

    private static final long serialVersionUID = -5649910420133501512L;

    private static final String OAUTH2_PARM_ERROR = "error";
    private static final String OAUTH2_PARM_ERROR_DESC = "error_description";

    private final String _errorCode;
    private final String _errorDescription;
    private int _httpStatus = -1;

    /**
     * Constructs an instance of this exception with the referenced arguments.
     * 
     * @param desription
     *            The error description for this exception. Can be <code>null</code> if the code is null
     * 
     * @param code
     *            The error code for this exception. Specify <code>null</code> if the code is unknown.
     * @param cause
     *            exception causing the problem 
     * @param httpStatus
     *            The HTTP status code to associate to this exception.
     */
    public OidcServerException(String description, String code, int httpStatus, Throwable cause) {
        super(code, description, cause); //$NON-NLS-1$
        _errorDescription = description;
        _errorCode = code;
        _httpStatus = httpStatus;
    }

    public OidcServerException(String description, String code, int httpStatus) {
        super(code, description, null); //$NON-NLS-1$
        _errorDescription = description;
        _errorCode = code;
        _httpStatus = httpStatus;
    }

    /**
     * Returns the error description for this exception, as an English string.
     * 
     * @return The OAuth error description.
     */
    public String getErrorDescription() {
        return _errorDescription;
    }

    /**
     * Returns the error code associated to this exception.
     * 
     * @return The error code for this exception.
     */
    public String getErrorCode() {
        return _errorCode;
    }

    /**
     * Returns the HTTP status code associated to this exception.
     * 
     * @return The HTTP status code. Will be -1 if no code was specified.
     */
    public int getHttpStatus() {
        return _httpStatus;
    }

    public boolean isComplete() {
        return !OidcOAuth20Util.isNullEmpty(_errorCode) && !OidcOAuth20Util.isNullEmpty(_errorDescription) && _httpStatus != -1;
    }

    /**
     * Constructs an OAuth 2.0 error response from the exception state, per RFC6749 section 5.2.
     * 
     * @return An error JSON string - never <code>null</code>.
     */
    public String toJSON() {

        JsonObject errorObject = new JsonObject();
        if (_errorCode != null) {
            errorObject.add(OAUTH2_PARM_ERROR, new JsonPrimitive(_errorCode));
        }
        if (_errorDescription != null) {
            errorObject.add(OAUTH2_PARM_ERROR_DESC, new JsonPrimitive(_errorDescription));
        }
        return errorObject.toString();
    }
}
