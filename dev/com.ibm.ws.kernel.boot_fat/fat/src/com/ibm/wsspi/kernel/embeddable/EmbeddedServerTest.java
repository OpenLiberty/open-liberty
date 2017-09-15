/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.embeddable;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import junit.framework.AssertionFailedError;

public class EmbeddedServerTest {

    static final Class<?> c = EmbeddedServerTest.class;

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
                        embeddedServerTestHelper(testName);
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

        File dest_TestPorts = new File(destDir, "/../testports.properties");
        File src_TestPorts = new File(srcDir, "/../testports.properties");
        copyFile(src_TestPorts, dest_TestPorts);
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
    }

    @Test
    public void testStoppingAStoppedServer() throws Throwable {}

    @Test
    public void testStartingAStoppedServer() throws Throwable {}

    @Test
    public void testStartingAStartedServer() throws Throwable {}

    @Test
    public void testStoppingAStartedServer() throws Throwable {}

    @Test
    public void testBadArgument() throws Throwable {}

    @Test
    public void testLaunchException() throws Throwable {}

    //@Test
    //public void testLocationException() throws Throwable {}

    private static void embeddedServerTestHelper(final String REMOTE_METHOD_NAME) throws Throwable {
        final String METHOD_NAME = "testEmbeddedServer";
        Log.info(c, METHOD_NAME, "Preparing to run: " + REMOTE_METHOD_NAME);

        Method testMethod = driverClazz.getDeclaredMethod(REMOTE_METHOD_NAME);

        Method initMethod = driverClazz.getDeclaredMethod("init", new Class[] { String.class });

        Method tearDownMethod = driverClazz.getDeclaredMethod("tearDown");

        Method getFailuresMethod = driverClazz.getDeclaredMethod("getFailures");

        try {

            initMethod.invoke(driver, new Object[] { REMOTE_METHOD_NAME });
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

    private static void copyFile(File fromFile, File toFile) throws IOException {
        // Open the source file
        FileInputStream fis = new FileInputStream(fromFile);
        try {
            // Open the destination file
            File destDir = toFile.getParentFile();
            if (!destDir.exists() && !destDir.mkdirs()) {
                throw new IOException("Failed to create path: " + destDir.getAbsolutePath());
            }

            System.out.println("Copying file from: " + fromFile.getAbsolutePath());
            System.out.println("Copying file to:   " + toFile.getAbsolutePath());

            FileOutputStream fos = new FileOutputStream(toFile);

            // Perform the transfer using nio channels; this is simpler, and usually
            // faster, than copying the file a chunk at a time
            try {
                FileChannel inChan = fis.getChannel();
                FileChannel outChan = fos.getChannel();
                inChan.transferTo(0, inChan.size(), outChan);
            } finally {
                fos.close();
            }
        } finally {
            fis.close();
        }
    }

    public static void copyDirectory(File source, File target) throws IOException {
        if (source.isDirectory()) {
            if (!target.exists()) {
                target.mkdir();
            }

            String[] children = source.list();
            for (int i = 0; i < children.length; i++) {
                copyDirectory(new File(source, children[i]),
                              new File(target, children[i]));
            }
        } else {
            InputStream in = new FileInputStream(source);
            OutputStream out = new FileOutputStream(target);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }
}
