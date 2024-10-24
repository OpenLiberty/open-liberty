/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.zip.ZipFile;

import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.install.repository.FeatureAsset;
import com.ibm.ws.install.repository.RepositoryException;
import com.ibm.ws.install.repository.RepositoryFactory;
import com.ibm.ws.install.repository.internal.ZipRepository;

import test.common.SharedOutputManager;

/**
 *
 */
public class ZipRepositoryTest {

    @Rule
    public static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");

    @Test
    public void testValidIndexFile() {
        try {
            File indexFile = new File("build/unittest/zips/com.ibm.websphere.liberty.repo.core.manifest_8.5.5005.zip");
            ZipRepository zipRepo = (ZipRepository) RepositoryFactory.getInstance(new ZipFile(indexFile));
            Collection<FeatureAsset> fas = zipRepo.getFeatures("com.ibm.websphere.appserver", "8.5.5.5", "InstallationManager", "ILAN", "ND");
            assertNotNull("getFeatures() should not return null", fas);
            assertTrue("getFeatures() should have 15 features", fas.size() == 15);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testValidIndexFile", t);
        }
    }

    @Test
    public void testInvalidIndexFile() {
        try {
            File zip = new File("build/unittest/zips/invalid.zip");
            RepositoryFactory.getInstance(new ZipFile(zip));
        } catch (RepositoryException e) {
            assertTrue("RepositoryFactory.getInstance() should throw exception CWWKF1502E.", e.getMessage().contains("CWWKF1502E"));
        } catch (Throwable t) {
            // TODO Auto-generated catch block
            outputMgr.failWithThrowable("testValidIndexFile", t);
        }
    }
}
