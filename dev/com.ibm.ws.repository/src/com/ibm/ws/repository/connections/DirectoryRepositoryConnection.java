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
package com.ibm.ws.repository.connections;

import java.io.File;

import com.ibm.ws.repository.connections.internal.AbstractRepositoryConnection;
import com.ibm.ws.repository.transport.client.DirectoryClient;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;

/**
 *
 */
public class DirectoryRepositoryConnection extends AbstractRepositoryConnection implements RepositoryConnection {

    private final File _root;

    /**
     * @param type
     */
    public DirectoryRepositoryConnection(File root) {
        _root = root;
    }

    public File getRoot() {
        return _root;
    }

    /** {@inheritDoc} */
    @Override
    public String getRepositoryLocation() {
        return _root.getAbsolutePath();
    }

    @Override
    public RepositoryReadableClient createClient() {
        return new DirectoryClient(getRoot());
    }

}
