/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.install.repository.RepositoryException;
import com.ibm.ws.install.repository.RepositoryFactory;

public class DirectoryRepositoryTest {

    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Test
    public void testNonExistDirectory() {
        try {
            RepositoryFactory.getInstance(new File("unknown"));
            fail("RepositoryFactory.getInstance() didn't throw exception CWWKF1500E.");
        } catch (RepositoryException e) {
            assertTrue("RepositoryFactory.getInstance() should throw exception CWWKF1500E.", e.getMessage().contains("CWWKF1500E"));
        }
    }

    @Test
    public void testFile() {
        try {
            RepositoryFactory.getInstance(new File("build/unittest/wlpDirs/developers/wlp/lib/versions/WebSphereApplicationServer.properties"));
            fail("RepositoryFactory.getInstance() didn't throw exception CWWKF1501E.");
        } catch (RepositoryException e) {
            assertTrue("RepositoryFactory.getInstance() should throw exception CWWKF1501E.", e.getMessage().contains("CWWKF1501E"));
        }
    }
}
