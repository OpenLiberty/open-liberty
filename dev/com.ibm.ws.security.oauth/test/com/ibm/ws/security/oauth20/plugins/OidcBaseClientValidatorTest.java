/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.ws.security.oauth20.util.OIDCConstants;

/**
 *
 */
public class OidcBaseClientValidatorTest {

    private final String clientId = "b0a376ec4b694b67b6baeb0604a312d8";
    private final String clientSecret = "secret";
    private final String clientName = "client123";
    private final String componentId = "TestComponent";
    private final String redirectUri1 = "https://localhost:8999/resource/redirect1";
    private final String redirectUri2 = "https://localhost:8999/resource/redirect2";
    private final JsonArray redirectUris = new JsonArray();
    private OidcBaseClient client;

    @Before
    public void setUp() {
        redirectUris.add(new JsonPrimitive(redirectUri1));
        redirectUris.add(new JsonPrimitive(redirectUri2));
        client = new OidcBaseClient(clientId, clientSecret, redirectUris, clientName, componentId, true);
    }

    @Test
    public void testClientValidation() {
        JsonArray grantTypes = new JsonArray();
        grantTypes.add(new JsonPrimitive("authorization_code"));
        grantTypes.add(new JsonPrimitive("client_credentials"));
        grantTypes.add(new JsonPrimitive("implicit"));
        grantTypes.add(new JsonPrimitive("refresh_token"));
        grantTypes.add(new JsonPrimitive("urn:ietf:params:oauth:grant-type:jwt-bearer"));
        client.setGrantTypes(grantTypes);

        JsonArray responseTypes = new JsonArray();
        responseTypes.add(new JsonPrimitive("code"));
        responseTypes.add(new JsonPrimitive("token"));
        responseTypes.add(new JsonPrimitive("id_token token"));
        client.setResponseTypes(responseTypes);

        JsonArray postLogoutRedirectUris = new JsonArray();
        postLogoutRedirectUris.add(new JsonPrimitive("https://localhost:9000/logout/"));
        postLogoutRedirectUris.add(new JsonPrimitive("https://localhost:9001/exit/"));
        client.setPostLogoutRedirectUris(postLogoutRedirectUris);

        JsonArray redirectUris = new JsonArray();
        redirectUris.add(new JsonPrimitive("https://localhost:8999/resource/redirect1"));
        redirectUris.add(new JsonPrimitive("https://localhost:9000/resource/redirect2"));
        client.setRedirectUris(redirectUris);

        client.setApplicationType("web");
        client.setSubjectType("public");
        client.setPreAuthorizedScope("openid profile email general");
        client.setTokenEndpointAuthMethod("client_secret_basic");
        client.setScope("openid profile email general");
        client.setIntrospectTokens(true);
        OidcBaseClientValidator validator = OidcBaseClientValidator.getInstance(client);
        try {
            validator.validateCreateUpdate();
        } catch (OidcServerException e) {
            fail("Threw OIDCServerException!");
        }
    }

