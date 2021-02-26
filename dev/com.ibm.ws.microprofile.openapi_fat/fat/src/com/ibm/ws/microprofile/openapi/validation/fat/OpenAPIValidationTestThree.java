package com.ibm.ws.microprofile.openapi.validation.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPIConnection;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPITestUtil;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * A class to test the OpenAPI validator. This class covers the scenario where the info and paths fields is missing from the OpenAPI model.
 *
 */
@RunWith(FATRunner.class)
public class OpenAPIValidationTestThree {

    @Server("validationServerThree")
    public static LibertyServer server;
    private static final String OPENAPI_VALIDATION_YAML = "Validation";

    @BeforeClass
    public static void setUpTest() throws Exception {
        HttpUtils.trustAllCertificates();

        server.startServer("OpenAPIValidationTestThree.log", true);

        server.validateAppLoaded(OPENAPI_VALIDATION_YAML);

        assertNotNull("The validation server did not start", server.waitForStringInLog("CWWKE0001I:.*"));
        //wait for endpoint to become available
        assertNotNull("Web application is not available at /Validation/",
                      server.waitForStringInLog("CWWKT0016I.*/Validation/"));
        // wait for server is ready to run a smarter planet message
        assertNotNull("CWWKF0011I.* not recieved on relationServer",
                      server.waitForStringInLog("CWWKF0011I.*"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKO1650E");
    }

    @Test
    public void testPaths() throws Exception {
        assertNotNull("The OpenAPI Validator should have been triggered by the missing 'paths' field",
                      server.waitForStringInLog("Message: Required \"paths\" field is missing or is set to an invalid value, Location: #"));
    }

    @Test
    @SkipForRepeat("mpOpenAPI-2.0")
    public void testBlankInfo() throws Exception {
        OpenAPITestUtil.waitForApplicationProcessorProcessedEvent(server, OPENAPI_VALIDATION_YAML);
        OpenAPITestUtil.waitForApplicationProcessorAddedEvent(server, OPENAPI_VALIDATION_YAML);
        String openapiDoc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(openapiDoc);
        OpenAPITestUtil.checkInfo(openapiNode, "Deployed APIs", "1.0.0");
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.NO_MODIFICATION, "mpOpenAPI-1.1" })
    public void testBlankInfo20() throws Exception {
        OpenAPITestUtil.waitForApplicationProcessorProcessedEvent(server, OPENAPI_VALIDATION_YAML);
        OpenAPITestUtil.waitForApplicationProcessorAddedEvent(server, OPENAPI_VALIDATION_YAML);
        String openapiDoc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(openapiDoc);
        OpenAPITestUtil.checkInfo(openapiNode, "Generated API", "1.0");
    }
}
