/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ParseException;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Testing servlet mapping and definition combination from web.xml, web-fragment.xml, and annotation
 * Other extensive tests in this area are in the legacy bucket
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class WebAndWebFragmentServletMappingTests {
    private static final Logger LOG = Logger.getLogger(WebAndWebFragmentServletMappingTests.class.getName());

    private static final String WAR_NAME = "WebFragmentDefinitionWebXmlMapping.war";
    private static final String WAR_RESOURCE = "com.ibm.ws.webFragment";
    private static final String WAR_RESOURCE2 = "com.ibm.ws.servletAnnotation";

    private static final String JAR_NAME = "WebFragment.jar";
    private static final String JAR_RESOURCE = "com.ibm.ws.jar.webFragmentJar";

    private static final String CTXT_ROOT = "WebFragmentDefinitionWebXmlMapping";

    @Server("servlet40_webFragment")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add " + WAR_NAME + " to the server if not already present.");

        // Create the JAR
        JavaArchive webFragmentJar = ShrinkWrap.create(JavaArchive.class, JAR_NAME);
        ShrinkHelper.addDirectory(webFragmentJar, "test-applications/" + JAR_NAME + "/resources");
        webFragmentJar.addPackage(JAR_RESOURCE);

        // Create the WAR
        WebArchive webAndWebFragmentWar = ShrinkWrap.create(WebArchive.class, WAR_NAME);
        webAndWebFragmentWar.addAsLibrary(webFragmentJar);
        webAndWebFragmentWar.addPackage(WAR_RESOURCE);
        webAndWebFragmentWar.addPackage(WAR_RESOURCE2);
        ShrinkHelper.addDirectory(webAndWebFragmentWar, "test-applications/" + WAR_NAME + "/resources");

        ShrinkHelper.exportToServer(server, "dropins", webAndWebFragmentWar);
        server.startServer(WebAndWebFragmentServletMappingTests.class.getSimpleName() + ".log");
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        if (server != null && server.isStarted()) {
            server.stopServer("SRVE0190E"); //Expected FNF exception
        }
    }

    /**
     * Test web.xml mapping but servlet definition is in web-fragment
     * Request to web.xml mapping.
     * Expect : 200 (response is from the web-fragment servlet definition)
     */
    @Test
    public void request1_webXMLMapping_no_definition_200() throws Exception {
        LOG.info(">>>>>>>>>> Testing request1_webXMLMapping_no_definition_200 ");
        sendRequest("MappingServletInWebXML_DefinitonInWebFragXML", 200);
    }

    /**
     * Test web-fragment.xml definition and mapping.
     * Request to web-fragment.xml mapping which is overridden by the web.xml mapping for same servlet-name
     * Expect : 404
     */
    @Test
    public void request2_webFragMappingXML_404() throws Exception {
        LOG.info(">>>>>>>>>> Testing request2_webFragMappingXML_404");
        sendRequest("MappingServletInWebFragmentXML", 404);
    }

    /**
     * Test definition and mapping in web.xml
     * Request to web.xml mapping
     * Expect : 200
     */
    @Test
    public void request3_webXMLMappingAndDefinition_200() throws Exception {
        LOG.info(">>>>>>>>>> Testing request3_webXMLMappingAndDefinition_200");
        sendRequest("DeclaredServletInWebXML", 200);
    }

    /**
     * Test @WebServlet mapping, for same servlet-name in web.xml but with different mapping.
     * Request to annotation mapping which is overridden by the web.xml mapping.
     * Expect : 404
     */
    @Test
    public void request4_annotationMapping_404() throws Exception {
        LOG.info(">>>>>>>>>> Testing request4_annotationMapping_404");
        sendRequest("AugmentedServletIgnore", 404);
    }

    /**
     * Test mapping in web.xml for a servlet which also has @WebServlet with a different mapping
     * Request to web.xml mapping.
     * Expect : 200
     */
    @Test
    public void request5_webXMLMapping_keep_200() throws Exception {
        LOG.info(">>>>>>>>>> Testing request5_webXMLMapping_keep_200");
        sendRequest("AugmentedServletKeepMapping", 200);
    }

    /**
     * Test definition and mapping from @WebServlet in web-fragment.jar
     * Request to annotation mapping which is overridden by the web-fragment.xml mapping
     * Expect : 404
     */
    @Test
    public void request6_annotatedMapping_in_webFragment_404() throws Exception {
        LOG.info(">>>>>>>>>> Testing request6_annotatedMapping_in_webFragment_404");
        sendRequest("AnnotatedServletInWebFragment", 404);
    }

    /**
     * Test definition and mapping in web-fragment.xml overrides the annotated mapping
     * Request to web-fragment.xml mapping.
     * Expect : 200
     */
    @Test
    public void request7_webFragmentXMLMapping_200() throws Exception {
        LOG.info(">>>>>>>>>> Testing request7_webFragmentXMLMapping_200");
        sendRequest("MappingServletInWebFragmentXML_Override_Annotation", 200);
    }

    private void sendRequest(String urlPattern, int status) throws IOException, ParseException {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + CTXT_ROOT + "/" + urlPattern;
        LOG.info("Send Request: [" + url + "]");

        HttpGet getMethod = new HttpGet(url);
        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                int returnCode = response.getCode();
                LOG.info("\n>>>>>> Response code [" + returnCode + "] | Expecting [" + status + "]");
                assertTrue("Expected status code [" + status + "] . Found [" + returnCode + "]", returnCode == status);
            } catch (Exception ex) {
                LOG.info("Exception [" + ex.toString() + "]");
                assertTrue("Exception has occurred!", false);
            }
        }
    }
}
