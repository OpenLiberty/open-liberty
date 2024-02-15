/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.tests;

import static componenttest.annotation.SkipForRepeat.EE10_OR_LATER_FEATURES;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests to execute on the jsfFeatureConflictServer.
 *
 * Part of these tests is to ensure that both the jsf-2.3 and jsf-2.2 features are singleton features
 * and only one can be loaded at a time. If both features are specified in a server.xml
 * the server should log an error.
 *
 * In addition these tests verify that the jsf-2.2 and jsf-2.3 features can not be loaded at the
 * same time as the faces-3.0 feature.
 *
 * Currently the JakartaEE9Action removes all EE6/7/8 features and replaces them with EE9 features. So
 * in this case :
 *
 * <feature>jsf-2.3</feature>
 * <feature>jsf-2.2</feature
 *
 * Becomes:
 *
 * <feature>faces-3.0</feature>
 *
 * So we need to use a new server and dynamically add the feature to test for conflicts.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
@SkipForRepeat(EE10_OR_LATER_FEATURES)
public class JSFFeatureConflictTests {

    protected static final Class<?> c = JSFFeatureConflictTests.class;
    private static final Logger LOG = Logger.getLogger(c.getName());

    @Rule
    public TestName name = new TestName();

    @Server("jsfFeatureConflictServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Start the server and use the class name so we can find logs easily.
        // Don't validate the timedexit-1.0 feature to avoid an unexpected FFDC when testing the faces-3.0 feature conflicts since no features are installed when a conflict is found:
        //                                   junit.framework.AssertionFailedError: [Unexpected FFDC reporting com.ibm.ws.timedexit.internal.RequiredTimedExitFeatureStoppedException was found
        //                                        >Exception = com.ibm.ws.timedexit.internal.RequiredTimedExitFeatureStoppedException
        //                                        >Source = com.ibm.ws.timedexit.internal.TimedExitComponent
        //                                        >Stack Dump = com.ibm.ws.timedexit.internal.RequiredTimedExitFeatureStoppedException: The timedexit-1.0 feature is being stopped before the server is stopping.  It must be enabled during ALL FAT bucket runs; make sure that fatTestPorts.xml is included in the server.xml.
        //                                        >       at com.ibm.ws.timedexit.internal.TimedExitComponent.stop(TimedExitComponent.java:59)
        server.startServer(c.getSimpleName() + ".log", true, true, false);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * CWWKF0033E: The singleton features jsf-2.2 and jsf-2.3 cannot be loaded at the same time.
     * The configured features jsf-2.2 and jsf-2.3 include one or more features that cause
     * the conflict.
     *
     * The above error must be seen if both the jsf-2.2 and the jsf-2.3 features are enabled at the
     * same time. This test will ensure the error exists in this error case.
     *
     * @throws Exception
     */
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES)
    public void testEnsureJSF23FeatureAndJSF22FeatureAreSingletonFeatures() throws Exception {
        // Make sure the test framework knows that CWWKF0033E is expected
        server.addIgnoredErrors(Arrays.asList("CWWKF0033E:.*"));

        String msgToSearchFor = "CWWKF0033E: The singleton features jsf-2.2 and jsf-2.3 cannot be loaded at the same time.  " +
                                "The configured features jsf-2.2 and jsf-2.3 include one or more features that cause the conflict.";

        // Search for the message in the logs to ensure that the correct exception was logged
        String areBothJSFFeaturesSingletons = server.waitForStringInLog(msgToSearchFor);

        assertNotNull("The following message was not found in the log: " + msgToSearchFor, areBothJSFFeaturesSingletons);
    }

