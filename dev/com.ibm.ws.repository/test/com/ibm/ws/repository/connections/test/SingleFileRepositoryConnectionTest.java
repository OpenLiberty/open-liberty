/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.connections.test;

import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.After;
import org.junit.Test;

import com.ibm.ws.repository.connections.SingleFileRepositoryConnection;
import com.ibm.ws.repository.resources.RepositoryResource;

/**
 * Tests specific to the SingleFileRepositoryConnection
 */
public class SingleFileRepositoryConnectionTest {

    private static final File FILE = new File("testSingleFileRepo");

    @After
    public void cleanup() {
        if (FILE.exists()) {
            FILE.delete();
        }
    }

    @Test
    public void testNoFileCheckStatus() {
        SingleFileRepositoryConnection repo = new SingleFileRepositoryConnection(FILE);
        try {
            repo.checkRepositoryStatus();
            fail("Repository with no file reported available");
        } catch (Exception ex) {
            // Expected
        }
    }

    @Test
    public void testNoFileGetAll() {
        SingleFileRepositoryConnection repo = new SingleFileRepositoryConnection(FILE);
        try {
            repo.getAllResources();
            fail("No exception thrown");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    public void testCreate() throws Exception {
        SingleFileRepositoryConnection repo = SingleFileRepositoryConnection.createEmptyRepository(FILE);
        repo.checkRepositoryStatus();
        assertThat(repo.getAllResources(), is(emptyCollectionOf(RepositoryResource.class)));

        // File should have been created, second repo should be valid
        SingleFileRepositoryConnection repo2 = new SingleFileRepositoryConnection(FILE);
        repo2.checkRepositoryStatus();
        assertThat(repo2.getAllResources(), is(emptyCollectionOf(RepositoryResource.class)));
    }

}
