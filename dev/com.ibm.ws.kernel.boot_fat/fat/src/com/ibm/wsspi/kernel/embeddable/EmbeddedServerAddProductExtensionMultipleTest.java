/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import junit.framework.AssertionFailedError;

@RunWith(FATRunner.class)
public class EmbeddedServerAddProductExtensionMultipleTest {

    static final Class<?> c = EmbeddedServerAddProductExtensionMultipleTest.class;

    static LibertyServer ls = null;
    static Object driver = null;

    static File wsServerBundle = null;
    static File testServerClasses = null;

    static Class<?> driverClazz = null;

    public String testName;

    static String serverName = "com.ibm.wsspi.kernel.embeddable.add.product.extension.multiple.fat";
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
        ls = LibertyServerFactory.getLibertyServer("com.ibm.wsspi.kernel.embeddable.add.product.extension.multiple.fat");
        if (ls.getMachine().getOperatingSystem().equals(OperatingSystem.WINDOWS)) {
            ls.setNeedsPostRecover(false); //avoid cleanup on Windows
        }
        testServerClasses = new File("build/classes");

        wsServerBundle = new File(ls.getInstallRoot() + "/bin/tools/ws-server.jar");

        String userDir = ls.getUserDir();
        String outputDir = userDir + "/servers";

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
                                 ls.getUserDir() + "/servers/" + serverName + " to " +
                                 outputAutoFVTDirectory.getAbsolutePath());

        File srcDir = new File(ls.getUserDir() + "/servers/" + serverName);
        copyDirectory(srcDir, outputAutoFVTDirectory.getAbsoluteFile());
    }

    @Test
    public void testAddProductExtensionMultiple() throws Throwable {}

    private static void embeddedServerTestHelper(final String REMOTE_METHOD_NAME) throws Throwable {
        final String METHOD_NAME = "embeddedServerTestHelper";
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
