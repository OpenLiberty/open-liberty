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
package com.ibm.ws.install.packaging.fat;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

public class InstallPackagesTest extends InstallPackagesToolTest {
    private static final Class<?> c = InstallPackagesTest.class;

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        Log.info(c, "BeforeClassSetup", "This FAT only runs on Linux (Intel or Power)");
        Assume.assumeTrue(isSupportedOS());
        setupEnv();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (isSupportedOS()) {
            final String METHOD_NAME = "cleanup";
            entering(c, METHOD_NAME);
            cleanupEnv();
            exiting(c, METHOD_NAME);
        } else {
            Log.info(c, "AfterClassCleanup", "This machine is not a supported OS for this FAT. Skipping cleanup.");
        }
    }

    /**
     * Update RPM Test.
     * <p>
     * 1. Install Test RPM
     * 2. Update to latest RPM
     * 3. Rollback to previous test RPM
     * 4. Uninstall TestRPM
     */
    @Test
    public void testUpdatePackage() throws Exception {
        Assume.assumeTrue(packagesBuilt);

        String METHOD_NAME = "testUpdatePackage";
        entering(c, METHOD_NAME);

        Boolean testsPassed = false;

        Log.info(c, METHOD_NAME, "Installing package:" + packageExt);
        ProgramOutput po1 = installOldPackage(METHOD_NAME, packageExt);

        Log.info(c, METHOD_NAME, "Updating package");
        ProgramOutput po2 = installCurrentPackage(METHOD_NAME, packageExt);

        Log.info(c, METHOD_NAME, "Roll back package");
        ProgramOutput po3 = rollbackPackage(METHOD_NAME, packageExt);

        Log.info(c, METHOD_NAME, "Uninstall package");
        ProgramOutput po4 = uninstallPackage(METHOD_NAME, packageExt);

        testsPassed = ((po1.getReturnCode() == 0) && (po2.getReturnCode() == 0) && (po3.getReturnCode() == 0) && (po4.getReturnCode() == 0));
        Assert.assertTrue("Non zero return code in update test case.\n"
                          + "Install Package RC1:" + po1.getReturnCode() + "\n"
                          + "Update Package RC2:" + po2.getReturnCode() + "\n"
                          + "Rollback Package RC3:" + po3.getReturnCode() + "\n"
                          + "Uninstall Package RC4:" + po4.getReturnCode() + "\n", testsPassed);
        exiting(c, METHOD_NAME);
    }

    /**
     * This test will validate user & group ownership of installed folders
     *
     * @throws Exception
     */
    @Test
    public void testFileUserGroupOwnership() throws Exception {
        Assume.assumeTrue(packagesBuilt);

        String METHOD_NAME = "testFileUserGroupOwnership";
        entering(c, METHOD_NAME);

        Boolean result1 = false;
        Boolean result2 = false;
        Boolean result3 = false;
        Boolean result4 = false;
        Boolean result5 = false;

        Boolean testsPassed = false;

        //Install package
        ProgramOutput po1 = installCurrentPackage(METHOD_NAME, packageExt);
        result1 = checkUserGroupOwnership(METHOD_NAME, "openliberty", "openliberty", "/var/lib/openliberty");
        Log.info(c, METHOD_NAME, "/var/lib/openliberty Result:" + result1);

        result2 = checkUserGroupOwnership(METHOD_NAME, "openliberty", "openliberty", "/var/log/openliberty");
        Log.info(c, METHOD_NAME, "/var/log/openliberty Result:" + result2);

//        result3 = checkUserGroupOwnership(METHOD_NAME, "root", "root", "/opt/ol");
//        Log.info(c, METHOD_NAME, "/opt/ol Result:" + result2);

        //Uninstall package
        ProgramOutput po6 = uninstallPackage(METHOD_NAME, packageExt);

        // check permissions on folders:
        /*
         * * /var/lib/openliberty
         * /var/log/openliberty
         * /var/run/openliberty
         * /etc/init.d/openliberty
         * /usr/share/doc/openliberty
         * /usr/share/openliberty
         */
        // checkUserGroupOwnership(METHOD_NAME,user,group,folder)

        // checkUserGroupOwnership(METHOD_NAME,"openliberty","openliberty",folder
        testsPassed = (result1 && result2);
        Assert.assertTrue("user or group ownership permission test failed. ", testsPassed);
        Log.info(c, METHOD_NAME, "checkUserGroupOwnership Test passed!");
        exiting(c, METHOD_NAME);
    }

}
