package com.ibm.ws.microprofile.openapi.validation.fat;

import static componenttest.rules.repeater.MicroProfileActions.MP70_EE10_ID;
import static componenttest.rules.repeater.MicroProfileActions.MP70_EE11_ID;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
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
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
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

    private static final String SERVER_NAME = "validationServerFour";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    private static final String OPENAPI_VALIDATION_YAML = "Validation";

    @BeforeClass
    public static void setUpTest() throws Exception {
        HttpUtils.trustAllCertificates();

        server.startServer("OpenAPIValidationTestFour.log", true);

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

    @SkipForRepeat({
        MP70_EE10_ID, // Disable for mpOpenAPI-4.0 until we have a fix for
        MP70_EE11_ID, // https://github.com/smallrye/smallrye-open-api/issues/1987
    })
    @Test
    public void testRef() throws Exception {
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/invalidRef/schemas/testSchema\" reference value is not in a valid format, Location: #/paths/~1/get/responses/200/content/application~1json/schema/items"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/invalidRef/schemas/testSchema\" value is an invalid reference, Location: #/paths/~1/get/responses/200/content/application~1json/schema/items"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/components/schemas/Date\" reference value is not in a valid format, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/components/schemas/Date\" value is an invalid reference, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/schemas/\" reference value is not in a valid format, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/schemas/\" value is an invalid reference, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/schemas/ \" reference value is not defined within the Components Object, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/schemas/ \" value is an invalid reference, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/schemas/Airport/Cat\" reference value is not in a valid format, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/schemas/Airport/Cat\" value is an invalid reference, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/schemas/#\" reference value is not defined within the Components Object, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/schemas/#\" value is an invalid reference, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/\" reference value is not in a valid format, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/\" value is an invalid reference, Location: #/paths/~1availability/get/parameters/schema"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/Flight\" reference value is not in a valid format, Location: #/paths/~1availability/get/responses/200/content/applictaion~1json/schema/items"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/Flight\" value is an invalid reference, Location: #/paths/~1availability/get/responses/200/content/applictaion~1json/schema/items"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components//Booking\" reference value is invalid, Location: #/paths/~1bookings/get/responses/200/content/application~1json/schema/items"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components//Booking\" value is an invalid reference, Location: #/paths/~1bookings/get/responses/200/content/application~1json/schema/items"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/schemas\" reference value is not in a valid format, Location: #/paths/~1bookings/post/callbacks/getBookings//get/responses/200/content/application~1json/schema/items"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/schemas\" value is an invalid reference, Location: #/paths/~1bookings/post/callbacks/getBookings//get/responses/200/content/application~1json/schema/items"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/requestBodies/Pet\" reference value is not defined within the Components Object, Location: #/paths/~1bookings/post/requestBody"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/requestBodies/Pet\" value is an invalid reference, Location: #/paths/~1bookings/post/requestBody"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/responses/Pet\" reference value is not defined within the Components Object,*"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server
                .findStringsInLogs(" - Message: The \"#/components/responses/Pet\" value is an invalid reference,*"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/schemas/schemas\" reference value is not defined within the Components Object,*"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/schemas/schemas\" value is an invalid reference,*"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/schemas/Pet\" reference value is not defined within the Components Object,*"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(" - Message: The \"#/components/schemas/Pet\" value is an invalid reference,*"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/examples/Pet\" reference value is not defined within the Components Object, Location: #/paths/~1reviews/post/requestBody/content/application~1json/examples/review"));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogs(
                " - Message: The \"#/components/examples/Pet\" value is an invalid reference, Location: #/paths/~1reviews/post/requestBody/content/application~1json/examples/review"));
    }

    @Test
    public void testCallbacks() throws Exception {
        assertNotEmpty("The Callback validator should have been triggered by the invalid URL",
            server.findStringsInLogs(
                " - Message: The URL template of Callback Object is empty and is not a valid URL, Location: #/paths/~1bookings/post/callbacks/getBookings"));
        assertNotEmpty(
            "The Callback validator should have been triggered by the invalid substitution variables in the URL",
            server.findStringsInLogs(" - Message: The Callback Object contains invalid substitution variables:*"));
        assertNotEmpty("The Callback validator should have been triggered by the invalid runtime expression",
            server.findStringsInLogs(
                " - Message: The Callback Object must contain a valid runtime expression as defined in the OpenAPI Specification.*"));
    }

    @Test
    public void testPathItems() throws Exception {
        assertNotEmpty("The PathItem validator should have been triggered by the by ",
            server.findStringsInLogs(" - Message: The Path Item Object must contain a valid path."));
        assertNotEmpty("The PathItem validator should have been triggered by the missing parameter definition",
            server
                .findStringsInLogs(" - Message: The Path Item Object must contain a valid path. The format of the"));
        assertThat("The PathItem validator should have been triggered by the by the duplicate path",
            server.findStringsInLogs(" - Message: The Path Item Object must contain a valid path."),
            hasSize(4));
    }

    /**
     * @param string
     * @param stringsInLogs
     */
    private void assertNotEmpty(String message,
                                List<String> stringsInLogs) {
        assertThat(message, stringsInLogs, not(empty()));
    }
}
