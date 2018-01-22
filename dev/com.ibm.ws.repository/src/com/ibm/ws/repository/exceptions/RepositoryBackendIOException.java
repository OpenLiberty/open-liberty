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

public class RepositoryBackendIOException extends RepositoryBackendException {

    private static final long serialVersionUID = -5918785594985895160L;

    public RepositoryBackendIOException() {
        super();
    }

    public RepositoryBackendIOException(String message, RepositoryConnection connection) {
        super(message, connection);
    }

    public RepositoryBackendIOException(Throwable cause, RepositoryConnection connection) {
        super(cause, connection);
    }

    public RepositoryBackendIOException(String message, Throwable cause, RepositoryConnection connection) {
        super(message, cause, connection);
    }

    @Override
    public Throwable getCause() {
        return super.getCause();
    }

}
