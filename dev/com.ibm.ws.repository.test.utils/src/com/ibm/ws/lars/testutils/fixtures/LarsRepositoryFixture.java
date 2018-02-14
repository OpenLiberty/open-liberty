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

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;

import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.client.RepositoryWriteableClient;
import com.ibm.ws.repository.transport.model.Asset;

/**
 *
 */
public class LarsRepositoryFixture extends RepositoryFixture {

    protected final String repositoryUrl;

    public static LarsRepositoryFixture createFixture(String repositoryUrl, String apiKey, String adminId, String adminPassword, String userId, String userPassword) {
        RestRepositoryConnection adminConnection = new RestRepositoryConnection(adminId, adminPassword, apiKey, repositoryUrl);
        RestRepositoryConnection userConnection = new RestRepositoryConnection(userId, userPassword, apiKey, repositoryUrl);

        RepositoryReadableClient adminClient = adminConnection.createClient();
        RepositoryWriteableClient writableClient = (RepositoryWriteableClient) adminClient;
        RepositoryReadableClient userClient = userConnection.createClient();
        return new LarsRepositoryFixture(adminConnection, userConnection, adminClient, writableClient, userClient, repositoryUrl);
    }

    protected LarsRepositoryFixture(RepositoryConnection adminConnection, RepositoryConnection userConnection, RepositoryReadableClient adminClient,
                                    RepositoryWriteableClient writableClient, RepositoryReadableClient userClient, String repositoryUrl) {
        super(adminConnection, userConnection, adminClient, writableClient, userClient);
        this.repositoryUrl = repositoryUrl;
    }

    @Override
    protected void createCleanRepository() throws Exception {
        for (Asset asset : adminClient.getAllAssets()) {
            writableClient.deleteAssetAndAttachments(asset.get_id());
        }
        assertEquals("Repository resources present", 0, adminConnection.getAllResourcesWithDupes().size());
    }

    @Override
    protected void cleanupRepository() throws Exception {
        for (Asset asset : adminClient.getAllAssets()) {
            writableClient.deleteAssetAndAttachments(asset.get_id());
        }
    }

    @Override
    public boolean isUpdateSupported() {
        return false;
    }

    @Override
    public String getHostedFileRoot() throws URISyntaxException {
        // Test files are served under /testFiles on the same host and port as the test repository
        URI uri = new URI(repositoryUrl);
        URI baseUri = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), "/testFiles", null, null);
        return baseUri.toString();
    }

    @Override
    public String toString() {
        return "LARS repo";
    }

}