    /**
     * Test to ensure the faces-3.0 and jsf-2.3 features can not be loaded at the same time.
     * When this test runs during the EE9 repeat, the server.xml that this test class uses
     * will have both the jsf-2.2 and the jsf-2.3 features replaced with a single faces-3.0 feature.
     *
     * This test will programmatically add the jsf-2.2 feature and test to ensure the proper
     * feature conflict message is output in the logs.
     *
     * FFDC1015I: An FFDC Incident has been created: "java.lang.IllegalArgumentException: Unable to load conflicting versions of features
     *
     * FFDC1015I: An FFDC Incident has been created: "com.ibm.ws.timedexit.internal.RequiredTimedExitFeatureStoppedException:
     * The timedexit-1.0 feature is being stopped before the server is stopping. It must be enabled during ALL FAT bucket runs;
     * make sure that fatTestPorts.xml is included in the server.xml.
     *
     * @throws Exception
     */
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "com.ibm.ws.timedexit.internal.RequiredTimedExitFeatureStoppedException" })
    public void testEnsureJSF22FeatureAndFaces30FeatureConflict() throws Exception {
        // Make sure the test framework knows that CWWKF0044E is expected
        // CWWKF0046W: The configuration includes an incompatible combination of features. As a result, the feature manager did not install any features.
        server.addIgnoredErrors(Arrays.asList("CWWKF0044E:.*", "CWWKF0046W:.*"));

        server.saveServerConfiguration();
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);
        configuration.getFeatureManager().getFeatures().add("jsf-2.2");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.emptySet(), true);

        try {
            // CWWKF0044E: The features jsf-2.2 and faces-3.0 cannot be loaded at the same time.
            // The feature jsf-2.2 of Java EE 7 is incompatible with the feature faces-3.0 of
            // Jakarta EE 9.  The configured features jsf-2.2 and faces-3.0 include an incompatible
            // combination of features.  Your configuration is not supported.  Update the
            // configuration to use features that support either the Jakarta EE or Java EE programming models,
            // but not both.
            assertNotNull(server.waitForStringInLogUsingMark("CWWKF0044E"));
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.emptySet(), true);
        }
    }

    /**
     * Test to ensure the faces-3.0 and jsf-2.3 features can not be loaded at the same time.
     * When this test runs during the EE9 repeat, the server.xml that this test class uses
     * will have both the jsf-2.2 and the jsf-2.3 features replaced with a single faces-3.0 feature.
     *
     * This test will programmatically add the jsf-2.3 feature and test to ensure the proper
     * feature conflict message is output in the logs.
     *
     * FFDC1015I: An FFDC Incident has been created: "java.lang.IllegalArgumentException: Unable to load conflicting versions of features
     *
     * FFDC1015I: An FFDC Incident has been created: "com.ibm.ws.timedexit.internal.RequiredTimedExitFeatureStoppedException:
     * The timedexit-1.0 feature is being stopped before the server is stopping. It must be enabled during ALL FAT bucket runs;
     * make sure that fatTestPorts.xml is included in the server.xml.
     *
     * @throws Exception
     */
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "com.ibm.ws.timedexit.internal.RequiredTimedExitFeatureStoppedException" })
    public void testEnsureJSF23FeatureAndFaces30FeatureConflict() throws Exception {
        // CWWKF0046W: The configuration includes an incompatible combination of features. As a result, the feature manager did not install any features.
        server.addIgnoredErrors(Arrays.asList("CWWKF0044E:.*", "CWWKF0046W:.*"));

        server.saveServerConfiguration();
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);
        configuration.getFeatureManager().getFeatures().add("jsf-2.3");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.emptySet(), true);

        try {
            // CWWKF0044E: The features jsf-2.3 and faces-3.0 cannot be loaded at the same time.
            // The feature jsf-2.3 of Java EE 8 is incompatible with the feature faces-3.0 of
            // Jakarta EE 9.  The configured features jsf-2.3 and faces-3.0 include an incompatible
            // combination of features.  Your configuration is not supported.  Update the
            // configuration to use features that support either the Jakarta EE or Java EE programming models,
            // but not both.
            assertNotNull(server.waitForStringInLogUsingMark("CWWKF0044E"));
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.emptySet(), true);
        }
    }

}
