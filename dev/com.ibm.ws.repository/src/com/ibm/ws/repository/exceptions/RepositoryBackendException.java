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

public abstract class RepositoryBackendException extends RepositoryException {

    private static final long serialVersionUID = -6160923624838346326L;
    private RepositoryConnection _failingBackend;

    public RepositoryBackendException() {
        super();
    }

    public RepositoryBackendException(String message, RepositoryConnection connection) {
        super(message);
        _failingBackend = connection;
    }

    public RepositoryBackendException(Throwable cause, RepositoryConnection connection) {
        super(cause);
        _failingBackend = connection;
    }

    public RepositoryBackendException(String message, Throwable cause, RepositoryConnection connection) {
        super(message, cause);
        _failingBackend = connection;
    }

    @Override
    public Throwable getCause() {
        return super.getCause();
    }

    public RepositoryConnection getFailingConnection() {
        return _failingBackend;
    }

}
