/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.fat.version;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.openapi20.fat.FATSuite;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPIConnection;
import io.openliberty.microprofile.openapi20.fat.utils.OpenAPITestUtil;

/**
 * Test configuration of {@code <mpOpenAPI openAPIVersion="3.0"/>}
 */
@RunWith(FATRunner.class)
public class OpenAPIVersionTest extends FATServletClient {

    /** Errors which may be emitted for an invalid configured version */
    private enum ErrorCondition {
        INVALID_FORMAT, NOT_SUPPORTED
    }

    public static final String SERVER_NAME = "VersionTestServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static TestRule r = FATSuite.repeatDefault(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "versionTest.war")
                                   .addClasses(VersionTestApp.class, VersionTestResource.class, VersionTestData.class);

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        server.stopServer("CWWKO1681W", // OpenAPI version invalid
                          "CWWKO1682W"); // OpenAPI version unsupported
    }

    @Test
    public void testValid30Versions() throws Exception {
        testVersion("3.0", "3.0.3");
        testVersion("3.0.2", "3.0.2");
        testVersion("3.0.3", "3.0.3");
    }

    @Test
    @SkipForRepeat({ MicroProfileActions.MP41_ID,
        MicroProfileActions.MP50_ID,
        MicroProfileActions.MP61_ID })
    public void testValid31Versions() throws Exception {
        testVersion("3.1", "3.1.0");
        testVersion("3.1.1", "3.1.1");
        testVersion("3.1.0", "3.1.0");
    }

    @Test
    @SkipForRepeat({ MicroProfileActions.MP70_EE10_ID,
        MicroProfileActions.MP70_EE11_ID })
    public void testInvalid31Versions() throws Exception {
        testInvalidVersion("3.1", ErrorCondition.NOT_SUPPORTED);
        testInvalidVersion("3.1.0", ErrorCondition.NOT_SUPPORTED);
    }

    @Test
    public void testInvalidVersions() throws Exception {
        testInvalidVersion("abc", ErrorCondition.INVALID_FORMAT);
        testInvalidVersion("2.0", ErrorCondition.NOT_SUPPORTED);
        testInvalidVersion("4.0.2", ErrorCondition.NOT_SUPPORTED);
    }

    private void testInvalidVersion(String invalidVersion, ErrorCondition errorCondition) throws Exception {
        configureVersion(invalidVersion); // Sets mark

        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        Log.info(OpenAPIVersionTest.class, "testVersion", doc);
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);

        // Check default version used in output document
        assertThat(openapiNode.path("openapi").asText(), equalTo(getDefaultVersion()));

        // Check that the correct warning is emitted
        String expectedMessage;
        switch (errorCondition) {
            case INVALID_FORMAT:
                expectedMessage = "CWWKO1681W: The configured " + invalidVersion + " OpenAPI version is not in a valid format";
                break;
            case NOT_SUPPORTED:
                expectedMessage = "CWWKO1682W: This version of mpOpenAPI does not support the configured " + invalidVersion + " OpenAPI version.";
                break;
            default:
                throw new IllegalStateException("Unknown expected error condition: " + errorCondition);
        }
        List<String> warnings = server.findStringsInLogsUsingMark(expectedMessage, server.getDefaultLogFile());
        assertThat(warnings, hasSize(1));
    }

    private void testVersion(String configuredVersion, String expectedVersion) throws Exception {
        configureVersion(configuredVersion);
        String doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        Log.info(OpenAPIVersionTest.class, "testVersion", doc);
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(doc);

        // Test openapi version property
        assertThat(openapiNode.path("openapi").asText(), equalTo(expectedVersion));

        // check exclusiveMaximum in schema
        // should be a boolean in 3.0 and a number in 3.1
        JsonNode testDataSchema = openapiNode.at("/components/schemas/VersionTestData/properties/hexDigit");
        if (expectedVersion.startsWith("3.0")) {
            assertTrue("exclusiveMaximum should be boolean", testDataSchema.path("exclusiveMaximum").isBoolean());
        } else if (expectedVersion.startsWith("3.1")) {
            assertTrue("exclusiveMaximum should be number", testDataSchema.path("exclusiveMaximum").isNumber());
        }
    }

    private void configureVersion(String configuredVersion) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setOpenApiVersion(configuredVersion);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);
    }

    /**
     * Gets the default OpenAPI version expected if none is configured.
     * <p>
     * This varies depending on the current repeat
     */
    private String getDefaultVersion() {
        if (OpenAPITestUtil.getOpenAPIFeatureVersion() >= 4.0f) {
            return "3.1.0";
        } else {
            return "3.0.3";
        }
    }
}
