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
import java.util.Collection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
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
 * Verify the feature manager reports compatible feature conflicts.
 */
@RunWith(FATRunner.class)
public class CompatibleFeatureTest {
    private static final Collection<String> TEST_FEATURES = Arrays.asList("test.compatible.modelA.eeCompatible-1.0",
                                                                          "test.compatible.modelA.eeCompatible-2.0",
                                                                          "test.compatible.modelA.eeCompatible-3.0",
                                                                          "test.compatible.modelA1.featureA-1.0",
                                                                          "test.compatible.modelA1.featureB-1.0",
                                                                          "test.compatible.modelA1.featureC-1.0",
                                                                          "test.compatible.modelA2.featureA-2.0",
                                                                          "test.compatible.modelA2.featureB-2.0",
                                                                          "test.compatible.modelA2.featureC-2.0",
                                                                          "test.compatible.modelA2.featureC-3.0",
                                                                          "test.compatible.modelB.eeCompatible-1.0",
                                                                          "test.compatible.modelB.eeCompatible-2.0",
                                                                          "test.compatible.modelB1.featureA-1.0",
                                                                          "test.compatible.modelB1.featureB-1.0",
                                                                          "test.compatible.modelB1.featureC-1.0",
                                                                          "test.compatible.modelB1.featureD-1.0",
                                                                          "test.compatible.modelB1.featureE-1.0",
                                                                          "test.compatible.modelB2.featureA-2.0",
                                                                          "test.compatible.modelB2.featureB-2.0",
                                                                          "test.compatible.modelB2.featureC-2.0");
    @Rule
    public TestName name = new TestName();
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.compatibility");
    private static String[] expectedStopErrors = new String[0];

