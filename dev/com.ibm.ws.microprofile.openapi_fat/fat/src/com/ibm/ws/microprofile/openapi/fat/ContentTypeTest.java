/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPIConnection;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPITestUtil;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

/**
 * Tests to ensure that we can request JSON and YAML versions of the OpenAPI document from /openapi
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class ContentTypeTest extends FATServletClient {
    private static final Class<?> c = ContentTypeTest.class;

    private static final String SERVER_NAME = "AnnotationProcessingServer";

    private static final String APP_NAME_1 = "appWithStaticDoc";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME,
        MicroProfileActions.MP60, // mpOpenAPI-3.1
        MicroProfileActions.MP50, // mpOpenAPI-3.0
        MicroProfileActions.MP41, // mpOpenAPI-2.0
        MicroProfileActions.MP33, // mpOpenAPI-1.1
        MicroProfileActions.MP22);// mpOpenAPI-1.0

    @BeforeClass
    public static void setUpTest() throws Exception {
        HttpUtils.trustAllCertificates();

        DeployOptions[] opts = {
            DeployOptions.SERVER_ONLY
        };
        ShrinkHelper.defaultApp(server, APP_NAME_1, opts);

        LibertyServer.setValidateApps(false);

        // Change server ports to the default ones
        OpenAPITestUtil.changeServerPorts(server, server.getHttpDefaultPort(), server.getHttpDefaultSecurePort());

        server.startServer(c.getSimpleName() + ".log");

        OpenAPITestUtil.addApplication(server, APP_NAME_1);
        OpenAPITestUtil.waitForApplicationProcessorProcessedEvent(server, APP_NAME_1);
        OpenAPITestUtil.waitForApplicationProcessorAddedEvent(server, APP_NAME_1);
    }

    /**
     * This ensures all the applications are removed before running each test to make sure
     * we start with a clean server.xml.
     */
    @Before
    public void setUp() throws Exception {
        // Change server ports to the default ones
        OpenAPITestUtil.changeServerPorts(server, server.getHttpDefaultPort(), server.getHttpDefaultSecurePort());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testNoAcceptHeader() throws Exception {
        String content = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        assertFalse(isJson(content));
    }

    @Test
    public void testAcceptJson() throws Exception {
        String content = OpenAPIConnection.openAPIDocsConnection(server, false)
            .header("Accept", "application/json")
            .download();
        assertTrue(isJson(content));
    }

    @Test
    public void testAcceptJsonPlus() throws Exception {
        String content = OpenAPIConnection.openAPIDocsConnection(server, false)
            .header("Accept", "application/json, */*")
            .download();
        assertTrue(isJson(content));
    }

    /**
     * Checks whether a string parses as valid JSON
     * @param content the string to parse
     * @return {@code true} if {@code content} parses as JSON, {@code false} otherwise
     */
    private boolean isJson(String content) {
        try {
            new ObjectMapper().readerFor(JsonNode.class).readValue(content);
            return true; // JSON parse was successful
        } catch (JsonProcessingException e) {
        }
        return false;
    }

}
