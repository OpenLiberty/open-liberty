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
package com.ibm.ws.lars.testutils.fixtures;

import java.io.File;

import com.ibm.ws.lars.testutils.clients.ZipWriteableClient;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.ZipRepositoryConnection;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.client.RepositoryWriteableClient;

public class ZipRepositoryFixture extends RepositoryFixture {

    public static ZipRepositoryFixture createFixture(File zip) {
        ZipRepositoryConnection connection = new ZipRepositoryConnection(zip);
        RepositoryConnection adminConnection = connection;
        RepositoryConnection userConnection = connection;
        RepositoryReadableClient adminClient = connection.createClient();
        RepositoryReadableClient userClient = adminClient;
        RepositoryWriteableClient writableClient = new ZipWriteableClient(zip);

        return new ZipRepositoryFixture(adminConnection, userConnection, adminClient, writableClient, userClient, zip);
    }

    private final File zip;

    private ZipRepositoryFixture(RepositoryConnection adminConnection, RepositoryConnection userConnection, RepositoryReadableClient adminClient,
                                 RepositoryWriteableClient writableClient, RepositoryReadableClient userClient, File zip) {
        super(adminConnection, userConnection, adminClient, writableClient, userClient);
        this.zip = zip;
    }

    /** {@inheritDoc} */
    @Override
    protected void createCleanRepository() throws Exception {
        if (zip.exists()) {
            zip.delete();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void cleanupRepository() throws Exception {
        if (zip.exists()) {
            zip.delete();
        }
    }

    @Override
    public String toString() {
        return "Zip repo";
    }

    @Override
    public boolean isUpdateSupported() {
        return false;
    }

}
