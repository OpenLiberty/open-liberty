package com.ibm.ws.microprofile.openapi.validation.fat;

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

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.microprofile.openapi.fat.FATSuite;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import componenttest.topology.utils.HttpUtils;

/**
 * Tests to ensure that OpenAPI model validation works,
 * model walker calls appropriate validators, and proper events (errors, warning) are reported.
 *
 * Tests for correct validation messages provided for the validation errors in the following models:
 *
 * Info, Contact, License, ServerVariable(s), Server(s), PathItem, Operation, ExternalDocumentation,
 * SecurityRequirement, RequestBody, Response, Responses
 *
 * The app with a static yaml file checks the following conditions for each model:
 * - Info: REQUIRED "title" and "version", valid "termsOfService" URL - all validation cases checked
 * - License: REQUIRED "name", and valid "url" URL - all validation cases checked
 * - Contact: valid url and email - all validation cases checked
 * - ServerVariable: REQUIRED "default" - all validation cases checked
 * - ServerVariables: null value results in invalid OpenAPI doc, null key is tested - all validation cases checked
 * - Server: "url" field is not null and is valid, and all server variables are defined - all validation cases checked
 * - PathItem: duplicate parameter, the 'required' field of path parameter, undeclared parameter, path string validity, operation parameters - all validation cases checked
 * - Operation: RQUIRED 'responses' field and unique operation IDs - all validation cases checked
 * - ExternalDocumentation: invalid url tested here, null url tested in OpenAPIValidationTestTwo
 * - SecurityRequirement: name undeclared in SecurityScheme tested, the rest of cases are tested in OpenAPIValidationTestTwo
 * - RequestBody: REQUIRED 'content' field tested - all validation cases checked
 * - Response: REQUIRED 'description' field tested - all validation cases checked
 * - Responses: at least one response code for successful operation tested - all validation cases checked
 *
 */
@RunWith(FATRunner.class)
public class OpenAPIValidationTestOne {

    private static final String SERVER_NAME = "validationServerOne";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    private static final String OPENAPI_VALIDATION_YAML = "openapi_validation";

