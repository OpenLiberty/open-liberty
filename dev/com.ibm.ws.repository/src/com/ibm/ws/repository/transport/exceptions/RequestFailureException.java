/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.transport.exceptions;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This exception is thrown when there was an error in the HTTP request to the
 * Massive repository that lead to a response code that was not successful
 * (2xx). The response code is available on {@link #getResponseCode()} and the
 * message from the error stream on the HTTP URL connection is available via {@link #getErrorMessage()} with the full contents of the error stream being
 * available through {@link #getErrorStreamContents()}.
 */
public class RequestFailureException extends Exception {

    private static final long serialVersionUID = -2336710508495285178L;
    private final int responseCode;
    private final String errorMessage;
    private final String errorStreamContents;

    public RequestFailureException(int responseCode, String errorMessage, URL url, String errorStreamMessage) {
        super("Server returned HTTP response code: " + responseCode + " for URL: " + url.toString() + " error message: \"" + errorMessage + "\"");
        this.responseCode = responseCode;
        this.errorMessage = errorMessage;
        this.errorStreamContents = errorStreamMessage;
    }

    /**
     * Returns the response code from the HTTP URL connection when connecting to the repository backend.
     *
     * @return The response code
     * @see HttpURLConnection#getResponseCode()
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Returns the error message written to the error stream on the HTTP URL connection when connecting to the repository backend.
     *
     * @return The error message
     * @see HttpURLConnection#getErrorStream()
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns the full contents of the error stream.
     *
     * @return The contents of the error stream
     * @see HttpURLConnection#getErrorStream()
     */
    public String getErrorStreamContents() {
        return errorStreamContents;
    }

}
