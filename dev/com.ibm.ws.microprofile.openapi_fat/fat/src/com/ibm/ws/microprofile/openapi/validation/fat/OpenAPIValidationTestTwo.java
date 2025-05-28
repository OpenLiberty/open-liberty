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

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpRequest;

/**
 * Tests to ensure that OpenAPI model validation works, model walker calls appropriate validators,
 * and proper events (errors, warning) are reported.
 *
 * Tests that correct validation messages are provided for the validation errors in the following models:
 *
 * Security Scheme, Security Requirement, OAuth Flow(s), MediaType, Example
 *
 * The app with a static yaml file checks the following conditions for each model:
 * - SecurityScheme: REQUIRED 'type' field, other required fields for each particular type - all validation cases checked
 * - SecurityRequirement: SecurityRequirement is declared, Scopes is present on appropriate types - all validation cases checked
 * - OAuthFlow: REQUIRED 'scopes' field, and valid url - all validation cases checked
 * - OAuthFlows: fields are defined for applicable flows objects - all validation cases checked
 * - MediaType: 'example' and 'examples', encoding not in schema, encoding but null schema - all validation cases checked
 * - Example: 'value' and 'extrenalValue' - all validation cases checked
 *
 */
@RunWith(FATRunner.class)
public class OpenAPIValidationTestTwo {

    @BeforeClass
    public static void setUpTest() throws Exception {
        // set mark
        ValidationSuite.server.setMarkToEndOfLog();
        // deploy app and wait for start
        WebArchive war = ShrinkWrap.create(WebArchive.class, "validationTwo.war")
            .addAsManifestResource(OpenAPIValidationTestTwo.class.getPackage(),
                "validationTestTwo.yaml",
                "openapi.yaml");
        ValidationSuite.deployApp(war);
        // Log OpenAPI doc
        String openApiDoc = new HttpRequest(server, "/openapi").run(String.class);
        Log.info(OpenAPIValidationTestTwo.class, "setUpTest", "OpenAPI doc:\n" + openApiDoc);

        assertThat("Validation errors were reported",
            server.findStringsInLogsUsingMark("CWWKO1650E", server.getDefaultLogFile()), not(empty()));
        assertThat("Validation warnings were reported",
            server.findStringsInLogsUsingMark("CWWKO1651W", server.getDefaultLogFile()), not(empty()));
    }

    @AfterClass
    public static void tearDownTest() throws Exception {
        ValidationSuite.removeApp("validationTwo.war");
    }

    @Test
    public void testSecuritySchemeValidation() throws Exception {
        assertNotEmpty("The SecurityScheme Validator should have been triggered by the missing \"type\" field",
            server.findStringsInLogsUsingMark(
                "Message: Required \"type\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/noType",
                server.getDefaultLogFile()));
        assertNotEmpty(
            "The SecurityScheme Validator should have been triggered by the missing \"openIdConnectUrl\" field",
            server.findStringsInLogsUsingMark(
                "Message: Required \"openIdConnectUrl\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/openIdConnectWithScheme",
                server.getDefaultLogFile()));
        assertNotEmpty("The SecurityScheme Validator should have been triggered by the missing \"scheme\" field",
            server.findStringsInLogsUsingMark(
                "Message: Required \"scheme\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/airlinesHttp",
                server.getDefaultLogFile()));
        assertNotEmpty("The SecurityScheme Validator should have been triggered by the missing \"flows\" field",
            server.findStringsInLogsUsingMark(
                "Message: Required \"flows\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/reviewoauth2",
                server.getDefaultLogFile()));
        assertNotEmpty("The SecurityScheme Validator should have been triggered by the missing \"scheme\" field",
            server.findStringsInLogsUsingMark(
                "Message: Required \"scheme\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/httpWithOpenIdConnectUrl",
                server.getDefaultLogFile()));
        assertNotEmpty("The SecurityScheme Validator should have been triggered by the missing \"name\" field",
            server.findStringsInLogsUsingMark(
                "Message: Required \"name\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/ApiKeyWithScheme",
                server.getDefaultLogFile()));
        assertNotEmpty("The SecurityScheme Validator should have been triggered by the missing \"in\" field",
            server.findStringsInLogsUsingMark(
                "Message: Required \"in\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/ApiKeyWithScheme",
                server.getDefaultLogFile()));
        assertNotEmpty("The SecurityScheme Validator should have been triggered by the missing \"in\" field",
            server.findStringsInLogsUsingMark(
                "Message: Required \"in\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/ApiKeyWithInvalidIn",
                server.getDefaultLogFile()));
        assertNotEmpty("The SecurityScheme Validator should have been triggered by invalid URL",
            server.findStringsInLogsUsingMark(
                "Message: The Security Scheme Object must contain a valid URL. The \"not a URL\" value specified for the URL is not valid*",
                server.getDefaultLogFile()));
        assertNotEmpty("The SecurityScheme Validator should have been triggered by a non-applicable field",
            server.findStringsInLogsUsingMark(
                "Message: The \"scheme\" field with \"openIdConnectWithScheme\" value is not applicable for \"Security Scheme Object\" of \"openIdConnect\" type",
                server.getDefaultLogFile()));
        assertNotEmpty("The SecurityScheme Validator should have been triggered by a non-applicable field",
            server.findStringsInLogsUsingMark(
                "Message: The \"name\" field with \"oauth2WithName\" value is not applicable for \"Security Scheme Object\" of \"oauth2\" type",
                server.getDefaultLogFile()));
        assertNotEmpty("The SecurityScheme Validator should have been triggered by a non-applicable field",
            server.findStringsInLogsUsingMark(
                "Message: The \"openIdConnectUrl\" field with \"http://www.url.com\" value is not applicable for \"Security Scheme Object\" of \"http\" type",
                server.getDefaultLogFile()));
        assertNotEmpty("The SecurityScheme Validator should have been triggered by a non-applicable field",
            server.findStringsInLogsUsingMark(
                "Message: The \"flows\" field is not applicable for \"Security Scheme Object\" of \"http\" type",
                server.getDefaultLogFile()));
    }

