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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.yasson.translation.web.YassonTranslationTestServlet;

@MinimumJavaLevel(javaLevel = 11)
@RunWith(FATRunner.class)
public class YassonTranslationTest extends FATServletClient {

    private static final String APP_NAME = "yassontranslationtestapp";
    private static final String CONTEXT = "YassonTranslationTestServlet";

    @Server("io.openliberty.jakarta.yasson.internal.fat.translation")
    @TestServlet(servlet = YassonTranslationTestServlet.class, contextRoot = CONTEXT)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "test.yasson.translation.web");
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

        server.stopServer();
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

        server.stopServer();
    }
}
