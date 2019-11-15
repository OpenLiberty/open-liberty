/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.oneexec;

import static org.junit.Assert.assertTrue;

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
}