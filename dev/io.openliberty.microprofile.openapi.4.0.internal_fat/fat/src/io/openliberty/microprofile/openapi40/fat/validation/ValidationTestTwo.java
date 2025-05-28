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
import io.openliberty.microprofile.openapi40.fat.FATSuite;

/**
 * Test validation rules for SecurityScheme, SecurityRequirement, OAuthFlow, OAuthFlows, MediaType and Example
 * <p>
 * Ported from OpenAPIValidationTestTwo and converted to run on OpenAPI v3.1
 */
@RunWith(FATRunner.class)
public class ValidationTestTwo {
    private static final String SERVER_NAME = "OpenAPIValidationServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "validation2.war")
                                   .addAsManifestResource(ValidationTestTwo.class.getPackage(), "validation2.yml", "openapi.yml");
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        server.stopServer("CWWKO1650E", // Validation errors found
                          "CWWKO1651W");// Validation warnings found
    }

    @Test
    public void testSecuritySchemeValidation() throws Exception {
        assertMessage(server, "Message: Required \"type\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/noType");
        assertMessage(server, "Message: Required \"openIdConnectUrl\" field is missing or is set to an invalid value,"
                              + " Location: #/components/securitySchemes/openIdConnectWithScheme");
        assertMessage(server, "Message: Required \"scheme\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/airlinesHttp");
        assertMessage(server, "Message: Required \"flows\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/reviewoauth2");
        assertMessage(server, "Message: Required \"scheme\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/httpWithOpenIdConnectUrl");
        assertMessage(server, "Message: Required \"name\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/ApiKeyWithScheme");
        assertMessage(server, "Message: Required \"in\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/ApiKeyWithScheme");
        assertMessage(server, "Message: Required \"in\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/ApiKeyWithInvalidIn");
        assertMessage(server, "Message: The Security Scheme Object must contain a valid URL. The \"not a URL\" value specified for the URL is not valid*");
        assertMessage(server, "Message: The \"scheme\" field with \"openIdConnectWithScheme\" value is not applicable for \"Security Scheme Object\" of \"openIdConnect\" type");
        assertMessage(server, "Message: The \"name\" field with \"oauth2WithName\" value is not applicable for \"Security Scheme Object\" of \"oauth2\" type");
        assertMessage(server, "Message: The \"openIdConnectUrl\" field with \"http://www.url.com\" value is not applicable for \"Security Scheme Object\" of \"http\" type");
        assertMessage(server, "Message: The \"flows\" field is not applicable for \"Security Scheme Object\" of \"http\" type");
    }

    @Test
    public void testSecurityRequirementValidation() throws Exception {
        assertMessage(server, "Message: The \"schemeNotInComponent\" name provided for the Security Requirement Object"
                              + " does not correspond to a declared security scheme, Location: #/paths/~1availability/get/security");
        assertMessage(server, "Message: The \"airlinesHttp\" field of Security Requirement Object should be empty, but is: \"\\[write:app, read:app\\]\"");
        assertMessage(server, "Message: The \"openIdConnectWithScheme\" Security Requirement Object should specify be a list of scope names required for execution");
    }

    @Test
    public void testOAuthFlowValidation() throws Exception {
        assertMessage(server, 3, "Message: Required \"scopes\" field is missing or is set to an invalid value");
        assertMessage(server, "Message: The OAuth Flow Object must contain a valid URL. The \"invalid URL example\" value");
    }

    @Test
    public void testOAuthFlowsValidation() throws Exception {
        assertMessage(server, 2, "Message: Required \"tokenUrl\" field is missing or is set to an invalid value");
        assertMessage(server, "Message: The \"authorizationUrl\" field with \"https://example.com/api/oauth/dialog\" value"
                              + " is not applicable for \"OAuth Flow Object\" of \"password\" type");
    }

    @Test
    public void testMediaTypeValidation() throws Exception {
        assertMessage(server, 2, "Message: The \"nonExistingField\" encoding property specified in the MediaType Object does not exist");
        assertMessage(server, "Message: The MediaType Object cannot have both \"examples\" and \"example\" fields");
        assertMessage(server, "Message: The encoding property specified cannot be validated because the corresponding schema property is null");
    }

    @Test
    public void testExampleValidation() throws Exception {
        assertMessage(server, "Message: The \"booking\" Example Object specifies both \"value\" and \"externalValue\" fields");
    }

}
