package com.ibm.ws.microprofile.openapi.validation.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * A class to test the Callbacks, Reference and PathItem validator.
 * Scenarios include:
 * Reference: all possible invalid references
 * Callback: invalid fields and missing required fields
 * PathItems: duplicate path items and invalid fields
 *
 */
@RunWith(FATRunner.class)
public class OpenAPIValidationTestFour {

    @Server("validationServerFour")
    public static LibertyServer server;

    private static final String OPENAPI_VALIDATION_YAML = "Validation";

    @BeforeClass
    public static void setUpTest() throws Exception {
        HttpUtils.trustAllCertificates();

        server.startServer("OpenAPIValidationTestFour.log", true);

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
    public void testRef() throws Exception {
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/invalidRef/schemas/testSchema\" reference value is not in a valid format, Location: #/paths/~1/get/responses/200/content/application~1json/schema/items"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/invalidRef/schemas/testSchema\" value is an invalid reference, Location: #/paths/~1/get/responses/200/content/application~1json/schema/items"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/components/schemas/Date\" reference value is not in a valid format, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/components/schemas/Date\" value is an invalid reference, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/schemas/\" reference value is not in a valid format, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/schemas/\" value is an invalid reference, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/schemas/ \" reference value is not defined within the Components Object, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/schemas/ \" value is an invalid reference, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/schemas/Airport/Cat\" reference value is not in a valid format, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/schemas/Airport/Cat\" value is an invalid reference, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/schemas/#\" reference value is not defined within the Components Object, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/schemas/#\" value is an invalid reference, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/\" reference value is not in a valid format, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/\" value is an invalid reference, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/Flight\" reference value is not in a valid format, Location: #/paths/~1availability/get/responses/200/content/applictaion~1json/schema/items"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/Flight\" value is an invalid reference, Location: #/paths/~1availability/get/responses/200/content/applictaion~1json/schema/items"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components//Booking\" reference value is invalid, Location: #/paths/~1bookings/get/responses/200/content/application~1json/schema/items"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components//Booking\" value is an invalid reference, Location: #/paths/~1bookings/get/responses/200/content/application~1json/schema/items"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/schemas\" reference value is not in a valid format, Location: #/paths/~1bookings/post/callbacks/getBookings//get/responses/200/content/application~1json/schema/items"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/schemas\" value is an invalid reference, Location: #/paths/~1bookings/post/callbacks/getBookings//get/responses/200/content/application~1json/schema/items"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/requestBodies/Pet\" reference value is not defined within the Components Object, Location: #/paths/~1bookings/post/requestBody"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/requestBodies/Pet\" value is an invalid reference, Location: #/paths/~1bookings/post/requestBody"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/responses/Pet\" reference value is not defined within the Components Object,*"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/responses/Pet\" value is an invalid reference,*"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/schemas/schemas\" reference value is not defined within the Components Object,*"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/schemas/schemas\" value is an invalid reference,*"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/schemas/Pet\" reference value is not defined within the Components Object,*"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/schemas/Pet\" value is an invalid reference,*"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/examples/Pet\" reference value is not defined within the Components Object, Location: #/paths/~1reviews/post/requestBody/content/application~1json/examples/review"));
        assertNotNull("The Reference validator should have been triggered by the invalid reference",
                      server.waitForStringInLog(" - Message: The \"#/components/examples/Pet\" value is an invalid reference, Location: #/paths/~1reviews/post/requestBody/content/application~1json/examples/review"));
    }

    @Test
    public void testCallbacks() throws Exception {
        assertNotNull("The Callback validator should have been triggered by the invalid URL",
                      server.waitForStringInLog(" - Message: The URL template of Callback Object is empty and is not a valid URL, Location: #/paths/~1bookings/post/callbacks/getBookings"));
        assertNotNull("The Callback validator should have been triggered by the invalid substitution variables in the URL",
                      server.waitForStringInLog(" - Message: The Callback Object contains invalid substitution variables:*"));
        assertNotNull("The Callback validator should have been triggered by the invalid URL",
                      server.waitForStringInLog(" - Message: The Callback Object must contain a valid URL. The \"h://localhost:9080/oas3-airlines/booking\" value specified for the URL is not valid,*"));
        assertNotNull("The Callback validator should have been triggered by the invalid runtime expression",
                      server.waitForStringInLog(" - Message: The Callback Object must contain a valid runtime expression as defined in the OpenAPI Specification.*"));
    }

    @Test
    public void testPathItems() throws Exception {
        assertNotNull("The PathItem validator should have been triggered by the by ",
                      server.waitForStringInLog(" - Message: The Path Item Object must contain a valid path.*"));
        assertNotNull("The PathItem validator should have been triggered by the missing parameter definition",
                      server.waitForStringInLog(" - Message: The Path Item Object must contain a valid path. The format of the*"));
        assertTrue("The PathItem validator should have been triggered by the by the duplicate path",
                   server.waitForMultipleStringsInLog(2, " - Message: The Path Item Object must contain a valid path.*") == 2);
    }
}
