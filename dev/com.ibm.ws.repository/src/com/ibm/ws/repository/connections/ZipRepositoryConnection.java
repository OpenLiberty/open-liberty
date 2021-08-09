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
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.client.ZipClient;

/**
 *
 */
public class ZipRepositoryConnection extends AbstractRepositoryConnection implements RepositoryConnection {

    private final File _zip;

    /**
     * @param type
     */
    public ZipRepositoryConnection(File zip) {
        _zip = zip;
    }

    public File getZip() {
        return _zip;
    }

    /** {@inheritDoc} */
    @Override
    public String getRepositoryLocation() {
        return _zip.getAbsolutePath();
    }

    @Override
    public RepositoryReadableClient createClient() {
        return new ZipClient(getZip());
    }

}
