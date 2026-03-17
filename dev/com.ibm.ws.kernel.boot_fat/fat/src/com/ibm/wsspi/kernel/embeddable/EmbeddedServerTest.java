/*******************************************************************************
 * Copyright (c) 2013, 2026 IBM Corporation and others.
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
package com.ibm.wsspi.kernel.embeddable;

import static componenttest.topology.utils.FileUtils.copyDirectory;
import static componenttest.topology.utils.FileUtils.copyFile;
import static componenttest.topology.utils.FileUtils.recursiveDelete;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import junit.framework.AssertionFailedError;

@RunWith(FATRunner.class)
public class EmbeddedServerTest {

    static final Class<?> c = EmbeddedServerTest.class;
    static final String SIMPLE_APP_WAR_NAME = "simpleApp";
    static WebArchive simpleAppWar;

    static LibertyServer ls = null;
    static Object driver = null;

    static File wsServerBundle = null;
    static File testServerClasses = null;

    static Class<?> driverClazz = null;

    public String testName;

    static String serverName = "com.ibm.wsspi.kernel.embeddable.fat";
    public static File outputAutoFVTDirectory;

    @Rule
    public TestRule testInvoker = new TestRule() {
        @Override
        public Statement apply(final Statement stmt, final Description desc) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    try {
                        testName = desc.getMethodName();
                        embeddedServerTestHelper(testName, ls.getHostname(), ls.getHttpDefaultPort());
                        stmt.evaluate();
                    } finally {
                        testName = null;
                    }
                }
            };
        }
    };

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final String METHOD_NAME = "setUpBeforeClass";

        // Save this off for the tearDown method to manually copy logs from /NonDefaultUser
        // folder to /autoFVT/output/servers/ folder.
        outputAutoFVTDirectory = new File("output/servers", serverName);
        Log.info(c, METHOD_NAME, "outputAutoFVTDirectory: " + outputAutoFVTDirectory.getAbsolutePath());

        // Find the necessary bundles in our install image
        ls = LibertyServerFactory.getLibertyServer("com.ibm.wsspi.kernel.embeddable.fat");
        if (ls.getMachine().getOperatingSystem().equals(OperatingSystem.WINDOWS)) {
            ls.setNeedsPostRecover(false); //avoid cleanup on Windows
        }

        testServerClasses = new File("build/classes");

        wsServerBundle = new File(ls.getInstallRoot() + "/bin/tools/ws-server.jar");

        // PI20344: Use non default directories.
        String userDir = ls.getUserDir() + "/../NonDefaultUser"; // originally ls.getUserDir()
        String outputDir = userDir + "/servers"; // originally ls.getServerRoot();

        // PI20344: Additional setup needed when using non default directories.
        // Copy the server directory to my output directory
        // Also need the test.properties file
        File destDir = new File(outputDir, serverName);
        File srcDir = new File(ls.getServerRoot());
        destDir.mkdirs();
        copyDirectory(srcDir, destDir);

        copyConfigFile("/../testports.properties", srcDir, destDir);
        copyConfigFile("/../fatTestPorts.xml", srcDir, destDir);
        copyConfigFile("/../fatTestCommon.xml", srcDir, destDir);
        // END PI20344

        Log.info(c, METHOD_NAME, "wsServerBundle: " + wsServerBundle.getAbsolutePath());
        Log.info(c, METHOD_NAME, "testServerClasses: " + testServerClasses.getAbsolutePath());

        URLClassLoader classloader = new URLClassLoader(new URL[] { wsServerBundle.toURI().toURL(),
                                                                    testServerClasses.toURI().toURL() }) {
            // Borrowed from the bvt tests
            @Override
            protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                Class<?> result = null;

                if (name == null || name.length() == 0)
                    return null;

                result = findLoadedClass(name);

                if (result == null && name != null) {
                    try {
                        // Try to load the class from the child classpath first...
                        result = findClass(name);
                    } catch (ClassNotFoundException cnfe) {
                        result = super.loadClass(name, resolve);
                    }
                }

                return result;
            }
        };

        driverClazz = classloader.loadClass("com.ibm.wsspi.kernel.embeddable.EmbeddedServerDriver");
        Constructor<?> dCTOR = driverClazz.getConstructor(String.class, String.class, String.class);
        driver = dCTOR.newInstance(serverName, userDir, outputDir);

        simpleAppWar = ShrinkHelper.buildDefaultApp(SIMPLE_APP_WAR_NAME + ".war",
                                                    com.ibm.ws.kernel.testapp.bootstrap.access.BootStrapAccess.class.getPackage().getName());
        ShrinkHelper.exportArtifact(simpleAppWar, destDir.getAbsolutePath() + "/dropins", false, true);
    }

    private static void copyConfigFile(String name, File srcDir, File destDir) throws IOException {
        File dest_config = new File(destDir, name);
        File src_config = new File(srcDir, name);
        copyFile(src_config, dest_config);
    }

    @AfterClass
    public static void tearDown() throws Throwable {
        final String METHOD_NAME = "tearDown";

        // Manually copying server logs since they are in a non-default location
        // and build script copy does not pick them up.
        outputAutoFVTDirectory.mkdirs();
        Log.info(c, METHOD_NAME, "Copying directory from " +
                                 ls.getUserDir() + "/../NonDefaultUser" + " to " +
                                 outputAutoFVTDirectory.getAbsolutePath());

        File srcDir = new File(ls.getUserDir() + "/../NonDefaultUser");
        copyDirectory(srcDir, outputAutoFVTDirectory.getAbsoluteFile());
        recursiveDelete(srcDir);
    }

    @Test
    public void testStoppingAStoppedServer() throws Throwable {
    }

    @Test
    public void testStartingAStoppedServer() throws Throwable {
    }

    @Test
    public void testStartingAStartedServer() throws Throwable {
    }

    @Test
    public void testStoppingAStartedServer() throws Throwable {
    }

    @Test
    public void testForceStoppingAStartedServer() throws Throwable {
    }

    @Test
    public void testBadArgument() throws Throwable {
    }

    @Test
    public void testLaunchException() throws Throwable {
    }

    @Test
    public void testServerDoesNotExist() throws Throwable {
    }

    @Test
    public void testBootstrapAccessPlatform() throws Throwable {
    }

    @Test
    public void testBootstrapAccessDefault() throws Throwable {
    }

    @Test
    @MinimumJavaLevel(javaLevel = 11)
    // cannot support configuring parent packages with Java 8
    public void testBootstrapAccessSystemNoPackages() throws Throwable {
    }

    @Test
    @MinimumJavaLevel(javaLevel = 11)
    // cannot support configuring parent packages with Java 8
    public void testBootstrapAccessSystemMultiPackage() throws Throwable {
    }

    private static void embeddedServerTestHelper(final String REMOTE_METHOD_NAME, Object... args) throws Throwable {
        final String METHOD_NAME = "embeddedServerTestHelper";
        Log.info(c, METHOD_NAME, "Preparing to run: " + REMOTE_METHOD_NAME);

        Method testMethod = driverClazz.getDeclaredMethod(REMOTE_METHOD_NAME);

        Method initMethod = driverClazz.getDeclaredMethod("init", new Class[] { String.class, Object[].class });

        Method tearDownMethod = driverClazz.getDeclaredMethod("tearDown");

        Method getFailuresMethod = driverClazz.getDeclaredMethod("getFailures");

        try {
            initMethod.invoke(driver, new Object[] { REMOTE_METHOD_NAME, args });
            testMethod.invoke(driver);
            tearDownMethod.invoke(driver);

            List<AssertionFailedError> failures = (List<AssertionFailedError>) getFailuresMethod.invoke(driver);
            for (AssertionFailedError fail : failures)
                Log.info(driverClazz, REMOTE_METHOD_NAME, fail.getMessage());

            if (!failures.isEmpty())
                //add string buffer here to show failures in detail
                assertTrue("Failures found " + failures, false);
        } catch (InvocationTargetException t) {
            throw t.getCause();
        }
    }

}
