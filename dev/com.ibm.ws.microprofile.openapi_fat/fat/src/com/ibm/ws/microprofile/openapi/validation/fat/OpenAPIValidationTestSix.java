package com.ibm.ws.microprofile.openapi.validation.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * A class to test an app with no validation errors.
 *
 */
@RunWith(FATRunner.class)
public class OpenAPIValidationTestSix {

    @Server("validationServerSix")
    public static LibertyServer server;

    private static final String OPENAPI_VALIDATION_YAML = "Validation";

    @BeforeClass
    public static void setUpTest() throws Exception {
        HttpUtils.trustAllCertificates();

        server.startServer("OpenAPIValidationTestSix.log", true);

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
        server.stopServer();
    }
}
