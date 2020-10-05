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

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

public class CheckTags extends InstallPackagesToolTest {
    private static final Class<?> c = CheckTags.class;

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
     * This test will verify existence of Open Liberty swidtags
     *
     * @throws Exception
     */
    @Test
    public void testSwidtagsExist() throws Exception {
        Assume.assumeTrue(packagesBuilt);

        String METHOD_NAME = "testSwidtagsExist";
        entering(c, METHOD_NAME);

        Boolean testsPassed = false;

        Boolean ol_swidtag_exists = false;
        Boolean IBM_ol_swidtag_exists = false;
        Boolean unexpected_file_found = false;

        String tagFolder = "/opt/ol/lib/versions/tags";

        //Install package
        Log.info(c, METHOD_NAME, "Installing Open Liberty:");

        ProgramOutput po1 = installCurrentPackage(METHOD_NAME, packageExt);
        Log.info(c, METHOD_NAME, "Installed Open Liberty RC:" + po1.getReturnCode());

        // check for ol tag openliberty.io_OpenLiberty.swidtag

        String filename;
        File folder = new File(tagFolder);
        File[] listOfFiles = folder.listFiles();

        System.out.println("Checking swidtags:\n");
        System.out.println("Total number of files:" + listOfFiles.length);
        for (int i = 0; i < listOfFiles.length; i++) {
            filename = listOfFiles[i].getName();
            if (listOfFiles[i].isFile()) {
                if (filename.startsWith("openliberty.io_OpenLiberty.swidtag")) {

                    ol_swidtag_exists = true;
                    System.out.println("found expected openliberty tag:" + filename);
                } else if (filename.startsWith("ibm.com_WebSphereLiberty-") && filename.endsWith(".swidtag")) {

                    IBM_ol_swidtag_exists = true;
                    System.out.println("found expected IBM openliberty:" + filename);
                } else {
                    System.out.println("UNEXPECTED FILE FOUND:" + filename);
                    unexpected_file_found = true;
                } ;
            } else if (listOfFiles[i].isDirectory()) {
                unexpected_file_found = true;
                System.out.println("Directory " + listOfFiles[i].getName());
            }
        }
        testsPassed = ol_swidtag_exists && IBM_ol_swidtag_exists && !unexpected_file_found;

        String assertMsg = "Test FAILED."
                           + "\n     openliberty .swidtag exists    :" + ol_swidtag_exists
                           + "\n     IBM openliberty .swidtag exists:" + IBM_ol_swidtag_exists
                           + "\n     Unexpected files found         :" + unexpected_file_found;


        //Uninstall package
        ProgramOutput po6 = uninstallPackage(METHOD_NAME, packageExt);
        Log.info(c, METHOD_NAME, "Uninstalled Open Liberty RC:" + po6.getReturnCode());
        
        Assert.assertTrue(assertMsg, testsPassed);
        Log.info(c, METHOD_NAME, "swidtags test passed!");
        exiting(c, METHOD_NAME);
    }

}
