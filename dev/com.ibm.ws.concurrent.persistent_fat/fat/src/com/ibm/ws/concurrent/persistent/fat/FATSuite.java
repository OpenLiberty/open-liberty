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
package com.ibm.ws.concurrent.persistent.fat;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.Machine;

import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;

@RunWith(Suite.class)
@SuiteClasses({ SchedulerTest.class })
public class FATSuite {
    static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.concurrent.persistent.fat");

    @BeforeClass
    public static void beforeSuite() throws Exception {
        // Delete the Derby database that might be used by the persistent scheduled executor and the Derby-only test database
        Machine machine = server.getMachine();
        String installRoot = server.getInstallRoot();
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/scheddb");
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/testdb");

        // Install liberty helper feature.
        server.copyFileToLibertyInstallRoot("lib/features/", "features/timerInterfacesTestFeature-1.0.mf");
        assertTrue("Helper feature should have been copied to lib/features.",
                   server.fileExistsInLibertyInstallRoot("lib/features/timerInterfacesTestFeature-1.0.mf"));
        server.copyFileToLibertyInstallRoot("lib/", "bundles/test.feature.sim.ejb.timer.jar");
        
        //Allows local tests to switch between using a local docker client, to using a remote docker client. 
        ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();
    }
}