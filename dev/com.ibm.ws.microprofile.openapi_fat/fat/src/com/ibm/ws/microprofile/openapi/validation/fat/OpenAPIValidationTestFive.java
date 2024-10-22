package com.ibm.ws.microprofile.openapi.validation.fat;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.microprofile.openapi.fat.FATSuite;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
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
 *
 */
@RunWith(FATRunner.class)
public class OpenAPIValidationTestFive {

    private static final String SERVER_NAME = "validationServerFive";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    private static final String OPENAPI_VALIDATION_YAML = "Validation";

    @BeforeClass
    public static void setUpTest() throws Exception {
        HttpUtils.trustAllCertificates();

        server.startServer("OpenAPIValidationTestFive.log", true);

        server.validateAppLoaded(OPENAPI_VALIDATION_YAML);

        assertNotNull("The validation server did not start", server.waitForStringInLog("CWWKE0001I:.*"));
        // wait for endpoint to become available
        assertNotNull("Web application is not available at /Validation/",
            server.waitForStringInLog("CWWKT0016I.*/Validation/"));
        // wait for server is ready to run a smarter planet message
        assertNotNull("CWWKF0011I.* not received on relationServer",
            server.waitForStringInLog("CWWKF0011I.*"));

    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKO1650E", "CWWKO1651W");
    }

    @Test
    public void testTags() throws Exception {
        assertNotEmpty("The Tag Validator should have been triggered by the missing 'name' field",
            server.findStringsInLogs(
                " - Message: Required \"name\" field is missing or is set to an invalid value, Location: #/tags"));
    }

    @Test
    public void testDiscriminator() throws Exception {
        assertNotEmpty("The Discriminator validator should have been triggered by the missing 'propertyName' field",
            server.findStringsInLogs(
                "- Message: Required \"propertyName\" field is missing or is set to an invalid value,*"));
    }

    @Test
    public void testSchema() throws Exception {
        assertNotEmpty("The Schema validator should have been triggered by the missing 'items' field",
            server.findStringsInLogs(
                " - Message: The Schema Object of \"array\" type must have \"items\" property defined, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotEmpty("The Schema validator should have been triggered by the invalid 'multipleOf' field",
            server.findStringsInLogs(
                " - Message: The Schema Object must have the \"multipleOf\" property set to a number strictly greater than zero, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotEmpty("The Schema validator should have been triggered by the invalid 'minItems' field",
            server.findStringsInLogs(
                "- Message: The \"minItems\" property of the Schema Object must be greater than or equal to zero, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotEmpty("The Schema validator should have been triggered by the invalid 'maxItems' field",
            server.findStringsInLogs(
                " - Message: The \"maxItems\" property of the Schema Object must be greater than or equal to zero, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotEmpty("The Schema validator should have been triggered by the invalid 'minProperties' field",
            server.findStringsInLogs(
                " - Message: The \"minProperties\" property of the Schema Object must be greater than or equal to zero, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotEmpty("The Schema validator should have been triggered by the invalid 'maxProperties' field",
            server.findStringsInLogs(
                " - Message: The \"maxProperties\" property of the Schema Object must be greater than or equal to zero, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotEmpty("The Schema validator should have been triggered by the invalid 'minItems' field",
            server.findStringsInLogs(
                " - Message: The \"minItems\" property is not appropriate for the Schema Object of \"object\" type, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotEmpty("The Schema validator should have been triggered by the invalid 'maxItems' field",
            server.findStringsInLogs(
                " - Message: The \"maxItems\" property is not appropriate for the Schema Object of \"object\" type, Location: #/paths/~1availability/get/parameters/schema"));
    }

    private void assertNotEmpty(String message,
                                List<String> stringsInLogs) {
        assertThat(message, stringsInLogs, not(empty()));
    }
}
