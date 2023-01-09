/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package test.concurrent.work.fat;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ManagedScheduledExecutorService;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.concurrent.work.cdi.WorkTestCDIServlet;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class ConcurrentWorkConfigFATTest extends FATServletClient {
    private static final String APP_NAME = "WorkTestApp";
    private static final String CDI_APP_NAME = "WorkTestCDIApp";

    @Server("com.ibm.ws.concurrent.fat.work.config")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "test.concurrent.work.app");
        ShrinkHelper.defaultDropinApp(server, CDI_APP_NAME, "test.concurrent.work.cdi");

        server.startServer();
    }

    /**
     * Verify that when 2 managed executors are configured to share the same concurrency policy,
     * removing one of the managed executors only cancels work from that managed executor
     * and not work that the other managed executor submitted in a different application.
     */
    @Test
    public void testManagedExecutorShutdownCancelsOnlyItsOwnWork() throws Exception {

        // Have both applications use different managed executors
        // that share the same concurrency policy:

        runTest(server, APP_NAME, "testSubmitBlockingWork");
        runTest(server, CDI_APP_NAME, "testSubmitBlockedWork");

        // Replace the managed executor that is used by the second application,

        ServerConfiguration config = server.getServerConfiguration();
        ManagedScheduledExecutorService WorkTestCDIApp_schedExec =
                config.getManagedScheduledExecutorServices().removeById("WorkTestCDIApp_schedExec");
        ManagedScheduledExecutorService replacement =
                (ManagedScheduledExecutorService) WorkTestCDIApp_schedExec.clone();
        replacement.setId("replacement");
        config.getManagedScheduledExecutorServices().add(replacement);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(CDI_APP_NAME));

        try {
            // which should cause its work to be cancelled, opening up queue positions to accept more work

            runTest(server, CDI_APP_NAME, "testSubmitBlockedWork");

            // The work that was submitted to the first managed executor should not be
            // impacted by this and should be able to run to successful completion,

            runTest(server, APP_NAME, "testStopBlockingWork");
        } finally {
            // Restore server configuration
            config.getManagedScheduledExecutorServices().remove(replacement);
            config.getManagedScheduledExecutorServices().add(WorkTestCDIApp_schedExec);

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(CDI_APP_NAME));
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted())
            server.stopServer();
    }
}