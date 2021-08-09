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
import java.util.Collection;

import com.ibm.ws.repository.connections.internal.AbstractRepositoryConnection;
import com.ibm.ws.repository.transport.client.LooseFileClient;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;

/**
 * Connection to a repository which consists of a loose collection of json files. No
 * attachments are supported for this type of repository and an UnsupportedOperationException
 * will be thrown if any attachment operations are invoked.
 */
public class LooseFileRepositoryConnection extends AbstractRepositoryConnection implements RepositoryConnection {

    // The collection of json files that this repository represents
    private final Collection<File> _assets;

    /**
     * @param assets The collection of json file names that this repository represents
     */
    public LooseFileRepositoryConnection(Collection<File> assets) {
        _assets = assets;
    }

    /**
     * This repository has no concept of a location since it contains a collection
     * of loose files
     */
    @Override
    public String getRepositoryLocation() {
        return null;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public RepositoryReadableClient createClient() {
        return new LooseFileClient(_assets);
    }

}
