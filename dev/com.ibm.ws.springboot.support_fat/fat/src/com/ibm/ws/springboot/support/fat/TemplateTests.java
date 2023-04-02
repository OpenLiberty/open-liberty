/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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
package com.ibm.ws.springboot.support.fat;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.common.apiservices.Bootstrap;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.utils.LibertyServerUtils;

@RunWith(FATRunner.class)
public class TemplateTests {

    // Class-specific fields
    private static Bootstrap bootstrap;
    private static Machine machine;
    private static String installPath;
    private static String previousWorkDir;

    @BeforeClass
    public static void setup() throws Exception {
        bootstrap = Bootstrap.getInstance();
        machine = LibertyServerUtils.createMachine(bootstrap);
        previousWorkDir = machine.getWorkDir();
        machine.setWorkDir(null);
        installPath = LibertyFileManager.getInstallPath(bootstrap);
        // Use absolute path in case this is running on Windows without CYGWIN
        bootstrap.setValue("libertyInstallPath", installPath);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        machine.setWorkDir(previousWorkDir);
    }

    // Test-specific fields
    private static String[] expectedFeatures;
    private static String serverName;
    private static String serverPath;

    @After
    public void cleanupServer() throws Exception {
        // Since we are not using the normal LibertyServer class, we need to explicitly
        // clean up a server. We cleanup before server creation to ensure a server does
        // not already exist, and then after each test to leave the test environment as
        // we found it.
        if (LibertyFileManager.libertyFileExists(machine, serverPath)) {
            LibertyFileManager.deleteLibertyDirectoryAndContents(machine, serverPath);
        }
    }

    @Test
    @Mode(FULL)
    public void testCreateServerFromSpringBoot1() throws Exception {
        expectedFeatures = new String[] { "servlet-4.0", "ssl-1.0", "transportSecurity-1.0", "websocket-1.1", "springBoot-1.5" };
        serverName = "com.ibm.ws.springboot.support.fat.TemplateServer1";
        serverPath = installPath + "/usr/servers/" + serverName;

        createServerFromTemplate("springBoot1", expectedFeatures);
    }

    @Test
    public void testCreateServerFromSpringBoot2() throws Exception {
        expectedFeatures = new String[] { "servlet-4.0", "ssl-1.0", "transportSecurity-1.0", "websocket-1.1", "springBoot-2.0" };
        serverName = "com.ibm.ws.springboot.support.fat.TemplateServer2";
        serverPath = installPath + "/usr/servers/" + serverName;

        createServerFromTemplate("springBoot2", expectedFeatures);
    }

    void createServerFromTemplate(String templateName, String[] expectedFeatures) throws Exception {
        cleanupServer();

        ProgramOutput po;
        try {
            po = LibertyServerUtils.executeLibertyCmd(bootstrap, "server", "create", serverName, "--template=" + templateName);
            assertEquals("Unexpected return code from server create command: STDOUT: " + po.getStdout() + " STDERR: " + po.getStderr(), 0, po.getReturnCode());

            po = LibertyServerUtils.executeLibertyCmd(bootstrap, "server", "start", serverName);
            assertEquals("Unexpected return code from server start command: STDOUT: " + po.getStdout() + " STDERR: " + po.getStderr(), 0, po.getReturnCode());

            RemoteFile serverLog = LibertyFileManager.getLibertyFile(machine, serverPath + "/logs/messages.log");

            // Scrape messages.log to see what features were installed
            List<String> installedFeaturesRaw = LibertyFileManager.findStringsInFile("CWWKF0012I: .*", serverLog);
            Set<String> installedFeatures = new HashSet<String>();
            for (String f : installedFeaturesRaw)
                for (String installedFeature : f.substring(0, f.lastIndexOf(']')).substring(f.lastIndexOf('[') + 1).split(","))
                    installedFeatures.add(installedFeature.trim().toLowerCase());

            for (String ef : expectedFeatures) {
                assertTrue("A required feature did not install on the server: " + ef, installedFeatures.contains(ef.toLowerCase()));
            }

        } finally {
            po = LibertyServerUtils.executeLibertyCmd(bootstrap, "server", "stop", serverName);
            assertEquals("Unexpected return code from server stop command: STDOUT: " + po.getStdout() + " STDERR: " + po.getStderr(), 0, po.getReturnCode());
        }
    }

}
