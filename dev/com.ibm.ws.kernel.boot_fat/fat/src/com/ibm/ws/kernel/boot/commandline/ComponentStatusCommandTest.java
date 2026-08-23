/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.kernel.boot.commandline;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
public class ComponentStatusCommandTest {

    private static final Class<?> c = PauseResumeCommandTest.class;

    private static final String COMPONENT_STATUS_SERVER_NAME = "com.ibm.ws.kernel.boot.component.status.fat";
    private static final String PAUSE_RESUME_SERVER_NAME = "com.ibm.ws.kernel.boot.pause.resume.fat";

    private static final LibertyServer componentStatusServer = LibertyServerFactory.getLibertyServer(COMPONENT_STATUS_SERVER_NAME);
    private static final LibertyServer pauseResumeServer = LibertyServerFactory.getLibertyServer(PAUSE_RESUME_SERVER_NAME);

    public void setup(LibertyServer server) throws Exception {
        server.startServer();
    }

    @After
    public void teardown() throws Exception {
        componentStatusServer.stopServer("CWWKE093*");
        pauseResumeServer.stopServer("CWWKE093*");
    }

    /**
     * Tests the case where the "server pause" command is issued then get the status of components
     *
     * @throws Exception
     */
    @Test
    public void testPauseStatusComponents() throws Exception {
        final String METHOD_NAME = "testPauseStatusComponents";
        Log.entering(c, METHOD_NAME);

        setup(componentStatusServer);

        String pauseOutput = componentStatusServer.executeServerScript("pause", null).getStdout();
        System.out.println(pauseOutput);

        String statusOutput = componentStatusServer.executeServerScript("compstatus", null).getStdout();
        System.out.println(statusOutput);

        assertTrue(statusOutput.contains("failed"));
        assertNotNull(componentStatusServer.waitForStringInLog("CWWKE0933W", 10000));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests the case where the "server resume" command is issued and no
     * pauseable components exist
     *
     * @throws Exception
     */
    @Test
    public void testPauseStatusNoPauseableComponents() throws Exception {
        final String METHOD_NAME = "testPauseStatusNoPauseableComponents";
        Log.entering(c, METHOD_NAME);

        setup(pauseResumeServer);

        String pauseOutput = pauseResumeServer.executeServerScript("pause", null).getStdout();
        System.out.println(pauseOutput);

        String statusOutput = pauseResumeServer.executeServerScript("compstatus", null).getStdout();
        System.out.println(statusOutput);

        assertTrue(statusOutput.contains("failed"));
        assertNotNull(pauseResumeServer.waitForStringInLog("CWWKE0933W", 10000));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests the case where the "server resume" command is issued and no
     * pauseable components exist
     *
     * @throws Exception
     */
    @Test
    public void testStressTestComponents() throws Exception {
        final String METHOD_NAME = "testPauseStatusNoPauseableComponents";
        Log.entering(c, METHOD_NAME);

        componentStatusServer.addDropinDefaultConfiguration("more-endpoints.xml");

        setup(componentStatusServer);

        String pauseOutput = componentStatusServer.executeServerScript("pause", null).getStdout();
        System.out.println(pauseOutput);

        String statusOutput = componentStatusServer.executeServerScript("compstatus", null).getStdout();
        System.out.println(statusOutput);

        assertTrue(statusOutput.contains("failed"));
        assertNotNull(componentStatusServer.waitForStringInLog("CWWKE0933W", 10000));

        Log.exiting(c, METHOD_NAME);
    }

    // /**
    //  * Tests the case where the "server pause" command is issued with an empty target list.
    //  *
    //  * @throws Exception
    //  */
    // @Test
    // public void testPauseEmptyTargetList() throws Exception {
    //     final String METHOD_NAME = "testPauseEmptyTargetList";
    //     Log.entering(c, METHOD_NAME);

    //     String output = componentStatusServer.executeServerScript("pause", new String[] { "--target=" }).getStdout();

    //     assertTrue(output.contains("failed"));
    //     assertNotNull(componentStatusServer.waitForStringInLog("CWWKE0931W", 10000));

    //     Log.exiting(c, METHOD_NAME);
    // }

    // /**
    //  * Tests the case where the "server resume" command is issued with an empty target list.
    //  *
    //  * @throws Exception
    //  */
    // @Test
    // public void testResumeEmptyTargetList() throws Exception {
    //     final String METHOD_NAME = "testResumeEmptyTargetList";
    //     Log.entering(c, METHOD_NAME);

    //     String output = componentStatusServer.executeServerScript("resume", new String[] { "--target=" }).getStdout();

    //     assertTrue(output.contains("failed"));
    //     assertNotNull(componentStatusServer.waitForStringInLog("CWWKE0932W", 10000));

    //     Log.exiting(c, METHOD_NAME);
    // }

    // /**
    //  * Tests the case where the "server resume" command is issued with an invalid target.
    //  *
    //  * @throws Exception
    //  */
    // @Test
    // public void testPauseInvalidTarget() throws Exception {
    //     final String METHOD_NAME = "testPauseInvalidTarget";
    //     Log.entering(c, METHOD_NAME);

    //     String output = componentStatusServer.executeServerScript("pause", new String[] { "--target=InvalidTarget" }).getStdout();

    //     assertTrue(output.contains("failed"));
    //     assertNotNull(componentStatusServer.waitForStringInLog("CWWKE0935W",
    //                                                        10000));

    //     Log.exiting(c, METHOD_NAME);
    // }

    // /**
    //  * Tests the case where the "server resume" command is issued with an empty target list.
    //  *
    //  * @throws Exception
    //  */
    // @Test
    // public void testResumeInvalidTarget() throws Exception {
    //     final String METHOD_NAME = "testResumeInvalidTarget";
    //     Log.entering(c, METHOD_NAME);

    //     String output = componentStatusServer.executeServerScript("resume", new String[] { "--target=InvalidTarget" }).getStdout();

    //     assertTrue(output.contains("failed"));
    //     assertNotNull(componentStatusServer.waitForStringInLog("CWWKE0936W",
    //                                                        10000));

    //     Log.exiting(c, METHOD_NAME);
    // }
}
