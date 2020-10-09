/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.tests;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests to execute on the jsf23jsf22Server.
 *
 * This test is to ensure that both the jsf-2.3 and jsf-2.2 features are singleton features
 * and only one can be loaded at a time. If both features are specified in a server.xml
 * the server should log an error.
 *
 * Ideally this test would be run in the com.ibm.ws.jsf.2.2_fat_jsf.2.3 toleration bucket.
 * However, the extraServerMunging was not consistent when trying to update the test class to use "jsf-2.3" instead of "jsf-2.2" as it would fail intermittently.
 * With this incosistency in the toleration bucket, it is safest to just copy and add a new test to the JSF 2.3 bucket.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
public class JSF23JSF22SingletonFeatureTest {

    protected static final Class<?> c = JSF23JSF22SingletonFeatureTest.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf23jsf22Server")
    public static LibertyServer jsf23jsf22Server;

    @BeforeClass
    public static void setup() throws Exception {
        // Start the server and use the class name so we can find logs easily.
        jsf23jsf22Server.startServer(JSF23JSF22SingletonFeatureTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf23jsf22Server != null && jsf23jsf22Server.isStarted()) {
            jsf23jsf22Server.stopServer();
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
    public void testEnsureJSF23FeatureAndJSF22FeatureAreSingletonFeatures() throws Exception {
        // Make sure the test framework knows that CWWKF0033E is expected
        jsf23jsf22Server.addIgnoredErrors(Arrays.asList("CWWKF0033E:.*"));

        String msgToSearchFor = "CWWKF0033E: The singleton features jsf-2.2 and jsf-2.3 cannot be loaded at the same time.  " +
                                "The configured features jsf-2.2 and jsf-2.3 include one or more features that cause the conflict.";

        // Search for the message in the logs to ensure that the correct exception was logged
        String areBothJSFFeaturesSingletons = jsf23jsf22Server.waitForStringInLog(msgToSearchFor);

        assertNotNull("The following message was not found in the log: " + msgToSearchFor, areBothJSFFeaturesSingletons);
    }

}
