package com.ibm.ws.microprofile.openapi.validation.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * A class to test the Tag, Discriminator, Schema and Extension validators.
 * Scenarios include
 * Tag: the name field is missing
 * Discriminator: propertyName field is missing
 * Schema: inappropriate fields for certain Schema types (min/max items or uniqueOnly fields on String type, min/max Length on array types)
 * invalid values for certain fields such as negative values for length
 * conflicting fields such as the readOnly and writeOnly fields
 * Extension: name not starting with "x-"
 *
 */
@RunWith(FATRunner.class)
public class OpenAPIValidationTestFive {

    @Server("validationServerFive")
    public static LibertyServer server;

    private static final String OPENAPI_VALIDATION_YAML = "Validation";

    @BeforeClass
    public static void setUpTest() throws Exception {
        HttpUtils.trustAllCertificates();

        server.startServer("OpenAPIValidationTestFive.log", true);

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
        server.stopServer("CWWKO1650E", "CWWKO1651W");
    }

    @Test
    public void testTags() throws Exception {
        assertNotNull("The Tag Validator should have been triggered by the missing 'name' field",
                      server.waitForStringInLog(" - Message: Required \"name\" field is missing or is set to an invalid value, Location: #/tags"));
    }

    @Test
    public void testDiscriminator() throws Exception {
        assertNotNull("The Discriminator validator should have been triggered by the missing 'propertyName' field",
                      server.waitForStringInLog("- Message: Required \"propertyName\" field is missing or is set to an invalid value,*"));
    }

    @Test
    public void testSchema() throws Exception {
        assertNotNull("The Schema validator should have been triggered by the missing 'items' field",
                      server.waitForStringInLog(" - Message: The Schema Object of \"array\" type must have \"items\" property defined, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotNull("The Schema validator should have been triggered by the invalid 'multipleOf' field",
                      server.waitForStringInLog(" - Message: The Schema Object must have the \"multipleOf\" property set to a number strictly greater than zero, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotNull("The Schema validator should have been triggered by the invalid 'minItems' field",
                      server.waitForStringInLog("- Message: The \"minItems\" property of the Schema Object must be greater than or equal to zero, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotNull("The Schema validator should have been triggered by the invalid 'maxItems' field",
                      server.waitForStringInLog(" - Message: The \"maxItems\" property of the Schema Object must be greater than or equal to zero, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotNull("The Schema validator should have been triggered by the invalid 'minProperties' field",
                      server.waitForStringInLog(" - Message: The \"minProperties\" property of the Schema Object must be greater than or equal to zero, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotNull("The Schema validator should have been triggered by the invalid 'maxProperties' field",
                      server.waitForStringInLog(" - Message: The \"maxProperties\" property of the Schema Object must be greater than or equal to zero, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotNull("The Schema validator should have been triggered by the invalid 'minItems' field",
                      server.waitForStringInLog(" - Message: The \"minItems\" property is not appropriate for the Schema Object of \"object\" type, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotNull("The Schema validator should have been triggered by the invalid 'maxItems' field",
                      server.waitForStringInLog(" - Message: The \"maxItems\" property is not appropriate for the Schema Object of \"object\" type, Location: #/paths/~1availability/get/parameters/schema"));
    }
}