    @Test
    public void testInvalidAppType() {
        // Set an invalid application type on client
        client.setApplicationType("mobile");
        OidcBaseClientValidator validator = OidcBaseClientValidator.getInstance(client);
        try {
            validator.validateCreateUpdate();
            fail("Did not throw OIDCServerException as expected!");
        } catch (OidcServerException e) {
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getHttpStatus());
            assertEquals(OIDCConstants.ERROR_INVALID_CLIENT_METADATA, e.getErrorCode());
            String expectedErrorMessage = "CWWKS1442E: The value mobile is not a supported value for the application_type client registration metadata field.";
            assertEquals(expectedErrorMessage, e.getErrorDescription());
        }
    }

    @Test
    public void testInvalidResponseType() throws OidcServerException {
        // Set an invalid response type on client
        JsonArray responseTypes = new JsonArray();
        responseTypes.add(new JsonPrimitive("code"));
        responseTypes.add(new JsonPrimitive("token"));
        responseTypes.add(new JsonPrimitive("code_invalid"));
        client.setResponseTypes(responseTypes);
        OidcBaseClientValidator validator = OidcBaseClientValidator.getInstance(client);
        try {
            validator.validateCreateUpdate();
            fail("Did not throw OIDCServerException as expected!");
        } catch (OidcServerException e) {
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getHttpStatus());
            assertEquals(OIDCConstants.ERROR_INVALID_CLIENT_METADATA, e.getErrorCode());
            String expectedErrorMessage = "CWWKS1442E: The value code_invalid is not a supported value for the response_type client registration metadata field.";
            assertEquals(expectedErrorMessage, e.getErrorDescription());
        }
    }

    @Test
    public void testDuplicateResponseType() throws OidcServerException {
        // Set an invalid response type on client
        JsonArray responseTypes = new JsonArray();
        responseTypes.add(new JsonPrimitive("code"));
        responseTypes.add(new JsonPrimitive("token"));
        responseTypes.add(new JsonPrimitive("id_token token"));
        responseTypes.add(new JsonPrimitive("token"));
        client.setResponseTypes(responseTypes);
        OidcBaseClientValidator validator = OidcBaseClientValidator.getInstance(client);
        try {
            validator.validateCreateUpdate();
            fail("Did not throw OIDCServerException as expected!");
        } catch (OidcServerException e) {
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getHttpStatus());
            assertEquals(OIDCConstants.ERROR_INVALID_CLIENT_METADATA, e.getErrorCode());
            String expectedErrorMessage = "CWWKS1443E: The value token is a duplicate for the response_type client registration metadata field.";
            assertEquals(expectedErrorMessage, e.getErrorDescription());
        }
    }

    @Test
    public void testInvalidGrantType() throws OidcServerException {
        // Set an invalid grant type on client
        JsonArray grantTypes = new JsonArray();
        grantTypes.add(new JsonPrimitive("authorization_code"));
        grantTypes.add(new JsonPrimitive("implicit"));
        grantTypes.add(new JsonPrimitive("server_credentials"));// invalid grant type
        client.setGrantTypes(grantTypes);
        OidcBaseClientValidator validator = OidcBaseClientValidator.getInstance(client);
        try {
            validator.validateCreateUpdate();
            fail("Did not throw OIDCServerException as expected!");
        } catch (OidcServerException e) {
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getHttpStatus());
            assertEquals(OIDCConstants.ERROR_INVALID_CLIENT_METADATA, e.getErrorCode());
            String expectedErrorMessage = "CWWKS1442E: The value server_credentials is not a supported value for the grant_type client registration metadata field.";
            assertEquals(expectedErrorMessage, e.getErrorDescription());
        }
    }

    @Test
    public void testDuplicateGrantType() throws OidcServerException {
        // Set an invalid grant type on client
        JsonArray grantTypes = new JsonArray();
        grantTypes.add(new JsonPrimitive("authorization_code"));
        grantTypes.add(new JsonPrimitive("implicit"));
        grantTypes.add(new JsonPrimitive("authorization_code"));// invalid grant type
        client.setGrantTypes(grantTypes);
        OidcBaseClientValidator validator = OidcBaseClientValidator.getInstance(client);
        try {
            validator.validateCreateUpdate();
            fail("Did not throw OIDCServerException as expected!");
        } catch (OidcServerException e) {
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getHttpStatus());
            assertEquals(OIDCConstants.ERROR_INVALID_CLIENT_METADATA, e.getErrorCode());
            String expectedErrorMessage = "CWWKS1443E: The value authorization_code is a duplicate for the grant_type client registration metadata field.";
            assertEquals(expectedErrorMessage, e.getErrorDescription());
        }
    }

    @Test
    public void testInvalidResponseAndGrantMatch() throws OidcServerException {
        JsonArray responseTypes = new JsonArray();
        responseTypes.add(new JsonPrimitive("code"));
        client.setResponseTypes(responseTypes);
        // Set an invalid grant type on client
        JsonArray grantTypes = new JsonArray();
        grantTypes.add(new JsonPrimitive("implicit"));
        client.setGrantTypes(grantTypes);
        OidcBaseClientValidator validator = OidcBaseClientValidator.getInstance(client);
        try {
            validator.validateCreateUpdate();
            fail("Did not throw OIDCServerException as expected!");
        } catch (OidcServerException e) {
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getHttpStatus());
            assertEquals(OIDCConstants.ERROR_INVALID_CLIENT_METADATA, e.getErrorCode());
            String expectedErrorMessage = "CWWKS1444E: The client registration metadata field response_type contains value code, which requires at least a matching grant_type value authorization_code.";
            assertEquals(expectedErrorMessage, e.getErrorDescription());
        }
    }

    @Test
    public void testInvalidResponseAndGrantMatch2() throws OidcServerException {
        JsonArray responseTypes = new JsonArray();
        responseTypes.add(new JsonPrimitive("id_token token"));
        client.setResponseTypes(responseTypes);
        // Set an invalid grant type on client
        JsonArray grantTypes = new JsonArray();
        grantTypes.add(new JsonPrimitive("authorization_code"));
        client.setGrantTypes(grantTypes);
        OidcBaseClientValidator validator = OidcBaseClientValidator.getInstance(client);
        try {
            validator.validateCreateUpdate();
            fail("Did not throw OIDCServerException as expected!");
        } catch (OidcServerException e) {
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getHttpStatus());
            assertEquals(OIDCConstants.ERROR_INVALID_CLIENT_METADATA, e.getErrorCode());
            String expectedErrorMessage = "CWWKS1444E: The client registration metadata field response_type contains value id_token token, which requires at least a matching grant_type value implicit.";
            assertEquals(expectedErrorMessage, e.getErrorDescription());
        }
    }

    @Test
    public void testMalformedRedirectURIs() throws OidcServerException {
        JsonArray redirectUris = new JsonArray();
        redirectUris.add(new JsonPrimitive("http://finance.yahoo.com/q/h?s=^IXIC"));// invalid URI
        client.setRedirectUris(redirectUris);
        OidcBaseClientValidator validator = OidcBaseClientValidator.getInstance(client);
        try {
            validator.validateCreateUpdate();
            fail("Did not throw OIDCServerException as expected!");
        } catch (OidcServerException e) {
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getHttpStatus());
            assertEquals(OIDCConstants.ERROR_INVALID_REDIRECT_URI, e.getErrorCode());
            String expectedErrorMessage = "CWWKS1445E: The value http://finance.yahoo.com/q/h?s=^IXIC for the client registration metadata field redirect_uris contains a malformed URI syntax.";
            assertEquals(expectedErrorMessage, e.getErrorDescription());
        }
    }

    @Test
    public void testNonAbsoluteRedirectURIs() throws OidcServerException {
        JsonArray redirectUris = new JsonArray();
        redirectUris.add(new JsonPrimitive("../finance.yahoo.com/q/h?s=time"));// invalid URI
        client.setRedirectUris(redirectUris);
        client.setApplicationType("web");
        OidcBaseClientValidator validator = OidcBaseClientValidator.getInstance(client);
        try {
            validator.validateCreateUpdate();
            fail("Did not throw OIDCServerException as expected!");
        } catch (OidcServerException e) {
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getHttpStatus());
            assertEquals(OIDCConstants.ERROR_INVALID_REDIRECT_URI, e.getErrorCode());
            String expectedErrorMessage = "CWWKS1446E: The value ../finance.yahoo.com/q/h?s=time for the client registration metadata field redirect_uris is not an absolute URI.";
            assertEquals(expectedErrorMessage, e.getErrorDescription());
        }
    }

    @Test
    public void testDuplicateRedirectURIs() throws OidcServerException {
        JsonArray redirectUris = new JsonArray();
        redirectUris.add(new JsonPrimitive("http://finance.yahoo.com/q/h"));
        redirectUris.add(new JsonPrimitive("http://finance.yahoo.com/q/h"));
        client.setRedirectUris(redirectUris);
        OidcBaseClientValidator validator = OidcBaseClientValidator.getInstance(client);
        try {
            validator.validateCreateUpdate();
            fail("Did not throw OIDCServerException as expected!");
        } catch (OidcServerException e) {
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getHttpStatus());
            assertEquals(OIDCConstants.ERROR_INVALID_CLIENT_METADATA, e.getErrorCode());
            String expectedErrorMessage = "CWWKS1443E: The value http://finance.yahoo.com/q/h is a duplicate for the redirect_uris client registration metadata field.";
            assertEquals(expectedErrorMessage, e.getErrorDescription());
        }
    }

    @Test
    public void testOutputParamtersPresent1() throws OidcServerException {
        client.setClientIdIssuedAt(4000);// non zero value
        OidcBaseClientValidator validator = OidcBaseClientValidator.getInstance(client);
        try {
            validator.validateCreateUpdate();
            fail("Did not throw OIDCServerException as expected!");
        } catch (OidcServerException e) {
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getHttpStatus());
            assertEquals(OIDCConstants.ERROR_INVALID_CLIENT_METADATA, e.getErrorCode());
            String expectedErrorMessage = "CWWKS1447E: The client registration metadata field client_id_issued_at cannot be specified for a create or update action because it is an output parameter.";
            assertEquals(expectedErrorMessage, e.getErrorDescription());
        }
    }

    @Test
    public void testOutputParamtersPresent2() throws OidcServerException {
        client.setClientSecretExpiresAt(5000);// non zero value
        OidcBaseClientValidator validator = OidcBaseClientValidator.getInstance(client);
        try {
            validator.validateCreateUpdate();
            fail("Did not throw OIDCServerException as expected!");
        } catch (OidcServerException e) {
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getHttpStatus());
            assertEquals(OIDCConstants.ERROR_INVALID_CLIENT_METADATA, e.getErrorCode());
            String expectedErrorMessage = "CWWKS1447E: The client registration metadata field client_secret_expires_at cannot be specified for a create or update action because it is an output parameter.";
            assertEquals(expectedErrorMessage, e.getErrorDescription());
        }
    }

    @Test
    public void testOutputParamtersPresent3() throws OidcServerException {
        client.setRegistrationClientUri("https://localhost:8999/resource/registration/abcd1234");
        OidcBaseClientValidator validator = OidcBaseClientValidator.getInstance(client);
        try {
            validator.validateCreateUpdate();
            fail("Did not throw OIDCServerException as expected!");
        } catch (OidcServerException e) {
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getHttpStatus());
            assertEquals(OIDCConstants.ERROR_INVALID_CLIENT_METADATA, e.getErrorCode());
            String expectedErrorMessage = "CWWKS1447E: The client registration metadata field registration_client_uri cannot be specified for a create or update action because it is an output parameter.";
            assertEquals(expectedErrorMessage, e.getErrorDescription());
        }
    }
}
