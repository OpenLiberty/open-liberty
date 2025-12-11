/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;

import componenttest.common.apiservices.Bootstrap;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.utils.FileUtils;
import componenttest.topology.utils.LibertyServerUtils;

@RunWith(FATRunner.class)
public class CreateCommandTest {

    private final static String serverName = "com.ibm.ws.kernel.boot.commandline.CreateCommandTest";
    private static Bootstrap bootstrap;
    private static Machine machine;
    private static String installPath;;
    private static String defaultServerPath;
    private static String previousWorkDir;

    @BeforeClass
    public static void setup() throws Exception {
        bootstrap = Bootstrap.getInstance();
        machine = LibertyServerUtils.createMachine(bootstrap);
        previousWorkDir = machine.getWorkDir();
        machine.setWorkDir(null);
        installPath = LibertyFileManager.getInstallPath(bootstrap);
        defaultServerPath = installPath + "/usr/servers/" + serverName;
        // Use absolute path in case this is running on Windows without CYGWIN
        bootstrap.setValue("libertyInstallPath", installPath);
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

    @Test
    public void testIsServerEnvCreated() throws Exception {

        ProgramOutput po = LibertyServerUtils.executeLibertyCmd(bootstrap, "server", "create", serverName);
        assertEquals("Unexpected return code from server create command: STDOUT: " + po.getStdout() + " STDERR: " + po.getStderr(), 0, po.getReturnCode());

        // check that server directory was created
        assertTrue("Expected server directory to exist at " + defaultServerPath + ", but does not", LibertyFileManager.libertyFileExists(machine, defaultServerPath));

        // check that server.xml exists
        String serverXmlPath = defaultServerPath + "/server.xml";
        assertTrue("Expected server.xml file to exist at " + serverXmlPath + ", but does not", LibertyFileManager.libertyFileExists(machine, serverXmlPath));

        String serverEnvPath = defaultServerPath + "/server.env";
        assertTrue("Expected server.env file to exist at " + serverEnvPath + ", but does not", LibertyFileManager.libertyFileExists(machine, serverEnvPath));

        String serverEnvContents = FileUtils.readFile(serverEnvPath);
        assertTrue("Expected server.env to contain generated keystore password at " + serverEnvPath, serverEnvContents.contains("keystore_password="));
        assertTrue("Expected server.env to NOT contain WLP_SKIP_MAXPERMSIZE at: " + serverEnvPath, !serverEnvContents.contains("WLP_SKIP_MAXPERMSIZE"));
    }

    @Test
    public void testServerEnvNoPassword() throws Exception {
        ProgramOutput po = LibertyServerUtils.executeLibertyCmd(bootstrap, "server", "create", serverName, "--no-password");
        assertEquals("Unexpected return code from server create command: STDOUT: " + po.getStdout() + " STDERR: " + po.getStderr(), 0, po.getReturnCode());

        // check that server directory was created
        assertTrue("Expected server directory to exist at " + defaultServerPath + ", but does not", LibertyFileManager.libertyFileExists(machine, defaultServerPath));

        // check that server.xml exists
        String serverXmlPath = defaultServerPath + "/server.xml";
        assertTrue("Expected server.xml file to exist at " + serverXmlPath + ", but does not", LibertyFileManager.libertyFileExists(machine, serverXmlPath));

        String serverEnvPath = defaultServerPath + "/server.env";
        assertTrue("Expected server.env file to exist at " + serverEnvPath + ", but does not", LibertyFileManager.libertyFileExists(machine, serverEnvPath));
        String serverEnvContents = FileUtils.readFile(serverEnvPath);
        assertFalse("Expected server.env to NOT contain generated keystore password at " + serverEnvPath, serverEnvContents.contains("keystore_password="));
        assertTrue("Expected server.env to NOT contain WLP_SKIP_MAXPERMSIZE at: " + serverEnvPath,
                   !serverEnvContents.contains("WLP_SKIP_MAXPERMSIZE"));
    }

    /**
     * Test that server create command rejects server names containing spaces.
     * Expected behavior: CWWKE0012E error message should be displayed indicating
     * that the server name contains an invalid character (space).
     */
    @Test
    public void testServerNameWithSpaceRejected() throws Exception {
        String invalidServerName = "my server";
        
        // Use the preserveSpaces flag to ensure the server name with space is passed as a single argument
        ProgramOutput po = LibertyServerUtils.executeLibertyCmd(bootstrap, "server", true, "create", invalidServerName);
        
        // Verify that the command failed (non-zero return code)
        assertTrue("Expected non-zero return code when creating server with space in name. STDOUT: " + po.getStdout() + " STDERR: " + po.getStderr(),
                   po.getReturnCode() != 0);
        
        // Error messages may be in stdout or stderr depending on how the command is executed
        String output = po.getStdout() + " " + po.getStderr();
        
        // Verify that the error output contains the CWWKE0012E error message
        assertTrue("Expected CWWKE0012E error message in output. OUTPUT: " + output,
                   output.contains("CWWKE0012E"));
        
        // Verify that the error message mentions the invalid server name
        assertTrue("Expected error message to mention the invalid server name '" + invalidServerName + "'. OUTPUT: " + output,
                   output.contains(invalidServerName));
        
        // Verify that the error message mentions "character that is not valid" or similar
        assertTrue("Expected error message to mention invalid character. OUTPUT: " + output,
                   output.contains("character") && (output.contains("not valid") || output.contains("invalid")));
        
        // Verify that the server directory was NOT created
        String invalidServerPath = installPath + "/usr/servers/" + invalidServerName;
        assertFalse("Server directory should NOT have been created for invalid server name at " + invalidServerPath,
                    LibertyFileManager.libertyFileExists(machine, invalidServerPath));
    }
}
