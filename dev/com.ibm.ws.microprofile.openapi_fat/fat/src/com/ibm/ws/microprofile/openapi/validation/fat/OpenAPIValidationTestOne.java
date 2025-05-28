/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.validation.fat;

import static com.ibm.ws.microprofile.openapi.validation.fat.ValidationSuite.server;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.topology.utils.HttpRequest;

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

    @BeforeClass
    public static void setUpTest() throws Exception {
        // set mark
        ValidationSuite.server.setMarkToEndOfLog();
        // deploy app and wait for start
        WebArchive war = ShrinkWrap.create(WebArchive.class, "validationOne.war")
            .addAsManifestResource(OpenAPIValidationTestOne.class.getPackage(),
                "validationTestOne.yaml",
                "openapi.yaml");
        ValidationSuite.deployApp(war);
        // Log OpenAPI doc
        String openApiDoc = new HttpRequest(server, "/openapi").run(String.class);
        Log.info(OpenAPIValidationTestOne.class, "setUpTest", "OpenAPI doc:\n" + openApiDoc);

        assertThat("Validation errors were reported",
            server.findStringsInLogsUsingMark("CWWKO1650E", server.getDefaultLogFile()), not(empty()));
        assertThat("Validation warnings were reported",
            server.findStringsInLogsUsingMark("CWWKO1651W", server.getDefaultLogFile()), not(empty()));
    }

    @AfterClass
    public static void tearDownTest() throws Exception {
        ValidationSuite.removeApp("validationOne.war");
    }

    @Test
    @SkipForRepeat({
        MicroProfileActions.MP41_ID,
        MicroProfileActions.MP50_ID,
        MicroProfileActions.MP60_ID,
        MicroProfileActions.MP61_ID,
        MicroProfileActions.MP70_EE10_ID,
        MicroProfileActions.MP70_EE11_ID,
        MicroProfileActions.MP71_EE10_ID,
        MicroProfileActions.MP71_EE11_ID,
    })
    public void testInfoValidation() throws Exception {

        assertNotEmpty("The Info Validator should have been triggered by invalid URL",
            server.findStringsInLogsUsingMark(
                "Message: The Info Object must contain a valid URL. The \"not in URL format\" value specified for \"termsOfService\"*",
                server.getDefaultLogFile()));
        assertNotEmpty("The Info Validator should have been triggered by missing \"version\" field",
            server.findStringsInLogsUsingMark(
                "Message: Required \"version\" field is missing or is set to an invalid value, Location: #/info",
                server.getDefaultLogFile()));
        assertNotEmpty("The Info Validator should have been triggered by missing \"title\" field",
            server.findStringsInLogsUsingMark(
                "Message: Required \"title\" field is missing or is set to an invalid value, Location: #/info",
                server.getDefaultLogFile()));
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
            server.findStringsInLogsUsingMark(
                "Message: The Info Object must contain a valid URL. The \"not in URL format\" value specified for \"termsOfService\"*",
                server.getDefaultLogFile()));
    }

    @Test
    public void testContactValidation() throws Exception {
        assertNotEmpty("The Contact Validator should have been triggered by invalid URL",
            server.findStringsInLogsUsingMark(
                "Message: The Contact Object must contain a valid URL. The \"not in URL Format\" value specified*",
                server.getDefaultLogFile()));
        assertNotEmpty("The Contact Validator should have been triggered by invalid email",
            server.findStringsInLogsUsingMark(
                "Message: The Contact Object must contain a valid email address. The \"not an email\" value*",
                server.getDefaultLogFile()));
    }

    @Test
    public void testLicenseValidation() throws Exception {
        assertNotEmpty("The License Validator should have been triggered by missing \"name\" field",
            server.findStringsInLogsUsingMark(
                "Message: Required \"name\" field is missing or is set to an invalid value",
                server.getDefaultLogFile()));
        assertNotEmpty("The License Validator should have been triggered by invalid URL",
            server.findStringsInLogsUsingMark(
                "The License Object must contain a valid URL. The \"not in URL format\" value",
                server.getDefaultLogFile()));
    }

    @Test
    public void testServerValidation() throws Exception {
        assertThat("The Server Validator should have been triggered by invalid URL",
            server.findStringsInLogsUsingMark("Message: The Server Object must contain a valid URL*",
                server.getDefaultLogFile()),
            hasSize(4));
        assertNotEmpty("The Server Validator should have been triggered by missing \"url\" field",
            server.findStringsInLogsUsingMark(
                "Message: Required \"url\" field is missing or is set to an invalid value, Location: #/paths/~1reviews/get/servers",
                server.getDefaultLogFile()));
        assertNotEmpty("The Server Validator should have been triggered by undefined variable",
            server.findStringsInLogsUsingMark("The \"extraVariable\" variable in the Server Object is not defined*",
                server.getDefaultLogFile()));
        assertNotEmpty("The Server Validator should have been triggered by undefined variable",
            server.findStringsInLogsUsingMark("Message: The \"id\" variable in the Server Object is not defined*",
                server.getDefaultLogFile()));
    }

    @Test
    public void testServerVariableValidation() throws Exception {
        assertNotEmpty("The Server Variable Validator should have been triggered by a missing \"default\" field",
            server.findStringsInLogsUsingMark(
                "Message: Required \"default\" field is missing or is set to an invalid value*",
                server.getDefaultLogFile()));
    }

    @Test
    public void testPathItemValidation() throws Exception {
        assertNotEmpty(
            "The PathItem Validator should have been triggered by teh missing \"required\" field in a path parameter",
            server.findStringsInLogsUsingMark(
                "The \"id\" path parameter from the \"GET\" operation of the path \"/bookings/\\{id\\}\" does not contain the \"required\" field or its value is not \"true\"",
                server.getDefaultLogFile()));
        assertNotEmpty("The PathItem Validator should have been triggered by an undeclared path parameter",
            server.findStringsInLogsUsingMark(
                "The \"GET\" operation of the \"/reviews/\\{id\\}\" path does not define a path parameter that is declared: \"id\"",
                server.getDefaultLogFile()));
        assertNotEmpty("The PathItem Validator should have been triggered by an invalid path",
            server.findStringsInLogsUsingMark(
                "The Path Item Object must contain a valid path. The \"GET\" operation from the \"/reviews/\\{airline\\}\" path defines a duplicated \"path\" parameter: \"airline\"",
                server.getDefaultLogFile()));
        assertNotEmpty("The PathItem Validator should have been triggered by an invalid path",
            server.findStringsInLogsUsingMark(
                "The Paths Object contains an invalid path. The \"noSlashPath\" path value does not begin with a slash*",
                server.getDefaultLogFile()));
        assertNotEmpty("The PathItem Validator should have been triggered by an invalid path",
            server.findStringsInLogsUsingMark(
                "The Path Item Object must contain a valid path. The format of the \"/availability/\"*",
                server.getDefaultLogFile()));
        assertNotEmpty(
            "The PathItem Validator should have been triggered by teh missing \"required\" field in a path parameter",
            server.findStringsInLogsUsingMark(
                " The \"userFirstName\" path parameter from the \"GET\" operation of the path \"/operationWithParam\" does not contain the \"required\" field",
                server.getDefaultLogFile()));
        assertNotEmpty("The PathItem Validator should have been triggered by an invalid path",
            server.findStringsInLogsUsingMark(
                "The Path Item Object must contain a valid path. The \"/\\{username\\}\" path defines \"3\" path parameters that are not declared: \"\\[pathWithUndeclaredParams, usernameParam, accountNumber\\]\"*",
                server.getDefaultLogFile()));
        assertNotEmpty("The PathItem Validator should have been triggered by an undeclared path parameter",
            server.findStringsInLogsUsingMark(
                "The \"GET\" operation from the \"/operationWithParam\" path defines one path parameter that is not declared: \"\\[userFirstName\\]\"",
                server.getDefaultLogFile()));
    }

    @Test
    public void testOperationValidation() throws Exception {
        assertNotEmpty("The Operation Validator should have been triggered by the missing \"responses\" field",
            server.findStringsInLogsUsingMark(
                "Message: Required \"responses\" field is missing or is set to an invalid value, Location: #/paths/~1/get",
                server.getDefaultLogFile()));
        assertNotEmpty("The Operation Validator should have been triggered by non-unique operationIDs",
            server.findStringsInLogsUsingMark(
                "Message: More than one Operation Objects with \"getReviewById\" value for \"operationId\" field was found. The \"operationId\" must be unique",
                server.getDefaultLogFile()));
    }

    @Test
    public void testExternalDocsValidation() throws Exception {
        assertNotEmpty("The ExternalDocumentation Validator should have been triggered by an invalid URL",
            server.findStringsInLogsUsingMark(
                "Message: The External Documentation Object must contain a valid URL. The \"not a URL\" value",
                server.getDefaultLogFile()));
    }

    @Test
    public void testSecurityRequirementValidation() throws Exception {
        assertNotEmpty("The Security Requirement Validator should have been triggered by undeclared Security Scheme",
            server.findStringsInLogsUsingMark(
                "The \"reviewoauth2\" name provided for the Security Requirement Object does not correspond to a declared security scheme",
                server.getDefaultLogFile()));
    }

    @Test
    public void testRequestBodyValidation() throws Exception {
        assertNotEmpty("The RequestBody Validator should have been triggered by the missing \"content\" field",
            server.findStringsInLogsUsingMark(
                "Message: Required \"content\" field is missing or is set to an invalid value, Location: #/paths/~1reviews/post/requestBody",
                server.getDefaultLogFile()));
    }

    @Test
    public void testResponseValidation() throws Exception {
        assertNotEmpty("The Response Validator should have been triggered by the missing \"description\" field",
            server.findStringsInLogsUsingMark(
                "Message: Required \"description\" field is missing or is set to an invalid value*",
                server.getDefaultLogFile()));
    }

    @Test
    public void testResponsesValidation() throws Exception {
        assertNotEmpty(
            "The Responses Validator should have been triggered by missing response code for successful operation",
            server.findStringsInLogsUsingMark(
                "Message: The Responses Object should contain at least one response code for a successful operation",
                server.getDefaultLogFile()));
    }

    private void assertNotEmpty(String message,
                                List<String> stringsInLogs) {
        assertThat(message, stringsInLogs, not(empty()));
    }
}
