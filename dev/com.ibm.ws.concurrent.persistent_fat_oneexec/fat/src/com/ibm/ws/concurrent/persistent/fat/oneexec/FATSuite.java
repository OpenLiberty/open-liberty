/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
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
package com.ibm.ws.concurrent.persistent.fat.oneexec;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({
    OneExecutorRunsAllTest.class,
    OneExecutorRunsAllWithFailoverEnabledTest.class
    })
public class FATSuite {
    static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.concurrent.persistent.fat.oneexec");

    @BeforeClass
    public static void beforeSuite() throws Exception {
        // Install liberty helper feature.
        server.copyFileToLibertyInstallRoot("lib/features/", "features/singletonTestFeature-1.0.mf");
        assertTrue("Helper feature should have been copied to lib/features.",
                   server.fileExistsInLibertyInstallRoot("lib/features/singletonTestFeature-1.0.mf"));
        server.copyFileToLibertyInstallRoot("lib/", "bundles/test.feature.ejb.singleton.jar");
    }

    @AfterClass
    public static void afterSuite() throws Exception {
        server.deleteFileFromLibertyInstallRoot("lib/features/singletonTestFeature-1.0.mf");
        assertFalse("Helper feature should have been deleted from lib/features.",
                   server.fileExistsInLibertyInstallRoot("lib/features/singletonTestFeature-1.0.mf"));
        server.deleteFileFromLibertyInstallRoot("lib/test.feature.ejb.singleton.jar");
    }
}