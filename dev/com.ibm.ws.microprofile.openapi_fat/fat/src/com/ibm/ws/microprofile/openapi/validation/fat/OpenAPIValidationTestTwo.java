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

    @Server("validationServerTwo")
    public static LibertyServer server;
    private static final String OPENAPI_VALIDATION_YAML = "openapi_validation";

    @BeforeClass
    public static void setUpTest() throws Exception {
        HttpUtils.trustAllCertificates();

        server.startServer("OpenAPIValidationTestTwo.log", true);

        server.validateAppLoaded(OPENAPI_VALIDATION_YAML);

        assertNotNull("Web application is not available at /openapi_validation/",
                      server.waitForStringInLog("CWWKT0016I.*/openapi_validation/")); //wait for endpoint to become available
        assertNotNull("CWWKF0011I.* not recieved on relationServer",
                      server.waitForStringInLog("CWWKF0011I.*")); // wait for server is ready to run a smarter planet message
        assertNotNull("The application openapi_validation was processed and an OpenAPI document was produced.",
                      server.waitForStringInLog("CWWKO1660I.*and an OpenAPI document was produced.")); //wait for application to be processed
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWWKO1650E", "CWWKO1651W");
        }
    }

    @Test
    public void testSecuritySchemeValidation() {
        assertNotNull("The SecurityScheme Validator should have been triggered by the missing \"type\" field",
                      server.waitForStringInLog("Message: Required \"type\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/noType"));
        assertNotNull("The SecurityScheme Validator should have been triggered by the missing \"openIdConnectUrl\" field",
                      server.waitForStringInLog("Message: Required \"openIdConnectUrl\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/openIdConnectWithScheme"));
        assertNotNull("The SecurityScheme Validator should have been triggered by the missing \"scheme\" field",
                      server.waitForStringInLog("Message: Required \"scheme\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/airlinesHttp"));
        assertNotNull("The SecurityScheme Validator should have been triggered by the missing \"flows\" field",
                      server.waitForStringInLog("Message: Required \"flows\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/reviewoauth2"));
        assertNotNull("The SecurityScheme Validator should have been triggered by the missing \"scheme\" field",
                      server.waitForStringInLog("Message: Required \"scheme\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/httpWithOpenIdConnectUrl"));
        assertNotNull("The SecurityScheme Validator should have been triggered by the missing \"name\" field",
                      server.waitForStringInLog("Message: Required \"name\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/ApiKeyWithScheme"));
        assertNotNull("The SecurityScheme Validator should have been triggered by the missing \"in\" field",
                      server.waitForStringInLog("Message: Required \"in\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/ApiKeyWithScheme"));
        assertNotNull("The SecurityScheme Validator should have been triggered by the missing \"in\" field",
                      server.waitForStringInLog("Message: Required \"in\" field is missing or is set to an invalid value, Location: #/components/securitySchemes/ApiKeyWithInvalidIn"));
        assertNotNull("The SecurityScheme Validator should have been triggered by invalid URL",
                      server.waitForStringInLog("Message: The Security Scheme Object must contain a valid URL. The \"not a URL\" value specified for the URL is not valid*"));
        assertNotNull("The SecurityScheme Validator should have been triggered by a non-applicable field",
                      server.waitForStringInLog("Message: The \"scheme\" field with \"openIdConnectWithScheme\" value is not applicable for \"Security Scheme Object\" of \"openIdConnect\" type"));
        assertNotNull("The SecurityScheme Validator should have been triggered by a non-applicable field",
                      server.waitForStringInLog("Message: The \"name\" field with \"oauth2WithName\" value is not applicable for \"Security Scheme Object\" of \"oauth2\" type"));
        assertNotNull("The SecurityScheme Validator should have been triggered by a non-applicable field",
                      server.waitForStringInLog("Message: The \"openIdConnectUrl\" field with \"http://www.url.com\" value is not applicable for \"Security Scheme Object\" of \"http\" type"));
        assertNotNull("The SecurityScheme Validator should have been triggered by a non-applicable field",
                      server.waitForStringInLog("Message: The \"flows\" field is not applicable for \"Security Scheme Object\" of \"http\" type"));
    }

    @Test
    public void testSecurityRequirementValidation() {
        assertNotNull("The SecurityRequirement Validator should have been triggered by SecurityScheme name that does not correspond to declared Security Scheme",
                      server.waitForStringInLog("Message: The \"schemeNotInComponent\" name provided for the Security Requirement Object does not correspond to a declared security scheme, Location: #/paths/~1availability/get/security"));
        assertNotNull("The SecurityRequirement Validator should have been triggered by non-empty scope for an http Security Requirement Object",
                      server.waitForStringInLog("Message: The \"airlinesHttp\" field of Security Requirement Object should be empty, but is: \"\\[write:app, read:app\\]\""));
        assertNotNull("The SecurityRequirement Validator should have been triggered by an empty scope for openIdConnect Security Requirement Object",
                      server.waitForStringInLog("Message: The \"openIdConnectWithScheme\" Security Requirement Object should specify be a list of scope names required for execution"));
    }

    @Test
    public void testOAuthFlowValidation() {
        assertNotNull("The OAuthFlow Validator should have been triggered by missing \"scopes\" field",
                      server.waitForMultipleStringsInLog(3, "Message: Required \"scopes\" field is missing or is set to an invalid value*"));
        assertNotNull("The OAuthFlow Validator should have been triggered by invalid URL",
                      server.waitForStringInLog("Message: The OAuth Flow Object must contain a valid URL. The \":invalidUrl-example\" value*"));
    }

    @Test
    public void testOAuthFlowsValidation() {
        assertNotNull("The OAuthFlows Validator should have been triggered by missing \"tokenUrl\" field",
                      server.waitForMultipleStringsInLog(2, "Message: Required \"tokenUrl\" field is missing or is set to an invalid value"));
        assertNotNull("The OAuthFlows Validator should have been triggered by non applicable field",
                      server.waitForStringInLog("Message: The \"authorizationUrl\" field with \"https://example.com/api/oauth/dialog\" value is not applicable for \"OAuth Flow Object\" of \"password\" type"));
    }

    @Test
    public void testMediaTypeValidation() {
        assertNotNull("The MediaType Validator should have been triggered by non-existant encoding property",
                      server.waitForMultipleStringsInLog(2, "Message: The \"nonExistingField\" encoding property specified in the MediaType Object does not exist"));
        assertNotNull("The MediaType Validator should have been triggered by mutually exclusive \"examples\" and \"example\" fields",
                      server.waitForStringInLog("Message: The MediaType Object cannot have both \"examples\" and \"example\" fields*"));
        assertNotNull("The MediaType Validator should have been triggered by null schema",
                      server.waitForStringInLog("Message: The encoding property specified cannot be validated because the corresponding schema property is null"));
    }

    @Test
    public void testExampleValidation() {
        assertNotNull("The Example Validator should have been triggered by mutually exclusive \"value\" and \"externalValue\" fields",
                      server.waitForStringInLog("Message: The \"booking\" Example Object specifies both \"value\" and \"externalValue\" fields*"));
    }
}
