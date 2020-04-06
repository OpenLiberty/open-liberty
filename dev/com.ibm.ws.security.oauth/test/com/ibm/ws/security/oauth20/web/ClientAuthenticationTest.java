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
package com.ibm.ws.security.oauth20.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.security.CustomRegistryException;
import com.ibm.websphere.security.PasswordCheckFailedException;
import com.ibm.websphere.security.UserRegistry;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedTokenCache;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;

import test.common.SharedOutputManager;

/**
 *
 */
public class ClientAuthenticationTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.oauth*=all");

    @Rule
    public TestRule outputRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    public interface MockInterface {
        public com.ibm.websphere.security.UserRegistry mockGetUserRegistry();
    }

    private final HttpServletRequest request = mock.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock.mock(HttpServletResponse.class);
    private final OAuth20Provider provider = mock.mock(OAuth20Provider.class);
    private final OidcOAuth20ClientProvider oocp = mock.mock(OidcOAuth20ClientProvider.class);
    private final OAuth20EnhancedTokenCache tokencache = mock.mock(OAuth20EnhancedTokenCache.class);
    private final OAuth20Token oauthToken = mock.mock(OAuth20Token.class);
    private final OidcBaseClient obc = mock.mock(OidcBaseClient.class);
    private final MockInterface mockInterface = mock.mock(MockInterface.class);
    private final UserRegistry ur = mock.mock(UserRegistry.class);
    private final PrintWriter writer = mock.mock(PrintWriter.class);

    private final static String AUTH_HEADER_NAME = "Authorization";
    private final static String AUTH_HEADER_ENCODING = "Authorization-Encoding";
    private final static String uid = "user1";
    private final static String pw = "security";
    private final static String[] uidArray = new String[] { uid };
    private final static String[] pwArray = new String[] { pw };
    private final static String[] GRANT_TYPE_DOESNT_MATTER = null;

    private final static String BASIC_AUTH_HEADER_VALUE = "Basic dXNlcjE6c2VjdXJpdHk=";
    private final static String BASIC_AUTH_HEADER_VALUE_EMPTY_CLIENT_ID = "Basic OnNlY3VyaXR5";
    private final static String BASIC_AUTH_HEADER_VALUE_EMPTY_CLIENT_SECRET = "Basic dXNlcjE6";
    private final static String BASIC_AUTH_HEADER_NO_CREDS = "Basic ";
    private final static String NOT_BASIC_AUTH = "Testing";
    private final static String AUTH_HEADER_VALUE_NOT_BASIC_AUTH = NOT_BASIC_AUTH + " dXNlcjE6c2VjdXJpdHk=";
    private final static String uri = "/test/endpoint";
    private final static String grantType = "authorization_code";

    // Test case name note: Unless listed otherwise in the name, tests are assumed to not use basic auth, grant type doesn't matter,
    // use a valid client_id/username, use a valid client_secret/password, allow public clients, treated as a confidential client,
    // client is valid, and verify() returns true.

    @After
    public void tearDown() throws Exception {
        mock.assertIsSatisfied();
    }

    @Rule
    public TestWatcher watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            System.out.println("********* Starting test: " + description.getMethodName());
        }
    };

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.token.
     * 1. Request uses basic auth with no credentials specified.
     * 2. Client authentication will fail because no credentials are provided.
     * Expected result: false, 401 is sent along with CWWKS1406E because this is a token endpoint request.
     */
    @Test
    public void verify_BasicAuth_NoCredentials_401() {
        final String methodName = "verify_BasicAuth_NoCredentials_401";

        EndpointType endpoint = EndpointType.token;

        String description = "CWWKS1406E: The " + endpoint + " request had an invalid client credential. The request URI was " + uri + ".";
        final String authHeaderResponse = createResponseAuthHeader("Basic", Constants.ERROR_CODE_INVALID_CLIENT, description);

        try {
            expectHeader(AUTH_HEADER_NAME, BASIC_AUTH_HEADER_NO_CREDS);
            neverGetClientIdOrClientSecretParams();
            expectGrantType(GRANT_TYPE_DOESNT_MATTER);

            neverSetAuthenticatedClientAttribute();
            expectGetRequestUri();
            expect401(authHeaderResponse);

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.coverage_map.
     * 1. Request uses basic auth with no credentials specified.
     * 2. Client authentication will fail because no credentials are provided.
     * Expected result: false, 401 is sent without an error description because this is a coverage map endpoint request.
     */
    @Test
    public void verify_BasicAuth_NoCredentials_CoverageMapEndpoint_401() {
        final String methodName = "verify_BasicAuth_NoCredentials_CoverageMapEndpoint_401";

        EndpointType endpoint = EndpointType.coverage_map;

        final String authHeaderResponse = createResponseAuthHeader("Basic", Constants.ERROR_CODE_INVALID_CLIENT, null);

        try {
            expectHeader(AUTH_HEADER_NAME, BASIC_AUTH_HEADER_NO_CREDS);
            neverGetClientIdOrClientSecretParams();
            expectGrantType(GRANT_TYPE_DOESNT_MATTER);

            neverSetAuthenticatedClientAttribute();
            expect401(authHeaderResponse);

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.revoke.
     * 1. Request uses basic auth with credentials that include an empty user name.
     * 2. Client authentication will fail because no client id was provided.
     * Expected result: false, 401 is sent along with CWWKS1406E because this is a revoke endpoint request.
     */
    @Test
    public void verify_BasicAuth_EmptyClientId_401() {
        final String methodName = "verify_BasicAuth_EmptyClientId_401";

        EndpointType endpoint = EndpointType.revoke;

        String description = "CWWKS1406E: The " + endpoint + " request had an invalid client credential. The request URI was " + uri + ".";
        final String authHeaderResponse = createResponseAuthHeader("Basic", Constants.ERROR_CODE_INVALID_CLIENT, description);

        try {
            expectHeader(AUTH_HEADER_NAME, BASIC_AUTH_HEADER_VALUE_EMPTY_CLIENT_ID);
            neverGetClientIdOrClientSecretParams();
            expectGrantType(GRANT_TYPE_DOESNT_MATTER);

            neverSetAuthenticatedClientAttribute();
            expectGetRequestUri();
            expect401(authHeaderResponse);

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.jwk.
     * 1. Request uses basic auth with credentials that include an empty user name.
     * 2. Client authentication will fail because no client id was provided.
     * Expected result: false, 401 is sent without an error description because this is a jwk endpoint request.
     */
    @Test
    public void verify_BasicAuth_EmptyClientId_JwkEndpoint_401() {
        final String methodName = "verify_BasicAuth_EmptyClientId_JwkEndpoint_401";

        EndpointType endpoint = EndpointType.jwk;

        final String authHeaderResponse = createResponseAuthHeader("Basic", Constants.ERROR_CODE_INVALID_CLIENT, null);

        try {
            expectHeader(AUTH_HEADER_NAME, BASIC_AUTH_HEADER_VALUE_EMPTY_CLIENT_ID);
            neverGetClientIdOrClientSecretParams();
            expectGrantType(GRANT_TYPE_DOESNT_MATTER);

            neverSetAuthenticatedClientAttribute();
            expect401(authHeaderResponse);

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.token.
     * 1. Request uses basic auth with credentials that include an empty password.
     * 2. Client provider is found.
     * 3. Do not allow public clients.
     * 4. Client validation is successful.
     * Expected result: true
     */
    @Test
    public void verify_BasicAuth_EmptyClientSecret_DisallowPublicClients() {
        final String methodName = "verify_BasicAuth_EmptyClientSecret_DisallowPublicClients";

        EndpointType endpoint = EndpointType.token;

        try {
            expectHeader(AUTH_HEADER_NAME, BASIC_AUTH_HEADER_VALUE_EMPTY_CLIENT_SECRET);
            neverGetClientIdOrClientSecretParams();
            expectGrantType(GRANT_TYPE_DOESNT_MATTER);
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(false);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectConfidentialClient(uid, "", true, true);
            validClientAllDone();

            ClientAuthentication ca = new ClientAuthentication();
            assertTrue(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.token.
     * 1. Request uses basic auth with credentials that include an empty password.
     * 2. Client provider is found.
     * 3. Do not allow public clients.
     * 4. Client validation fails.
     * Expected result: false, 401 is sent along with CWWKS1406E because this is a token endpoint request.
     */
    @Test
    public void verify_BasicAuth_EmptyClientSecret_DisallowPublicClients_Invalid_401() {
        final String methodName = "verify_BasicAuth_EmptyClientSecret_DisallowPublicClients_Invalid_401";

        EndpointType endpoint = EndpointType.token;

        String description = "CWWKS1406E: The " + endpoint + " request had an invalid client credential. The request URI was " + uri + ".";
        final String authHeaderResponse = createResponseAuthHeader("Basic", Constants.ERROR_CODE_INVALID_CLIENT, description);

        try {
            expectHeader(AUTH_HEADER_NAME, BASIC_AUTH_HEADER_VALUE_EMPTY_CLIENT_SECRET);
            neverGetClientIdOrClientSecretParams();
            expectGrantType(GRANT_TYPE_DOESNT_MATTER);
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(false);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectConfidentialClient(uid, "", false, true);

            neverSetAuthenticatedClientAttribute();
            expectGetRequestUri();
            expect401(authHeaderResponse);

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.introspect.
     * 1. Request uses basic auth with valid credentials
     * 2. Grant type is authorization code.
     * 3. Client provider is found.
     * 4. Allow public clients.
     * 5. Treated as confidential client due to non-empty password.
     * 6. Client validation succeeds.
     * Expected result: true
     */
    @Test
    public void verify_BasicAuth_GrantTypeAuthzCode() {
        final String methodName = "verify_BasicAuth_GrantTypeAuthzCode";

        EndpointType endpoint = EndpointType.introspect;

        try {
            expectHeader(AUTH_HEADER_NAME, BASIC_AUTH_HEADER_VALUE);
            neverGetClientIdOrClientSecretParams();
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(false);
            expectClientCfgSpecifiesPublicClient(uid, true);
            expectConfidentialClient(uid, pw, true, true);
            validClientAllDone();

            ClientAuthentication ca = new ClientAuthentication();
            assertTrue(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.authorize.
     * 1. Request uses basic auth with credentials that include an empty password.
     * 2. Grant type is authorization code.
     * 3. Client provider is found.
     * 4. Allow public clients.
     * 5. Treated as public client due to empty password.
     * 6. Client not found.
     * Expected result: false, 401 is sent along with CWWKS1406E because this is an authorize endpoint request.
     */
    @Test
    public void verify_BasicAuth_GrantTypeAuthzCode_EmptyClientSecret_PublicClient_ClientNotFound_401() {
        final String methodName = "verify_BasicAuth_GrantTypeAuthzCode_EmptyClientSecret_PublicClient_ClientNotFound_401";

        EndpointType endpoint = EndpointType.authorize;

        String description = "CWWKS1406E: The " + endpoint + " request had an invalid client credential. The request URI was " + uri + ".";
        final String authHeaderResponse = createResponseAuthHeader("Basic", Constants.ERROR_CODE_INVALID_CLIENT, description);

        try {
            expectHeader(AUTH_HEADER_NAME, BASIC_AUTH_HEADER_VALUE_EMPTY_CLIENT_SECRET);
            neverGetClientIdOrClientSecretParams();
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectPublicClient(uid, false, true);

            neverSetAuthenticatedClientAttribute();
            expectGetRequestUri();
            expect401(authHeaderResponse);

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.introspect.
     * 1. Request uses basic auth with credentials that include an empty password.
     * 2. Grant type is implicit.
     * 3. Client provider is found.
     * 4. Allow public clients.
     * 5. Treated as public client due to empty password.
     * 6. Client validation is successful.
     * Expected result: true
     */
    @Test
    public void verify_BasicAuth_GrantTypeImplicit_EmptyClientSecret_PublicClient() {
        final String methodName = "verify_BasicAuth_GrantTypeImplicit_EmptyClientSecret_PublicClient";

        EndpointType endpoint = EndpointType.introspect;

        try {
            expectHeader(AUTH_HEADER_NAME, BASIC_AUTH_HEADER_VALUE_EMPTY_CLIENT_SECRET);
            neverGetClientIdOrClientSecretParams();
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_IMPLICIT });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(false);
            expectClientCfgSpecifiesPublicClient(uid, true);
            expectPublicClient(uid, true, true);
            validClientAllDone();

            ClientAuthentication ca = new ClientAuthentication();
            assertTrue(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.authorize.
     * 1. Request uses basic auth with valid user id and password.
     * 2. Grant type is client_credentials.
     * 3. Client provider is found.
     * 4. Allow public clients.
     * 5. Treated as confidential client due to non-empty password.
     * 6. Client validation is successful.
     * Expected result: true
     */
    @Test
    public void verify_BasicAuth_GrantTypeClientCredentials() {
        final String methodName = "verify_BasicAuth_GrantTypeClientCredentials";

        EndpointType endpoint = EndpointType.authorize;

        try {
            expectHeader(AUTH_HEADER_NAME, BASIC_AUTH_HEADER_VALUE);
            neverGetClientIdOrClientSecretParams();
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(false);
            expectClientCfgSpecifiesPublicClient(uid, true);
            expectConfidentialClient(uid, pw, true, true);
            validClientAllDone();

            ClientAuthentication ca = new ClientAuthentication();
            assertTrue(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.check_session_iframe.
     * 1. Request uses basic auth with credentials that include an empty password.
     * 2. Grant type is client_credentials.
     * 3. Client provider is found.
     * 4. Allow public clients.
     * 5. Treated as public client due to empty password.
     * 6. Authentication will fail because a public client was used when the grant type requires a confidential client.
     * Expected result: false, 401 is sent along with CWOAU0071E
     */
    @Test
    public void verify_BasicAuth_GrantTypeClientCredentials_EmptyClientSecret_PublicClient_401() {
        final String methodName = "verify_BasicAuth_GrantTypeClientCredentials_EmptyClientSecret_PublicClient_401";

        EndpointType endpoint = EndpointType.check_session_iframe;

        String description = "CWOAU0071E: A public client attempted to access the " + endpoint + " endpoint using the " + OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS
                + " grant type. This grant type can only be used by confidential clients. The client_id is: " + uid;
        final String authHeaderResponse = createResponseAuthHeader("Basic", Constants.ERROR_CODE_INVALID_CLIENT, description);

        try {
            expectHeader(AUTH_HEADER_NAME, BASIC_AUTH_HEADER_VALUE_EMPTY_CLIENT_SECRET);
            neverGetClientIdOrClientSecretParams();
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            neverSetAuthenticatedClientAttribute();
            expect401(authHeaderResponse);

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.token.
     * 1. Request uses basic auth with valid user id and password.
     * 2. Grant type is password.
     * 3. Client provider is found.
     * 4. Allow public clients.
     * 5. Treated as confidential client due to non-empty password.
     * 6. Client validation is successful.
     * 7. Don't skip user validation.
     * 8. Resource owner credentials are invalid.
     * Expected result: false, 401 is sent along with CWOAU0069E because this was a token endpoint request
     */
    @Test
    public void verify_BasicAuth_GrantTypePassword_InvalidResourceOwnerCreds_TokenEndpoint_401() {
        final String methodName = "verify_BasicAuth_GrantTypePassword_InvalidResourceOwnerCreds_TokenEndpoint_401";

        ClientAuthentication testClientAuth = new ClientAuthentication() {
            @Override
            protected UserRegistry getUserRegistry() {
                return mockInterface.mockGetUserRegistry();
            }
        };

        EndpointType endpoint = EndpointType.token;

        String description = "CWOAU0069E: The resource owner could not be verified. Either the resource owner: " + uid + " or password is incorrect.";
        final String authHeaderResponse = createResponseAuthHeader("Basic", Constants.ERROR_CODE_INVALID_CLIENT, description);

        try {
            expectHeader(AUTH_HEADER_NAME, BASIC_AUTH_HEADER_VALUE);
            neverGetClientIdOrClientSecretParams();
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_PASSWORD });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectConfidentialClient(uid, pw, true, true);

            expectSetAuthenticatedClientAttribute();

            expectSkipUserValidation(false);
            // Credentials invalid
            expectStandardCheckPasswordResult(null);
            expectParameter(OAuth20Constants.RESOURCE_OWNER_USERNAME, uid);
            expect401(authHeaderResponse);

            assertFalse(testClientAuth.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.coverage_map.
     * 1. Request uses basic auth with valid user id and password.
     * 2. Grant type is password.
     * 3. Client provider is found.
     * 4. Allow public clients.
     * 5. Treated as confidential client due to non-empty password.
     * 6. Client validation is successful.
     * 7. Don't skip user validation.
     * 8. Resource owner credentials are invalid.
     * Expected result: false, 401 is sent without an error description because this was a coverage_map endpoint request.
     */
    @Test
    public void verify_BasicAuth_GrantTypePassword_InvalidResourceOwnerCreds_CoverageMapEndpoint_401() {
        final String methodName = "verify_BasicAuth_GrantTypePassword_InvalidResourceOwnerCreds_CoverageMapEndpoint_401";

        ClientAuthentication testClientAuth = new ClientAuthentication() {
            @Override
            protected UserRegistry getUserRegistry() {
                return mockInterface.mockGetUserRegistry();
            }
        };

        EndpointType endpoint = EndpointType.coverage_map;

        final String authHeaderResponse = createResponseAuthHeader("Basic", Constants.ERROR_CODE_INVALID_CLIENT, null);

        try {
            expectHeader(AUTH_HEADER_NAME, BASIC_AUTH_HEADER_VALUE);
            neverGetClientIdOrClientSecretParams();
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_PASSWORD });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectConfidentialClient(uid, pw, true, true);

            expectSetAuthenticatedClientAttribute();

            expectSkipUserValidation(false);
            // Credentials invalid
            expectStandardCheckPasswordResult(null);
            expect401(authHeaderResponse);

            assertFalse(testClientAuth.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * 1. Request does not have Authorization header.
     * 2. The client provider is not found.
     * Expected result: false, 400 is sent
     */
    @Test
    public void verify_NoClientProvider_400() {
        final String methodName = "verify_NoClientProvider_400";

        EndpointType endpoint = EndpointType.authorize;

        try {
            expectHeader(AUTH_HEADER_NAME, null);
            expectStandardClientIdAndClientSecretParams();
            expectGrantType(GRANT_TYPE_DOESNT_MATTER);
            expectClientProvider(null);

            neverSetAuthenticatedClientAttribute();
            expect400();

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * 1. Request has an Authorization header.
     * 2. The client provider is not found.
     * Expected result: false, 401 is sent along with CWOAU0070E because an Authorization header was found.
     */
    @Test
    public void verify_NoClientProvider_AuthHeaderPresent_401() {
        final String methodName = "verify_NoClientProvider_AuthHeaderPresent_401";

        EndpointType endpoint = EndpointType.introspect;

        String description = "CWOAU0070E: A client provider was not found for the OAuth provider.";
        final String authHeaderResponse = createResponseAuthHeader("Basic", Constants.ERROR_CODE_INVALID_CLIENT, description);

        try {
            expectHeader(AUTH_HEADER_NAME, BASIC_AUTH_HEADER_VALUE);
            neverGetClientIdOrClientSecretParams();
            expectGrantType(GRANT_TYPE_DOESNT_MATTER);
            expectClientProvider(null);

            neverSetAuthenticatedClientAttribute();
            expect401(authHeaderResponse);

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.authorize.
     * 1. Request does not have Authorization header.
     * 2. Request contains valid user id but NO client secret.
     * 3. Grant type is authorization_code.
     * 4. Client provider is found.
     * 5. Allow public clients.
     * 6. Treated as public client due to missing password.
     * Expected result: true
     */
    @Test
    public void verify_GrantTypeAuthzCode_NoPassword_PublicClient() {
        final String methodName = "verify_GrantTypeAuthzCode_NoPassword_PublicClient";

        EndpointType endpoint = EndpointType.authorize;

        try {
            expectHeader(AUTH_HEADER_NAME, null);
            expectParameterValues(OAuth20Constants.CLIENT_ID, uidArray);
            expectParameterValues(OAuth20Constants.CLIENT_SECRET, null);
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectPublicClient(uid, true, true);
            validClientAllDone();

            ClientAuthentication ca = new ClientAuthentication();
            assertTrue(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.authorize.
     * 1. Request does not have Authorization header.
     * 2. Request contains valid user id and empty client secret.
     * 3. Grant type is authorization_code.
     * 4. Client provider is found.
     * 5. Allow public clients.
     * 6. Treated as public client due to empty password.
     * Expected result: true
     */
    @Test
    public void verify_GrantTypeAuthzCode_EmptyPassword_PublicClient() {
        final String methodName = "verify_GrantTypeAuthzCode_EmptyPassword_PublicClient";

        EndpointType endpoint = EndpointType.authorize;

        try {
            expectHeader(AUTH_HEADER_NAME, null);
            expectParameterValues(OAuth20Constants.CLIENT_ID, uidArray);
            expectParameterValues(OAuth20Constants.CLIENT_SECRET, new String[] { "" });
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectPublicClient(uid, true, true);
            validClientAllDone();

            ClientAuthentication ca = new ClientAuthentication();
            assertTrue(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.authorize.
     * 1. Request does not have Authorization header.
     * 2. Request contains valid user id and empty client secret.
     * 3. Grant type is authorization_code.
     * 4. Client provider is found.
     * 5. Allow public clients.
     * 6. Treated as public client due to empty password.
     * 7. Client is disabled
     * Expected result: false, 400 is sent
     */
    @Test
    public void verify_GrantTypeAuthzCode_EmptyPassword_PublicClient_ClientDisabled() {
        final String methodName = "verify_GrantTypeAuthzCode_EmptyPassword_PublicClient_ClientDisabled";

        EndpointType endpoint = EndpointType.authorize;

        try {
            expectHeader(AUTH_HEADER_NAME, null);
            expectParameterValues(OAuth20Constants.CLIENT_ID, uidArray);
            expectParameterValues(OAuth20Constants.CLIENT_SECRET, new String[] { "" });
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectPublicClient(uid, true, false);

            neverSetAuthenticatedClientAttribute();
            expectGetRequestUri();
            expect400();

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.introspect.
     * 1. Request does not have Authorization header.
     * 2. Request contains valid user id and client secret.
     * 3. Grant type is client_credentials.
     * 4. Client provider is found.
     * 5. Allow public clients.
     * 6. Treated as confidential client due to non-empty password.
     * 7. Client validation is successful.
     * Expected result: true
     */
    @Test
    public void verify_GrantTypeClientCredentials() {
        final String methodName = "verify_GrantTypeClientCredentials";

        EndpointType endpoint = EndpointType.introspect;

        try {
            expectHeader(AUTH_HEADER_NAME, null);
            expectStandardClientIdAndClientSecretParams();
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectConfidentialClient(uid, pw, true, true);
            validClientAllDone();

            ClientAuthentication ca = new ClientAuthentication();
            assertTrue(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.introspect.
     * 1. Request does not have Authorization header.
     * 2. Request contains valid user id and client secret.
     * 3. Grant type is client_credentials.
     * 4. Client provider is found.
     * 5. Allow public clients.
     * 6. Treated as confidential client due to non-empty password.
     * 7. Client validation is successful.
     * 8. Client is disabled
     * Expected result: false, 400 is sent
     */
    @Test
    public void verify_GrantTypeClientCredentials_ClientDisabled() {
        final String methodName = "verify_GrantTypeClientCredentials";

        EndpointType endpoint = EndpointType.introspect;

        try {
            expectHeader(AUTH_HEADER_NAME, null);
            expectStandardClientIdAndClientSecretParams();
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectConfidentialClient(uid, pw, true, false);

            neverSetAuthenticatedClientAttribute();
            expectGetRequestUri();
            expect400();

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.revoke.
     * 1. Request does not have Authorization header.
     * 2. Request contains valid user id and empty client secret.
     * 3. Grant type is client_credentials.
     * 4. Client provider is found.
     * 5. Allow public clients.
     * 6. Treated as public client due to empty password.
     * 7. Authentication will fail because a public client was used when the grant type requires a confidential client.
     * Expected result: false, 400 is sent
     */
    @Test
    public void verify_GrantTypeClientCredentials_EmptyPassword_PublicClient_400() {
        final String methodName = "verify_GrantTypeClientCredentials_EmptyPassword_PublicClient_400";

        EndpointType endpoint = EndpointType.revoke;

        try {
            expectHeader(AUTH_HEADER_NAME, null);
            expectParameterValues(OAuth20Constants.CLIENT_ID, uidArray);
            expectParameterValues(OAuth20Constants.CLIENT_SECRET, new String[] { "" });
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);

            expect400();

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.revoke.
     * 1. Request does not have Authorization header.
     * 2. Request contains valid user id and empty client secret.
     * 3. Client provider is found.
     * 4. Do not allow public clients.
     * 5. Client validation is successful.
     * Expected result: true
     */
    @Test
    public void verify_EmptyPassword_DisallowPublicClients() {
        final String methodName = "verify_EmptyPassword_DisallowPublicClients";

        EndpointType endpoint = EndpointType.revoke;

        try {
            expectHeader(AUTH_HEADER_NAME, null);
            expectParameterValues(OAuth20Constants.CLIENT_ID, uidArray);
            expectParameterValues(OAuth20Constants.CLIENT_SECRET, new String[] { "" });
            expectGrantType(GRANT_TYPE_DOESNT_MATTER);
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(false);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectConfidentialClient(uid, "", true, true);
            validClientAllDone();

            ClientAuthentication ca = new ClientAuthentication();
            assertTrue(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * 1. Request does not have Authorization header.
     * 2. Request contains multiple values for client_id parameter.
     * Expected result: false, 400 is sent
     */
    @Test
    public void verify_DuplicateClientId_400() {
        final String methodName = "verify_DuplicateClientId_400";

        EndpointType endpoint = EndpointType.authorize;

        try {
            expectHeaderOnce(AUTH_HEADER_NAME, null);
            expectParameterValues(OAuth20Constants.CLIENT_SECRET, pwArray);
            expectParameterValues(OAuth20Constants.CLIENT_ID, new String[] { uid, "extra" });
            expect400();

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * 1. Request does not have Authorization header.
     * 2. Request contains multiple values for client_secret parameter.
     * Expected result: false, 400 is sent
     */
    @Test
    public void verify_DuplicateClientSecret_400() {
        final String methodName = "verify_DuplicateClientSecret_400";

        EndpointType endpoint = EndpointType.end_session;

        try {
            expectHeaderOnce(AUTH_HEADER_NAME, null);
            expectParameterValues(OAuth20Constants.CLIENT_SECRET, new String[] { pw, "extra" });
            expect400();

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * 1. Request does not have Authorization header.
     * 2. Request contains valid user id and client secret.
     * 3. Request contains multiple values for grant_type parameter.
     * Expected result: false, 400 is sent
     */
    @Test
    public void verify_DuplicateGrantType_400() {
        final String methodName = "verify_DuplicateGrantType_400";

        EndpointType endpoint = EndpointType.coverage_map;

        try {
            expectHeaderOnce(AUTH_HEADER_NAME, null);
            expectStandardClientIdAndClientSecretParams();
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS, "extra" });
            expect400();

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.token.
     * 1. Request does not have Authorization header.
     * 2. Request contains valid user id and password.
     * 3. Client provider is found.
     * 4. Disallow public clients.
     * 5. Client validation is successful.
     * Expected result: true
     */
    @Test
    public void verify_DisallowPublicClients() {
        final String methodName = "verify_DisallowPublicClients";

        EndpointType endpoint = EndpointType.token;

        try {
            expectHeader(AUTH_HEADER_NAME, null);
            expectStandardClientIdAndClientSecretParams();
            expectGrantType(GRANT_TYPE_DOESNT_MATTER);
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(false);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectConfidentialClient(uid, pw, true, true);
            validClientAllDone();

            ClientAuthentication ca = new ClientAuthentication();
            assertTrue(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.authorize.
     * 1. Request does not have Authorization header.
     * 2. Request contains credentials considered invalid.
     * 3. Client provider is found.
     * 4. Allow public clients.
     * 5. Treated as confidential client due to non-empty password.
     * 6. Client validation fails for user id/password.
     * Expected result: false, 400 is sent
     */
    @Test
    public void verify_InvalidClient_AuthorizeEndpoint_400() {
        final String methodName = "verify_InvalidClient_AuthorizeEndpoint_400";

        EndpointType endpoint = EndpointType.authorize;

        try {
            expectHeader(AUTH_HEADER_NAME, null);
            expectStandardClientIdAndClientSecretParams();
            expectGrantType(GRANT_TYPE_DOESNT_MATTER);
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectConfidentialClient(uid, pw, false, true);

            neverSetAuthenticatedClientAttribute();
            expectGetRequestUri();
            expect400();

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.introspect.
     * 1. Request does not have Authorization header.
     * 2. Request contains credentials considered invalid.
     * 3. Client provider is found.
     * 4. Allow public clients.
     * 5. Treated as confidential client due to non-empty password.
     * 6. Client validation fails for user id/password.
     * Expected result: false, 400 is sent
     */
    @Test
    public void verify_InvalidClient_IntrospectEndpoint_400() {
        final String methodName = "verify_InvalidClient_IntrospectEndpoint_400";

        EndpointType endpoint = EndpointType.introspect;

        try {
            expectHeader(AUTH_HEADER_NAME, null);
            expectStandardClientIdAndClientSecretParams();
            expectGrantType(GRANT_TYPE_DOESNT_MATTER);
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectConfidentialClient(uid, pw, false, true);

            neverSetAuthenticatedClientAttribute();
            expectGetRequestUri();
            expect400();

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.proxy.
     * 1. Request does not have Authorization header.
     * 2. Request contains valid user id and password.
     * 3. Grant type is password.
     * 4. Client provider is found.
     * 5. Allow public clients.
     * 6. Treated as confidential client due to non-empty password.
     * 7. Client validation is successful.
     * 8. Skip user validation.
     * Expected result: true
     */
    @Test
    public void verify_GrantTypePassword_SkipUserValidation() {
        final String methodName = "verify_GrantTypePassword_SkipUserValidation";

        EndpointType endpoint = EndpointType.proxy;

        try {
            expectHeader(AUTH_HEADER_NAME, null);
            expectStandardClientIdAndClientSecretParams();
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_PASSWORD });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectConfidentialClient(uid, pw, true, true);

            expectSetAuthenticatedClientAttribute();
            expectSkipUserValidation(true);
            neverSetStatusCodeOrHeader();

            ClientAuthentication ca = new ClientAuthentication();
            assertTrue(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * verify that when this method detects mismatch between supplied id and
     * security id, it sets a request attribute to the security id so the 
     * token(s) will be built with the correct id.
     */
    @Test
    public void verify_validateResourceOwnerCredentialSetsUserNameAttribute() {
        final String methodName = "verify_validateResourceOwnerCredentialSetsUserNameAttribute";
        ClientAuthentication testClientAuth = new ClientAuthentication() {
            @Override
            protected UserRegistry getUserRegistry() {
                return mockInterface.mockGetUserRegistry();
            }
        };

        try {

            mock.checking(new Expectations() {
                {
                    allowing(provider).isROPCPreferUserSecurityName();
                    will(returnValue(true));
                    allowing(request).getParameterValues(OAuth20Constants.RESOURCE_OWNER_USERNAME);
                    will(returnValue(new String[] { "user" }));
                    allowing(request).getParameterValues(OAuth20Constants.RESOURCE_OWNER_PASSWORD);
                    will(returnValue(new String[] { "password" }));

                    allowing(ur).checkPassword("user", "password");
                    will(returnValue("ok"));
                    allowing(mockInterface).mockGetUserRegistry();
                    will(returnValue(ur));
                    allowing(ur).getUserSecurityName("user");
                    will(returnValue("newuser")); // switch
                    // check that switched value is set as attrib.
                    one(request).setAttribute(OAuth20Constants.RESOURCE_OWNER_OVERRIDDEN_USERNAME, "newuser");

                }
            });

            testClientAuth.validateResourceOwnerCredential(provider, request, response, EndpointType.token);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.discovery.
     * 1. Request does not have Authorization header.
     * 2. Request contains valid user id and password.
     * 3. Grant type is password.
     * 4. Client provider is found.
     * 5. Allow public clients.
     * 6. Treated as confidential client due to non-empty password.
     * 7. Client validation is successful.
     * 8. Don't skip user validation.
     * 9. Resource owner credentials are valid.
     * Expected result: true
     */
    @Test
    public void verify_GrantTypePassword_ValidResourceOwnerCreds() {
        final String methodName = "verify_GrantTypePassword_ValidResourceOwnerCreds";

        ClientAuthentication testClientAuth = new ClientAuthentication() {
            @Override
            protected UserRegistry getUserRegistry() {
                return mockInterface.mockGetUserRegistry();
            }
        };

        EndpointType endpoint = EndpointType.discovery;

        try {
            expectHeader(AUTH_HEADER_NAME, null);
            expectStandardClientIdAndClientSecretParams();
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_PASSWORD });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectConfidentialClient(uid, pw, true, true);

            expectSetAuthenticatedClientAttribute();
            expectSkipUserValidation(false);
            expectStandardCheckPasswordResult(uid);

            neverSetStatusCodeOrHeader();
            expectParameter(OAuth20Constants.RESOURCE_OWNER_USERNAME, uid);
            expectProviderCacheSize(6);
            expectProviderCache(tokencache);
            expectCacheReturnsTokens(uid);
            expectTokenData(uid);
            mock.checking(new Expectations() {
                {
                    allowing(provider).isROPCPreferUserSecurityName();
                    will(returnValue(false));
                }
            });
            assertTrue(testClientAuth.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.token.
     * 1. Request does not have Authorization header.
     * 2. Request contains valid user id and password.
     * 3. Grant type is password.
     * 4. Client provider is found.
     * 5. Allow public clients.
     * 6. Treated as confidential client due to non-empty password.
     * 7. Client validation is successful.
     * 8. Don't skip user validation.
     * 9. Request contains empty username parameter.
     * Expected result: false, 400 is sent
     */
    @Test
    public void verify_GrantTypePassword_EmptyResourceOwnerUsername_400() {
        final String methodName = "verify_GrantTypePassword_EmptyResourceOwnerUsername_400";

        ClientAuthentication testClientAuth = new ClientAuthentication() {
            @Override
            protected UserRegistry getUserRegistry() {
                return mockInterface.mockGetUserRegistry();
            }
        };

        EndpointType endpoint = EndpointType.token;

        try {
            expectHeader(AUTH_HEADER_NAME, null);
            expectStandardClientIdAndClientSecretParams();
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_PASSWORD });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectConfidentialClient(uid, pw, true, true);

            expectSetAuthenticatedClientAttribute();

            expectSkipUserValidation(false);
            expectUserRegistry();
            expectParameterValues(OAuth20Constants.RESOURCE_OWNER_USERNAME, new String[] { "" });
            expect400();

            assertFalse(testClientAuth.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.token.
     * 1. Request does not have Authorization header.
     * 2. Request contains valid user id and password.
     * 3. Grant type is password.
     * 4. Client provider is found.
     * 5. Allow public clients.
     * 6. Treated as confidential client due to non-empty password.
     * 7. Client validation is successful.
     * 8. Don't skip user validation.
     * 9. Request contains empty password parameter.
     * Expected result: false, 400 is sent
     */
    @Test
    public void verify_GrantTypePassword_EmptyResourceOwnerPassword_400() {
        final String methodName = "verify_GrantTypePassword_EmptyResourceOwnerPassword_400";

        ClientAuthentication testClientAuth = new ClientAuthentication() {
            @Override
            protected UserRegistry getUserRegistry() {
                return mockInterface.mockGetUserRegistry();
            }
        };

        EndpointType endpoint = EndpointType.token;

        try {
            expectHeader(AUTH_HEADER_NAME, null);
            expectStandardClientIdAndClientSecretParams();
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_PASSWORD });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectConfidentialClient(uid, pw, true, true);

            expectSetAuthenticatedClientAttribute();

            expectSkipUserValidation(false);
            expectUserRegistry();
            expectParameterValues(OAuth20Constants.RESOURCE_OWNER_USERNAME, uidArray);
            expectParameterValues(OAuth20Constants.RESOURCE_OWNER_PASSWORD, new String[] { "" });
            expect400();

            assertFalse(testClientAuth.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.token.
     * 1. Request does not have Authorization header.
     * 2. Request contains valid user id and password.
     * 3. Grant type is password.
     * 4. Client provider is found.
     * 5. Allow public clients.
     * 6. Treated as confidential client due to non-empty password.
     * 7. Client validation is successful.
     * 8. Don't skip user validation.
     * 9. Resource owner credentials are invalid.
     * Expected result: false, 400 is sent
     */
    @Test
    public void verify_GrantTypePassword_InvalidResourceOwnerCreds_TokenEndpoint_400() {
        final String methodName = "verify_GrantTypePassword_InvalidResourceOwnerCreds_TokenEndpoint_400";

        ClientAuthentication testClientAuth = new ClientAuthentication() {
            @Override
            protected UserRegistry getUserRegistry() {
                return mockInterface.mockGetUserRegistry();
            }
        };

        EndpointType endpoint = EndpointType.token;

        try {
            expectHeader(AUTH_HEADER_NAME, null);
            expectStandardClientIdAndClientSecretParams();
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_PASSWORD });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectConfidentialClient(uid, pw, true, true);

            expectSetAuthenticatedClientAttribute();

            expectSkipUserValidation(false);
            expectStandardCheckPasswordResult(null);
            // Get username for CWOAU0069E message
            expectParameter(OAuth20Constants.RESOURCE_OWNER_USERNAME, uid);
            expect400();

            assertFalse(testClientAuth.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.end_session.
     * 1. Request does not have Authorization header.
     * 2. Request contains valid user id and password.
     * 3. Grant type is password.
     * 4. Client provider is found.
     * 5. Allow public clients.
     * 6. Treated as confidential client due to non-empty password.
     * 7. Client validation is successful.
     * 8. Don't skip user validation.
     * 9. Resource owner credentials are invalid.
     * Expected result: false, 401 is sent without an error description because this is an end_session endpoint request.
     */
    @Test
    public void verify_GrantTypePassword_InvalidResourceOwnerCreds_EndSessionEndpoint_401() {
        final String methodName = "verify_GrantTypePassword_InvalidResourceOwnerCreds_EndSessionEndpoint_401";

        ClientAuthentication testClientAuth = new ClientAuthentication() {
            @Override
            protected UserRegistry getUserRegistry() {
                return mockInterface.mockGetUserRegistry();
            }
        };

        EndpointType endpoint = EndpointType.end_session;

        final String authHeaderResponse = createResponseAuthHeader("Basic", Constants.ERROR_CODE_INVALID_CLIENT, null);

        try {
            expectHeader(AUTH_HEADER_NAME, null);
            expectStandardClientIdAndClientSecretParams();
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_PASSWORD });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectConfidentialClient(uid, pw, true, true);

            expectSetAuthenticatedClientAttribute();

            expectSkipUserValidation(false);
            expectStandardCheckPasswordResult(null);
            expect401(authHeaderResponse);

            assertFalse(testClientAuth.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * 1. Request does not have Authorization header.
     * 2. Request contains valid user id and password.
     * 3. Grant type is password.
     * 4. Client provider is found.
     * 5. Allow public clients.
     * 6. Treated as confidential client due to non-empty password.
     * 7. Client validation is successful.
     * 8. Don't skip user validation.
     * 9. Request contains multiple values for username parameter.
     * Expected result: false, 400 is sent
     */
    @Test
    public void verify_DuplicateResourceOwnerName_400() {
        final String methodName = "verify_DuplicateResourceOwnerName_400";

        ClientAuthentication testClientAuth = new ClientAuthentication() {
            @Override
            protected UserRegistry getUserRegistry() {
                return mockInterface.mockGetUserRegistry();
            }
        };

        EndpointType endpoint = EndpointType.token;

        try {
            expectHeader(AUTH_HEADER_NAME, null);
            expectStandardClientIdAndClientSecretParams();
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_PASSWORD });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectConfidentialClient(uid, pw, true, true);

            expectSetAuthenticatedClientAttribute();

            expectSkipUserValidation(false);
            expectUserRegistry();
            expectParameterValues(OAuth20Constants.RESOURCE_OWNER_USERNAME, new String[] { uid, "extra" });
            expect400();

            assertFalse(testClientAuth.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.introspect.
     * 1. Request has Authorization header with an authentication scheme other than Basic.
     * 2. Request contains credentials considered invalid.
     * 3. Client provider is found.
     * 4. Allow public clients.
     * 5. Treated as confidential client due to non-empty password.
     * 6. Client validation fails for user id/password.
     * Expected result: false, 401 is sent along with CWWKS1406E because an Authorization header was found.
     */
    @Test
    public void verify_NonBasicAuth_InvalidClient_IntrospectEndpoint_401() {
        final String methodName = "verify_NonBasicAuth_InvalidClient_IntrospectEndpoint_401";

        EndpointType endpoint = EndpointType.introspect;

        String description = "CWWKS1406E: The " + endpoint + " request had an invalid client credential. The request URI was " + uri + ".";
        final String authHeaderResponse = createResponseAuthHeader(NOT_BASIC_AUTH, Constants.ERROR_CODE_INVALID_CLIENT, description);

        try {
            expectHeader(AUTH_HEADER_NAME, AUTH_HEADER_VALUE_NOT_BASIC_AUTH);
            expectStandardClientIdAndClientSecretParams();
            expectGrantType(GRANT_TYPE_DOESNT_MATTER);
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectConfidentialClient(uid, pw, false, true);

            neverSetAuthenticatedClientAttribute();

            expectGetRequestUri();
            expect401(authHeaderResponse);

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * 1. Request has Authorization header with a different authentication scheme than Basic.
     * 2. Request contains empty client_id parameter.
     * Expected result: false, 400 is sent due to missing client_id
     */
    @Test
    public void verify_NonBasicAuth_EmptyClientId_400() {
        final String methodName = "verify_NonBasicAuth_EmptyClientId_400";

        EndpointType endpoint = EndpointType.revoke;

        try {
            expectHeader(AUTH_HEADER_NAME, AUTH_HEADER_VALUE_NOT_BASIC_AUTH);
            expectParameterValues(OAuth20Constants.CLIENT_ID, new String[] { "" });
            expectParameterValues(OAuth20Constants.CLIENT_SECRET, pwArray);
            expectGrantType(GRANT_TYPE_DOESNT_MATTER);
            neverSetAuthenticatedClientAttribute();
            expectGetRequestUri();
            expect400();

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * 1. Request has Authorization header with a different authentication scheme than Basic.
     * 2. Request is missing client_id.
     * Expected result: false, 400 is sent due to missing client_id
     */
    @Test
    public void verify_NonBasicAuth_MissingClientId_400() {
        final String methodName = "verify_NonBasicAuth_MissingClientId_400";

        EndpointType endpoint = EndpointType.revoke;

        try {
            expectHeader(AUTH_HEADER_NAME, AUTH_HEADER_VALUE_NOT_BASIC_AUTH);
            expectParameterValues(OAuth20Constants.CLIENT_ID, null);
            expectParameterValues(OAuth20Constants.CLIENT_SECRET, pwArray);
            expectGrantType(GRANT_TYPE_DOESNT_MATTER);
            neverSetAuthenticatedClientAttribute();
            expectGetRequestUri();
            expect400();

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.token.
     * 1. Request has Authorization header with an authentication scheme other than Basic.
     * 2. Request contains valid user id but NO client secret.
     * 3. Grant type is authorization_code.
     * 4. Client provider is found.
     * 5. Allow public clients.
     * 6. Treated as public client due to empty password.
     * Expected result: true
     */
    @Test
    public void verify_NonBasicAuth_GrantTypeAuthzCode_MissingClientSecret() {
        final String methodName = "verify_NonBasicAuth_GrantTypeAuthzCode_MissingClientSecret";

        EndpointType endpoint = EndpointType.token;

        try {
            expectHeader(AUTH_HEADER_NAME, AUTH_HEADER_VALUE_NOT_BASIC_AUTH);
            expectParameterValues(OAuth20Constants.CLIENT_ID, uidArray);
            expectParameterValues(OAuth20Constants.CLIENT_SECRET, null);
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectPublicClient(uid, true, true);
            validClientAllDone();

            ClientAuthentication ca = new ClientAuthentication();
            assertTrue(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.revoke.
     * 1. Request has Authorization header with an authentication scheme other than Basic.
     * 2. Request contains valid user id but NO client secret.
     * 3. Grant type is jwt-bearer.
     * 4. Client provider is found.
     * 5. Allow public clients.
     * 6. Treated as public client due to empty password.
     * 7. Authentication will fail because a public client was used when the grant type requires a confidential client.
     * Expected result: false, 401 is sent along with CWOAU0071E because an Authorization header was present
     */
    @Test
    public void verify_NonBasicAuth_GrantTypeJwt_MissingClientSecret_InvalidClient_RevokeEndpoint_401() {
        final String methodName = "verify_NonBasicAuth_GrantTypeJwt_MissingClientSecret_InvalidClient_RevokeEndpoint_401";

        EndpointType endpoint = EndpointType.revoke;

        String description = "CWOAU0071E: A public client attempted to access the " + endpoint + " endpoint using the " + OAuth20Constants.GRANT_TYPE_JWT
                + " grant type. This grant type can only be used by confidential clients. The client_id is: " + uid;
        final String authHeaderResponse = createResponseAuthHeader(NOT_BASIC_AUTH, Constants.ERROR_CODE_INVALID_CLIENT, description);

        try {
            expectHeader(AUTH_HEADER_NAME, AUTH_HEADER_VALUE_NOT_BASIC_AUTH);
            expectParameterValues(OAuth20Constants.CLIENT_ID, uidArray);
            expectParameterValues(OAuth20Constants.CLIENT_SECRET, null);
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_JWT });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);

            neverSetAuthenticatedClientAttribute();
            expect401(authHeaderResponse);

            ClientAuthentication ca = new ClientAuthentication();
            assertFalse(ca.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests verify method with following conditions:
     * - EndpointType is EndpointType.userinfo.
     * 1. Request has Authorization header with an authentication scheme other than Basic.
     * 2. Grant type is password.
     * 3. Client provider is found.
     * 4. Allow public clients.
     * 5. Treated as confidential client due to non-empty password.
     * 6. Client validation is successful.
     * 7. Don't skip user validation.
     * 8. Resource owner credentials are invalid.
     * Expected result: false, 401 is sent because this was a userinfo endpoint request.
     */
    @Test
    public void verify_NonBasicAuth_GrantTypePassword_InvalidResourceOwnerCreds_UserInfoEndpoint_401() {
        final String methodName = "verify_NonBasicAuth_GrantTypePassword_InvalidResourceOwnerCreds_UserInfoEndpoint_401";

        ClientAuthentication testClientAuth = new ClientAuthentication() {
            @Override
            protected UserRegistry getUserRegistry() {
                return mockInterface.mockGetUserRegistry();
            }
        };

        EndpointType endpoint = EndpointType.userinfo;

        final String authHeaderResponse = createResponseAuthHeader(NOT_BASIC_AUTH, Constants.ERROR_CODE_INVALID_CLIENT, null);

        try {
            expectHeader(AUTH_HEADER_NAME, AUTH_HEADER_VALUE_NOT_BASIC_AUTH);
            expectStandardClientIdAndClientSecretParams();
            expectGrantType(new String[] { OAuth20Constants.GRANT_TYPE_PASSWORD });
            expectClientProvider(oocp);
            expectAllowPublicClientsAndAppPasswordCheck(true);
            expectClientCfgSpecifiesPublicClient(uid, false);
            expectConfidentialClient(uid, pw, true, true);
            expectSetAuthenticatedClientAttribute();
            expectSkipUserValidation(false);

            expectStandardCheckPasswordResult(null);
            expect401(authHeaderResponse);

            assertFalse(testClientAuth.verify(provider, request, response, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Creates the Authorization header for the response in the same format as the WebUtils.java product code.
     *
     * @param authScheme
     * @param errorCode
     * @param errorDescription
     * @return
     */
    private String createResponseAuthHeader(String authScheme, String errorCode, String errorDescription) {
        String error = "error";
        String error_description = "error_description";
        final String responseAuthHeader = authScheme + " " + error + "=\"" + errorCode + "\", " +
                error_description + "=\"" + errorDescription + "\", realm=\"\"";
        return responseAuthHeader;
    }

    private void expectHeader(final String headerName, final String headerValue) {
        mock.checking(new Expectations() {
            {
                allowing(response).isCommitted();
                will(returnValue(false));

                exactly(2).of(request).getHeader(headerName);
                will(returnValue(headerValue));
            }
        });
        if (headerValue != null && headerValue.startsWith("Basic")) {
            // Basic auth header will also result in getting the header encoding
            mock.checking(new Expectations() {
                {
                    atMost(1).of(request).getHeader(AUTH_HEADER_ENCODING);
                    will(returnValue("ISO-8859-1"));
                }
            });
        }
    }

    private void expectHeaderOnce(final String headerName, final String headerValue) {
        mock.checking(new Expectations() {
            {
                one(request).getHeader(headerName);
                will(returnValue(headerValue));
            }
        });
        if (headerValue != null && headerValue.startsWith("Basic")) {
            // Basic auth header will also result in getting the header encoding
            mock.checking(new Expectations() {
                {
                    one(request).getHeader(AUTH_HEADER_ENCODING);
                    will(returnValue("ISO-8859-1"));
                }
            });
        }
    }

    private void expectStandardClientIdAndClientSecretParams() {
        expectParameterValues(OAuth20Constants.CLIENT_ID, uidArray);
        expectParameterValues(OAuth20Constants.CLIENT_SECRET, pwArray);
    }

    private void neverGetClientIdOrClientSecretParams() {
        mock.checking(new Expectations() {
            {
                never(request).getParameterValues(OAuth20Constants.CLIENT_ID);
                never(request).getParameterValues(OAuth20Constants.CLIENT_SECRET);
            }
        });
    }

    private void expectGrantType(final String[] grantType) {
        mock.checking(new Expectations() {
            {
                one(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                will(returnValue(grantType));
            }
        });
    }

    private void expectSetAuthenticatedClientAttribute() {
        mock.checking(new Expectations() {
            {
                one(request).setAttribute("authenticatedClient", uid);
            }
        });
    }

    private void neverSetAuthenticatedClientAttribute() {
        mock.checking(new Expectations() {
            {
                never(request).setAttribute("authenticatedClient", uid);
            }
        });
    }

    private void expectGetRequestUri() {
        mock.checking(new Expectations() {
            {
                one(request).getRequestURI();
                will(returnValue(uri));
            }
        });
    }

    private void expectClientProvider(final OidcOAuth20ClientProvider oocp) {
        mock.checking(new Expectations() {
            {
                one(provider).getClientProvider();
                will(returnValue(oocp));
            }
        });
    }

    private void expectProviderCacheSize(final long tokencount) {
        mock.checking(new Expectations() {
            {
                allowing(provider).getClientTokenCacheSize();
                will(returnValue(tokencount));
            }
        });
    }

    private void expectProviderCache(final OAuth20EnhancedTokenCache tokenCache) {
        mock.checking(new Expectations() {
            {
                allowing(provider).getTokenCache();
                will(returnValue(tokenCache));
            }
        });
    }

    /**
     * @param uid
     */
    private void expectCacheReturnsTokens(final String uid) {
        final Collection<OAuth20Token> oauthTokens = new ArrayList<OAuth20Token>();
        oauthTokens.add(oauthToken);
        mock.checking(new Expectations() {
            {
                allowing(tokencache).getUserAndClientTokens(uid, uid);
                will(returnValue(oauthTokens));
            }
        });

    }

    private void expectTokenData(final String uid) {

        mock.checking(new Expectations() {
            {
                allowing(oauthToken).getClientId();
                will(returnValue(uid));
                allowing(oauthToken).getGrantType();
                will(returnValue(grantType));
            }
        });

    }

    private void expectAllowPublicClientsAndAppPasswordCheck(final boolean allowPublicClients) {
        mock.checking(new Expectations() {
            {
                one(provider).isAllowPublicClients();
                will(returnValue(allowPublicClients));
                allowing(provider).isPasswordGrantRequiresAppPassword();
                allowing(provider).getClientProvider();
            }
        });
    }

    private void expectClientCfgSpecifiesPublicClient(final String username, final boolean isPublic) throws OidcServerException {
        mock.checking(new Expectations() {
            {
                one(oocp).get(username);
                will(returnValue(obc));
                one(obc).isPublicClient();
                will(returnValue(isPublic));
            }
        });
    }

    private void expectConfidentialClient(final String username, final String password, final boolean isValid, final boolean isEnabled) throws OidcServerException {
        mock.checking(new Expectations() {
            {
                one(oocp).validateClient(username, password);
                will(returnValue(isValid));
            }
        });
        if (isValid) {
            mock.checking(new Expectations() {
                {
                    one(oocp).get(username);
                    will(returnValue(obc));
                    one(obc).isEnabled();
                    will(returnValue(isEnabled));
                }
            });
        }
    }

    private void neverSetStatusCodeOrHeader() {
        mock.checking(new Expectations() {
            {
                never(response).setStatus(with(any(int.class)));
                never(response).setHeader(with(any(String.class)), with(any(String.class)));
            }
        });
    }

    private void expectPublicClient(final String username, final boolean exists, final boolean isEnabled) throws OidcServerException {
        mock.checking(new Expectations() {
            {
                one(oocp).exists(username);
                will(returnValue(exists));
            }
        });
        if (exists) {
            mock.checking(new Expectations() {
                {
                    one(oocp).get(username);
                    will(returnValue(obc));
                    one(obc).isEnabled();
                    will(returnValue(isEnabled));
                }
            });
        }
    }

    private void expectParameterValues(final String parameter, final String[] value) {
        mock.checking(new Expectations() {
            {
                one(request).getParameterValues(parameter);
                will(returnValue(value));
            }
        });
    }

    private void expectCheckPassword(final String username, final String password,
            final String result) throws PasswordCheckFailedException, CustomRegistryException, RemoteException {
        mock.checking(new Expectations() {
            {
                one(ur).checkPassword(username, password);
                will(returnValue(result));
            }
        });
    }

    private void expectUserRegistry() {
        mock.checking(new Expectations() {
            {
                one(mockInterface).mockGetUserRegistry();
                will(returnValue(ur));
            }
        });
    }

    private void expectStandardCheckPasswordResult(final String checkPasswordResult) throws PasswordCheckFailedException, CustomRegistryException, RemoteException {
        mock.checking(new Expectations() {
            {
                one(mockInterface).mockGetUserRegistry();
                will(returnValue(ur));
            }
        });
        expectParameterValues(OAuth20Constants.RESOURCE_OWNER_USERNAME, uidArray);
        expectParameterValues(OAuth20Constants.RESOURCE_OWNER_PASSWORD, pwArray);
        expectCheckPassword(uid, pw, checkPasswordResult);
    }

    private void expectParameter(final String parameter, final String value) {
        mock.checking(new Expectations() {
            {
                one(request).getParameter(parameter);
                will(returnValue(value));
            }
        });
    }

    private void expectSkipUserValidation(final boolean skipUserValidation) {
        mock.checking(new Expectations() {
            {
                one(provider).isSkipUserValidation();
                will(returnValue(skipUserValidation));
            }
        });
    }

    private void validClientAllDone() {
        expectSetAuthenticatedClientAttribute();
        neverSetStatusCodeOrHeader();
    }

    private void expect400() throws IOException {
        mock.checking(new Expectations() {
            {
                one(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
                one(response).setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE,
                        OAuth20Constants.HTTP_CONTENT_TYPE_JSON);
                never(response).setHeader(with(Constants.WWW_AUTHENTICATE), with(any(String.class)));
                one(response).getWriter();
                will(returnValue(writer));
                one(writer).write(with(any(String.class)));
                one(writer).flush();
            }
        });
    }

    private void expect401(final String authHeaderResponse) throws IOException {
        mock.checking(new Expectations() {
            {
                one(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                one(response).setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE,
                        OAuth20Constants.HTTP_CONTENT_TYPE_JSON);
                one(response).setHeader(with(Constants.WWW_AUTHENTICATE), with(authHeaderResponse));
                one(response).getWriter();
                will(returnValue(writer));
                one(writer).write(with(any(String.class)));
                one(writer).flush();
            }
        });
    }

}
