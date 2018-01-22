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

package com.ibm.ws.repository.connections.test;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import com.ibm.ws.repository.connections.DirectoryRepositoryConnection;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.connections.ZipRepositoryConnection;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.resources.internal.SampleResourceImpl;

/**
 *
 */
public class RepositoryConnectionTest {

    @Test
    public void testMassiveRepoLocation() {
        String repoURL = "htttp://blah";
        RestRepositoryConnection lie = new RestRepositoryConnection("a", "b", "c", repoURL);
        RepositoryResourceImpl mr = new SampleResourceImpl(lie);
        assertEquals("The repo url in the resource is not the one we set",
                     repoURL, mr.getRepositoryConnection().getRepositoryLocation());
    }

    @Test
    public void testDirectoryRepoLocation() {
        File root = new File("C:/root");
        DirectoryRepositoryConnection dirCon = new DirectoryRepositoryConnection(root);
        RepositoryResourceImpl mr = new SampleResourceImpl(dirCon);
        assertEquals("The repo url in the resource is not the one we set",
                     root.getAbsolutePath(), mr.getRepositoryConnection().getRepositoryLocation());
    }

    @Test
    public void testZipRepoLocation() {
        File zip = new File("repo.zip");
        ZipRepositoryConnection zipCon = new ZipRepositoryConnection(zip);
        RepositoryResourceImpl mr = new SampleResourceImpl(zipCon);
        assertEquals("The repo url in the resource is not the one we set",
                     zip.getAbsolutePath(), mr.getRepositoryConnection().getRepositoryLocation());
    }
}
