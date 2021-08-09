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
package com.ibm.ws.repository.exceptions;

import java.net.HttpURLConnection;

import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;

/**
 * This exception is thrown when there was an error in the HTTP request to the repository that lead to a response code that was not successful (2xx). The response code is
 * available on {@link #getResponseCode()} and the message from the error stream on the HTTP URL connection is available via {@link #getErrorMessage()} with the full contents of
 * the error stream being available through {@link #getErrorStreamContents()}.
 */
public class RepositoryBackendRequestFailureException extends RepositoryBackendException {

    private static final long serialVersionUID = 5987530731417694030L;
    private final RequestFailureException requestFailureException;

    public RepositoryBackendRequestFailureException(RequestFailureException cause, RepositoryConnection connection) {
        super(cause, connection);
        this.requestFailureException = cause;
    }

    /**
     * Returns the response code from the HTTP URL connection when connecting to the repository backend.
     *
     * @return The response code
     * @see HttpURLConnection#getResponseCode()
     */
    public int getResponseCode() {
        return requestFailureException.getResponseCode();
    }

    /**
     * Returns the error message written to the error stream on the HTTP URL connection when connecting to the repository backend.
     *
     * @return The error message
     * @see HttpURLConnection#getErrorStream()
     */
    public String getErrorMessage() {
        return requestFailureException.getErrorMessage();
    }

    /**
     * Returns the full contents of the error stream.
     *
     * @return The contents of the error stream
     * @see HttpURLConnection#getErrorStream()
     */
    public String getErrorStreamContents() {
        return requestFailureException.getErrorStreamContents();
    }

}
