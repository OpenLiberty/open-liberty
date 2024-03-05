/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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
package com.ibm.ws.os.packaging.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

public class InstallUbuntuTest extends InstallUtilityToolTest {
    private static final Class<?> c = InstallUbuntuTest.class;
    public static File openLib = new File("/var/lib/openliberty");
    public static boolean openLibExists = openLib.exists();

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        Assume.assumeTrue(isLinuxUbuntu());
        //Assume.assumeTrue(ConnectedToIMRepo);
        if (openLibExists) {
            logger.info("/var/lib/openliberty found. OpenLiberty is Installed");
            setupEnv();
            createServerEnv();
        } else {
            logger.info("OpenLiberty did not install successfully");
            Assume.assumeTrue(openLibExists);
        }
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (openLibExists) {
            if (isLinuxUbuntu()) {
                final String METHOD_NAME = "cleanup";
                entering(c, METHOD_NAME);
                cleanupEnv();
                exiting(c, METHOD_NAME);
            } else {
                logger.info("This machine is not ubuntu");
            }
        } else {
            logger.info("OpenLiberty did not install successfully");
        }
    }

    @Test
    public void testVerifyDebInstall() throws Exception {
        String METHOD_NAME = "testVerifyDebInstall";
        entering(c, METHOD_NAME);

        //check Open Liberty was installed

        String[] paramInstallStatus = { "-s", "openliberty" };
        ProgramOutput poInstallStatus = runCommand(METHOD_NAME, "dpkg", paramInstallStatus);
        assertEquals("Expected exit code", 0, poInstallStatus.getReturnCode());
        String output = poInstallStatus.getStdout();
        assertTrue("Should contain installed status",
                   output.indexOf("Status: install ok installed") >= 0);

        // test Open Liberty services
        // append JAVA_HOME to server.env
        //sudo sh -c 'echo line > file'

        String[] paramSetJavaHome = { "sh -c ", " 'echo JAVA_HOME=" + javaHome + " >> /opt/ol/etc/server.env'" };
        ProgramOutput poSetJavaHome = runCommand(METHOD_NAME, "sudo ", paramSetJavaHome);
        Log.info(c, METHOD_NAME, "setup server.env permissions RC:" + poSetJavaHome.getReturnCode());

        // Output contents of server.env
        Log.info(c, METHOD_NAME, "Contents of opt/ol/etc/server.env");
        String[] paramCatServerEnv = { "cat", "/opt/ol/etc/server.env" };
        ProgramOutput poCatServerEnv = runCommand(METHOD_NAME, "sudo ", paramCatServerEnv);

        // service tests
        Log.info(c, METHOD_NAME, "Starting defaultServer");
        ProgramOutput poServerStart = serviceCommand(METHOD_NAME, "start", "defaultServer");
        TimeUnit.SECONDS.sleep(2);
        ProgramOutput poServerStatus1 = serviceCommand(METHOD_NAME, "status", "defaultServer");

        Log.info(c, METHOD_NAME, "Stopping defaultServer");
        ProgramOutput poServerStop1 = serviceCommand(METHOD_NAME, "stop", "defaultServer");
        TimeUnit.SECONDS.sleep(2);
        ProgramOutput poServerStatus2 = serviceCommand(METHOD_NAME, "status", "defaultServer");

        Log.info(c, METHOD_NAME, "Re-starting defaultServer");
        ProgramOutput poServerRestart = serviceCommand(METHOD_NAME, "restart", "defaultServer");
        TimeUnit.SECONDS.sleep(2);
        ProgramOutput poServerStatus3 = serviceCommand(METHOD_NAME, "status", "defaultServer");

        Log.info(c, METHOD_NAME, "Stopping defaultServer");
        ProgramOutput poServerStop2 = serviceCommand(METHOD_NAME, "stop", "defaultServer");

        Log.info(c, METHOD_NAME, "Test Results Summary:\n"
                                 + "===================="
                                 + "start defaultServer.service RC2:" + poServerStart.getReturnCode() + "\n"
                                 + "status defaultServer.service RC2a:" + poServerStatus1.getReturnCode() + "\n"
                                 + "stop defaultServer.service RC3:" + poServerStop1.getReturnCode() + "\n"
                                 + "status defaultServer.service RC3a:" + poServerStatus2.getReturnCode() + "\n"
                                 + "restart defaultServer.service RC4:" + poServerRestart.getReturnCode() + "\n"
                                 + "status defaultServer.service RC4a:" + poServerStatus3.getReturnCode() + "\n"
                                 + "stop defaultServer.service RC5:" + poServerStop2.getReturnCode() + "\n");

        Boolean testsPassed = ((poServerStart.getReturnCode() == 0) && (poServerStop1.getReturnCode() == 0) && (poServerRestart.getReturnCode() == 0)
                               && (poServerStop2.getReturnCode() == 0));
        Assert.assertTrue("Non zero return code in service test case. "
                          + "start defaultServer.service RC2:" + poServerStart.getReturnCode() + "\n"
                          + "status defaultServer.service RC2a:" + poServerStatus1.getReturnCode() + "\n"
                          + "stop defaultServer.service RC3:" + poServerStop1.getReturnCode() + "\n"
                          + "status defaultServer.service RC3a:" + poServerStatus2.getReturnCode() + "\n"
                          + "restart defaultServer.service RC4:" + poServerRestart.getReturnCode() + "\n"
                          + "status defaultServer.service RC4a:" + poServerStatus3.getReturnCode() + "\n"
                          + "stop defaultServer.service RC5:" + poServerStop2.getReturnCode() + "\n", testsPassed);

        // test Uninstall

        String[] paramUninstall = { "remove", "-y", "openliberty" };
        ProgramOutput poUninstall = runCommand(METHOD_NAME, "sudo apt-get", paramUninstall);
        assertEquals("Expected exit code", 0, poUninstall.getReturnCode());

        exiting(c, METHOD_NAME);
    }
}