    @BeforeClass
    public static void setUpTest() throws Exception {
        HttpUtils.trustAllCertificates();

        server.startServer("OpenAPIValidationTestOne.log", true);

        server.validateAppLoaded(OPENAPI_VALIDATION_YAML);

        assertNotNull("Web application is not available at /openapi_validation/",
            server.waitForStringInLog("CWWKT0016I.*/openapi_validation/")); // wait for endpoint to become available
        assertNotNull("CWWKF0011I.* not received on relationServer",
            server.waitForStringInLog("CWWKF0011I.*")); // wait for server is ready to run a smarter planet message
        assertNotNull("The application openapi_validation was processed and an OpenAPI document was produced.",
            server.waitForStringInLog("CWWKO1660I.*and an OpenAPI document was produced.")); // wait for application to
                                                                                             // be processed
        String openApiDoc = new HttpRequest(server, "/openapi").run(String.class);
        Log.info(OpenAPIValidationTestOne.class, "setUpTest", "OpenAPI doc:\n" + openApiDoc);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWWKO1650E", "CWWKO1651W");
        }
    }

    @Test
    @SkipForRepeat({
        MicroProfileActions.MP41_ID,
        MicroProfileActions.MP50_ID,
        MicroProfileActions.MP60_ID,
        MicroProfileActions.MP61_ID,
        MicroProfileActions.MP70_EE10_ID,
        MicroProfileActions.MP70_EE11_ID
    })
    public void testInfoValidation() throws Exception {

        assertNotEmpty("The Info Validator should have been triggered by invalid URL",
            server.findStringsInLogs(
                "Message: The Info Object must contain a valid URL. The \"not in URL format\" value specified for \"termsOfService\"*"));
        assertNotEmpty("The Info Validator should have been triggered by missing \"version\" field",
            server.findStringsInLogs(
                "Message: Required \"version\" field is missing or is set to an invalid value, Location: #/info"));
        assertNotEmpty("The Info Validator should have been triggered by missing \"title\" field",
            server.findStringsInLogs(
                "Message: Required \"title\" field is missing or is set to an invalid value, Location: #/info"));
    }

    @Test
    @SkipForRepeat({
        MicroProfileActions.MP22_ID, MicroProfileActions.MP33_ID
    })
    public void testInfoValidation20() throws Exception {

        /*
         * The SmallRye implementation always injects a title and version if one is not
         * present... so remove the relevant assertions when running against the
         * mpOpenAPI-2.0 feature
         */
        assertNotEmpty("The Info Validator should have been triggered by invalid URL",
            server.findStringsInLogs(
                "Message: The Info Object must contain a valid URL. The \"not in URL format\" value specified for \"termsOfService\"*"));
    }

    @Test
    public void testContactValidation() throws Exception {
        assertNotEmpty("The Contact Validator should have been triggered by invalid URL",
            server.findStringsInLogs(
                "Message: The Contact Object must contain a valid URL. The \"not in URL Format\" value specified*"));
        assertNotEmpty("The Contact Validator should have been triggered by invalid email",
            server.findStringsInLogs(
                "Message: The Contact Object must contain a valid email address. The \"not an email\" value*"));
    }

    @Test
    public void testLicenseValidation() throws Exception {
        assertNotEmpty("The License Validator should have been triggered by missing \"name\" field",
            server.findStringsInLogs("Message: Required \"name\" field is missing or is set to an invalid value"));
        assertNotEmpty("The License Validator should have been triggered by invalid URL",
            server.findStringsInLogs("The License Object must contain a valid URL. The \"not in URL format\" value"));
    }

    @Test
    public void testServerValidation() throws Exception {
        assertThat("The Server Validator should have been triggered by invalid URL",
            server.findStringsInLogs("Message: The Server Object must contain a valid URL*"),
            hasSize(4));
        assertNotEmpty("The Server Validator should have been triggered by missing \"url\" field",
            server.findStringsInLogs(
                "Message: Required \"url\" field is missing or is set to an invalid value, Location: #/paths/~1reviews/get/servers"));
        assertNotEmpty("The Server Validator should have been triggered by undefined variable",
            server.findStringsInLogs("The \"extraVariable\" variable in the Server Object is not defined*"));
        assertNotEmpty("The Server Validator should have been triggered by undefined variable",
            server.findStringsInLogs("Message: The \"id\" variable in the Server Object is not defined*"));
    }

    @Test
    public void testServerVariableValidation() throws Exception {
        assertNotEmpty("The Server Variable Validator should have been triggered by a missing \"default\" field",
            server.findStringsInLogs("Message: Required \"default\" field is missing or is set to an invalid value*"));
    }

    @Test
    public void testPathItemValidation() throws Exception {
        assertNotEmpty(
            "The PathItem Validator should have been triggered by teh missing \"required\" field in a path parameter",
            server.findStringsInLogs(
                "The \"id\" path parameter from the \"GET\" operation of the path \"/bookings/\\{id\\}\" does not contain the \"required\" field or its value is not \"true\""));
        assertNotEmpty("The PathItem Validator should have been triggered by an undeclared path parameter",
            server.findStringsInLogs(
                "The \"GET\" operation of the \"/reviews/\\{id\\}\" path does not define a path parameter that is declared: \"id\""));
        assertNotEmpty("The PathItem Validator should have been triggered by an invalid path",
            server.findStringsInLogs(
                "The Path Item Object must contain a valid path. The \"GET\" operation from the \"/reviews/\\{airline\\}\" path defines a duplicated \"path\" parameter: \"airline\""));
        assertNotEmpty("The PathItem Validator should have been triggered by an invalid path",
            server.findStringsInLogs(
                "The Paths Object contains an invalid path. The \"noSlashPath\" path value does not begin with a slash*"));
        assertNotEmpty("The PathItem Validator should have been triggered by an invalid path",
            server.findStringsInLogs(
                "The Path Item Object must contain a valid path. The format of the \"/availability/\"*"));
        assertNotEmpty(
            "The PathItem Validator should have been triggered by teh missing \"required\" field in a path parameter",
            server.findStringsInLogs(
                " The \"userFirstName\" path parameter from the \"GET\" operation of the path \"/operationWithParam\" does not contain the \"required\" field"));
        assertNotEmpty("The PathItem Validator should have been triggered by an invalid path",
            server.findStringsInLogs(
                "The Path Item Object must contain a valid path. The \"/\\{username\\}\" path defines \"3\" path parameters that are not declared: \"\\[pathWithUndeclaredParams, usernameParam, accountNumber\\]\"*"));
        assertNotEmpty("The PathItem Validator should have been triggered by an undeclared path parameter",
            server.findStringsInLogs(
                "The \"GET\" operation from the \"/operationWithParam\" path defines one path parameter that is not declared: \"\\[userFirstName\\]\""));
    }

    @Test
    public void testOperationValidation() throws Exception {
        assertNotEmpty("The Operation Validator should have been triggered by the missing \"responses\" field",
            server.findStringsInLogs(
                "Message: Required \"responses\" field is missing or is set to an invalid value, Location: #/paths/~1/get"));
        assertNotEmpty("The Operation Validator should have been triggered by non-unique operationIDs",
            server.findStringsInLogs(
                "Message: More than one Operation Objects with \"getReviewById\" value for \"operationId\" field was found. The \"operationId\" must be unique"));
    }

    @Test
    public void testExternalDocsValidation() throws Exception {
        assertNotEmpty("The ExternalDocumentation Validator should have been triggered by an invalid URL",
            server.findStringsInLogs(
                "Message: The External Documentation Object must contain a valid URL. The \"not a URL\" value"));
    }

    @Test
    public void testSecurityRequirementValidation() throws Exception {
        assertNotEmpty("The Security Requirement Validator should have been triggered by undeclared Security Scheme",
            server.findStringsInLogs(
                "The \"reviewoauth2\" name provided for the Security Requirement Object does not correspond to a declared security scheme"));
    }

    @Test
    public void testRequestBodyValidation() throws Exception {
        assertNotEmpty("The RequestBody Validator should have been triggered by the missing \"content\" field",
            server.findStringsInLogs(
                "Message: Required \"content\" field is missing or is set to an invalid value, Location: #/paths/~1reviews/post/requestBody"));
    }

    @Test
    public void testResponseValidation() throws Exception {
        assertNotEmpty("The Response Validator should have been triggered by the missing \"description\" field",
            server.findStringsInLogs(
                "Message: Required \"description\" field is missing or is set to an invalid value*"));
    }

    @Test
    public void testResponsesValidation() throws Exception {
        assertNotEmpty(
            "The Responses Validator should have been triggered by missing response code for successful operation",
            server.findStringsInLogs(
                "Message: The Responses Object should contain at least one response code for a successful operation"));
    }

    private void assertNotEmpty(String message,
                                List<String> stringsInLogs) {
        assertThat(message, stringsInLogs, not(empty()));
    }
}