    @Test
    public void testSecurityRequirementValidation() throws Exception {
        assertNotEmpty(
            "The SecurityRequirement Validator should have been triggered by SecurityScheme name that does not correspond to declared Security Scheme",
            server.findStringsInLogsUsingMark(
                "Message: The \"schemeNotInComponent\" name provided for the Security Requirement Object does not correspond to a declared security scheme, Location: #/paths/~1availability/get/security",
                server.getDefaultLogFile()));
        assertNotEmpty(
            "The SecurityRequirement Validator should have been triggered by non-empty scope for an http Security Requirement Object",
            server.findStringsInLogsUsingMark(
                "Message: The \"airlinesHttp\" field of Security Requirement Object should be empty, but is: \"\\[write:app, read:app\\]\"",
                server.getDefaultLogFile()));
        assertNotEmpty(
            "The SecurityRequirement Validator should have been triggered by an empty scope for openIdConnect Security Requirement Object",
            server.findStringsInLogsUsingMark(
                "Message: The \"openIdConnectWithScheme\" Security Requirement Object should specify be a list of scope names required for execution",
                server.getDefaultLogFile()));
    }

    @Test
    public void testOAuthFlowValidation() throws Exception {
        assertThat("The OAuthFlow Validator should have been triggered by missing \"scopes\" field",
            server.findStringsInLogsUsingMark(
                "Message: Required \"scopes\" field is missing or is set to an invalid value*",
                server.getDefaultLogFile()),
            hasSize(3));
        assertNotEmpty("The OAuthFlow Validator should have been triggered by invalid URL",
            server.findStringsInLogsUsingMark(
                "Message: The OAuth Flow Object must contain a valid URL. The \"invalid URL example\" value*",
                server.getDefaultLogFile()));
    }

    @Test
    public void testOAuthFlowsValidation() throws Exception {
        assertThat("The OAuthFlows Validator should have been triggered by missing \"tokenUrl\" field",
            server.findStringsInLogsUsingMark(
                "Message: Required \"tokenUrl\" field is missing or is set to an invalid value",
                server.getDefaultLogFile()),
            hasSize(2));
        assertNotEmpty("The OAuthFlows Validator should have been triggered by non applicable field",
            server.findStringsInLogsUsingMark(
                "Message: The \"authorizationUrl\" field with \"https://example.com/api/oauth/dialog\" value is not applicable for \"OAuth Flow Object\" of \"password\" type",
                server.getDefaultLogFile()));
    }

    @Test
    public void testMediaTypeValidation() throws Exception {
        assertThat("The MediaType Validator should have been triggered by non-existant encoding property",
            server.findStringsInLogsUsingMark(
                "Message: The \"nonExistingField\" encoding property specified in the MediaType Object does not exist",
                server.getDefaultLogFile()),
            hasSize(2));
        assertNotEmpty(
            "The MediaType Validator should have been triggered by mutually exclusive \"examples\" and \"example\" fields",
            server.findStringsInLogsUsingMark(
                "Message: The MediaType Object cannot have both \"examples\" and \"example\" fields*",
                server.getDefaultLogFile()));
        assertNotEmpty("The MediaType Validator should have been triggered by null schema",
            server.findStringsInLogsUsingMark(
                "Message: The encoding property specified cannot be validated because the corresponding schema property is null",
                server.getDefaultLogFile()));
    }

    @Test
    public void testExampleValidation() throws Exception {
        assertNotEmpty(
            "The Example Validator should have been triggered by mutually exclusive \"value\" and \"externalValue\" fields",
            server.findStringsInLogsUsingMark(
                "Message: The \"booking\" Example Object specifies both \"value\" and \"externalValue\" fields*",
                server.getDefaultLogFile()));
    }

    private void assertNotEmpty(String message,
                                List<String> stringsInLogs) {
        assertThat(message, stringsInLogs, not(empty()));
    }
}
