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

import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.SingleFileRepositoryConnection;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.client.RepositoryWriteableClient;

/**
 * Repository fixture for a single file json repository
 */
public class SingleFileRepositoryFixture extends RepositoryFixture {

    private final File jsonFile;

    public static SingleFileRepositoryFixture createFixture(File jsonFile) {

        SingleFileRepositoryConnection connection = new SingleFileRepositoryConnection(jsonFile);
        RepositoryConnection adminConnection = connection;
        RepositoryConnection userConnection = connection;
        RepositoryReadableClient adminClient = connection.createClient();
        RepositoryReadableClient userClient = adminClient;
        RepositoryWriteableClient writableClient = (RepositoryWriteableClient) adminClient;

        return new SingleFileRepositoryFixture(adminConnection, userConnection, adminClient, writableClient, userClient, jsonFile);
    }

    private SingleFileRepositoryFixture(RepositoryConnection adminConnection, RepositoryConnection userConnection, RepositoryReadableClient adminClient,
                                        RepositoryWriteableClient writableClient, RepositoryReadableClient userClient, File jsonFile) {
        super(adminConnection, userConnection, adminClient, writableClient, userClient);
        this.jsonFile = jsonFile;
    }

    @Override
    protected void createCleanRepository() throws Exception {
        cleanupRepository();
        SingleFileRepositoryConnection.createEmptyRepository(jsonFile);
    }

    @Override
    protected void cleanupRepository() throws Exception {
        if (jsonFile.exists()) {
            jsonFile.delete();
        }
    }

    @Override
    public String toString() {
        return "Single file repo";
    }

    @Override
    public boolean isUpdateSupported() {
        return false;
    }

    /**
     * This repository does not support attachments
     */
    @Override
    public boolean isAttachmentSupported() {
        return false;
    }

}
