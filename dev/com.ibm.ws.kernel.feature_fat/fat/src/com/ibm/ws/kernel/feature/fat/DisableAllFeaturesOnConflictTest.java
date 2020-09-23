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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class DisableAllFeaturesOnConflictTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.disable.onconflict");
    private static final Collection<String> TEST_FEATURES = Arrays.asList("test.disable.all.features.on.conflict.false.a",
                                                                          "test.disable.all.features.on.conflict.false.b",
                                                                          "test.disable.all.features.on.conflict.false.c",
                                                                          "test.disable.all.features.on.conflict.false.d",
                                                                          "test.disable.all.features.on.conflict.false.e",
                                                                          "test.disable.all.features.on.conflict.falseConflict-1.0",
                                                                          "test.disable.all.features.on.conflict.falseConflict-2.0",
                                                                          "test.disable.all.features.on.conflict.falseConflict-3.0",
                                                                          "test.disable.all.features.on.conflict.true.a",
                                                                          "test.disable.all.features.on.conflict.true.b",
                                                                          "test.disable.all.features.on.conflict.true.c",
                                                                          "test.disable.all.features.on.conflict.true.d",
                                                                          "test.disable.all.features.on.conflict.trueConflict-1.0",
                                                                          "test.disable.all.features.on.conflict.trueConflict-2.0");
    static final String INSTALLED_FEATURES = "CWWKF0012I.*";
    static final String REMOVED_FEATURES = "CWWKF0013I.*";
    static final String CONFLICT = "CWWKF0033E.*";
    static final String NO_FEATURES = "CWWKF0046W.*";

    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "com.ibm.ws.timedexit.internal.RequiredTimedExitFeatureStoppedException" })
    public void testDisableAllFeaturesOnConflict() throws Exception {

        // NOTE that we use smallest timeout because we first wait for config update
        // message.  This is the last message and all messages we expect will happen
        // before the config update message.
        server.startServer();
        server.setMarkToEndOfLog();

        verifyFeaturesEnabled(Arrays.asList("falseA-1.0", "falseD-1.0"), Arrays.asList("falseA-1.0", "falseB-1.0", "falseC-1.0", "falseD-1.0"));

        verifyFeaturesDisabled(Arrays.asList("falseA-1.0", "falseD-1.0", "trueConflict-1.0"), Arrays.asList("falseA-1.0", "falseB-1.0", "falseC-1.0", "falseD-1.0"));

        verifyFeaturesEnabled(Arrays.asList("falseA-1.0", "falseD-1.0"), Arrays.asList("falseA-1.0", "falseB-1.0", "falseC-1.0", "falseD-1.0"));

        verifyFeaturesDisabled(Arrays.asList("falseA-1.0", "falseD-1.0", "falseE-1.0"), Arrays.asList("falseA-1.0", "falseB-1.0", "falseC-1.0", "falseD-1.0"));

        verifyFeaturesEnabled(Arrays.asList("trueA-1.0"), Arrays.asList("trueA-1.0", "trueB-1.0", "trueC-1.0", "trueConflict-1.0"));

        verifyFeaturesDisabled(Arrays.asList("trueA-1.0", "trueD-1.0"), Arrays.asList("trueA-1.0", "trueB-1.0", "trueC-1.0", "trueConflict-1.0"));

    }

    private void verifyFeaturesEnabled(List<String> rootFeatures, List<String> expectedInstalledFeatures) throws Exception {
        server.changeFeatures(rootFeatures);
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        assertNull("Expected some features to be installed.", server.waitForStringInLog(NO_FEATURES, 1));
        String installedFeatures = server.waitForStringInLogUsingMark(INSTALLED_FEATURES, 1);
        expectedInstalledFeatures.forEach((f) -> {
            assertTrue("Server should have installed the feature: " + f, installedFeatures.contains(f));
        });
        server.setMarkToEndOfLog();
    }

    private void verifyFeaturesDisabled(List<String> rootFeatures, List<String> expectedRemovedFeatures) throws Exception {
        server.changeFeatures(rootFeatures);
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        assertNotNull("Expected no features to be installed.", server.waitForStringInLog(NO_FEATURES, 1));
        String removedFeatures = server.waitForStringInLogUsingMark(REMOVED_FEATURES, 1);
        expectedRemovedFeatures.forEach((f) -> {
            assertTrue("Server should have removed the feature: " + f, removedFeatures.contains(f));
        });
        String installedFeatures = server.waitForStringInLogUsingMark(INSTALLED_FEATURES, 1);
        assertNull("Should be no features installed.", installedFeatures);
        server.setMarkToEndOfLog();
    }

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

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(CONFLICT, NO_FEATURES);
        }
    }

}