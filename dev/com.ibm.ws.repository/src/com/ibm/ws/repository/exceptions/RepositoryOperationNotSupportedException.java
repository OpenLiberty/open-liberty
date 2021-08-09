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

import com.ibm.ws.repository.connections.RepositoryConnection;

/**
 * This exception is thrown if an operation is attempted on a client that does not support that operation.
 * For example some clients can only perform read operations and trying to perform write operations will
 * cause this exception to be thrown
 */
public class RepositoryOperationNotSupportedException extends RepositoryBackendException {

    private static final long serialVersionUID = -8787752138728688643L;

    public RepositoryOperationNotSupportedException() {
        super();
    }

    public RepositoryOperationNotSupportedException(String message, RepositoryConnection connection) {
        super(message, connection);
    }

    public RepositoryOperationNotSupportedException(Throwable cause, RepositoryConnection connection) {
        super(cause, connection);
    }

    public RepositoryOperationNotSupportedException(String message, Throwable cause, RepositoryConnection connection) {
        super(message, cause, connection);
    }

    @Override
    public Throwable getCause() {
        return super.getCause();
    }
}
