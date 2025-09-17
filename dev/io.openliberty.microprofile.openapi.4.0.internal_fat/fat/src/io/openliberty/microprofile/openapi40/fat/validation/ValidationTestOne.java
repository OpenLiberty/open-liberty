/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.fat.validation;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static io.openliberty.microprofile.openapi40.fat.validation.ValidationTestUtils.assertMessage;
import static io.openliberty.microprofile.openapi40.fat.validation.ValidationTestUtils.assertNoMessage;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.openapi40.fat.FATSuite;

/**
 * Does the same validation checks from OpenAPIValidationTestOne but using an OpenAPI 3.1 document
 *
 * Covers: Info, Contact, License, ServerVariable(s), Server(s), PathItem, Operation, ExternalDocumentation,
 * SecurityRequirement, RequestBody, Response, Responses
 */
@RunWith(FATRunner.class)
public class ValidationTestOne extends FATServletClient {

    private static final String SERVER_NAME = "OpenAPIValidationServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "validation.war")
                                   .addAsManifestResource(ValidationTestOne.class.getPackage(), "validation1.yml", "openapi.yml");
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        server.stopServer("CWWKO1650E", // Validation errors found
                          "CWWKO1651W");// Validation warnings found
    }

    @Test
    public void testErrorAndWarningMessages() throws Exception {
        assertMessage(server, "CWWKO1650E"); // Validation errors found
        assertMessage(server, "CWWKO1651W"); // Validation warnings found
    }

    @Test
    public void testInfoValidation() throws Exception {
        assertMessage(server, "Message: The Info Object must contain a valid URL. The \"not in URL format\" value specified for \"termsOfService\"");
    }

    @Test
    public void testContactValidation() throws Exception {
        assertMessage(server, "Message: The Contact Object must contain a valid URL. The \"not in URL Format\" value specified");
        assertMessage(server, "Message: The Contact Object must contain a valid email address. The \"not an email\" value");
    }

    @Test
    public void testServerValidation() throws Exception {
        assertMessage(server, 4, "Message: The Server Object must contain a valid URL");
        assertMessage(server, "Message: Required \"url\" field is missing or is set to an invalid value, Location: #/paths/~1reviews/get/servers");
        assertMessage(server, "The \"extraVariable\" variable in the Server Object is not defined");
        assertMessage(server, "Message: The \"id\" variable in the Server Object is not defined");
    }

    @Test
    public void testServerVariableValidation() throws Exception {
        assertMessage(server, "Message: Required \"default\" field is missing or is set to an invalid value, Location: .*id");
        assertMessage(server, "Message: The \"foo\" value of the \"default\" property is not listed in the \"enum\" array, Location: .*name1");
        assertMessage(server, "Message: The \"enum\" array in the Server Variable Object is empty, Location: .*name2");
    }

    @Test
    public void testPathItemValidation() throws Exception {
        assertMessage(server,
                      "The \"id\" path parameter from the \"GET\" operation of the path \"/bookings/\\{id\\}\" does not contain the \"required\" field or its value is not \"true\"");
        assertMessage(server, "The \"GET\" operation of the \"/reviews/\\{id\\}\" path does not define a path parameter that is declared: \"id\"");
        assertMessage(server,
                      "The Path Item Object must contain a valid path. The \"GET\" operation from the \"/reviews/\\{airline\\}\" path defines a duplicated \"path\" parameter: \"airline\"");
        assertMessage(server, "The Paths Object contains an invalid path. The \"noSlashPath\" path value does not begin with a slash");
        assertMessage(server, "The Path Item Object must contain a valid path. The format of the \"/availability/");
        assertMessage(server, " The \"userFirstName\" path parameter from the \"GET\" operation of the path \"/operationWithParam\" does not contain the \"required\" field");
        assertMessage(server,
                      "The Path Item Object must contain a valid path. The \"/\\{username\\}\" path defines \"3\" path parameters that are not declared: \"\\[pathWithUndeclaredParams, usernameParam, accountNumber\\]\"");
        assertMessage(server, "The \"GET\" operation from the \"/operationWithParam\" path defines one path parameter that is not declared: \"\\[userFirstName\\]\"");
    }

    @Test
    public void testOperationValidation() throws Exception {
        // This is no longer an error in OpenAPI 3.1
        assertNoMessage(server, "Message: Required \"responses\" field is missing or is set to an invalid value, Location: #/paths/~1/get");
        assertMessage(server, "Message: More than one Operation Objects with \"getReviewById\" value for \"operationId\" field was found. The \"operationId\" must be unique");
    }

    @Test
    public void testExternalDocsValidation() throws Exception {
        assertMessage(server, "Message: The External Documentation Object must contain a valid URL. The \"not a URL\" value");
    }

    @Test
    public void testSecurityRequirementValidation() throws Exception {
        assertMessage(server, "The \"reviewoauth2\" name provided for the Security Requirement Object does not correspond to a declared security scheme");
    }

    @Test
    public void testRequestBodyValidation() throws Exception {
        assertMessage(server, "Message: Required \"content\" field is missing or is set to an invalid value, Location: #/paths/~1reviews/post/requestBody");
    }

    @Test
    public void testResponseValidation() throws Exception {
        assertMessage(server, "Message: Required \"description\" field is missing or is set to an invalid value");
    }

    @Test
    public void testResponsesValidation() throws Exception {
        assertMessage(server, "Message: The Responses Object should contain at least one response code for a successful operation");
    }

}
