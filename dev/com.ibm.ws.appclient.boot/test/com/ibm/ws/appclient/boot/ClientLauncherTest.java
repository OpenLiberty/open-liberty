/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.appclient.boot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.PrintStream;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.kernel.boot.cmdline.ExitCode;

import test.common.SharedOutputManager;

public class ClientLauncherTest {
    static final String testBuildDir = System.getProperty("test.buildDir", "generated");
    static final String TEST_TMP_ROOT = testBuildDir + "/tmp/";
    static final File TEST_TMP_ROOT_FILE = new File(TEST_TMP_ROOT);
    static final File defaultClient = new File(TEST_TMP_ROOT, "usr/clients/defaultClient");
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule outputRule = outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() {
        cleanTempFiles();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        cleanTempFiles();
    }

    public static void cleanTempFiles() {
        recursiveClean(TEST_TMP_ROOT_FILE);
    }

    private static boolean recursiveClean(final File fileToRemove) {
        if (fileToRemove == null)
            return true;

        if (!fileToRemove.exists())
            return true;

        boolean success = true;

        if (fileToRemove.isDirectory()) {
            File[] files = fileToRemove.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    success |= recursiveClean(file);
                } else {
                    success |= file.delete();
                }
            }
            files = fileToRemove.listFiles();
            if (files.length == 0)
                success |= fileToRemove.delete();
        } else {
            success |= fileToRemove.delete();
        }
        return success;
    }

    TestLauncher tlauncher = new TestLauncher();

    // Grab streams set by SharedOutputManager
    PrintStream outputMgrOut;
    PrintStream outputMgrErr;

    @Before
    public void setUp() {
        outputMgr.resetStreams();
        Set<String> keys = System.getProperties().stringPropertyNames();
        for (String key : keys) {
            // Restrict the system properties we pick up and pass to the
            // framework for launch
            if (key.contains("osgi") || key.contains("was") || key.contains("equinox"))
                System.clearProperty(key);
        }

        outputMgrOut = System.out;
        outputMgrErr = System.err;

        // Create server/workarea directories...
        defaultClient.mkdir();
    }

    @After
    public void tearDown() {
        recursiveClean(defaultClient);
    }

    @Test
    public void testParameterHelp() {
        String[] args = new String[] { "--help" };

        int rc = tlauncher.createPlatform(args);
        assertEquals(ExitCode.OK, rc);
        assertTrue("ws-client.jar should be displayed for java -jar help", outputMgr.checkForStandardOut("ws-client.jar"));

        // More detailed testing of help is done in LauncherVerificationTest BVT
        // because script vs. not-script is determined by an env variable..
    }

    /**
     */
    @Test
    public void testParameterUnknownBad() {
        String args[] = new String[] { "--garbage" };

        int rc = tlauncher.createPlatform(args);
        assertTrue(outputMgr.checkForStandardOut("CWWKE0013E"));
        assertEquals(ExitCode.BAD_ARGUMENT, rc);
    }

    /**
     * Simple extension of the launcher for toggling of the behavior of some
     * protected/internal methods in order to control code coverage in the
     * parent.
     */
    class TestLauncher extends ClientLauncher {}
}
