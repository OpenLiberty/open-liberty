/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * This test bucket tests the server startup process.
 */
public class ServerStartTest {
    private static final Class<?> c = ServerStartTest.class;

    private static final String SERVER_NAME = "com.ibm.ws.kernel.boot.serverstart.fat";
    private static final String SYMLINK_NAME = "com.ibm.ws.kernel.boot.serverstart.fat.symlink";

    private static LibertyServer server;

    @Rule
    public TestName testName = new TestName();

    @Before
    public void before() {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
    }

    @After
    public void after() throws Exception {
        if (server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    /**
     * This test validates that the server start script functions correctly when the CDPATH environment variable
     * is present.
     *
     * @throws Exception
     */
    public void testServerStartWithCDPATH() throws Exception {
        final String METHOD_NAME = "testServerStartWithCDPATH";
        Log.entering(c, METHOD_NAME);

        // issuing the command from the Liberty install root while supplying the bin directory as
        // part of the command itself causes the server script to cd to the bin directory, which
        // is where we noticed problems when CDPATH is set
        String executionDir = server.getInstallRoot();
        String command = "bin" + File.separator + "server";

        String[] parms = new String[2];
        parms[0] = "start";
        parms[1] = SERVER_NAME;

        Properties envVars = new Properties();
        envVars.put("CDPATH", ".");

        ProgramOutput po = server.getMachine().execute(command, parms, executionDir, envVars);
        Log.info(c, METHOD_NAME, "server start stdout = " + po.getStdout());
        Log.info(c, METHOD_NAME, "server start stderr = " + po.getStderr());

        server.waitForStringInLog("CWWKF0011I");

        // because we didn't start the server using the LibertyServer APIs, we need to have it detect
        // its started state so it will stop and save logs properly
        server.resetStarted();

        assertTrue("the server should have been started", server.isStarted());

        Log.exiting(c, METHOD_NAME);
    }

    @Test
    /**
     * This test validates that the server start functions correctly when the server is referenced
     * via a symbolic link (on those systems which support same)
     *
     * @throws Exception
     */
    public void testServerStartViaSymbolicLink() throws Exception {
        String linkCommand = "/bin/ln";

        // only try this test if the unix ln command exists and can be executed
        File ln = new File(linkCommand);
        if (ln.exists() && ln.canExecute()) {
            // Copy the server directory elsewhere
            String originalPath = server.getServerRoot();
            String relocatedPath = server.getServerRoot() + "/../../" + server.getServerName();
            String copyCommand = "cp";
            String[] copyParms = new String[] { "-r", originalPath, relocatedPath }; // Make sure -r works on all these platforms...
            ProgramOutput po = server.getMachine().execute(copyCommand, copyParms);

            // Set up the symlink
            String symPath = server.getServerRoot() + "/../" + SYMLINK_NAME;
            String unlinkCommand = "rm";
            String[] unlinkParms = new String[] { symPath };
            po = server.getMachine().execute(unlinkCommand, unlinkParms);

            String[] linkParms = new String[] { "-s", relocatedPath, symPath }; // Symbolic link relocated copy as
            po = server.getMachine().execute(linkCommand, linkParms);

            // Get a handle to the alias.Note that this must use a new entry point to get a handle to an already configured server.
            LibertyServer symlink_server = LibertyServerFactory.getExistingLibertyServer(SYMLINK_NAME);

            // Confirm it can start and stop
            server.stopServer(); // Pause the original
            symlink_server.startServer(); // Start the alias (and confirm)
            symlink_server.stopServer(); // stop the alias (and confirm)
            server.startServer(); // Resume original (in case following test needs it).
        }

    }

    @Test
    /**
     * This test ensures that the servers starts without error when using the -Xfuture command line
     * argument. This argument enforces strict class file verification. One customer ran into
     * BundleExceptions due to empty package-info.class files (defect 177872).
     */
    public void testServerStartWithDashXFutureCmdArg() throws Exception {
        server.copyFileToTempDir("server.xml", "origServer.xml");
        try {
            server.setServerConfigurationFile("Xfuture/server.xml");
            server.copyFileToLibertyServerRoot("Xfuture/jvm.options");

            server.startServer();
        } finally {
            server.deleteDirectoryFromLibertyServerRoot("jvm.options");
            server.setServerConfigurationFile("tmp/origServer.xml");
        }
    }
}
