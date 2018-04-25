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
package com.ibm.ws.install;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.install.internal.asset.ServerAsset;

/**
 *
 */
public class ServerAssetTest {

    private static File testDir;

    /**
     * Set up directories
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        testDir = new File("build/unittest/serverAssetTest").getAbsoluteFile();
        testDir.mkdirs();

        if (testDir == null || !testDir.isDirectory())
            throw new IllegalArgumentException("Test requires an existing root directory, but it could not be found: " + testDir.getAbsolutePath());
    }

    /**
     * Final teardown work when class is exiting.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        deleteDirectory(testDir);
    }

    private void initializeTestCase(String testName) {
        System.out.println(ServerAssetTest.class.getSimpleName() + "." + testName
                           + " ***********************************************************************************************");
        System.out.println(ServerAssetTest.class.getSimpleName() + "." + testName + "                     " + testName);
        System.out.println(ServerAssetTest.class.getSimpleName() + "." + testName
                           + " ***********************************************************************************************");
    }

    @Test
    public void testGetServerAssetName() throws Exception {
        initializeTestCase("testGetServerAssetName");

        File sServerXML = new File("publish/servers/serverA/server.xml");
        File dServerXML = new File(testDir, "server.xml");

        try {
            copyFile(sServerXML, dServerXML);
            assertTrue("Cannot find server.xml file: " + dServerXML.getCanonicalPath(), dServerXML.isFile());

            ServerAsset sa = new ServerAsset(dServerXML);
            String name = sa.getServerName();

            assertTrue("Expecting server name start with <tempServer> actual server name <" + name + ">", name.startsWith("tempServer"));
        } finally {
            if (dServerXML.exists())
                dServerXML.delete();
        }
    }

    private static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                try {
                    source.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
            if (destination != null) {
                try {
                    destination.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    private static boolean deleteDirectory(File directory) {
        if (null == directory) {
            return true;
        } else if (directory.exists()) {
            File[] files = directory.listFiles();
            if (null != files) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    }
                    else {
                        file.delete();
                    }
                }
            }
        }
        return (directory.delete());
    }
}
