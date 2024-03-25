/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jakarta.jsonb.internal.fat;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.yasson.translation.web.YassonTranslationTestServlet;

@MinimumJavaLevel(javaLevel = 11)
@RunWith(FATRunner.class)
public class YassonTranslationTest extends FATServletClient {

    private static final String APP_NAME = "yassontranslationtestapp";
    private static final String CONTEXT = "YassonTranslationTestServlet";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE10_FEATURES().fullFATOnly());

    @Server("io.openliberty.jakarta.yasson.internal.fat.translation")
    @TestServlet(servlet = YassonTranslationTestServlet.class, contextRoot = CONTEXT)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        DeployOptions[] options = { DeployOptions.SERVER_ONLY };
        ShrinkHelper.defaultApp(server, APP_NAME, options, "test.yasson.translation.web");
    }

    @Before
    public void setup() {
        if (JakartaEEAction.isEE10OrLaterActive()) {
            server.addEnvVar("YASSON_JAR", "io.openliberty.org.eclipse.yasson.3.0*jar");
        } else if (JakartaEEAction.isEE9Active()) {
            server.addEnvVar("YASSON_JAR", "com.ibm.ws.org.eclipse.yasson.2.0*jar");
        } else {
            server.addEnvVar("YASSON_JAR", "com.ibm.ws.org.eclipse.yasson.1.0*jar");
        }
    }

    @After
    public void cleanup() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWWKE0955E");
        }
    }

    @Test
    public void testYassonTransaltionFunction() throws Exception {
        // Ensure default server settings
        Map<String, String> jvmOptions = server.getJvmOptionsAsMap();
        jvmOptions.remove("-Duser.language");
        jvmOptions.remove("-Duser.country");
        server.setJvmOptions(jvmOptions);

        server.startServer();

        runTest(server, APP_NAME + "/" + CONTEXT, "testTranslationMessageDefault");
        runTest(server, APP_NAME + "/" + CONTEXT, "testTranslationMessageProvidedLocale");
    }

    @Test
    public void testServerTranslationFunction() throws Exception {
        // Ensure Japanese language and Japan country
        Map<String, String> jvmOptions = server.getJvmOptionsAsMap();
        jvmOptions.put("-Duser.language", "ja");
        jvmOptions.put("-Duser.country", "JP");
        server.setJvmOptions(jvmOptions);

        server.startServer();

        runTest(server, APP_NAME + "/" + CONTEXT, "testTranslationMessageServerLocale");
        runTest(server, APP_NAME + "/" + CONTEXT, "testTranslationMessageServerException");
    }
}
