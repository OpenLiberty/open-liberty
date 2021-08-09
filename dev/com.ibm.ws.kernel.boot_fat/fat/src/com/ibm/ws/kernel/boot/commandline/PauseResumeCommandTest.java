/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class PauseResumeCommandTest {

    private static final Class<?> c = PauseResumeCommandTest.class;

    private static final String PAUSE_RESUME_SERVER_NAME = "com.ibm.ws.kernel.boot.pause.resume.fat";

    private static final LibertyServer pauseResumeServer = LibertyServerFactory.getLibertyServer(PAUSE_RESUME_SERVER_NAME);

    @Before
    public void setup() throws Exception {
        pauseResumeServer.startServer();
    }

    @After
    public void teardown() throws Exception {
        pauseResumeServer.stopServer("CWWKE093*");
    }

    /**
     * Tests the case where the "server pause" command is issued and no
     * pauseable components exist
     *
     * @throws Exception
     */
    @Test
    public void testPauseNoPauseableComponents() throws Exception {
        final String METHOD_NAME = "testPauseNoPauseableComponents";
        Log.entering(c, METHOD_NAME);

        String output = pauseResumeServer.executeServerScript("pause", null).getStdout();

        assertTrue(output.contains("failed"));
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
    public void testResumeNoPauseableComponents() throws Exception {
        final String METHOD_NAME = "testResumeNoPauseableComponents";
        Log.entering(c, METHOD_NAME);

        String output = pauseResumeServer.executeServerScript("resume", null).getStdout();

        assertTrue(output.contains("failed"));
        assertNotNull(pauseResumeServer.waitForStringInLog("CWWKE0934W", 10000));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests the case where the "server pause" command is issued with an empty target list.
     *
     * @throws Exception
     */
    @Test
    public void testPauseEmptyTargetList() throws Exception {
        final String METHOD_NAME = "testPauseEmptyTargetList";
        Log.entering(c, METHOD_NAME);

        String output = pauseResumeServer.executeServerScript("pause", new String[] { "--target=" }).getStdout();

        assertTrue(output.contains("failed"));
        assertNotNull(pauseResumeServer.waitForStringInLog("CWWKE0931W", 10000));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests the case where the "server resume" command is issued with an empty target list.
     *
     * @throws Exception
     */
    @Test
    public void testResumeEmptyTargetList() throws Exception {
        final String METHOD_NAME = "testResumeEmptyTargetList";
        Log.entering(c, METHOD_NAME);

        String output = pauseResumeServer.executeServerScript("resume", new String[] { "--target=" }).getStdout();

        assertTrue(output.contains("failed"));
        assertNotNull(pauseResumeServer.waitForStringInLog("CWWKE0932W", 10000));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests the case where the "server resume" command is issued with an invalid target.
     *
     * @throws Exception
     */
    @Test
    public void testPauseInvalidTarget() throws Exception {
        final String METHOD_NAME = "testPauseInvalidTarget";
        Log.entering(c, METHOD_NAME);

        String output = pauseResumeServer.executeServerScript("pause", new String[] { "--target=InvalidTarget" }).getStdout();

        assertTrue(output.contains("failed"));
        assertNotNull(pauseResumeServer.waitForStringInLog("CWWKE0935W",
                                                           10000));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests the case where the "server resume" command is issued with an empty target list.
     *
     * @throws Exception
     */
    @Test
    public void testResumeInvalidTarget() throws Exception {
        final String METHOD_NAME = "testResumeInvalidTarget";
        Log.entering(c, METHOD_NAME);

        String output = pauseResumeServer.executeServerScript("resume", new String[] { "--target=InvalidTarget" }).getStdout();

        assertTrue(output.contains("failed"));
        assertNotNull(pauseResumeServer.waitForStringInLog("CWWKE0936W",
                                                           10000));

        Log.exiting(c, METHOD_NAME);
    }
}
