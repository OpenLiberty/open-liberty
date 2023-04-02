/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package com.ibm.ws.microprofile.openapi.fat.annotations;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.openapi.fat.annotations.NonPublicFieldVisibilityApplication.NonPublicFieldVisibilityDataObject;
import com.ibm.ws.microprofile.openapi.fat.annotations.PrivateFieldVisibilityApplication.PrivateFieldVisibilityDataObject;
import com.ibm.ws.microprofile.openapi.fat.annotations.RequiredCustomNameApplication.RequiredCustomNameDataObject;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPIConnection;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPITestUtil;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * Annotation processing correctness tests which aren't adequately covered by
 * the TCK
 */
@RunWith(FATRunner.class)
public class AnnotationProcessingTest {

    private static final String SERVER_NAME = "AnnotationProcessingServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME,
        MicroProfileActions.MP60, // mpOpenAPI-3.1, LITE
        MicroProfileActions.MP50, // mpOpenAPI-3.0, FULL
        MicroProfileActions.MP41, // mpOpenAPI-2.0, FULL
        MicroProfileActions.MP33, // mpOpenAPI-1.1, FULL
        MicroProfileActions.MP22);// mpOpenAPI-1.0, FULL

    @BeforeClass
    public static void setup() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testPrivateFieldVisibility() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testPrivateFieldVisibility.war")
            .addClass(PrivateFieldVisibilityApplication.class);

        try {
            ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

            ObjectNode openApi = (ObjectNode) OpenAPITestUtil
                .readYamlTree(new OpenAPIConnection(server, OpenAPIConnection.OPEN_API_DOCS).download());

            ObjectNode endpointResponse = (ObjectNode) OpenAPITestUtil
                .readYamlTree(new OpenAPIConnection(server, "/testPrivateFieldVisibility/").download());

            assertDataMatchesSchema(endpointResponse, openApi, PrivateFieldVisibilityDataObject.class.getSimpleName());
        } finally {
            server.removeAndStopDropinsApplications("testPrivateFieldVisibility.war");
        }
    }

    @Test
    public void testNonPublicFieldVisibility() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testNonPublicFieldVisibility.war")
            .addClass(NonPublicFieldVisibilityApplication.class);

        try {
            ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

            ObjectNode openApi = (ObjectNode) OpenAPITestUtil
                .readYamlTree(new OpenAPIConnection(server, OpenAPIConnection.OPEN_API_DOCS).download());

            ObjectNode endpointResponse = (ObjectNode) OpenAPITestUtil
                .readYamlTree(new OpenAPIConnection(server, "/testNonPublicFieldVisibility/").download());

            assertDataMatchesSchema(endpointResponse, openApi,
                NonPublicFieldVisibilityDataObject.class.getSimpleName());
        } finally {
            server.removeAndStopDropinsApplications("testNonPublicFieldVisibility.war");
        }
    }

    @Test
    public void testRequiredCustomNameField() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testRequiredCustomNameField.war")
            .addClass(RequiredCustomNameApplication.class);

        try {
            ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

            ObjectNode openApi = (ObjectNode) OpenAPITestUtil
                .readYamlTree(new OpenAPIConnection(server, OpenAPIConnection.OPEN_API_DOCS).download());

            JsonNode schema = openApi.path("components").path("schemas")
                .path(RequiredCustomNameDataObject.class.getSimpleName());
            assertFalse("schema missing", schema.isMissingNode());

            Set<String> propertyNames = toSet(schema.path("properties").fieldNames());
            assertThat(propertyNames, Matchers.containsInAnyOrder("test_a", "testB"));

            Set<String> requiredNames = toSet(schema.path("required").elements())
                .stream()
                .map(JsonNode::textValue)
                .collect(Collectors.toSet());
            assertThat(requiredNames, Matchers.containsInAnyOrder("test_a", "testB"));

        } finally {
            server.removeAndStopDropinsApplications("testRequiredCustomNameField.war");
        }
    }

    /**
     * Assert that a data object matches an openapi schema
     * <p>
     * This is a light check, it only validates that the fields in the data object
     * match the properties listed in the schema.
     *
     * @param data       the data object
     * @param openApiDoc the OpenAPI document containing the schema
     * @param schemaName the name of the schema to use
     */
    private void assertDataMatchesSchema(JsonNode data,
                                         JsonNode openApiDoc,
                                         String schemaName) {

        JsonNode schema = openApiDoc.path("components").path("schemas")
            .path(schemaName);

        if (schema.isMissingNode()) {
            throw new AssertionError("Expected: schema named " + schemaName + " to exist\n"
                + "but openApi document was: " + openApiDoc.toPrettyString());
        }

        Set<String> schemaNames = toSet(schema.path("properties").fieldNames());

        if (!data.isObject()) {
            throw new AssertionError("Expected: data to be an object\n"
                + "   but was: " + data.toPrettyString());
        }

        Set<String> dataNames = toSet(data.fieldNames());

        if (!schemaNames.equals(dataNames)) {
            throw new AssertionError("The fields in the data object do not match those listed in the schema\n"
                + "data: " + dataNames + "\n"
                + "schema: " + schemaNames);
        }
    }

    private <T> Set<T> toSet(Iterator<T> i) {
        Set<T> result = new HashSet<>();
        while (i.hasNext()) {
            result.add(i.next());
        }
        return result;
    }

}
