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
package com.ibm.ws.kernel.feature.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Verify the feature manager reports ee compatibility conflicts.
 */
@RunWith(FATRunner.class)
public class EECompatibilityTest {
    @Rule
    public TestName name = new TestName();
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.compatibility");

    @BeforeClass
    public static void installFeatures() throws Exception {
        // Tip: Re-purpose the NewEe features when developing new EE versions
        // and the product lacks supporting features.
        server.installSystemFeature("test.compatibility.newEe-1.0");
        server.installSystemBundle("test.compatibility.newEe");
        server.installSystemFeature("test.compatibility.needsNewEe-1.0");
    }

    @AfterClass
    public static void uninstallFeatures() throws Exception {
        server.uninstallSystemFeature("test.compatibility.newEe-1.0");
        server.uninstallSystemBundle("test.compatibility.newEe");
        server.uninstallSystemFeature("test.compatibility.needsNewEe-1.0");
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    // Singleton toleration conflict regex's
    static final String CONFLICT = "CWWKF0033E.*",
                    SAME_EE_CONFLICT = "CWWKF0043E.*",
                    DIFF_EE_CONFLICT = "CWWKF0044E.*",
                    SAME_INDIRECT_MODEL_CONFLICT = "CWWKF0047E.*",
                    EE_CONFLICT = SAME_EE_CONFLICT + "|" + DIFF_EE_CONFLICT + "|" + SAME_INDIRECT_MODEL_CONFLICT,
                    ANY_CONFLICT = CONFLICT + "|" + EE_CONFLICT;

    static final String RESOLUTION_ERROR = "CWWKE0702E.*";
    static final String INSTALLED_FEATURES = "CWWKF0012I.*";
    static final String NO_FEATURES = "CWWKF0046W.*";

    private static String msg;
    private static long shortTimeOut = 5 * 1000;

    @Test
    public void testCompatibleFeaturesEE7and8() throws Exception {
        server.changeFeatures(Arrays.asList("jca-1.7", "jsf-2.3"));
        server.startServer();
        msg = server.waitForStringInLogUsingMark(ANY_CONFLICT, shortTimeOut);
        assertTrue("The feature manager should not report any conflict for the configured EE 7 and 8 features, but it did: msg=" + msg,
                   msg == null);
        server.stopServer();
    }

    @Test
    public void testCompatibleFeaturesEE9() throws Exception {
        server.changeFeatures(Arrays.asList("persistenceContainer-3.0", "servlet-5.0"));
        server.startServer();
        msg = server.waitForStringInLogUsingMark(ANY_CONFLICT, shortTimeOut);
        assertTrue("The feature manager should not report any conflict for the configured EE 9 features, but it did: msg=" + msg,
                   msg == null);
        server.stopServer();
    }

    // A "root" feature is a feature that is declared in the server configuration

    @Test
    @ExpectedFFDC("java.lang.IllegalArgumentException") // Expected when feature manager cannot load conflicting versions of features
    public void testConflictingRootFeaturesEE8and7() throws Exception {
        server.changeFeatures(Arrays.asList("servlet-4.0", "servlet-3.1"));
        server.startServer();
        msg = server.waitForStringInLogUsingMark(CONFLICT, shortTimeOut);
        assertTrue("The feature manager should report a singleton conflict for the root (i.e. configured) features, but it did not: msg=" + msg,
                   msg != null && msg.contains("servlet-4.0") && msg.contains("servlet-3.1"));
        msg = server.waitForStringInLogUsingMark(EE_CONFLICT, shortTimeOut);
        assertTrue("The feature manager should not report an EE compatibility conflict for the root features, but it did: msg=" + msg,
                   msg == null);
        msg = server.waitForStringInLogUsingMark(INSTALLED_FEATURES, shortTimeOut);
        assertTrue("The server should not install the conflicting features servlet-4.0 nor servlet-3.1 , but it did: " + msg,
                   msg != null && !msg.contains("servlet-4.0") && !msg.contains("servlet-3.1"));
        server.stopServer(ANY_CONFLICT + "|" + RESOLUTION_ERROR);
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException" })
    public void testConflictingRootFeaturesEE9and8() throws Exception {
        server.changeFeatures(Arrays.asList("servlet-5.0", "servlet-4.0"));
        server.startServer(name.getMethodName() + ".log", true, true, false);
        msg = server.waitForStringInLogUsingMark(CONFLICT, shortTimeOut);
        assertTrue("The feature manager should report a singleton conflict for the root (i.e. configured) features, but it did not: msg=" + msg,
                   msg != null && msg.contains("servlet-5.0") && msg.contains("servlet-4.0"));
        msg = server.waitForStringInLogUsingMark(EE_CONFLICT, shortTimeOut);
        assertTrue("The feature manager should not report an EE compatibility conflict for the root features, but it did: msg=" + msg,
                   msg == null);
        assertNotNull("Expected no features to be installed.", server.waitForStringInLog(NO_FEATURES, shortTimeOut));
        server.stopServer(ANY_CONFLICT + "|" + NO_FEATURES);
    }

    @Test
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testConflictingFeaturesEE8and6() throws Exception {
        server.changeFeatures(Arrays.asList("jsfContainer-2.3", "jsp-2.2")); // Conflict: -->jsp-2.3, jsp-2.2
        server.startServer();
        msg = server.waitForStringInLogUsingMark(CONFLICT, shortTimeOut);
        // Note black spaces in msg check for Java EE. Verify msg contains expected platform
        assertTrue("The feature manager should report a singleton conflict for the configured features, but it did not: msg=" + msg,
                   msg != null && msg.contains("jsfContainer-2.3") && msg.contains("jsp-2.2"));
        msg = server.waitForStringInLogUsingMark(INSTALLED_FEATURES, shortTimeOut);
        assertTrue("The server should not install conflicting features jsp-2.3 nor jsp-2.2, but it did: " + msg,
                   msg != null && !msg.contains("jsp-2.3") && !msg.contains("jsp-2.2"));
        server.stopServer(ANY_CONFLICT + "|" + RESOLUTION_ERROR);
    }

    @Test
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testConflictingEeCompatibleFeaturesEE8and7() throws Exception {
        server.changeFeatures(Arrays.asList("servlet-4.0", "jpa-2.1"));
        server.startServer();
        msg = server.waitForStringInLogUsingMark(SAME_INDIRECT_MODEL_CONFLICT, shortTimeOut);
        assertTrue("The feature manager should report an EE compatibility conflict for the configured features, but it did not: msg=" + msg,
                   msg != null && msg.contains("servlet-4.0") && msg.contains("jpa-2.1"));
        server.stopServer(ANY_CONFLICT + "|" + RESOLUTION_ERROR);
    }

    @Test
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testConflictingEeCompatibleFeaturesEE7and6() throws Exception {
        server.changeFeatures(Arrays.asList("ejbLite-3.2", "jsp-2.2"));
        server.startServer();
        msg = server.waitForStringInLogUsingMark(SAME_EE_CONFLICT, shortTimeOut);
        assertTrue("The feature manager should report an EE compatibility conflict for the configured features, but it did not: msg=" + msg,
                   msg != null && msg.contains("ejbLite-3.2") && msg.contains("jsp-2.2") && msg.contains("Java EE 7") && msg.contains("Java EE 6"));
        server.stopServer(ANY_CONFLICT + "|" + RESOLUTION_ERROR);
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException" })
    public void testConflictingEeCompatibleFeaturesEE9andJakartaEE8() throws Exception {
        // This configuration requires you add many, many features to the
        // tested.features property in the bnd.bnd file
        server.changeFeatures(Arrays.asList("servlet-5.0", "jakartaee-8.0"));
        server.startServer(name.getMethodName() + ".log", true, true, false);
        msg = server.waitForStringInLogUsingMark(DIFF_EE_CONFLICT, shortTimeOut);
        // Note that feature jakartaee-8.0 is actually ee8 compatible, so we expect
        // some Java EE feature to conflict.
        assertTrue("The feature manager should report an EE compatibility conflict for the configured servlet-5.0(EE9) and a dependent feature of jakartaee-8.0(EE8), but it did not: msg="
                   + msg, msg != null && msg.contains("servlet-5.0") && msg.contains("Jakarta EE 9") && msg.contains("Java EE"));
        assertNotNull("Expected no features to be installed.", server.waitForStringInLog(NO_FEATURES, shortTimeOut));
        server.stopServer(ANY_CONFLICT + "|" + NO_FEATURES);
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException" })
    public void testConflictingHelperFeaturesJakartaEE9and8() throws Exception {
        server.changeFeatures(Arrays.asList("jakartaee-9.1", "jakartaee-8.0"));
        server.startServer(name.getMethodName() + ".log", true, true, false);
        // The server reports a singleton conflict and installs neither helper feature!
        msg = server.waitForStringInLogUsingMark(DIFF_EE_CONFLICT, shortTimeOut);
        assertTrue("The feature manager should report a singleton conflict for the incompatible jakartaee helper features, but it did not: msg="
                   + msg, msg != null && msg.contains("jakartaee-9.1") && msg.contains("Jakarta EE 9") && msg.contains("Java EE"));
        assertNotNull("Expected no features to be installed.", server.waitForStringInLog(NO_FEATURES, shortTimeOut));
        server.stopServer(ANY_CONFLICT + "|" + NO_FEATURES);
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException" })
    public void testConflictingEeCompatibleFeaturesEE9and8() throws Exception {
        // These configuration causes one conflict, only: the ee conflict
        server.changeFeatures(Arrays.asList("persistenceContainer-3.0", "jsf-2.3")); // Conflict: -->ee9, -->ee8
        server.startServer(name.getMethodName() + ".log", true, true, false);
        msg = server.waitForStringInLogUsingMark(DIFF_EE_CONFLICT, shortTimeOut);
        assertTrue("The feature manager should report an EE compatibility conflict for the configured persistenceContainer-3.0 (jakarta) and jsf-2.3 (javaee) features, but it did not: msg="
                   + msg, msg != null && msg.contains("persistenceContainer-3.0") && msg.contains("jsf-2.3"));
        assertNotNull("Expected no features to be installed.", server.waitForStringInLog(NO_FEATURES, shortTimeOut));
        server.stopServer(ANY_CONFLICT + "|" + NO_FEATURES);
    }

    // Re-purpose this test and NewEe features for early
    // development on new EE versions
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException" })
    public void testConflictingEeCompatibleFeaturesNewEEand8() throws Exception {
        // This configuration causes more than one conflict, including the ee conflict.
        server.changeFeatures(Arrays.asList("needsNewEe-1.0", "jsf-2.3")); // Conflict: -->newEe-1.0-->ee9, jsf-2.3-->ee8
        server.startServer(name.getMethodName() + ".log", true, true, false);
        msg = server.waitForStringInLogUsingMark(DIFF_EE_CONFLICT, shortTimeOut);
        assertTrue("The feature manager should report an EE compatibility conflict for the configured jakarta and javaee features, but it did not: msg=" + msg,
                   msg != null && msg.contains("newEe-1.0") && msg.contains("jsf-2.3"));
        assertNotNull("Expected no features to be installed.", server.waitForStringInLog(NO_FEATURES, shortTimeOut));
        server.stopServer(ANY_CONFLICT + "|" + NO_FEATURES);
    }
}