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
     * check if this is Open or WebSphere Liberty
     * 
     * @return
     */
    private static boolean isClosedLibertyWlp() {
        return new File(installRoot + "/lib/versions/WebSphereApplicationServer.properties").exists();
    }

    /**
     * Count number of occurences of files beginning with string beginsWith
     * 
     * @param listOfFiles
     * @param beginsWith
     * @return int
     */
    private int countFiles(File[] listOfFiles, String beginsWith) {
        String METHODNAME = "countFiles";
        int count = 0;
        String filename;
        for (int i = 0; i < listOfFiles.length; i++) {
            filename = listOfFiles[i].getName();
            if (listOfFiles[i].isFile()) {
                if (filename.startsWith(beginsWith)) {
                    count++;
                    Log.info(c, METHODNAME, "found file starting with " + beginsWith + "expected :" + filename);
                } ;
            }
        }

        return count;
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

        Log.info(c, METHOD_NAME, "## Checking swidtags:\n");
        Log.info(c, METHOD_NAME, "Total number of files:" + listOfFiles.length);

        int tag1 = countFiles(listOfFiles, "openliberty.io_OpenLiberty.swidtag");
        int tag2 = countFiles(listOfFiles, "ibm.com_WebSphereLiberty-");
        int tag3 = countFiles(listOfFiles, "ibm.com_WebSphere_Application_Server_Liberty");

        // Check for OL swidtag (should only be in Open Liberty)
        if (tag1 > 0) {
            Log.info(c, METHOD_NAME, "OL tag found:" + tag2);
            ol_swidtag_exists = true;
        }
        
        // Check for IBM OL swidtag (should only be in Open Liberty)
        if (tag2 > 0) {
            Log.info(c, METHOD_NAME, "IBM OL tag found:" + tag2);
            IBM_ol_swidtag_exists = true;
        }

        // Check for WebSphere Liberty tag (should only be in WebSphere Liberty)
        if (tag3 > 0) {
            Log.info(c, METHOD_NAME, "Closed Liberty tags found:" + tag3);
            unexpected_file_found = true;
        }

        testsPassed = ol_swidtag_exists && IBM_ol_swidtag_exists && !unexpected_file_found;

        String assertMsg = "Incorrect number of swidtags found."
                           + "\n    " + tag1 + "/1 openliberty .swidtag found"
                           + "\n    " + tag2 + "/1 IBM openliberty .swidtag found"
                           + "\n    " + tag3 + "/0 other tag files found";

        if (!testsPassed) {
            Log.info(c, METHOD_NAME, "INCORRECT number of tags found.\n");
            // start DEBUG code here:
            Log.info(c, METHOD_NAME, "####DEBUG INFO FOLLOWS####\n");

            String debugpo1args[] = { "-l", tagFolder };
            ProgramOutput debugpo1 = runCommand(METHOD_NAME, "ls", debugpo1args);
            Log.info(c, METHOD_NAME, "Tag File Directory=" + tagFolder);
            Log.info(c, METHOD_NAME, "Tag File Directory listing.:\n" + debugpo1.getStdout());

            String debugpo2args[] = { "-l", installRoot + "/lib/versions/*properties" };
            ProgramOutput debugpo2 = runCommand(METHOD_NAME, "ls", debugpo2args);
            Log.info(c, METHOD_NAME, "Listing Property Files in " +
                                     installRoot + "/lib/versions/*properties:\n" + debugpo2.getStdout());
            // end DEBUG code
        }

        //Uninstall package
        ProgramOutput po6 = uninstallPackage(METHOD_NAME, packageExt);
        Log.info(c, METHOD_NAME, "Uninstalled Open Liberty RC:" + po6.getReturnCode());

        Assert.assertTrue(assertMsg, testsPassed);
        Log.info(c, METHOD_NAME, "swidtags test passed!");
        exiting(c, METHOD_NAME);
    }

}
