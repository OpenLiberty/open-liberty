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

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assume.assumeThat;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.client.RepositoryWriteableClient;

/**
 * A fixture for a repository server.
 * <p>
 * This rule will empty the repository before each test.
 */
public abstract class RepositoryFixture implements TestRule {

    protected final RepositoryConnection adminConnection;
    protected final RepositoryConnection userConnection;
    protected final RepositoryReadableClient adminClient;
    protected final RepositoryWriteableClient writableClient;
    protected final RepositoryReadableClient userClient;

    /**
     * @param adminConnection a repository connection with admin privileges
     * @param userConnection a repository connection with user privileges
     * @param adminClient a read client with admin privileges
     * @param writableClient a write client with admin privileges
     * @param userClient a read client with user privileges
     */
    protected RepositoryFixture(RepositoryConnection adminConnection, RepositoryConnection userConnection, RepositoryReadableClient adminClient,
                                RepositoryWriteableClient writableClient, RepositoryReadableClient userClient) {
        this.adminConnection = adminConnection;
        this.userConnection = userConnection;
        this.adminClient = adminClient;
        this.writableClient = writableClient;
        this.userClient = userClient;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                assumeFatFilterMatches(description);
                createCleanRepository();
                try {
                    base.evaluate();
                } finally {
                    cleanupRepository();
                }
            }
        };
    }

    /**
     * Assume (in the JUnit sense) that the testDescription is matched by the fatFilter
     * <p>
     * <ul>
     * <li>If the "fatFilter" property is set, the test will be skipped if its name does not contain
     * the fatFilter string</li>
     * <li>If the "fatFilter" property is not set, this method does nothing and the test is run as
     * normal</li>
     * </ul>
     */
    protected void assumeFatFilterMatches(Description testDescription) {
        String fatFilter = System.getProperty("fatFilter");
        if (fatFilter != null) {
            assumeThat(testDescription.getDisplayName(), containsString(fatFilter));
        }
    }

    protected abstract void createCleanRepository() throws Exception;

    protected abstract void cleanupRepository() throws Exception;

    public RepositoryConnection getAdminConnection() {
        return adminConnection;
    }

    public RepositoryConnection getUserConnection() {
        return userConnection;
    }

    public RepositoryReadableClient getAdminClient() {
        return adminClient;
    }

    public RepositoryWriteableClient getWriteableClient() {
        return writableClient;
    }

    public RepositoryReadableClient getUserClient() {
        return userClient;
    }

    public void refreshTextIndex(String assetId) throws IOException {}

    /**
     * Returns true if this repository supports updating assets in place
     *
     * @return
     */
    public boolean isUpdateSupported() {
        return true;
    }

    /**
     * Returns true if this repository supports attachments
     *
     * @return
     */
    public boolean isAttachmentSupported() {
        return true;
    }

    /**
     * Returns the root URL where hosted testfiles can be found
     * <p>
     * Not applicable to all repository types
     *
     * @throws URISyntaxException
     */
    public String getHostedFileRoot() throws URISyntaxException {
        return null;
    }

    @Override
    public abstract String toString();

}
