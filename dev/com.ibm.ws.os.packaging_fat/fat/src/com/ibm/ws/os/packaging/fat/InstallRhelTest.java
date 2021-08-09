/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.os.packaging.fat;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

public class InstallRhelTest extends InstallUtilityToolTest {
    private static final Class<?> c = InstallRhelTest.class;
    public static File openLib = new File("/var/lib/openliberty");
    public static boolean openLibExists = openLib.exists();

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        Assume.assumeTrue(isLinuxRhel());
        if (openLibExists) {
            logger.info("/var/lib/openliberty found. OpenLiberty is Installed");
            setupEnv();
            createServerEnv();
        } else {
            logger.info("OpenLiberty did not install successfully");
        }
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (openLibExists) {
            if (isLinuxRhel()) {
                final String METHOD_NAME = "cleanup";
                entering(c, METHOD_NAME);
                cleanupEnv();
                exiting(c, METHOD_NAME);
            } else {
                logger.info("This machine is not RHEL");
            }
        } else {
            logger.info("OpenLiberty did not install successfully");
        }
    }

    @Test
    public void testVerifyRpmInstall() throws Exception {

        if (openLibExists) {
            String METHOD_NAME = "testVerifyRpmInstall";
            entering(c, METHOD_NAME);

            String[] param1s = { "-qi", "openliberty" };
            ProgramOutput po = runCommand(METHOD_NAME, "rpm", param1s);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            exiting(c, METHOD_NAME);
        } else {
            logger.info("OpenLiberty did not install successfully");
        }
    }

    @Test
    public void testServices() throws Exception {

        String METHOD_NAME = "testServices";
        entering(c, METHOD_NAME);
        // append JAVA_HOME to server.env
        //sudo sh -c 'echo line > file'

        String[] param1j = { "sh -c ", " 'echo JAVA_HOME=" + javaHome + " >> /opt/ol/etc/server.env'" };
        ProgramOutput po1j = runCommand(METHOD_NAME, "sudo ", param1j);
        Log.info(c, METHOD_NAME, "setup server.env permissions RC:" + po1j.getReturnCode());

        // Output contents of server.env
        Log.info(c, METHOD_NAME, "Contents of opt/ol/etc/server.env");
        String[] param2b = { "cat", "/opt/ol/etc/server.env" };
        ProgramOutput po2b = runCommand(METHOD_NAME, "sudo ", param2b);

        // service tests
        Log.info(c, METHOD_NAME, "Starting defaultServer");
        ProgramOutput po2 = serviceCommand(METHOD_NAME, "start", "defaultServer");
        TimeUnit.SECONDS.sleep(2);
        ProgramOutput po2a = serviceCommand(METHOD_NAME, "status", "defaultServer");

        Log.info(c, METHOD_NAME, "Stopping defaultServer");
        ProgramOutput po3 = serviceCommand(METHOD_NAME, "stop", "defaultServer");
        TimeUnit.SECONDS.sleep(2);
        ProgramOutput po3a = serviceCommand(METHOD_NAME, "status", "defaultServer");

        Log.info(c, METHOD_NAME, "Re-starting defaultServer");
        ProgramOutput po4 = serviceCommand(METHOD_NAME, "restart", "defaultServer");
        TimeUnit.SECONDS.sleep(2);
        ProgramOutput po4a = serviceCommand(METHOD_NAME, "status", "defaultServer");

        Log.info(c, METHOD_NAME, "Stopping defaultServer");
        ProgramOutput po5 = serviceCommand(METHOD_NAME, "stop", "defaultServer");

        Log.info(c, METHOD_NAME, "Test Results Summary:\n"
                                 + "===================="
                                 + "start defaultServer.service RC2:" + po2.getReturnCode() + "\n"
                                 + "status defaultServer.service RC2a:" + po2a.getReturnCode() + "\n"
                                 + "stop defaultServer.service RC3:" + po3.getReturnCode() + "\n"
                                 + "status defaultServer.service RC3a:" + po3a.getReturnCode() + "\n"
                                 + "restart defaultServer.service RC4:" + po4.getReturnCode() + "\n"
                                 + "status defaultServer.service RC4a:" + po4a.getReturnCode() + "\n"
                                 + "stop defaultServer.service RC5:" + po5.getReturnCode() + "\n");

        Boolean testsPassed = ((po2.getReturnCode() == 0) && (po3.getReturnCode() == 0) && (po4.getReturnCode() == 0)
                               && (po5.getReturnCode() == 0));
        Assert.assertTrue("Non zero return code in service test case. "
                          + "start defaultServer.service RC2:" + po2.getReturnCode() + "\n"
                          + "status defaultServer.service RC2a:" + po2a.getReturnCode() + "\n"
                          + "stop defaultServer.service RC3:" + po3.getReturnCode() + "\n"
                          + "status defaultServer.service RC3a:" + po3a.getReturnCode() + "\n"
                          + "restart defaultServer.service RC4:" + po4.getReturnCode() + "\n"
                          + "status defaultServer.service RC4a:" + po4a.getReturnCode() + "\n"
                          + "stop defaultServer.service RC5:" + po5.getReturnCode() + "\n", testsPassed);
    }

    @Test
    public void testUninstallRpm() throws Exception {

        if (openLibExists) {
            String METHOD_NAME = "testUninstallRpm";
            entering(c, METHOD_NAME);

            String[] param1s = { "remove", "-y", "openliberty" };
            ProgramOutput po = runCommand(METHOD_NAME, "sudo yum", param1s);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            exiting(c, METHOD_NAME);
        } else {
            logger.info("OpenLiberty did not install successfully");
        }
    }
}
