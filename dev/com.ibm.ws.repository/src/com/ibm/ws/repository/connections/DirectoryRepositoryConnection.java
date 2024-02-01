/*******************************************************************************
 * Copyright (c) 2015, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.connections;

import java.io.File;

import com.ibm.ws.repository.common.enums.ReadMode;
import com.ibm.ws.repository.connections.internal.AbstractRepositoryConnection;
import com.ibm.ws.repository.transport.client.DirectoryClient;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;

/**
 *
 */
public class DirectoryRepositoryConnection extends AbstractRepositoryConnection implements RepositoryConnection {

    private final File _root;
    private final ReadMode _readMode;
    private DirectoryClient client;

    /**
     * @deprecated Use {@link #DirectoryRepositoryConnection(File, ReadMode)}
     */
    @Deprecated
    public DirectoryRepositoryConnection(File root) {
        this(root, ReadMode.DETECT_CHANGES);
    }

    public DirectoryRepositoryConnection(File root, ReadMode readMode) {
        _root = root;
        _readMode = readMode;
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
    public synchronized RepositoryReadableClient createClient() {
        if (client == null) {
            client = new DirectoryClient(getRoot(), _readMode);
        }
        return client;
    }

}
