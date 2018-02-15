package com.ibm.ws.microprofile.openapi.validation.fat;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
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

    private static final int TIMEOUT = 10000; //in milliseconds (10 seconds)

    @BeforeClass
    public static void setUpTest() throws Exception {
        HttpUtils.trustAllCertificates();

        server.startServer("OpenAPIValidationTestThree.log", true);

        server.validateAppLoaded(OPENAPI_VALIDATION_YAML);

        assertNotNull("Web application is not available at /Validation/",
                      server.waitForStringInLog("CWWKT0016I.*/Validation/")); //wait for endpoint to become available
        assertNotNull("CWWKF0011I.* not recieved on relationServer",
                      server.waitForStringInLog("CWWKF0011I.*")); // wait for server is ready to run a smarter planet message
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKO1603E", "CWWKO1650E");
    }

    @Test
    public void testStartup() throws Exception {
        // Tests that correct validation messages are provided for the validation errors in the following models:
        // OpenAPI and Paths
        assertNotNull("The validation server did not start", server.waitForStringInLog("CWWKE0001I:.*"));
    }

    @Test
    public void testPaths() throws Exception {
        assertNotNull("The OpenAPI Validator should have been triggered by the missing 'paths' field",
                             server.waitForStringInLog("Message: Required \"paths\" field is missing or is set to an invalid value, Location: #"));
        assertNotNull("The OpenAPI Validator should have been triggered by the missing 'info' field",
                             server.waitForStringInLog("Message: Required \"info\" field is missing or is set to an invalid value, Location: #"));
    }

}
