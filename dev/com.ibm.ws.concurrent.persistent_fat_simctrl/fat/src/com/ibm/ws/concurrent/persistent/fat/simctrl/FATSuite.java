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
package com.ibm.ws.concurrent.persistent.fat.simctrl;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.Machine;

import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({ SimulatedControllerTest.class })
public class FATSuite {
    static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.concurrent.persistent.fat.simctrl");


    @AfterClass
    public static void afterSuite() throws Exception {
        // Remove the user extension added during the build process.
        //server.deleteDirectoryFromLibertyInstallRoot("usr/extension/");
    }


    @BeforeClass
    public static void beforeSuite() throws Exception {
        // Delete the Derby-only database that is used by the persistent scheduled executor
        Machine machine = server.getMachine();
        String installRoot = server.getInstallRoot();
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/persistctrldb");

        // Install user feature
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/simulatedController-1.0.mf");
        assertTrue("Product feature should have been copied to usr/extension/lib/features.",
                   server.fileExistsInLibertyInstallRoot("usr/extension/lib/features/simulatedController-1.0.mf"));
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/test.concurrent.persistent.fat.simctrl.jar");
        assertTrue("Product bundle should have been copied to usr/extension/lib.",
                   server.fileExistsInLibertyInstallRoot("usr/extension/lib/test.concurrent.persistent.fat.simctrl.jar"));

        // Install liberty helper feature.
        server.copyFileToLibertyInstallRoot("lib/features/", "features/controllerTestFeature-1.0.mf");
        assertTrue("Helper feature should have been copied to lib/features.",
                   server.fileExistsInLibertyInstallRoot("lib/features/controllerTestFeature-1.0.mf"));
    }
}