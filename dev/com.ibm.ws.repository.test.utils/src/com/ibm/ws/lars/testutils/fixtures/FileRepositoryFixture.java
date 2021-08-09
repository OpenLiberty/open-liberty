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

import com.ibm.ws.lars.testutils.clients.DirectoryWriteableClient;
import com.ibm.ws.repository.connections.DirectoryRepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.client.RepositoryWriteableClient;

/**
 *
 */
public class FileRepositoryFixture extends RepositoryFixture {

    private final File root;

    public static FileRepositoryFixture createFixture(File root) {
        DirectoryRepositoryConnection connection = new DirectoryRepositoryConnection(root);
        RepositoryConnection adminConnection = connection;
        RepositoryConnection userConnection = connection;
        RepositoryReadableClient adminClient = connection.createClient();
        RepositoryReadableClient userClient = adminClient;
        RepositoryWriteableClient writableClient = new DirectoryWriteableClient(root);

        return new FileRepositoryFixture(adminConnection, userConnection, adminClient, writableClient, userClient, root);
    }

    private FileRepositoryFixture(RepositoryConnection adminConnection, RepositoryConnection userConnection, RepositoryReadableClient adminClient,
                                  RepositoryWriteableClient writableClient, RepositoryReadableClient userClient, File root) {
        super(adminConnection, userConnection, adminClient, writableClient, userClient);
        this.root = root;
    }

    @Override
    protected void createCleanRepository() throws Exception {
        recursiveDelete(root);
        root.mkdir();
    }

    @Override
    protected void cleanupRepository() throws Exception {
        recursiveDelete(root);
    }

    private void recursiveDelete(File file) {
        if (!file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                recursiveDelete(child);
            }
        }

        file.delete();
    }

    @Override
    public String toString() {
        return "Directory repo";
    }

    public DirectoryRepositoryConnection getWritableConnection() {
        return new DirectoryRepositoryConnection(root) {
            @Override
            public RepositoryReadableClient createClient() {
                return new DirectoryWriteableClient(root);
            }
        };
    }

}
