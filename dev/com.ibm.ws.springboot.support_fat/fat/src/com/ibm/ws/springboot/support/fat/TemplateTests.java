/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.junit.AfterClass;
import org.junit.Before;
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

    private final static String serverName = "com.ibm.ws.springboot.support.fat.TemplateServer";
    private static Bootstrap bootstrap;
    private static Machine machine;
    private static String installPath;;
    private static String springBootServerPath;
    private static String previousWorkDir;

    @BeforeClass
    public static void setup() throws Exception {
        bootstrap = Bootstrap.getInstance();
        machine = LibertyServerUtils.createMachine(bootstrap);
        previousWorkDir = machine.getWorkDir();
        machine.setWorkDir(null);
        installPath = LibertyFileManager.getInstallPath(bootstrap);
        springBootServerPath = installPath + "/usr/servers/" + serverName;
        // Use absolute path in case this is running on Windows without CYGWIN
        bootstrap.setValue("libertyInstallPath", installPath);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        machine.setWorkDir(previousWorkDir);
    }

    @Before
    public void cleanupBeforeRun() throws Exception {
        // since we are not using the normal LibertyServer class for this server,
        // we need to make sure to explicitly clean up.  We do this before running
        // the test in order to preserve the contents on disk.
        if (LibertyFileManager.libertyFileExists(machine, springBootServerPath)) {
            LibertyFileManager.deleteLibertyDirectoryAndContents(machine, springBootServerPath);
        }
    }

    static String[] expectedFeatures;

    @Test
    @Mode(FULL)
    public void testCreateServerFromSpringBoot1() throws Exception {
        expectedFeatures = new String[] { "servlet-4.0", "ssl-1.0", "transportSecurity-1.0", "websocket-1.1", "springBoot-1.5" };

        createServerFromTemplate("springBoot1", expectedFeatures);
    }

    @Test
    public void testCreateServerFromSpringBoot2() throws Exception {
        expectedFeatures = new String[] { "servlet-4.0", "ssl-1.0", "transportSecurity-1.0", "websocket-1.1", "springBoot-2.0" };

        createServerFromTemplate("springBoot2", expectedFeatures);
    }

    void createServerFromTemplate(String templateName, String[] expectedFeatures) throws Exception {
        ProgramOutput po;
        try {
            po = LibertyServerUtils.executeLibertyCmd(bootstrap, "server", "create", serverName, "--template=" + templateName);
            assertEquals("Unexpected return code from server create command: STDOUT: " + po.getStdout() + " STDERR: " + po.getStderr(), 0, po.getReturnCode());

            po = LibertyServerUtils.executeLibertyCmd(bootstrap, "server", "start", serverName);
            assertEquals("Unexpected return code from server start command: STDOUT: " + po.getStdout() + " STDERR: " + po.getStderr(), 0, po.getReturnCode());

            RemoteFile serverLog = LibertyFileManager.getLibertyFile(machine, springBootServerPath + "/logs/messages.log");

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
