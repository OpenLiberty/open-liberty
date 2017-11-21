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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;

import componenttest.common.apiservices.Bootstrap;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.utils.LibertyServerUtils;

/**
 *
 */
public class StartCommandTest {

    private final static double javaLevel = Double.parseDouble(System.getProperty("java.specification.version"));

    private final static String defaultServerName = "defaultServer";
    private static Bootstrap bootstrap;
    private static Machine machine;
    private static String installPath;
    private static String defaultServerPath;
    private static String previousWorkDir;

    @BeforeClass
    public static void setup() throws Exception {
        bootstrap = Bootstrap.getInstance();
        machine = LibertyServerUtils.createMachine(bootstrap);
        previousWorkDir = machine.getWorkDir();
        machine.setWorkDir(null);
        installPath = LibertyFileManager.getInstallPath(bootstrap);
        defaultServerPath = installPath + "/usr/servers/" + defaultServerName;
    }

    @AfterClass
    public static void tearDown() {
        machine.setWorkDir(previousWorkDir);
    }

    @Before
    public void cleanupBeforeRun() throws Exception {
        // since we are not using the normal LibertyServer class for this server,
        // we need to make sure to explicitly clean up.  We do this before running
        // the test in order to preserve the contents on disk.

        if (LibertyFileManager.libertyFileExists(machine, defaultServerPath)) {
            LibertyFileManager.deleteLibertyDirectoryAndContents(machine, defaultServerPath);
        }
    }

    /**
     * Tests the case where the "server run" command is issued and defaultServer
     * does not exist. In this case, defaultServer is created on the fly.
     *
     * @throws Exception
     */
    @Test
    public void testIsServerEnvCreatedForImplicitServerCreate() throws Exception {

        ProgramOutput po = LibertyServerUtils.executeLibertyCmd(bootstrap, "server", "start");
        assertEquals("Unexpected return code from server start command STDOUT: \" + po.getStdout() + \" STDERR: \" + po.getStderr()", 0, po.getReturnCode());

        try {
            // check that server directory was created
            assertTrue("Expected server directory to exist at " + defaultServerPath + ", but does not", LibertyFileManager.libertyFileExists(machine, defaultServerPath));

            // check that server.xml exists
            String serverXmlPath = defaultServerPath + "/server.xml";
            assertTrue("Expected server.xml file to exist at " + serverXmlPath + ", but does not", LibertyFileManager.libertyFileExists(machine, serverXmlPath));

            // if we are running in a JVM that is version 1.8 or higher, we also need to check for the server.env file
            if (javaLevel >= 1.8) {
                String serverEnvPath = defaultServerPath + "/server.env";
                assertTrue("Expected server.env file to exist at " + serverEnvPath + ", but does not", LibertyFileManager.libertyFileExists(machine, serverEnvPath));
            }
        } finally {
            po = LibertyServerUtils.executeLibertyCmd(bootstrap, "server", "stop");
            assertEquals("Unexpected return code from server stop command", 0, po.getReturnCode());
        }

    }
}