    @BeforeClass
    public static void installFeatures() throws Exception {
        TEST_FEATURES.forEach((f) -> {
            try {
                server.installSystemFeature(f);
            } catch (Exception e) {
                sneakyThrow(e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    @AfterClass
    public static void uninstallFeatures() throws Exception {
        TEST_FEATURES.forEach((f) -> {
            try {
                server.uninstallSystemFeature(f);
            } catch (Exception e) {
                sneakyThrow(e);
            }
        });
    }

    @Before
    public void clearExpectedStopErrors() {
        expectedStopErrors = new String[0];
    }

    @After
    public void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer(expectedStopErrors);
        }
    }

    // Singleton toleration conflict regex's
    static final String CONFLICT = "CWWKF0033E.*",
                    SAME_MODEL_CONFLICT = "CWWKF0043E.*",
                    DIFF_MODEL_CONFLICT = "CWWKF0044E.*",
                    SAME_INDIRECT_MODEL_CONFLICT = "CWWKF0047E.*",
                    EE_CONFLICT = SAME_MODEL_CONFLICT + "|" + DIFF_MODEL_CONFLICT + "|" + SAME_INDIRECT_MODEL_CONFLICT,
                    ANY_CONFLICT = CONFLICT + "|" + EE_CONFLICT;

    static final String INSTALLED_FEATURES = "CWWKF0012I.*";
    static final String NO_FEATURES = "CWWKF0046W.*";

    private final static long shortTimeOut = 5 * 1000;

    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException" })
    public void testMultiLevelCompatibleSameFamilyModel() throws Exception {
        expectedStopErrors = new String[] { SAME_MODEL_CONFLICT, NO_FEATURES };
        server.changeFeatures(Arrays.asList("modelA.FeatureA-1.0", "modelA.FeatureB-2.0"));
        server.startServer(name.getMethodName() + ".log", true, true, false);
        String msg = server.waitForStringInLogUsingMark(SAME_MODEL_CONFLICT, shortTimeOut);
        assertNotNull("Expected conflict message", msg);
        String expectedMessagePattern1 = ".*CWWKF0043E:.*modelA.FeatureA-1.0.*Model A 1.*modelA.FeatureB-2.0.*Model A 2.*Model A.*Model A 1.*Model A 2.*";
        String expectedMessagePattern2 = ".*CWWKF0043E:.*modelA.FeatureB-2.0.*Model A 2.*modelA.FeatureA-1.0.*Model A 1.*Model A.*Model A 2.*Model A 1.*";

        assertTrue("Unexpected massage text: " + msg, msg.matches(expectedMessagePattern1) || msg.matches(expectedMessagePattern2));
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException" })
    public void testMultiLevelCompatibleSameFamilyDifferentModel() throws Exception {
        expectedStopErrors = new String[] { DIFF_MODEL_CONFLICT, NO_FEATURES };
        server.changeFeatures(Arrays.asList("modelA.FeatureA-1.0", "modelA.FeatureC-3.0"));
        server.startServer(name.getMethodName() + ".log", true, true, false);
        String msg = server.waitForStringInLogUsingMark(DIFF_MODEL_CONFLICT, shortTimeOut);
        assertNotNull("Expected conflict message", msg);
        String expectedMessagePattern1 = ".*CWWKF0044E:.*modelA.FeatureA-1.0.*modelA.FeatureC-3.0.*modelA.FeatureA-1.0.*Model A 1.*modelA.FeatureC-3.0.*Model APrime 3.*modelA.FeatureA-1.0.*modelA.FeatureC-3.0.*Model A.*Model APrime.*";
        String expectedMessagePattern2 = ".*CWWKF0044E:.*modelA.FeatureC-3.0.*modelA.FeatureA-1.0.*modelA.FeatureC-3.0.*Model APrime 3.*modelA.FeatureA-1.0.*Model A 1.*modelA.FeatureC-3.0.*modelA.FeatureA-1.0.*Model APrime.*Model A.*";
        assertTrue("Unexpected massage text: " + msg, msg.matches(expectedMessagePattern1) || msg.matches(expectedMessagePattern2));
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException" })
    public void testMultiFamilyModelConflicts() throws Exception {
        expectedStopErrors = new String[] { SAME_MODEL_CONFLICT, NO_FEATURES };
        server.changeFeatures(Arrays.asList("modelA.FeatureA-1.0", "modelA.FeatureB-2.0", "modelB.FeatureA-1.0", "modelB.FeatureB-2.0"));
        server.startServer(name.getMethodName() + ".log", true, true, false);
        // we expect two conflicts on two different families of programming models
        String msg1 = server.waitForStringInLogUsingLastOffset(SAME_MODEL_CONFLICT, shortTimeOut);
        assertNotNull("Expected conflict message", msg1);
        String msg2 = server.waitForStringInLogUsingLastOffset(SAME_MODEL_CONFLICT, shortTimeOut);
        assertNotNull("Expected conflict message", msg2);

        String expectedMessagePattern1a = ".*CWWKF0043E:.*modelA.FeatureA-1.0.*Model A 1.*modelA.FeatureB-2.0.*Model A 2.*Model A.*Model A 1.*Model A 2.*";
        String expectedMessagePattern1b = ".*CWWKF0043E:.*modelA.FeatureB-2.0.*Model A 2.*modelA.FeatureA-1.0.*Model A 1.*Model A.*Model A 2.*Model A 1.*";
        String expectedMessagePattern2a = ".*CWWKF0043E:.*modelB.FeatureA-1.0.*Model B 1.*modelB.FeatureB-2.0.*Model B 2.*Model B.*Model B 1.*Model B 2.*";
        String expectedMessagePattern2b = ".*CWWKF0043E:.*modelB.FeatureB-2.0.*Model B 2.*modelB.FeatureA-1.0.*Model B 1.*Model B.*Model B 2.*Model B 1.*";

        if (msg1.matches(expectedMessagePattern1a) || (msg1.matches(expectedMessagePattern1b))) {
            assertTrue("Unexpected massage text: " + msg2, msg2.matches(expectedMessagePattern2a) || msg2.matches(expectedMessagePattern2b));
        } else {
            assertTrue("Unexpected massage text: " + msg1, msg1.matches(expectedMessagePattern2a) || msg1.matches(expectedMessagePattern2b));
            assertTrue("Unexpected massage text: " + msg2, msg2.matches(expectedMessagePattern1a) || msg2.matches(expectedMessagePattern1b));
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException" })
    public void testTwoDirectModelsWithSingleFeatureConflictsBoth() throws Exception {
        expectedStopErrors = new String[] { SAME_MODEL_CONFLICT, NO_FEATURES };
        server.changeFeatures(Arrays.asList("modelA.FeatureB-2.0", "modelB.FeatureD-1.0", "modelB.FeatureA-2.0"));
        server.startServer(name.getMethodName() + ".log", true, true, false);

        // we expect two conflicts on two different families of programming models
        String msg1 = server.waitForStringInLogUsingLastOffset(SAME_MODEL_CONFLICT, shortTimeOut);
        assertNotNull("Expected conflict message", msg1);
        String msg2 = server.waitForStringInLogUsingLastOffset(SAME_MODEL_CONFLICT, shortTimeOut);
        assertNotNull("Expected conflict message", msg2);

        String expectedMessagePattern1a = ".*CWWKF0043E:.*modelB.FeatureD-1.0.*Model A 1.*modelA.FeatureB-2.0.*Model A 2.*Model A.*Model A 1.*Model A 2.*";
        String expectedMessagePattern1b = ".*CWWKF0043E:.*modelA.FeatureB-2.0.*Model A 2.*modelB.FeatureD-1.0.*Model A 1.*Model A.*Model A 2.*Model A 1.*";
        String expectedMessagePattern2a = ".*CWWKF0043E:.*modelB.FeatureA-2.0.*Model B 2.*modelB.FeatureD-1.0.*Model B 1.*Model B.*Model B 2.*Model B 1.*";
        String expectedMessagePattern2b = ".*CWWKF0043E:.*modelB.FeatureD-1.0.*Model B 1.*modelB.FeatureA-2.0.*Model B 2.*Model B.*Model B 1.*Model B 2.*";

        if (msg1.matches(expectedMessagePattern1a) || (msg1.matches(expectedMessagePattern1b))) {
            assertTrue("Unexpected massage text: " + msg2, msg2.matches(expectedMessagePattern2a) || msg2.matches(expectedMessagePattern2b));
        } else {
            assertTrue("Unexpected massage text: " + msg1, msg1.matches(expectedMessagePattern2a) || msg1.matches(expectedMessagePattern2b));
            assertTrue("Unexpected massage text: " + msg2, msg2.matches(expectedMessagePattern1a) || msg2.matches(expectedMessagePattern1b));
        }
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException" })
    public void testIndirectModelsFeatureConflict() throws Exception {
        expectedStopErrors = new String[] { SAME_INDIRECT_MODEL_CONFLICT, NO_FEATURES };
        server.changeFeatures(Arrays.asList("modelB.FeatureE-1.0", "modelA.FeatureC-2.0"));
        server.startServer(name.getMethodName() + ".log", true, true, false);

        // we expect two conflicts on two different families of programming models
        String msg = server.waitForStringInLogUsingLastOffset(SAME_INDIRECT_MODEL_CONFLICT, shortTimeOut);
        assertNotNull("Expected conflict message", msg);
        String expectedMessagePattern1 = ".*CWWKF0047E:.*modelA.FeatureA-1.0.*modelA.FeatureC-2.0.*modelA.FeatureA-1.0.*Model A 1.*modelA.FeatureC-2.0.*Model A 2.*Model A.*modelB.FeatureE-1.0.*modelA.FeatureC-2.0.*Model A 1.*Model A 2.*.";
        String expectedMessagePattern2 = ".*CWWKF0047E:.*modelA.FeatureC-w.0.*modelA.FeatureA-1.0.*modelA.FeatureC-2.0.*Model A 2.*modelA.FeatureA-1.0.*Model A 1.*Model A.*modelB.FeatureC-2.0.*modelA.FeatureE-1.0.*Model A 2.*Model A 1.*.";

        assertTrue("Unexpected massage text: " + msg, msg.matches(expectedMessagePattern1) || msg.matches(expectedMessagePattern2));
    }
}