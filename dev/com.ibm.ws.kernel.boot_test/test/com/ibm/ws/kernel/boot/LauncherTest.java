/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.FileUtils;

import test.common.SharedOutputManager;
import test.shared.Constants;
import test.shared.TestUtils;

public class LauncherTest {
    static final File defaultServer = new File(Constants.TEST_TMP_ROOT, "usr/servers/defaultServer");
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule outputRule = outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() {
        TestUtils.cleanTempFiles();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        TestUtils.cleanTempFiles();
    }

    TestLauncher tlauncher = new TestLauncher();
    BootstrapConfig config = new BootstrapConfig();

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
        defaultServer.mkdir();
    }

    @After
    public void tearDown() {
        tlauncher.fakeEnv.clear();
        FileUtils.recursiveClean(defaultServer);
    }

    @Test
    public void testHardCodedMainClass() throws Exception {
        File bootJar = TestUtils.findBuiltKernelBundle();
        URLClassLoader loader = new URLClassLoader(new URL[] { bootJar.toURI().toURL() }, null);
        // Our main class is not API/SPI, but we do hard code it in several
        // places, so we use this test to ensure they're all updated.
        // - /com.ibm.ws.kernel.boot/build.xml
        //   - wlp/bin/tools/ws-server.jar!/META-INF/MANIFEST.MF Main-Class
        //   - wlp/lib/ws-launch.jar!/META-INF/MANIFEST.MF Main-Class
        // - /com.ibm.zos.native/server_launcher.c
        // - CMVC NATV/ws/code/os400.native/src/script/qwlpstrsvr.cpp
        loader.loadClass("com.ibm.ws.kernel.boot.cmdline.EnvCheck");
    }

    @Test
    public void testParameterVersion() throws Exception {
        String[] args = new String[] { "--version" };

        // This needs to crack open a manifest for a jar, so feed it a jar...
        TestUtils.setKernelUtilsBootstrapJar(TestUtils.findBuiltKernelBundle());
        TestUtils.setKernelUtilsBootstrapLibDir(Constants.TEST_DIST_DIR_FILE);
        TestUtils.setUtilsInstallDir(Constants.TEST_DATA_FILE);

        try {
            int rc = tlauncher.createPlatform(args);
            assertEquals(ReturnCode.OK.val, rc);
            assertTrue(outputMgr.checkForStandardOut("WebSphere Application Server")); // config-root
            // message
        } finally {
            TestUtils.setKernelUtilsBootstrapJar(null);
            TestUtils.setKernelUtilsBootstrapLibDir(null);
            TestUtils.setUtilsInstallDir(null);
        }
    }

    @Test
    public void testParameterHelp() {
        String[] args = new String[] { "--help" };

        int rc = tlauncher.createPlatform(args);
        assertEquals(ReturnCode.OK.val, rc);
        assertTrue("ws-server.jar should be displayed for java -jar help", outputMgr.checkForStandardOut("ws-server.jar"));

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
        assertEquals(ReturnCode.BAD_ARGUMENT.val, rc);
    }

    /**
     * When launched with --version, we still read the initial configuration
     * and environment variables: use that to verify that we *are* reading
     * the environment variables so we don't have to deal with launchPlatform.
     */
    @Test
    public void testFindLocationsEnv() {
        final String m = "testFindLocationsEnv";

        try {
            File log_dir = TestUtils.createTempDirectory("log_dir");
            File x_log_dir = TestUtils.createTempDirectory("x_log_dir");
            BootstrapConfig bootProps = new BootstrapConfig();

            // add a fake environment variable for the private/calculated temp dir X_LOG_DIR
            tlauncher.fakeEnv.put(BootstrapConstants.ENV_X_LOG_DIR, x_log_dir.getCanonicalPath());

            tlauncher.findLocations(bootProps, "defaultServer");
            assertEquals("The logDirectory value should be value of X_LOG_DIR",
                         x_log_dir.getCanonicalPath(),
                         bootProps.getLogDirectory().getCanonicalPath());

            // add a fake environment variable for the LOG_DIR. This value should be ignored because X_LOG_DIR exists.
            tlauncher.fakeEnv.put(BootstrapConstants.ENV_LOG_DIR, log_dir.getCanonicalPath());
            tlauncher.findLocations(bootProps, "defaultServer");
            assertEquals("The logDirectory value should be value of X_LOG_DIR",
                         x_log_dir.getCanonicalPath(),
                         bootProps.getLogDirectory().getCanonicalPath());

            // clear out the value of X_LOG_DIR. Now the logDirectory should use LOG_DIR
            tlauncher.fakeEnv.remove(BootstrapConstants.ENV_X_LOG_DIR);
            tlauncher.findLocations(bootProps, "defaultServer");
            assertEquals("The logDirectory value should be value of LOG_DIR",
                         log_dir.getCanonicalPath(),
                         bootProps.getLogDirectory().getCanonicalPath());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
            TestUtils.cleanTempFiles();
        }
    }

    /**
     * Simple extension of the launcher for toggling of the behavior of some
     * protected/internal methods in order to control code coverage in the
     * parent.
     */
    class TestLauncher extends Launcher {
        int exceptionType = 0;
        boolean superGetDefs = false;
        int fakeJar = 0;

        final Map<String, String> fakeEnv = new HashMap<String, String>();

        @Override
        protected String getEnv(String key) {
            return fakeEnv.get(key);
        }
    }
}
