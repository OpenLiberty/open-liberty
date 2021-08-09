/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.testutils.FatUtils;
import com.ibm.ws.lars.testutils.fixtures.RepositoryFixture;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;

public class RepositoryUtilsTest {

    @Rule
    public final RepositoryFixture fixture = FatUtils.getRestFixture();

    private final RepositoryConnection repoConnection = fixture.getAdminConnection();

    /**
     * Tests that the test repository is available
     */
    @Test
    public void testRepositoryAvailable() {
        assertTrue("The test repository should be available", repoConnection.isRepositoryAvailable());
    }

    /**
     * Tests that the test repository is available
     *
     * @throws RequestFailureException
     * @throws IOException
     */
    @Test
    public void testRepositoryStatusOK() throws IOException, RequestFailureException {

        // An exception is thrown if this fails
        repoConnection.checkRepositoryStatus();
    }

    /**
     * Tests that an invalid repository is not available
     */
    @Test
    public void testInvalidRepositoryNotAvailable() {
        RestRepositoryConnection invalidLoginInfo = new RestRepositoryConnection("I", "don't", "exist", "http://dont.exist.com");
        assertFalse("An invalid test repository should not be available", invalidLoginInfo.isRepositoryAvailable());
    }

    @Test
    public void testInvalidRepositoryThrowsException() throws IOException, RequestFailureException {
        RestRepositoryConnection invalidLoginInfo = new RestRepositoryConnection("I", "don't", "exist", "http://dont.exist.com");
        try {
            invalidLoginInfo.checkRepositoryStatus();
            fail("Should not have been able to reach here, repository status should have thrown an exception");
        } catch (IOException io) {
            // expected
        } catch (RequestFailureException rfe) {
            // also possible
        }
    }

}
