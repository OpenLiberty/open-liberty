package com.ibm.ws.microprofile.openapi.validation.fat;

import static org.junit.Assert.assertNotNull;

import javax.servlet.http.HttpUtils;

import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * A class to test the Tag, Discriminator, Schema and Extension validators.
 * Scenarios include
 * Tag: the name field is missing
 * Discriminator: propertyName field is missing
 * Schema: inappropriate fields for certain Schema types (min/max items or uniqueOnly fields on String type, min/max Length on array types)
 * invalid values for certain fields such as negative values for length
 * conflicting fields such as the readOnly and writeOnly fields
 * Extension: name not starting wiht "x-"
 *
 */
@RunWith(FATRunner.class)
public class OpenAPIValidationTestFive {

    @Server("validationServerFive")
    public static LibertyServer server;
    private static final String OPENAPI_VALIDATION_YAML = "Validation";

    private static final int TIMEOUT = 10000; //in milliseconds (10 seconds)

    @BeforeClass
    public static void setUpTest() throws Exception {
        HttpUtils.trustAllCertificates();

        server.startServer("OpenAPIValidationTestFive.log", true);

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

        assertNotNull("The validation server did not start", server.waitForStringInLog("CWWKE0001I:.*"));
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
    public void testExtension() throws Exception {
        assertNotNull("The Extension validator should have been triggered by the invalid 'name' field", server.waitForStringInLog("extension must begin with \"x-\""));
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

        assertNotNull("The Schema validator should have been triggered by the conflicting field declarations",
                      server.waitForStringInLog("The Schema Object must not have both \"readOnly\" and \"writeOnly\" fields set to true"));
        assertNotNull("The Schema validator should have been triggered by the invalid 'uniqueItems'field",
                      server.waitForStringInLog("The \"uniqueItems\" property is not appropriate for the Schema Object of \"string\" type"));
        assertNotNull("The Schema validator should have been triggered by the invalid 'maxLength'field",
                      server.waitForStringInLog("The \"maxLength\" property of the Schema Object must be greater than or equal to zero"));
        assertNotNull("The Schema validator should have been triggered by the invalid 'minLength'field",
                      server.waitForStringInLog("The \"minLength\" property of the Schema Object must be greater than or equal to zero"));

    }

}
