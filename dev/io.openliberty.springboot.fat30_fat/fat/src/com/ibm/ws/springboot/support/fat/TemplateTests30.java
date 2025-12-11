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

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.common.apiservices.Bootstrap;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.utils.LibertyServerUtils;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class TemplateTests30 {
    private static Bootstrap bootstrap;
    private static String installPath;

    private static Machine machine;
    private static String previousWorkDir;

    @BeforeClass
    public static void setup() throws Exception {
        bootstrap = Bootstrap.getInstance();
        installPath = LibertyFileManager.getInstallPath(bootstrap);
        // Use absolute path in case this is running on Windows without CYGWIN
        bootstrap.setValue("libertyInstallPath", installPath);

        machine = LibertyServerUtils.createMachine(bootstrap);
        previousWorkDir = machine.getWorkDir();
        machine.setWorkDir(null);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        machine.setWorkDir(previousWorkDir);
    }

    //

    private String templateName;
    private Set<String> templateFeatures;

    private String serverName;
    private String serverPath;

    @Before
    public void setupTest() throws Exception {
        templateName = "springBoot3";

        templateFeatures = new HashSet<String>();
        templateFeatures.add("expressionLanguage-5.0");
        templateFeatures.add("pages-3.1");
        templateFeatures.add("servlet-6.0");
        templateFeatures.add("springBoot-3.0");
        templateFeatures.add("ssl-1.0");
        templateFeatures.add("transportSecurity-1.0");
        templateFeatures.add("websocket-2.1");

        serverName = "spring3";
        serverPath = installPath + "/usr/servers/" + serverName;

        cleanupServer();
    }

    @After
    public void tearDownTest() throws Exception {
        cleanupServer();
    }

    // Since we are not using the normal LibertyServer class, the server
    // directory must be explicitly deleted.

    public void cleanupServer() throws Exception {
        if (LibertyFileManager.libertyFileExists(machine, serverPath)) {
            LibertyFileManager.deleteLibertyDirectoryAndContents(machine, serverPath);
        }
    }

    //

    @Test
    public void testCreateSpringBoot3() throws Exception {
        System.out.println("Test create SpringBoot3 server from template");

        System.out.println("  Template: " + templateName);
        System.out.println("  Template features: " + templateFeatures);

        System.out.println("  Server: " + serverName);
        System.out.println("  Server home: " + serverPath);

        verifyCmd("Create server [ " + serverName + " ] using template [ " + templateName + " ]", OK_CODE,
                  "server", "create", serverName, "--template=" + templateName);
        try {
            verifyCmd("Start server [ " + serverName + " ]", OK_CODE, "server", "start", serverName);
            verifyFeatures();
        } finally {
            verifyCmd("Stop server [ " + serverName + " ]", OK_CODE, "server", "stop", serverName);
        }
    }

    private void verifyFeatures() throws Exception {
        Set<String> installedFeatures = scanFeatures(serverPath);
        Set<String> extraFeatures = new HashSet<>();
        Set<String> missingFeatures = new HashSet<>();
        Set<String> matchedFeatures = new HashSet<>();

        validate(templateFeatures, installedFeatures,
                 extraFeatures, missingFeatures, matchedFeatures);

        for (String extraFeature : extraFeatures) {
            System.out.println("Extra installed feature: [ " + extraFeature + " ]");
        }
        for (String missingFeature : missingFeatures) {
            System.out.println("Missing installed feature: [ " + missingFeature + " ]");
        }
        for (String matchedFeature : matchedFeatures) {
            System.out.println("Matched installed feature: [ " + matchedFeature + " ]");
        }

        int numMatched = matchedFeatures.size();
        int numExpected = templateFeatures.size();
        if (numMatched != numExpected) {
            assertEquals("Incorrect installed features", numExpected, numMatched);
        }
    }

    private void validate(Set<String> allExpected, Set<String> allActual,
                          Set<String> extra, Set<String> missing, Set<String> matched) {

        for (String expected : allExpected) {
            if (!allActual.contains(expected)) {
                missing.add(expected);
            } else {
                matched.add(expected);
            }
        }

        for (String actual : allActual) {
            if (!allExpected.contains(actual)) {
                extra.add(actual);
            }
        }
    }

    private Set<String> scanFeatures(String serverRoot) throws Exception {
        RemoteFile serverLog = LibertyFileManager.getLibertyFile(machine, serverRoot + "/logs/messages.log");

        Set<String> installed = new HashSet<String>();

        List<String> installLines = LibertyFileManager.findStringsInFile("CWWKF0012I: .*", serverLog);
        for (String installLine : installLines) {
            int startPos = installLine.lastIndexOf('[') + 1;
            int endPos = installLine.lastIndexOf(']');
            if ((startPos == -1) || (endPos == -1) || (startPos >= endPos)) {
                System.out.println("Unexpected feature text [ " + installLine + " ]");
                continue;
            }
            String[] packedFeatures = installLine.substring(startPos, endPos).split(",");
            for (String feature : packedFeatures) {
                installed.add(feature.trim());
            }
        }

        return installed;
    }

    private static ProgramOutput executeLibertyCmd(String command, String... parms) throws Exception {
        return LibertyServerUtils.executeLibertyCmd(bootstrap, command, parms);
    }

    private static final int OK_CODE = 0;

    private void verifyCmd(String actionTag, int expectedCode, String command, String... parms) throws Exception {
        ProgramOutput po = executeLibertyCmd(command, parms);
        verify(actionTag, expectedCode, po);
    }

    private void verify(String actionTag, int expectedCode, ProgramOutput po) {
        int actualCode = po.getReturnCode();
        if (actualCode == expectedCode) {
            System.out.println("Verified result [ " + actionTag + " ]: Expected [ " + expectedCode + " ]");
            return;
        }

        System.out.println("Failed to verify result [ " + actionTag + " ]:" +
                           " Expected [ " + expectedCode + " ]" +
                           " actual [ " + actualCode + " ]");
        System.out.println("STDOUT:");
        System.out.println(po.getStdout());
        System.out.println("STDERR:");
        System.out.println(po.getStderr());

        assertEquals("Unexpected result [ " + actionTag + " ]", expectedCode, actualCode);
    }
}
