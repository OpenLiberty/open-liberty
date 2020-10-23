/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.packaging.fat;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

public class ServicesTest extends InstallPackagesToolTest {
    private static final Class<?> c = ServicesTest.class;

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        Assume.assumeTrue(isSupportedOS());
        setupEnv();
        Assume.assumeTrue(isjavaHomeExecutable);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (isSupportedOS()) {
            final String METHOD_NAME = "cleanup";
            entering(c, METHOD_NAME);
            cleanupEnv();
            exiting(c, METHOD_NAME);
        } else {
            logger.info("This machine is not a supported OS for this FAT. Skipping cleanup.");
        }
    }

    /**
     * Service Test.
     * <p>
     * 1. install open liberty
     * 2. start service
     * 3. stop service
     * 4. restart service
     * 5. stop service
     * 6. uninstall open liberty
     */
    @Test
    public void testService() throws Exception {

        String METHOD_NAME = "testService";
        entering(c, METHOD_NAME);

        Boolean testsPassed = false;

        //Install package
        Log.info(c, METHOD_NAME, "Installing Open Liberty package.");
        ProgramOutput po1 = installCurrentPackage(METHOD_NAME, packageExt);

        // append JAVA_HOME to server.env
        Log.info(c, METHOD_NAME, "Configure Open Liberty to use test java.");
        String[] param1j = { "sh -c ", " 'echo JAVA_HOME=" + javaHome + " >> /opt/ol/etc/server.env'" };
        ProgramOutput po1j = runCommand(METHOD_NAME, "sudo ", param1j);
        Log.info(c, METHOD_NAME, "setup server.env permissions RC:" + po1j.getReturnCode());

        // Output contents of server.env
        Log.info(c, METHOD_NAME, "Contents of opt/ol/etc/server.env:");
        String[] param2b = { "cat", "/opt/ol/etc/server.env" };
        ProgramOutput po2b = runCommand(METHOD_NAME, "sudo ", param2b);

        // service tests
        Log.info(c, METHOD_NAME, "Starting defaultServer");

        ProgramOutput po2 = serviceCommand(METHOD_NAME, "start", "defaultServer");
        Thread.sleep(2000);
        ProgramOutput po2a = serviceCommand(METHOD_NAME, "status", "defaultServer");
        
        Log.info(c, METHOD_NAME, "Stopping defaultServer");
        ProgramOutput po3 = serviceCommand(METHOD_NAME, "stop", "defaultServer");
        Thread.sleep(2000);
        ProgramOutput po3a = serviceCommand(METHOD_NAME, "status", "defaultServer");

        Log.info(c, METHOD_NAME, "Re-starting defaultServer");
        ProgramOutput po4 = serviceCommand(METHOD_NAME, "restart", "defaultServer");
        Thread.sleep(2000);
        ProgramOutput po4a = serviceCommand(METHOD_NAME, "status", "defaultServer");
        Log.info(c, METHOD_NAME, "Stopping defaultServer");
        ProgramOutput po5 = serviceCommand(METHOD_NAME, "stop", "defaultServer");

        //Uninstall package
        ProgramOutput po6 = uninstallPackage(METHOD_NAME, packageExt);
        Log.info(c, METHOD_NAME, "Test Results Summary:\n" 
                          +"===================="
                          + "Install Current openliberty RC1:" + po1.getReturnCode() + "\n"
                          + "start defaultServer.service RC2:" + po2.getReturnCode() + "\n"
                          + "status defaultServer.service RC2a:" + po2a.getReturnCode() + "\n"
                          + "stop defaultServer.service RC3:" + po3.getReturnCode() + "\n"
                          + "status defaultServer.service RC3a:" + po3a.getReturnCode() + "\n"
                          + "restart defaultServer.service RC4:" + po4.getReturnCode() + "\n"
                          + "status defaultServer.service RC4a:" + po4a.getReturnCode() + "\n"
                          + "stop defaultServer.service RC5:" + po5.getReturnCode() + "\n"
                          + "uninstall openliberty RC6:" + po6.getReturnCode() + "\n");
        testsPassed = ((po1.getReturnCode() == 0) 
                        && (po2.getReturnCode() == 0) 
                        && (po3.getReturnCode() == 0) 
                        && (po4.getReturnCode() == 0)
                        && (po5.getReturnCode() == 0)
                        && (po6.getReturnCode() == 0));
        Assert.assertTrue("Non zero return code in service test case. " + packageExt
                          + "Install Current openliberty RC1:" + po1.getReturnCode() + "\n"
                          + "start defaultServer.service RC2:" + po2.getReturnCode() + "\n"
                          + "status defaultServer.service RC2a:" + po2a.getReturnCode() + "\n"
                          + "stop defaultServer.service RC3:" + po3.getReturnCode() + "\n"
                          + "status defaultServer.service RC3a:" + po3a.getReturnCode() + "\n"
                          + "restart defaultServer.service RC4:" + po4.getReturnCode() + "\n"
                          + "status defaultServer.service RC4a:" + po4a.getReturnCode() + "\n"
                          + "stop defaultServer.service RC5:" + po5.getReturnCode() + "\n"
                          + "uninstall openliberty RC6:" + po6.getReturnCode() + "\n", testsPassed);
        exiting(c, METHOD_NAME);
    }

}
