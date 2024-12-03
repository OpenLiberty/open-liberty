/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.fat.config;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPIConnection;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPITestUtil;

import componenttest.topology.impl.LibertyServer;

/**
 * Provides higher-level server assertions for the OpenAPI config tests
 */
public class ConfigServerTestHelper {

    private final LibertyServer server;

    public ConfigServerTestHelper(LibertyServer server) {
        super();
        this.server = server;
    }

    /**
     * Assert that a CWWKT0016I: Web application available message is seen for a web app with the given path
     *
     * @param path the path to expect
     */
    public void assertWebAppStarts(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        if (!path.endsWith("/")) {
            path = path + "/";
        }

        // E.g. CWWKT0016I: Web application available (default_host):
        // http://1.2.3.4:8010/bar/
        assertNotNull("Web application available message not found for " + path,
            server.waitForStringInLog("CWWKT0016I:.*:\\d+" + path + "$"));
    }

    /**
     * Assert that a CWWKT0016I: Web application available message is not seen for a web app with the given path
     *
     * @param path the path to check for
     */
    public void assertNoWebAppStart(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        if (!path.endsWith("/")) {
            path = path + "/";
        }

        // E.g. CWWKT0016I: Web application available (default_host):
        // http://1.2.3.4:8010/bar/
        assertNull("Unexpected web application available message found for " + path,
            server.waitForStringInLog("CWWKT0016I:.*:\\d+" + path + "$", 1000));
    }

    /**
     * Assert that the OpenAPI UI is being served from the given path
     *
     * @param path the path
     * @throws Exception
     */
    public void assertUiPath(String path) throws Exception {
        // Check that we get something back
        String uiHTML = new OpenAPIConnection(server, path).download();
        // Check that it appears to be the UI HTML
        assertThat(uiHTML, containsString("oauth2RedirectUrl: SwaggerUI.getMpOAuth2Url()"));
    }

    /**
     * Assert that the OpenAPI document is being served from the given path
     *
     * @param path the path
     * @throws Exception
     */
    public void assertDocumentPath(String path) throws Exception {
        // Check that it parses as a model and contains the expected path from the test
        // app
        String doc = new OpenAPIConnection(server, path).download();
        JsonNode model = OpenAPITestUtil.readYamlTree(doc);
        OpenAPITestUtil.checkPaths(model, 1, "/configTestPath");
    }

    /**
     * Assert that nothing is found at the given path (request returns 404)
     *
     * @param path the path
     * @throws Exception
     */
    public void assertMissing(String path) throws Exception {
        OpenAPIConnection connection = new OpenAPIConnection(server, path);
        connection.expectedResponseCode(404);
        connection.download();
    }

    public List<String> getServerUrls(String openapiYaml) {
        JsonNode node = OpenAPITestUtil.readYamlTree(openapiYaml);
        JsonNode servers = node.get("servers");
        if (!servers.isArray()) {
            return Collections.emptyList();
        }

        ArrayList<String> result = new ArrayList<>();
        for (JsonNode server : servers) {
            String url = server.get("url").asText(null);
            if (url != null) {
                result.add(url);
            }
        }

        return result;
    }

}
