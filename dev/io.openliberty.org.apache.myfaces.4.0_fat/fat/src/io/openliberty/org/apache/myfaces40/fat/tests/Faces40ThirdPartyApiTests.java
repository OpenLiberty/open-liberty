/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces40.fat.tests;

import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Tests that the correct MyFaces packages are accessible when an application is configured to use third-party APIs
 * Modelled off of com.ibm.ws.jsf.2.2_fat/fat/src/com/ibm/ws/jsf22/fat/tests/JSF22ThirdPartyApiTests.java
 */
@RunWith(FATRunner.class)
public class Faces40ThirdPartyApiTests {

    protected static final Class<?> clazz = Faces40ThirdPartyApiTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("faces40_thirdPartyAPIServer")
    public static LibertyServer faces40_thirdPartyAPIServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultApp(faces40_thirdPartyAPIServer, "Faces40ThirdPartyApi.war", "io.openliberty.org.apache.faces40.fat.thirdpartyapi");

        faces40_thirdPartyAPIServer.startServer(Faces40ThirdPartyApiTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (faces40_thirdPartyAPIServer != null && faces40_thirdPartyAPIServer.isStarted()) {
            faces40_thirdPartyAPIServer.stopServer();
        }
    }

    /**
     * Test that an application with the "third-party" classloader visibility enabled has access to the faces-4.0 org.apache.myfaces packages.
     *
     * @throws Exception
     */
    @Test
    public void testJSFThirdPartyAPIAccess() throws Exception {
        String contextRoot = "Faces40ThirdPartyApi";
        try (WebClient webClient = new WebClient()) {

            URL url = HttpUtils.createURL(faces40_thirdPartyAPIServer, "/" + contextRoot + "/Faces40ThirdPartyAPI.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(clazz, name.getMethodName(), page.asXml());

            assertTrue("The MyFaces API classes were not accessible by the application:\n" + page.asText(), page.asXml().contains("test passed!"));
        }
    }

    /**
     * Test that the same app cannot access those org.apache.myfaces packages which have not been exposed
     *
     * @throws Exception
     */
    @Test
    public void testJSFThirdPartyAPIAccessFails() throws Exception {
        String contextRoot = "Faces40ThirdPartyApi";
        try (WebClient webClient = new WebClient()) {

            URL url = HttpUtils.createURL(faces40_thirdPartyAPIServer, "/" + contextRoot + "/Faces40ThirdPartyAPIFailure.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(clazz, name.getMethodName(), page.asXml());

            assertTrue("org.apache.myfaces classes were accessible when they should not have been:\n" + page.asText(), page.asXml().contains("test passed!"));
        }
    }
}
