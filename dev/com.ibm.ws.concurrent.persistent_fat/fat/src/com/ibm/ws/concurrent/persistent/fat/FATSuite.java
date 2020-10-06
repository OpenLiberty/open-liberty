/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;

@RunWith(Suite.class)
@SuiteClasses({
    PersistentExecutorTest.class,
    PersistentExecutorWithFailoverEnabledTest.class,
    })
public class FATSuite {
    static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.concurrent.persistent.fat");

    @BeforeClass
    public static void beforeSuite() throws Exception {
        // Install liberty helper feature.
        server.copyFileToLibertyInstallRoot("lib/features/", "features/timerInterfacesTestFeature-1.0.mf");
        server.copyFileToLibertyInstallRoot("lib/features/", "features/timerInterfacesTestFeature-2.0.mf");
        assertTrue("Helper feature 1 should have been copied to lib/features.",
                   server.fileExistsInLibertyInstallRoot("lib/features/timerInterfacesTestFeature-1.0.mf"));
        assertTrue("Helper feature 2 should have been copied to lib/features.",
                   server.fileExistsInLibertyInstallRoot("lib/features/timerInterfacesTestFeature-2.0.mf"));

        // Transform userfeature bundle to Jakarta
        Path javaEEBundle = Paths.get("lib", "LibertyFATTestFiles", "bundles", "test.feature.sim.ejb.timer.jar");
        Path jakartaEEBundle = Paths.get("lib", "LibertyFATTestFiles", "bundles", "test.feature.sim.ejb.timer.jakarta.jar");
        JakartaEE9Action.transformApp(javaEEBundle, jakartaEEBundle);
        Log.info(PersistentExecutorWithFailoverEnabledTest.class, "setUp", "Transformed app " + javaEEBundle + " to " + jakartaEEBundle);

        server.copyFileToLibertyInstallRoot("lib/", "bundles/test.feature.sim.ejb.timer.jar");
        server.copyFileToLibertyInstallRoot("lib/", "bundles/test.feature.sim.ejb.timer.jakarta.jar");

        //Allows local tests to switch between using a local docker client, to using a remote docker client. 
        ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();
    }
}