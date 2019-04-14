/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.asyncshutdown;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Test for an application shutting down while an asynchronous method is running
 */
@RunWith(FATRunner.class)
public class AsyncShutdownTest {

    private static final String SERVER_NAME = "FaultToleranceMultiModule";
    private static final String APP_NAME = "ftAsyncShutdown";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        server.startServer();
    }

    @Test
    public void testAsyncShutdown() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage(AsyncShutdownTest.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, app, SERVER_ONLY);

        beginAsyncTask();

        // Async task should keep starting new async methods until one of them fails with the expected error because the application has shut down
        server.deleteDirectoryFromLibertyServerRoot("dropins/" + APP_NAME + ".war");
        assertNotNull("application did not stop", server.waitForStringInLog("CWWKZ0009I.*" + APP_NAME));
        assertNotNull("Error was not logged", server.waitForStringInLog("CWMFT0002W"));
    }

    private void beginAsyncTask() throws Exception {
        HttpUtils.findStringInReadyUrl(server, APP_NAME + "/begin-async-task", "OK");
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("CWMFT0002W");
    }

}
