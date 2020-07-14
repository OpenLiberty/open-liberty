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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.Attribute;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20BadParameterFormatException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20DuplicateParameterException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidClientException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidGrantTypeException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidResponseTypeException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidScopeException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.OAuthResultImpl;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClientScopeReducer;
import com.ibm.ws.security.oauth20.util.OIDCConstants;

import test.common.SharedOutputManager;

public class ClientAuthorizationTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule outputRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final HttpServletRequest request = mock.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock.mock(HttpServletResponse.class);
    private final OAuth20Provider provider = mock.mock(OAuth20Provider.class);
    private final Principal principal = mock.mock(Principal.class);
    private final OidcOAuth20ClientProvider oocp = mock.mock(OidcOAuth20ClientProvider.class);
    private final OidcBaseClient obc = mock.mock(OidcBaseClient.class);
    private final OidcBaseClientScopeReducer reducer = mock.mock(OidcBaseClientScopeReducer.class, "reducer");

    private final static String UID = "user1";
    private final static String CID = "client01";
    private final static String[] CID_ARRAY = { CID };
    private final static String INVALID_CLIENT_MSG = "CWOAU0023E: The OAuth service provider could not find the client ";
    private final static String REQUIRED_PARAM_MISSING_MSG = "CWOAU0033E: A required runtime parameter was missing: ";
    private final static String DUPLICATE_PARAM_MSG = "CWOAU0022E: The following OAuth parameter was provided more than once in the request: ";
    private final static String GRANT_TYPE_UNKNOWN = "unknown";

    private final static String URI = "http://localhost/test/endpoint";
    private final static String[] RUA = { URI };
    private final String[] RT_CODE = { OAuth20Constants.RESPONSE_TYPE_CODE };
    private final String[] RT_TOKEN = { OAuth20Constants.RESPONSE_TYPE_TOKEN };
    private final String[] RT_ID_TOKEN = { OIDCConstants.RESPONSE_TYPE_ID_TOKEN };
    private final String SCOPE = "openid";
    private final String INVALID_SCOPE = "invalid_scope";
    private final String STATE = "statevalue";
    private final String[] STATE_ARRAY = { "statevalue" };
    private final String OPTIONAL_PARAMS_NAME = "optional_params";
    private final String OPTIONAL_PARAMS_VALUE = "optional_params_value";
    private final String AUTHZ_NAME = "authz_name";

    final static JsonArray validRedirectUriArray = new JsonArray();
    static {
        validRedirectUriArray.add(new JsonPrimitive(URI));
    }
    final static JsonArray respCode = new JsonArray();
    static {
        respCode.add(new JsonPrimitive(OAuth20Constants.RESPONSE_TYPE_CODE));
    }
    final static JsonArray respToken = new JsonArray();
    static {
        respToken.add(new JsonPrimitive(OAuth20Constants.RESPONSE_TYPE_TOKEN));
    }
    final static JsonArray respIdToken = new JsonArray();
    static {
        respIdToken.add(new JsonPrimitive(OIDCConstants.RESPONSE_TYPE_ID_TOKEN));
    }
    static List<String> clientGrantTypesList = new ArrayList<String>();
    final static JsonArray clientGrantTypes = new JsonArray();
    static {
        clientGrantTypes.add(new JsonPrimitive(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE));
        clientGrantTypes.add(new JsonPrimitive(OAuth20Constants.GRANT_TYPE_PASSWORD));
        clientGrantTypes.add(new JsonPrimitive(OAuth20Constants.GRANT_TYPE_REFRESH_TOKEN));
        clientGrantTypes.add(new JsonPrimitive(OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS));
        clientGrantTypes.add(new JsonPrimitive(OAuth20Constants.GRANT_TYPE_IMPLICIT));
        clientGrantTypes.add(new JsonPrimitive(OAuth20Constants.GRANT_TYPE_JWT));

        // Convert and store in separate list
        for (int i = 0; i < clientGrantTypes.size(); i++) {
            clientGrantTypesList.add(clientGrantTypes.get(i).getAsString());
        }
    }
    static List<String> clientResponseTypesList = new ArrayList<String>();
    final static JsonArray clientResponseTypes = new JsonArray();
    static {
        clientResponseTypes.add(new JsonPrimitive(OAuth20Constants.RESPONSE_TYPE_CODE));
        clientResponseTypes.add(new JsonPrimitive(OAuth20Constants.RESPONSE_TYPE_TOKEN));
        clientResponseTypes.add(new JsonPrimitive(OIDCConstants.RESPONSE_TYPE_ID_TOKEN));

        // Convert and store in separate list
        for (int i = 0; i < clientResponseTypes.size(); i++) {
            clientResponseTypesList.add(clientResponseTypes.get(i).getAsString());
        }
    }
    final String[] reqGrantType0 = new String[] {};
    final String[] reqGrantType1 = new String[] { OAuth20Constants.RESPONSE_TYPE_CODE };
    final String basicClient01 = "Basic Y2xpZW50MDE6c2VjcmV0";

    @Rule
    public TestName name = new TestName();

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void starting(org.junit.runner.Description desc) {
            System.out.println("*** Starting Test Method: " + desc.getMethodName());
        }
    };

    @After
    public void endTest() {
        mock.assertIsSatisfied();
    }

    private void getOidcOAuth20ClientExpectations(final OAuth20Provider provider, final OidcOAuth20ClientProvider clientProvider,
            final String clientId, final OidcBaseClient client) throws OidcServerException {
        if (provider == null) {
            return;
        }
        mock.checking(new Expectations() {
            {
                allowing(provider).getClientProvider();
                will(returnValue(clientProvider));
            }
        });
        if (clientProvider != null) {
            mock.checking(new Expectations() {
                {
                    allowing(clientProvider).get(clientId);
                    will(returnValue(client));
                }
            });
        }
    }

    private void createInitialExpectations(final Principal p, final String[] stateArray) throws OidcServerException {
        mock.checking(new Expectations() {
            {
                one(request).getUserPrincipal();
                will(returnValue(p));
            }
        });
        if (p != null) {
            mock.checking(new Expectations() {
                {
                    one(principal).getName();
                    will(returnValue(UID));
                }
            });
        }
        mock.checking(new Expectations() {
            {
                allowing(request).getParameterValues(OAuth20Constants.RESOURCE); //
                will(returnValue(null));//
                one(request).getParameterValues(OAuth20Constants.STATE);
                will(returnValue(stateArray));
                one(request).getParameterValues(OAuth20Constants.CLIENT_ID);
                will(returnValue(CID_ARRAY));
                between(1, 3).of(provider).getClientProvider();
                will(returnValue(oocp));
                between(1, 3).of(oocp).get(CID);
                will(returnValue(obc));
                one(obc).isEnabled();
                will(returnValue(true));
                one(obc).getClientId();
                will(returnValue(CID));
                allowing(obc).getAllowRegexpRedirects();
                will(returnValue(false));
            }
        });
    }

    private void redirectUriExpectations(final String[] redirectUriParamValues, final JsonArray redirectUris) throws OidcServerException {
        mock.checking(new Expectations() {
            {
                between(1, 2).of(request).getParameterValues(OAuth20Constants.REDIRECT_URI);
                will(returnValue(redirectUriParamValues));
                one(obc).getRedirectUris();
                will(returnValue(redirectUris));
            }
        });
    }

    private void responseTypeExpectations(final String[] responseTypeParamValues, final JsonArray registeredResponseTypes) {
        mock.checking(new Expectations() {
            {
                one(request).getParameterValues(OAuth20Constants.RESPONSE_TYPE);
                will(returnValue(responseTypeParamValues));
            }
        });
        if (responseTypeParamValues != null && responseTypeParamValues.length == 1 && !responseTypeParamValues[0].isEmpty()) {
            mock.checking(new Expectations() {
                {
                    one(obc).getResponseTypes();
                    will(returnValue(registeredResponseTypes));
                }
            });
        }
    }

    private void grantTypeExpectations(final String[] grantTypeParamValues, final JsonArray clientGrantTypes, final String[] grantTypesAllowed) {
        mock.checking(new Expectations() {
            {
                one(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                will(returnValue(grantTypeParamValues));
            }
        });
        if (grantTypeParamValues != null && grantTypeParamValues.length > 1) {
            // Duplicate grant type parameters
            return;
        }
        mock.checking(new Expectations() {
            {
                one(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                will(returnValue(grantTypeParamValues));
                one(obc).getGrantTypes();
                will(returnValue(clientGrantTypes));
                one(provider).getGrantTypesAllowed();
                will(returnValue(grantTypesAllowed));
            }
        });
    }

    private void oidcRequestExpectation(final String oidcRequestAttr) {
        mock.checking(new Expectations() {
            {
                one(request).getAttribute(OAuth20Constants.OIDC_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(oidcRequestAttr));
            }
        });
    }

    private void scopeExpectations(final String[] scopeParamValues, final String clientScope, final String clientPreauthorizedScope) {
        mock.checking(new Expectations() {
            {
                between(1, 2).of(request).getParameterValues(OAuth20Constants.SCOPE);
                will(returnValue(scopeParamValues));
                between(1, 2).of(obc).getScope();
                will(returnValue(clientScope));
                one(obc).getPreAuthorizedScope();
                will(returnValue(clientPreauthorizedScope));
            }
        });
    }

    private void extendedPropertiesExpectations(final StringTokenizer paramNames, final String[] optionalParamValues) {
        mock.checking(new Expectations() {
            {
                one(request).getParameterNames();
                will(returnValue(paramNames));
            }
        });
        if (paramNames != null) {
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OPTIONAL_PARAMS_NAME);
                    will(returnValue(optionalParamValues));
                }
            });
        }
    }

    /**
     * Asserts that an OAuth20MissingParameterException was caught and that the provided parameter is specified as the
     * missing parameter. A CWOAU0033E message is expected.
     *
     * @param result
     * @param parameter
     */
    private void missingParameterException(OAuthResult result, String parameter) {
        OAuthException oe = result.getCause();
        missingParameterException(oe, parameter);
    }

    /**
     * Asserts that an OAuth20MissingParameterException was caught and that the provided parameter is specified as the
     * missing parameter. A CWOAU0033E message is expected.
     *
     * @param exception
     * @param parameter
     */
    private void missingParameterException(OAuthException exception, String parameter) {
        assertTrue("Exception was not of expected type OAuth20MissingParameterException.", exception instanceof OAuth20MissingParameterException);

        OAuth20MissingParameterException ompe = (OAuth20MissingParameterException) exception;

        assertEquals("Did not get the expected parameter in the exception.", parameter, ompe.getParam());
        assertEquals("Did not get the expected exception error.", OAuth20Exception.INVALID_REQUEST, ompe.getError());
        assertEquals(REQUIRED_PARAM_MISSING_MSG + ompe.getParam(), ompe.getMessage());
    }

    /**
     * Asserts that an OAuth20DuplicateParameterException was caught and that the provided parameter is specified as the
     * duplicate parameter. A CWOAU0022E message is expected.
     *
     * @param result
     * @param parameter
     */
    private void duplicateParameterException(OAuthResult result, String parameter) {
        OAuthException exception = result.getCause();
        duplicateParameterException(exception, parameter);
    }

    /**
     * Asserts that an OAuth20DuplicateParameterException was caught and that the provided parameter is specified as the
     * duplicate parameter. A CWOAU0022E message is expected.
     *
     * @param exception
     * @param parameter
     */
    private void duplicateParameterException(OAuthException exception, String parameter) {
        assertTrue("Exception was not of expected type OAuth20DuplicateParameterException.", exception instanceof OAuth20DuplicateParameterException);

        OAuth20DuplicateParameterException odpe = (OAuth20DuplicateParameterException) exception;

        assertEquals("Did not get the expected parameter in the exception.", parameter, odpe.getParamName());
        assertEquals("Did not get the expected exception error.", OAuth20Exception.INVALID_REQUEST, odpe.getError());
        assertEquals(DUPLICATE_PARAM_MSG + odpe.getParamName(), odpe.getMessage());
    }

    /**
     * Asserts that an OAuth20BadParameterFormatException was caught and that the provided parameter is specified as the
     * bad parameter and has the specified value. A CWOAU0021E message is expected.
     *
     * @param exception
     * @param paramName
     * @param paramValue
     */
    private void badParameterException(OAuthException exception, String paramName, String paramValue) {
        assertTrue("Exception was not of expected type OAuth20BadParameterFormatException.", exception instanceof OAuth20BadParameterFormatException);

        OAuth20BadParameterFormatException obpfe = (OAuth20BadParameterFormatException) exception;
        assertEquals("Did not get the expected param name.", paramName, obpfe.getParamName());
        assertEquals("Did not get the expected param value.", paramValue, obpfe.getParamValue());
        assertEquals("Did not get the expected exception error.", OAuth20Exception.INVALID_REQUEST, obpfe.getError());
        assertEquals("CWOAU0021E: The parameter [" + obpfe.getParamName() + "] contains an illegally formatted value: [" + obpfe.getParamValue() + "].", obpfe.getMessage());
    }

    /**
     * Asserts that an OAuth20InvalidClientException was caught and that the provided parameter is specified as the
     * duplicate parameter. A CWOAU0022E message is expected.
     *
     * @param result
     * @param clientId
     */
    private void invalidClientException(OAuthResult result, String clientId) {
        OAuthException oe = result.getCause();
        invalidClientException(oe, clientId);
    }

    /**
     * Asserts that an OAuth20InvalidClientException was caught and that the provided parameter is specified as the
     * duplicate parameter. A CWOAU0022E message is expected.
     *
     * @param exception
     * @param clientId
     */
    private void invalidClientException(OAuthException exception, String clientId) {
        assertTrue("Exception was not of expected type OAuth20InvalidClientException.", exception instanceof OAuth20InvalidClientException);

        OAuth20InvalidClientException oice = (OAuth20InvalidClientException) exception;
        assertEquals("Did not get the expected client id.", clientId, oice.getClientId());
        assertEquals("Did not get the expected exception error.", OAuth20Exception.UNAUTHORIZED_CLIENT, oice.getError());
        assertEquals(INVALID_CLIENT_MSG + oice.getClientId() + ".", oice.getMessage());
    }

    /**
     * Asserts that an OAuth20InvalidResponseTypeException was caught and that the provided response type is specified as the
     * invalid response type. A CWOAU0027E message is expected.
     *
     * @param result
     * @param responseType
     */
    private void invalidResponseTypeException(OAuthResult result, String responseType) {
        OAuthException oe = result.getCause();
        invalidResponseTypeException(oe, responseType);
    }

    /**
     * Asserts that an OAuth20InvalidResponseTypeException was caught and that the provided response type is specified as the
     * invalid response type. A CWOAU0027E message is expected.
     *
     * @param exception
     * @param responseType
     */
    private void invalidResponseTypeException(OAuthException exception, String responseType) {
        assertTrue("Exception was not of expected type OAuth20InvalidResponseTypeException.", exception instanceof OAuth20InvalidResponseTypeException);

        OAuth20InvalidResponseTypeException ire = (OAuth20InvalidResponseTypeException) exception;

        assertEquals("Did not get the expected response type in the exception.", responseType, ire.getResponseType());
        assertEquals("Did not get the expected exception error.", OAuth20Exception.UNSUPPORTED_RESPONSE_TPE, ire.getError());
        assertEquals("CWOAU0027E: The response_type parameter was invalid: " + ire.getResponseType(), ire.getMessage());
    }

    /**
     * Asserts that an OAuth20InvalidGrantTypeException was caught and that the provided grant type is specified as the
     * invalid grant type. A CWOAU0025E message is expected.
     *
     * @param result
     * @param grantType
     */
    private void invalidGrantTypeException(OAuthResult result, String grantType) {
        OAuthException oe = result.getCause();
        invalidGrantTypeException(oe, grantType);
    }

    /**
     * Asserts that an OAuth20InvalidGrantTypeException was caught and that the provided grant type is specified as the
     * invalid grant type. A CWOAU0025E message is expected.
     *
     * @param exception
     * @param grantType
     */
    private void invalidGrantTypeException(OAuthException exception, String grantType) {
        assertTrue("Exception was not of expected type OAuth20InvalidGrantTypeException.", exception instanceof OAuth20InvalidGrantTypeException);

        OAuth20InvalidGrantTypeException igte = (OAuth20InvalidGrantTypeException) exception;
        assertEquals("Did not get the expected grant type in the exception.", grantType, igte.getGrantType());
        assertEquals("Did not get the expected exception error.", OAuth20Exception.UNSUPPORED_GRANT_TPE, igte.getError());
        assertEquals("CWOAU0025E: The grant_type parameter was invalid: " + igte.getGrantType(), igte.getMessage());
    }

    /**
     * Asserts that an OAuth20InvalidScopeException was caught and that the provided grant type is specified as the
     * invalid grant type. A CWOAU0064E message is expected.
     *
     * @param result
     * @param approvedScopes
     * @param requestedScopes
     */
    private void invalidScopeException(OAuthResult result, String[] approvedScopes, String[] requestedScopes) {
        OAuthException exception = result.getCause();
        assertTrue("Exception was not of expected type OAuth20InvalidScopeException.", exception instanceof OAuth20InvalidScopeException);

        OAuth20InvalidScopeException ise = (OAuth20InvalidScopeException) exception;
        assertEquals("Did not get the expected list of approved scopes.", Arrays.toString(approvedScopes), Arrays.toString(ise.getApprovedScope()));
        assertEquals("Did not get the expected list of requested scopes.", Arrays.toString(requestedScopes), Arrays.toString(ise.getRequestedScope()));
        assertEquals("Did not get the expected exception error.", OAuth20Exception.INVALID_SCOPE, ise.getError());

        if (approvedScopes == null) {
            approvedScopes = new String[0];
        }
        if (requestedScopes == null) {
            requestedScopes = new String[0];
        }
        assertEquals("CWOAU0064E: The requested scope " + Arrays.toString(requestedScopes) + " and registered scope " +
                Arrays.toString(approvedScopes) + " of the client [" + CID + "] does not have a common scope among them. The resultant scope is empty.", ise.getMessage());
    }

    /**
     * Asserts that an OAuth20InvalidScopeException was caught and that a CWOAU0065E error message was produced.
     *
     * @param result
     */
    private void missingScopeException(OAuthResult result) {
        OAuthException exception = result.getCause();
        missingScopeException(exception);
    }

    /**
     * Asserts that an OAuth20InvalidScopeException was caught and that a CWOAU0065E error message was produced.
     *
     * @param exception
     */
    private void missingScopeException(OAuthException exception) {
        String requestType = "OpenID Connect request";

        assertTrue("Exception was not of expected type OAuth20InvalidScopeException.", exception instanceof OAuth20InvalidScopeException);

        OAuth20InvalidScopeException ise = (OAuth20InvalidScopeException) exception;
        assertEquals("Did not get the expected exception error.", OAuth20Exception.INVALID_SCOPE, ise.getError());
        assertEquals("CWOAU0065E: The authorization server cannot process the [" + requestType + "] request. It is missing the required scope parameter.", ise.getMessage());
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. request contains valid user principal
     * 2. client provider is found
     * 3. OAuth20 request
     * 4. Response type is code
     * 5. valid state
     * 6. valid scope
     * Expected result: STATUS_OK, only requested scopes that are registered and preauthorized included in result
     */
    @Test
    public void validateAuthorization_ValidUser() {

        final String[] stateArray = STATE_ARRAY;
        final String[] requestedScopes = new String[] { SCOPE + " scope1 scope3 scope4" };
        final String registeredScopes = "scope1 scope2 " + SCOPE;
        final String preauthorizedScopes = SCOPE + " scope1 scope3";

        final StringTokenizer pn = new StringTokenizer(OAuth20Constants.CLIENT_ID + " " + OAuth20Constants.REDIRECT_URI + " " + OPTIONAL_PARAMS_NAME);

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_CODE, respCode);
            grantTypeExpectations(null, clientGrantTypes, new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE });
            oidcRequestExpectation(null);
            scopeExpectations(requestedScopes, registeredScopes, preauthorizedScopes);
            extendedPropertiesExpectations(pn, new String[] { OPTIONAL_PARAMS_VALUE });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_OK);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            // Ensure that only the requested scopes that are registered and preauthorized with the client are returned and extra scopes are ignored
            AttributeList attributeList = result.getAttributeList();
            String[] scopeAttrs = attributeList.getAttributeValuesByName(OAuth20Constants.SCOPE);
            assertEquals("Did not get expected scope list in result.", formatScopeStrings(new String[] { SCOPE + " scope1" }), formatScopeStrings(scopeAttrs));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. no username
     * 2. client provider is found
     * 3. OIDC request
     * 4. Response type is code
     * 5. valid state
     * 6. valid scope
     * Expected result: STATUS_OK
     */
    @Test
    public void validateAuthorization_NoUser() {

        final String[] stateArray = STATE_ARRAY;

        final StringTokenizer pn = new StringTokenizer(OAuth20Constants.CLIENT_ID + " " + OAuth20Constants.REDIRECT_URI + " " + OPTIONAL_PARAMS_NAME);

        try {
            createInitialExpectations(null, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_CODE, respCode);
            grantTypeExpectations(null, clientGrantTypes, new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE });
            oidcRequestExpectation("oidcreq");
            scopeExpectations(new String[] { SCOPE }, SCOPE, SCOPE);
            extendedPropertiesExpectations(pn, new String[] { OPTIONAL_PARAMS_VALUE });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_OK);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. no clientID
     * Expected result: STATUS_FAILED with OAuth20MissingParameterException
     */
    @Test
    public void validateAuthorization_NoClientId() {

        final String[] stateArray = STATE_ARRAY;

        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getUserPrincipal();
                    will(returnValue(null));
                    one(request).getParameterValues(OAuth20Constants.STATE);
                    will(returnValue(stateArray));
                    one(request).getParameterValues(OAuth20Constants.CLIENT_ID);
                    will(returnValue(null));
                }
            });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            missingParameterException(result, OAuth20Constants.CLIENT_ID);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. duplicate clientID
     * Expected result: STATUS_FAILED with OAuth20DuplicateParameterException
     */
    @Test
    public void validateAuthorization_DuplicateClientIds() {

        final String[] invalidCidArray = { CID, "duplicate_client" };
        final String[] stateArray = STATE_ARRAY;

        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getUserPrincipal();
                    will(returnValue(null));
                    one(request).getParameterValues(OAuth20Constants.STATE);
                    will(returnValue(stateArray));
                    one(request).getParameterValues(OAuth20Constants.CLIENT_ID);
                    will(returnValue(invalidCidArray));
                }
            });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            duplicateParameterException(result, OAuth20Constants.CLIENT_ID);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. client_id = ""
     * Expected result: STATUS_FAILED with OAuth20MissingParameterException
     */
    @Test
    public void validateAuthorization_EmptyClientId() {

        final String[] stateArray = STATE_ARRAY;
        final String[] emptyCidArray = { "" };

        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getUserPrincipal();
                    will(returnValue(null));
                    one(request).getParameterValues(OAuth20Constants.STATE);
                    will(returnValue(stateArray));
                    one(request).getParameterValues(OAuth20Constants.CLIENT_ID);
                    will(returnValue(emptyCidArray));
                }
            });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            missingParameterException(result, OAuth20Constants.CLIENT_ID);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Client not found
     * Expected result: STATUS_FAILED with OAuth20InvalidClientException
     */
    @Test
    public void validateAuthorization_NoClient() {

        final String[] stateArray = STATE_ARRAY;

        try {
            mock.checking(new Expectations() {
                {
                    one(request).getUserPrincipal();
                    will(returnValue(principal));
                    one(principal).getName();
                    will(returnValue(UID));
                    one(request).getParameterValues(OAuth20Constants.STATE);
                    will(returnValue(stateArray));
                    one(request).getParameterValues(OAuth20Constants.CLIENT_ID);
                    will(returnValue(CID_ARRAY));
                    exactly(1).of(provider).getClientProvider();
                    will(returnValue(oocp));
                    exactly(1).of(oocp).get(CID);
                    will(returnValue(null));
                }
            });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            invalidClientException(result, CID);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /***************************************** Redirect URI tests *****************************************/

    /**
     * Tests validateAuthorization method with following conditions
     * 1. multiple redirect uris
     * Expected result: STATUS_FAILED with OAuth20DuplicateParameterException
     */
    @Test
    public void validateAuthorization_MultipleRedirectUris() {

        final String[] stateArray = STATE_ARRAY;
        final String[] multipleRua = { URI, "http://localhost/duplicate" };

        try {
            createInitialExpectations(principal, stateArray);

            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.REDIRECT_URI);
                    will(returnValue(multipleRua));
                }
            });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            duplicateParameterException(result, OAuth20Constants.REDIRECT_URI);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Null redirect URI
     * 2. No registered URIs
     * Expected result: STATUS_FAILED with OAuth20MissingParameterException
     */
    @Test
    public void validateAuthorization_NullRedirectUriNoRegisteredUri() {

        final String[] stateArray = STATE_ARRAY;
        final String[] nullRua = null;

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(nullRua, null);

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            missingParameterException(result, OAuth20Constants.REDIRECT_URI);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Empty redirect URI
     * 2. No registered URIs
     * Expected result: STATUS_FAILED with OAuth20MissingParameterException
     */
    @Test
    public void validateAuthorization_EmptyRedirectUriNoRegisteredUri() {

        final String[] stateArray = STATE_ARRAY;
        final String[] emptyRua = new String[] { "" };

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(emptyRua, null);

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            missingParameterException(result, OAuth20Constants.REDIRECT_URI);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Null redirect URI
     * 2. Invalid registered redirect URI
     * Expected result: STATUS_FAILED with OAuth20InvalidRedirectUriException
     */
    @Test
    public void validateAuthorization_NullRedirectUriInvalidRegisteredRedirectUri() {

        final String[] stateArray = STATE_ARRAY;
        final String[] nullOrEmptyRua = null;

        final JsonArray jarua = new JsonArray();
        final String INVALID_URI = "/invalid/uri";
        jarua.add(new JsonPrimitive(INVALID_URI));

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(nullOrEmptyRua, jarua);

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            OAuthException oe = result.getCause();
            assertTrue("Exception was not of expected type OAuth20InvalidRedirectUriException.", oe instanceof OAuth20InvalidRedirectUriException);

            OAuth20InvalidRedirectUriException oire = (OAuth20InvalidRedirectUriException) oe;
            assertEquals("Did not get the expected redirect URI.", INVALID_URI, oire.getRedirectURI());
            assertEquals("Did not get the expected exception error.", OAuth20Exception.INVALID_REQUEST, oire.getError());
            assertEquals("CWOAU0055E: The redirect URI specified in the registered client of the OAuth provider is not valid: " + oire.getRedirectURI(), oire.getMessage());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Empty redirect URI
     * 2. Invalid registered redirect URI
     * Expected result: STATUS_FAILED with OAuth20InvalidRedirectUriException
     */
    @Test
    public void validateAuthorization_EmptyRedirectUriInvalidRegisteredRedirectUri() {

        final String[] stateArray = STATE_ARRAY;
        final String[] nullOrEmptyRua = new String[] { "" };

        final JsonArray jarua = new JsonArray();
        final String INVALID_URI = "/invalid/uri";
        jarua.add(new JsonPrimitive(INVALID_URI));

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(nullOrEmptyRua, jarua);

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            OAuthException oe = result.getCause();
            assertTrue("Exception was not of expected type OAuth20InvalidRedirectUriException.", oe instanceof OAuth20InvalidRedirectUriException);

            OAuth20InvalidRedirectUriException oire = (OAuth20InvalidRedirectUriException) oe;
            assertEquals("Did not get the expected redirect URI.", INVALID_URI, oire.getRedirectURI());
            assertEquals("Did not get the expected exception error.", OAuth20Exception.INVALID_REQUEST, oire.getError());
            assertEquals("CWOAU0055E: The redirect URI specified in the registered client of the OAuth provider is not valid: " + oire.getRedirectURI(), oire.getMessage());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Null redirect URI
     * 2. One valid registered redirect URI
     * Expected result: STATUS_OK
     */
    @Test
    public void validateAuthorization_NullRedirectUriValidRegisteredUri() {

        final String[] stateArray = STATE_ARRAY;
        final String[] nullOrEmptyRua = null;

        final StringTokenizer pn = new StringTokenizer(OAuth20Constants.CLIENT_ID + " " + OAuth20Constants.REDIRECT_URI + " " + OPTIONAL_PARAMS_NAME);

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(nullOrEmptyRua, validRedirectUriArray);
            responseTypeExpectations(RT_CODE, respCode);
            grantTypeExpectations(null, clientGrantTypes, new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE });
            oidcRequestExpectation(null);
            scopeExpectations(new String[] { SCOPE }, SCOPE, SCOPE);
            extendedPropertiesExpectations(pn, new String[] { OPTIONAL_PARAMS_VALUE });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_OK);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            assertEquals("Redirect URI provided in result does not match expected registered redirect URI.", URI,
                    result.getAttributeList().getAttributeValueByName(OAuth20Constants.REDIRECT_URI));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Empty redirect URI
     * 2. One valid registered redirect URI
     * Expected result: STATUS_OK
     */
    @Test
    public void validateAuthorization_EmptyRedirectUriValidRegisteredUri() {

        final String[] stateArray = STATE_ARRAY;
        final String[] emptyRua = new String[] { "" };

        final StringTokenizer pn = new StringTokenizer(OAuth20Constants.CLIENT_ID + " " + OAuth20Constants.REDIRECT_URI + " " + OPTIONAL_PARAMS_NAME);

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(emptyRua, validRedirectUriArray);
            responseTypeExpectations(RT_CODE, respCode);
            grantTypeExpectations(null, clientGrantTypes, new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE });
            oidcRequestExpectation(null);
            scopeExpectations(new String[] { SCOPE }, SCOPE, SCOPE);
            extendedPropertiesExpectations(pn, new String[] { OPTIONAL_PARAMS_VALUE });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_OK);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            assertEquals("Redirect URI provided in result does not match expected registered redirect URI.", URI,
                    result.getAttributeList().getAttributeValueByName(OAuth20Constants.REDIRECT_URI));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. invalid redirect uri value.
     * Expected result: STATUS_FAILED with OAuth20InvalidRedirectUriException
     */
    @Test
    public void validateAuthorization_InvalidRedirectUri() {

        final String[] stateArray = STATE_ARRAY;

        final String INVALID_URI = "/invalid";
        final String[] invalidRua = { INVALID_URI };

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(invalidRua, validRedirectUriArray);

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            OAuthException oe = result.getCause();
            assertTrue("Exception was not of expected type OAuth20InvalidRedirectUriException.", oe instanceof OAuth20InvalidRedirectUriException);

            OAuth20InvalidRedirectUriException oire = (OAuth20InvalidRedirectUriException) oe;
            assertEquals("Did not get the expected redirect URI.", INVALID_URI, oire.getRedirectURI());
            assertEquals("Did not get the expected exception error.", OAuth20Exception.INVALID_REQUEST, oire.getError());
            assertEquals("CWOAU0026E: The redirect URI parameter was invalid: " + oire.getRedirectURI(), oire.getMessage());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. no registered redirect uri value.
     * Expected result: STATUS_FAILED with OAuth20InvalidRedirectUriException
     */
    @Test
    public void validateAuthorization_NoRegisteredRedirectUri() {

        final String[] stateArray = STATE_ARRAY;

        final JsonArray emptyArray = new JsonArray();

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, emptyArray);

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            OAuthException oe = result.getCause();
            assertTrue("Exception was not of expected type OAuth20InvalidRedirectUriException.", oe instanceof OAuth20InvalidRedirectUriException);

            OAuth20InvalidRedirectUriException oire = (OAuth20InvalidRedirectUriException) oe;
            assertEquals("Did not get the expected redirect URI.", URI, oire.getRedirectURI());
            assertEquals("Did not get the expected exception error.", OAuth20Exception.INVALID_REQUEST, oire.getError());
            assertEquals("CWOAU0056E: The redirect URI parameter [" + oire.getRedirectURI()
                    + "] provided in the OAuth or OpenID Connect request did not match any of the redirect URIs registered with the OAuth provider [].", oire.getMessage());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Multiple registered redirect URI values.
     * 2. Redirect URI provided in request is one of the registered values
     * Expected result: STATUS_OK
     */
    @Test
    public void validateAuthorization_MultipleRegisteredRedirectUriIncludeRedirectUri() {

        final String[] stateArray = STATE_ARRAY;

        final JsonArray multipleRedirectUris = new JsonArray();
        multipleRedirectUris.add(new JsonPrimitive("http://localhost/other/redirect1"));
        multipleRedirectUris.add(new JsonPrimitive("http://localhost/other/redirect2"));
        multipleRedirectUris.add(new JsonPrimitive(URI));

        final StringTokenizer pn = new StringTokenizer(OAuth20Constants.CLIENT_ID + " " + OAuth20Constants.REDIRECT_URI + " " + OPTIONAL_PARAMS_NAME);

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, multipleRedirectUris);
            responseTypeExpectations(RT_CODE, respCode);
            grantTypeExpectations(null, clientGrantTypes, new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE });
            oidcRequestExpectation("oidcreq");
            scopeExpectations(new String[] { SCOPE }, SCOPE, SCOPE);
            extendedPropertiesExpectations(pn, new String[] { OPTIONAL_PARAMS_VALUE });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_OK);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Multiple registered redirect URI values.
     * 2. Redirect URI provided in request is NOT one of the registered values
     * Expected result: STATUS_FAILED with OAuth20InvalidRedirectUriException
     */
    @Test
    public void validateAuthorization_MultipleRegisteredRedirectUriMissingRedirectUri() {

        final String[] stateArray = STATE_ARRAY;

        final JsonArray multipleRedirectUris = new JsonArray();
        multipleRedirectUris.add(new JsonPrimitive("http://localhost/other/redirect1"));
        multipleRedirectUris.add(new JsonPrimitive("http://localhost/other/redirect2"));

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, multipleRedirectUris);

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            OAuthException oe = result.getCause();
            assertTrue("Exception was not of expected type OAuth20InvalidRedirectUriException.", oe instanceof OAuth20InvalidRedirectUriException);

            OAuth20InvalidRedirectUriException oire = (OAuth20InvalidRedirectUriException) oe;
            assertEquals("Did not get the expected redirect URI.", URI, oire.getRedirectURI());
            assertEquals("Did not get the expected exception error.", OAuth20Exception.INVALID_REQUEST, oire.getError());
            String redirectUriString = "";
            for (int i = 0; i < multipleRedirectUris.size(); i++) {
                redirectUriString = redirectUriString + " " + multipleRedirectUris.get(i).getAsString();
            }
            redirectUriString = redirectUriString.trim();
            assertEquals("CWOAU0056E: The redirect URI parameter [" + oire.getRedirectURI()
                    + "] provided in the OAuth or OpenID Connect request did not match any of the redirect URIs registered with the OAuth provider ["
                    + redirectUriString + "].", oire.getMessage());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /***************************************** Response type tests *****************************************/

    /**
     * Tests validateAuthorization method with following conditions
     * 1. No response type.
     * Expected result: STATUS_FAILED with OAuth20MissingParameterException
     */
    @Test
    public void validateAuthorization_NoResponseType() {

        final String[] stateArray = STATE_ARRAY;

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);

            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.RESPONSE_TYPE);
                    will(returnValue(null));
                }
            });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            missingParameterException(result, OAuth20Constants.RESPONSE_TYPE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1.Multiple response type.
     * Expected result: STATUS_FAILED with OAuth20DuplicateParameterException
     */
    @Test
    public void validateAuthorization_MultipleResponseType() {

        final String[] stateArray = STATE_ARRAY;

        final String[] multipleResponseTypes = { OAuth20Constants.RESPONSE_TYPE_CODE, OAuth20Constants.RESPONSE_TYPE_TOKEN };

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);

            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.RESPONSE_TYPE);
                    will(returnValue(multipleResponseTypes));
                }
            });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            duplicateParameterException(result, OAuth20Constants.RESPONSE_TYPE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Empty response type.
     * Expected result: STATUS_FAILED with OAuth20MissingParameterException
     */
    @Test
    public void validateAuthorization_EmptyResponseType() {

        final String[] stateArray = STATE_ARRAY;

        final String[] emptyResponseType = { "" };

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);

            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.RESPONSE_TYPE);
                    will(returnValue(emptyResponseType));
                }
            });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            missingParameterException(result, OAuth20Constants.RESPONSE_TYPE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Requested response type is "code"
     * 2. Provider only supports implicit grant type
     * Expected result: STATUS_FAILED with OAuth20InvalidGrantTypeException
     */
    @Test
    public void validateAuthorization_MismatchResponseType() {

        final String[] stateArray = STATE_ARRAY;

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_CODE, respCode);
            grantTypeExpectations(null, clientGrantTypes, new String[] { OAuth20Constants.GRANT_TYPE_IMPLICIT });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            invalidGrantTypeException(result, OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Registered client response types is null
     * Expected result: STATUS_FAILED with OAuth20InvalidResponseTypeException
     */
    @Test
    public void validateAuthorization_NullRegisteredResponseTypes() {

        final String[] stateArray = STATE_ARRAY;

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_CODE, null);

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            verifyState(result, stateArray);

            invalidResponseTypeException(result, RT_CODE[0]);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Registered client response types is empty
     * Expected result: STATUS_FAILED with OAuth20InvalidResponseTypeException
     */
    @Test
    public void validateAuthorization_NoRegisteredResponseTypes() {

        final String[] stateArray = STATE_ARRAY;

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_CODE, new JsonArray());

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            verifyState(result, stateArray);

            invalidResponseTypeException(result, RT_CODE[0]);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Response type in request is not registered in the client
     * Expected result: STATUS_FAILED with OAuth20InvalidResponseTypeException
     */
    @Test
    public void validateAuthorization_RequestedResponseTypeNotRegistered() {

        final String[] stateArray = STATE_ARRAY;

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_CODE, respToken);

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            verifyState(result, stateArray);

            invalidResponseTypeException(result, OAuth20Constants.RESPONSE_TYPE_CODE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /***************************************** Grant type tests *****************************************/

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Duplicated grant_type parameter
     * Expected result: STATUS_FAILED with OAuth20DuplicateParameterException
     */
    @Test
    public void validateAuthorization_MultipleGrantTypeParams() {

        final String[] stateArray = STATE_ARRAY;

        final String[] grantTypeArray = new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE, OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE };

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_CODE, respCode);
            grantTypeExpectations(grantTypeArray, null, null);

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            verifyState(result, stateArray);

            duplicateParameterException(result, OAuth20Constants.GRANT_TYPE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Requested response_type = code
     * 2. Requested grant_type = implicit
     * Expected result: STATUS_FAILED with OAuth20InvalidResponseTypeException
     */
    @Test
    public void validateAuthorization_CodeResponseTypeMismatchForImplicitGrantType() {

        final String[] stateArray = STATE_ARRAY;
        final String[] grantTypeArray = new String[] { OAuth20Constants.GRANT_TYPE_IMPLICIT };

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_CODE, respCode);
            grantTypeExpectations(grantTypeArray, clientGrantTypes, grantTypeArray);

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            verifyState(result, stateArray);

            invalidResponseTypeException(result, OAuth20Constants.RESPONSE_TYPE_CODE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Requested response_type = token
     * 2. Requested grant_type = authorization_code
     * Expected result: STATUS_FAILED with OAuth20InvalidResponseTypeException
     */
    @Test
    public void validateAuthorization_TokenResponseTypeMismatchForAuthorizationGrantType() {

        final String[] stateArray = STATE_ARRAY;
        final String[] grantTypeArray = new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE };

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_TOKEN, respToken);
            grantTypeExpectations(grantTypeArray, clientGrantTypes, grantTypeArray);

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            verifyState(result, stateArray);

            invalidResponseTypeException(result, OAuth20Constants.RESPONSE_TYPE_TOKEN);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Requested response_type = id_token
     * 2. Requested grant_type = authorization_code
     * Expected result: STATUS_FAILED with OAuth20InvalidResponseTypeException
     */
    @Test
    public void validateAuthorization_IdTokenResponseTypeMismatchForAuthorizationGrantType() {

        final String[] stateArray = STATE_ARRAY;
        final String[] grantTypeArray = new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE };

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_ID_TOKEN, respIdToken);
            grantTypeExpectations(grantTypeArray, clientGrantTypes, grantTypeArray);

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            verifyState(result, stateArray);

            invalidResponseTypeException(result, OIDCConstants.RESPONSE_TYPE_ID_TOKEN);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Requested response_type = token
     * 2. Requested grant_type = implicit
     * Expected result: STATUS_OK
     */
    @Test
    public void validateAuthorization_ImplicitFlowToken() {

        final String[] stateArray = STATE_ARRAY;
        final String[] grantTypeArray = new String[] { OAuth20Constants.GRANT_TYPE_IMPLICIT };

        final StringTokenizer pn = new StringTokenizer(OAuth20Constants.CLIENT_ID + " " + OAuth20Constants.REDIRECT_URI + " " + OPTIONAL_PARAMS_NAME);

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_TOKEN, respToken);
            grantTypeExpectations(grantTypeArray, clientGrantTypes, grantTypeArray);
            oidcRequestExpectation(null);
            scopeExpectations(new String[] { SCOPE + " scope1" }, SCOPE, SCOPE);
            extendedPropertiesExpectations(pn, new String[] { OPTIONAL_PARAMS_VALUE });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_OK);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            // Ensure that only the requested scopes that are registered and preauthorized with the client are returned and extra scopes are ignored
            AttributeList attributeList = result.getAttributeList();
            String[] scopeAttrs = attributeList.getAttributeValuesByName(OAuth20Constants.SCOPE);
            assertEquals("Did not get expected scope list in result.", formatScopeStrings(SCOPE), formatScopeStrings(scopeAttrs));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Requested response_type = id_token
     * 2. Requested grant_type = implicit
     * Expected result: STATUS_OK
     */
    @Test
    public void validateAuthorization_ImplicitFlowIdToken() {

        final String[] stateArray = STATE_ARRAY;
        final String[] grantTypeArray = new String[] { OAuth20Constants.GRANT_TYPE_IMPLICIT };

        final StringTokenizer pn = new StringTokenizer(OAuth20Constants.CLIENT_ID + " " + OAuth20Constants.REDIRECT_URI + " " + OPTIONAL_PARAMS_NAME);

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_ID_TOKEN, respIdToken);
            grantTypeExpectations(grantTypeArray, clientGrantTypes, grantTypeArray);
            oidcRequestExpectation(null);
            scopeExpectations(new String[] { SCOPE }, SCOPE, SCOPE);
            extendedPropertiesExpectations(pn, new String[] { OPTIONAL_PARAMS_VALUE });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_OK);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Requested grant_type = code
     * 2. Grant types allowed = null
     * 3. Reduced grant types = empty set
     * Expected result: STATUS_OK
     */
    @Test
    public void validateAuthorization_AuthorizationCodeGrantTypeEmptyReducedGrantTypes() {

        final String[] stateArray = STATE_ARRAY;
        final String[] grantTypeArray = new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE };

        final StringTokenizer pn = new StringTokenizer(OAuth20Constants.CLIENT_ID + " " + OAuth20Constants.REDIRECT_URI + " " + OPTIONAL_PARAMS_NAME);

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_CODE, respCode);
            grantTypeExpectations(grantTypeArray, clientGrantTypes, null);
            oidcRequestExpectation(null);
            scopeExpectations(new String[] { SCOPE }, SCOPE, SCOPE);
            extendedPropertiesExpectations(pn, new String[] { OPTIONAL_PARAMS_VALUE });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_OK);

            verifyState(result, stateArray);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Requested grant_type = implicit
     * 2. Grant types allowed = empty list
     * 3. Reduced grant types = empty set
     * Expected result: STATUS_OK
     */
    @Test
    public void validateAuthorization_ImplicitGrantTypeEmptyReducedGrantTypes() {

        final String[] stateArray = STATE_ARRAY;
        final String[] grantTypeArray = new String[] { OAuth20Constants.GRANT_TYPE_IMPLICIT };

        final StringTokenizer pn = new StringTokenizer(OAuth20Constants.CLIENT_ID + " " + OAuth20Constants.REDIRECT_URI + " " + OPTIONAL_PARAMS_NAME);

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_TOKEN, respToken);
            grantTypeExpectations(grantTypeArray, clientGrantTypes, new String[0]);
            oidcRequestExpectation(null);
            scopeExpectations(new String[] { SCOPE }, SCOPE, SCOPE);
            extendedPropertiesExpectations(pn, new String[] { OPTIONAL_PARAMS_VALUE });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_OK);

            verifyState(result, stateArray);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Requested grant_type = none
     * 2. Requested response_type = token
     * Expected result: STATUS_OK
     */
    @Test
    public void validateAuthorization_NoRequestedGrantTypeTokenResponseType() {

        final String[] stateArray = STATE_ARRAY;

        final StringTokenizer pn = new StringTokenizer(OAuth20Constants.CLIENT_ID + " " + OAuth20Constants.REDIRECT_URI + " " + OPTIONAL_PARAMS_NAME);

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_TOKEN, respToken);
            grantTypeExpectations(null, clientGrantTypes, new String[] { OAuth20Constants.GRANT_TYPE_IMPLICIT });
            oidcRequestExpectation(null);
            scopeExpectations(new String[] { SCOPE }, SCOPE, SCOPE);
            extendedPropertiesExpectations(pn, new String[] { OPTIONAL_PARAMS_VALUE });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_OK);

            verifyState(result, stateArray);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Requested grant_type = none
     * 2. Requested response_type = id_token
     * Expected result: STATUS_OK
     */
    @Test
    public void validateAuthorization_NoRequestedGrantTypeIdTokenResponseType() {

        final String[] stateArray = STATE_ARRAY;

        final StringTokenizer pn = new StringTokenizer(OAuth20Constants.CLIENT_ID + " " + OAuth20Constants.REDIRECT_URI + " " + OPTIONAL_PARAMS_NAME);

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_ID_TOKEN, respIdToken);
            grantTypeExpectations(null, clientGrantTypes, new String[] { OAuth20Constants.GRANT_TYPE_IMPLICIT });
            oidcRequestExpectation(null);
            scopeExpectations(new String[] { SCOPE }, SCOPE, SCOPE);
            extendedPropertiesExpectations(pn, new String[] { OPTIONAL_PARAMS_VALUE });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_OK);

            verifyState(result, stateArray);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Requested grant_type = none
     * 2. Requested response_type = some unknown value
     * 3. Requested response type is not registered
     * Expected result: STATUS_FAILED with OAuth20InvalidGrantTypeException
     */
    @Test
    public void validateAuthorization_NoRequestedGrantTypeOtherUnregisteredResponseType() {

        final String[] stateArray = STATE_ARRAY;

        String unknownResponseType = "some_other_rt";

        final JsonArray unknownRtArray = new JsonArray();
        unknownRtArray.add(new JsonPrimitive(unknownResponseType));

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(new String[] { unknownResponseType }, unknownRtArray);
            grantTypeExpectations(null, clientGrantTypes, new String[] { OAuth20Constants.GRANT_TYPE_IMPLICIT });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            verifyState(result, stateArray);

            invalidGrantTypeException(result, GRANT_TYPE_UNKNOWN);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Requested grant_type = none
     * 2. Requested response_type = some unknown value
     * 3. Requested response type is not registered
     * 3. Requested response type is allowed by provider
     * Expected result: STATUS_FAILED with OAuth20InvalidResponseTypeException
     */
    @Test
    public void validateAuthorization_NoRequestedGrantTypeOtherUnregisteredResponseTypeAllowed() {

        final String[] stateArray = STATE_ARRAY;

        String unknownResponseType = "some_other_rt";

        final JsonArray unknownRtArray = new JsonArray();
        unknownRtArray.add(new JsonPrimitive(unknownResponseType));

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(new String[] { unknownResponseType }, unknownRtArray);
            grantTypeExpectations(null, clientGrantTypes, new String[] { unknownResponseType });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            verifyState(result, stateArray);

            invalidResponseTypeException(result, unknownResponseType);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Requested grant_type = none
     * 2. Requested response_type = some unknown value
     * 3. Requested response type is registered
     * 3. Requested response type is allowed by provider
     * Expected result: STATUS_FAILED with OAuth20InvalidGrantTypeException
     */
    @Test
    public void validateAuthorization_NoRequestedGrantTypeOtherRegisteredResponseTypeAllowed() {

        final String[] stateArray = STATE_ARRAY;

        String unknownResponseType = "some_other_rt";

        final JsonArray unknownRtArray = new JsonArray();
        unknownRtArray.add(new JsonPrimitive(unknownResponseType));

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(new String[] { unknownResponseType }, unknownRtArray);
            grantTypeExpectations(null, unknownRtArray, new String[] { unknownResponseType });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            verifyState(result, stateArray);

            invalidGrantTypeException(result, GRANT_TYPE_UNKNOWN);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Requested grant_type = none
     * 2. Requested response_type = "unknown"
     * 3. Requested response type is registered
     * 3. Requested response type is allowed by provider
     * Expected result: STATUS_FAILED with OAuth20InvalidResponseTypeException
     */
    @Test
    public void validateAuthorization_NoRequestedGrantTypeUnknownResponseTypeRegisteredAndAllowed() {

        final String[] stateArray = STATE_ARRAY;

        String unknownReturnType = GRANT_TYPE_UNKNOWN;

        final JsonArray unknownRtArray = new JsonArray();
        unknownRtArray.add(new JsonPrimitive(unknownReturnType));

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(new String[] { unknownReturnType }, unknownRtArray);
            grantTypeExpectations(null, unknownRtArray, new String[] { unknownReturnType });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            verifyState(result, stateArray);

            invalidResponseTypeException(result, unknownReturnType);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /***************************************** State tests *****************************************/

    /**
     * Tests validateAuthorization method with following conditions
     * 1. duplicated state
     * Expected result: STATUS_FAILED with OAuth20DuplicateParameterException
     */
    @Test
    public void validateAuthorization_MultipleState() {

        final String[] stateArray = new String[] { STATE, STATE };

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_CODE, respCode);
            grantTypeExpectations(null, clientGrantTypes, new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            verifyState(result, stateArray);

            duplicateParameterException(result, OAuth20Constants.STATE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /***************************************** Scope tests *****************************************/

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Request is OAuth request
     * 2. No scope parameter provided
     * Expected result: STATUS_OK, no scopes included in the result
     */
    @Test
    public void validateAuthorization_MissingScopeOAuthRequest() {

        final String[] stateArray = STATE_ARRAY;

        final StringTokenizer pn = new StringTokenizer(OAuth20Constants.CLIENT_ID + " " + OAuth20Constants.REDIRECT_URI + " " + OPTIONAL_PARAMS_NAME);

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_CODE, respCode);
            grantTypeExpectations(null, clientGrantTypes, new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE });
            oidcRequestExpectation(null);
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(null));
                }
            });
            extendedPropertiesExpectations(pn, new String[] { OPTIONAL_PARAMS_VALUE });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_OK);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            // Ensure that no scopes are present in the result's attribute list
            AttributeList attributeList = result.getAttributeList();
            String[] scopeAttrs = attributeList.getAttributeValuesByName(OAuth20Constants.SCOPE);
            assertNull("Found scopes in result when there should not have been any: " + formatScopeStrings(scopeAttrs), scopeAttrs);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Request is OIDC request
     * 2. No scope parameter provided
     * Expected result: STATUS_FAILED with OAuth20InvalidScopeException
     */
    @Test
    public void validateAuthorization_MissingScopeOidcRequest() {

        final String[] stateArray = STATE_ARRAY;

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_CODE, respCode);
            grantTypeExpectations(null, clientGrantTypes, new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE });
            oidcRequestExpectation("req");

            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(null));
                }
            });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            missingScopeException(result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. duplicated scope
     * Expected result: STATUS_FAILED with OAuth20DuplicateParameterException
     */
    @Test
    public void validateAuthorization_MultipleScope() {

        final String[] stateArray = STATE_ARRAY;

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_CODE, respCode);
            grantTypeExpectations(null, clientGrantTypes, new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE });
            oidcRequestExpectation(null);

            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(new String[] { SCOPE, SCOPE }));
                }
            });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            duplicateParameterException(result, OAuth20Constants.SCOPE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Scope mismatch
     * 2. OAuth request
     * Expected result: STATUS_OK, no scopes included in the result
     */
    @Test
    public void validateAuthorization_MismatchScopeOAuthRequest() {

        final String[] stateArray = STATE_ARRAY;

        final StringTokenizer pn = new StringTokenizer(OAuth20Constants.CLIENT_ID + " " + OAuth20Constants.REDIRECT_URI + " " + OPTIONAL_PARAMS_NAME);

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_CODE, respCode);
            grantTypeExpectations(null, clientGrantTypes, new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE });
            oidcRequestExpectation(null);
            scopeExpectations(new String[] { "different" }, SCOPE, SCOPE);
            extendedPropertiesExpectations(pn, new String[] { OPTIONAL_PARAMS_VALUE });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_OK);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            // Ensure that no scopes are included in the request since the requested scope was neither registered nor preauthorized
            AttributeList attributeList = result.getAttributeList();
            String[] scopeAttrs = attributeList.getAttributeValuesByName(OAuth20Constants.SCOPE);
            assertNull("Found scopes in result when none should be present: " + formatScopeStrings(scopeAttrs), scopeAttrs);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. scope mismatch
     * 2. OidcRequest
     * Expected result: STATUS_OK, no scopes included in the result
     */
    @Test
    public void validateAuthorization_MismatchScopeOidcRequest() {

        final String[] stateArray = STATE_ARRAY;

        final StringTokenizer pn = new StringTokenizer(OAuth20Constants.CLIENT_ID + " " + OAuth20Constants.REDIRECT_URI + " " + OPTIONAL_PARAMS_NAME);

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_CODE, respCode);
            grantTypeExpectations(null, clientGrantTypes, new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE });
            oidcRequestExpectation("oidcreq");
            scopeExpectations(new String[] { "different" }, SCOPE, SCOPE);
            extendedPropertiesExpectations(pn, new String[] { OPTIONAL_PARAMS_VALUE });

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_OK);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            // Ensure that no scopes are included in the request since the requested scope was neither registered nor preauthorized
            AttributeList attributeList = result.getAttributeList();
            String[] scopeAttrs = attributeList.getAttributeValuesByName(OAuth20Constants.SCOPE);
            assertNull("Found scopes in result when none should be present: " + formatScopeStrings(scopeAttrs), scopeAttrs);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAuthorization method with following conditions
     * 1. Invalid scope string
     * Expected result: STATUS_FAILED with OAuth20BadParameterFormatException
     */
    @Test
    public void validateAuthorization_InvalidScope() {

        final String[] stateArray = STATE_ARRAY;

        final String INVALID_SCOPE = "invalid\\scope";

        try {
            createInitialExpectations(principal, stateArray);
            redirectUriExpectations(RUA, validRedirectUriArray);
            responseTypeExpectations(RT_CODE, respCode);
            grantTypeExpectations(null, clientGrantTypes, new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE });
            oidcRequestExpectation(null);
            scopeExpectations(new String[] { INVALID_SCOPE }, INVALID_SCOPE, INVALID_SCOPE);

            OAuthResult result = validateAuthorization(OAuthResult.STATUS_FAILED);

            // If a state was randomly chosen to be included, it must be included in the response
            verifyState(result, stateArray);

            OAuthException oe = result.getCause();
            badParameterException(oe, OAuth20Constants.SCOPE, INVALID_SCOPE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**************************************** validateAndHandle2LegsScope tests ****************************************/

    private void grantTypeExpectations2Legs(final String[] grantTypeParamValues, final JsonArray clientGrantTypes, final String[] grantTypesAllowed) throws OidcServerException {
        mock.checking(new Expectations() {
            {
                one(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                will(returnValue(grantTypeParamValues));
                one(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                will(returnValue(grantTypeParamValues));
            }
        });
        if (grantTypeParamValues != null && grantTypeParamValues.length > 1) {
            // Duplicate grant type parameters
            return;
        }
        getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
        mock.checking(new Expectations() {
            {
                one(obc).getGrantTypes();
                will(returnValue(clientGrantTypes));
                one(provider).getGrantTypesAllowed();
                will(returnValue(grantTypesAllowed));
            }
        });
    }

    private void scopeExpectations2Legged(final String[] scopeParamValues, final String clientScope, final String clientPreauthorizedScope) {
        mock.checking(new Expectations() {
            {
                between(1, 3).of(request).getParameterValues(OAuth20Constants.SCOPE);
                will(returnValue(scopeParamValues));
            }
        });
        if (scopeParamValues != null && scopeParamValues.length == 1) {
            mock.checking(new Expectations() {
                {
                    between(1, 2).of(obc).getScope();
                    will(returnValue(clientScope));
                    between(1, 3).of(obc).getPreAuthorizedScope();
                    will(returnValue(clientPreauthorizedScope));
                }
            });
        }
    }

    private void autoAuthzExpectations(final String autoAuthzParam, final String authzParamValue, final boolean isAuthoAuthz, final String[] autoAuthzClients) {
        mock.checking(new Expectations() {
            {
                one(provider).getAutoAuthorizeParam();
                will(returnValue(autoAuthzParam));
            }
        });
        if (autoAuthzParam != null && !autoAuthzParam.isEmpty()) {
            mock.checking(new Expectations() {
                {
                    one(request).getParameter(autoAuthzParam);
                    will(returnValue(authzParamValue));
                    one(provider).isAutoAuthorize();
                    will(returnValue(isAuthoAuthz));
                }
            });
            if ("true".equalsIgnoreCase(authzParamValue) || isAuthoAuthz) {
                mock.checking(new Expectations() {
                    {
                        one(provider).getAutoAuthorizeClients();
                        will(returnValue(autoAuthzClients));
                    }
                });
            }
        }
    }

    private void setAttributeExpectations() {
        mock.checking(new Expectations() {
            {
                one(request).setAttribute(with(any(String.class)), with(any(AttributeList.class)));
            }
        });
    }

    private void neverSetAttributeExpectations() {
        mock.checking(new Expectations() {
            {
                never(request).setAttribute(with(any(String.class)), with(any(AttributeList.class)));
            }
        });
    }

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. Client with the provided ID not found
     * Expected result: STATUS_FAILED with OAuth20InvalidClientException
     */
    @Test
    public void validateAndHandle2LegsScope_InvalidClient() {
        // Behavior should be the same for client_credentials, password, and jwt grant types
        final String[] requestedGrantType = new String[] { OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS };

        try {
            mock.checking(new Expectations() {
                {
                    exactly(2).of(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(requestedGrantType));
                    exactly(2).of(provider).getClientProvider();
                    will(returnValue(oocp));
                    exactly(2).of(oocp).get(CID);
                    will(returnValue(null));
                    one(provider).getGrantTypesAllowed();
                    will(returnValue(requestedGrantType));
                }
            });
            oidcRequestExpectation("oidcreq");
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(new String[] { SCOPE }));
                }
            });
            neverSetAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_FAILED);

            invalidClientException(result, CID);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. OIDC request
     * 2. AuthAuthz is true
     * Expected result: STATUS_OK, requested scope contained in the result
     */
    @Test
    public void validateAndHandle2LegsScope_AutoAuthzTrue() {

        // Behavior should be the same for client_credentials, password, and jwt grant types
        final String[] requestedGrantType = new String[] { OAuth20Constants.GRANT_TYPE_PASSWORD };

        try {
            grantTypeExpectations2Legs(requestedGrantType, clientGrantTypes, requestedGrantType);
            oidcRequestExpectation("oidcreq");
            scopeExpectations2Legged(new String[] { SCOPE }, SCOPE, SCOPE);
            autoAuthzExpectations(AUTHZ_NAME, "true", true, CID_ARRAY);
            setAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_OK);

            verify2LegsResult(result, true, OAuth20Constants.REQUEST_FEATURE_OIDC, SCOPE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. OIDC request
     * 2. AuthAuthz is false
     * Expected result: STATUS_OK, requested scope contained in the result
     */
    @Test
    public void validateAndHandle2LegsScope_AutoAuthzFalse() {

        // Behavior should be the same for client_credentials, password, and jwt grant types
        final String[] requestedGrantType = new String[] { OAuth20Constants.GRANT_TYPE_JWT };

        try {
            grantTypeExpectations2Legs(requestedGrantType, clientGrantTypes, requestedGrantType);
            oidcRequestExpectation("oidcreq");
            scopeExpectations2Legged(new String[] { SCOPE }, SCOPE, SCOPE);
            autoAuthzExpectations(AUTHZ_NAME, "true", false, CID_ARRAY);
            setAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_OK);

            verify2LegsResult(result, true, OAuth20Constants.REQUEST_FEATURE_OIDC, SCOPE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /***************************************** Grant type tests *****************************************/

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. Request does not contain grant_type param
     * 2. Unknown grant type is not registered
     * Expected result: STATUS_FAILED with OAuth20InvalidGrantTypeException
     */
    @Test
    public void validateAndHandle2LegsScope_NoGrantType() {

        try {
            // Behavior should be the same regardless of what grant types are allowed by the provider
            grantTypeExpectations2Legs(null, clientGrantTypes, new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE });
            neverSetAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_FAILED);

            invalidGrantTypeException(result, GRANT_TYPE_UNKNOWN);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. Request does not contain grant_type param, so "unknown" grant type is assumed
     * 2. "unknown" grant type is registered
     * Expected result: STATUS_OK with no attributes in the result
     */
    @Test
    public void validateAndHandle2LegsScope_NoGrantTypeUnknownGrantTypeRegistered() {

        JsonArray unknownGrantTypeArray = new JsonArray();
        unknownGrantTypeArray.add(new JsonPrimitive(GRANT_TYPE_UNKNOWN));

        try {
            grantTypeExpectations2Legs(null, unknownGrantTypeArray, new String[] { GRANT_TYPE_UNKNOWN });
            neverSetAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_OK);

            verifyEmpty2LegsResult(result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. grant_type = ""
     * Expected result: STATUS_FAILED with OAuth20InvalidGrantTypeException
     */
    @Test
    public void validateAndHandle2LegsScope_EmptyGrantType() {

        try {
            // Behavior should be the same regardless of what grant types are allowed by the provider
            grantTypeExpectations2Legs(new String[] { "" }, clientGrantTypes, new String[] { OAuth20Constants.GRANT_TYPE_PASSWORD });
            neverSetAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_FAILED);

            invalidGrantTypeException(result, "");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. grant_type = multiple parameter values
     * Expected result: STATUS_FAILED with OAuth20DuplicateParameterException
     */
    @Test
    public void validateAndHandle2LegsScope_MultipleGrantType() {

        final String[] grantTypeParamValues = new String[] { OAuth20Constants.GRANT_TYPE_JWT, OAuth20Constants.GRANT_TYPE_JWT };

        try {
            grantTypeExpectations2Legs(grantTypeParamValues, null, null);
            neverSetAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_FAILED);

            duplicateParameterException(result, OAuth20Constants.GRANT_TYPE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. grant_type = client_credentials
     * 1. client_credentials grant type is not supported
     * Expected result: STATUS_FAILED with OAuth20InvalidGrantTypeException
     */
    @Test
    public void validateAndHandle2LegsScope_ClientCredentialsGrantTypeNotSupported() {

        final String[] providerAllowedGrantTypes = new String[] { OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE,
                OAuth20Constants.GRANT_TYPE_JWT };
        try {
            grantTypeExpectations2Legs(new String[] { OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS }, clientGrantTypes, providerAllowedGrantTypes);
            neverSetAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_FAILED);

            invalidGrantTypeException(result, OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. grant_type = client_credentials or password (chosen at random)
     * 2. OAuth request
     * Expected result: STATUS_OK
     */
    @Test
    public void validateAndHandle2LegsScope_ClientCredentialsOrPasswordGrantType() {

        // Behavior should be the same for the client_credentials and password grant types
        final String[] clientCredentialsArray = new String[] { OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS };

        try {
            grantTypeExpectations2Legs(clientCredentialsArray, clientGrantTypes, clientCredentialsArray);
            oidcRequestExpectation(null);
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(new String[] { "scope1 scope2 scope1" }));
                }
            });
            setAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_OK);

            verify2LegsResult(result, true, OAuth20Constants.REQUEST_FEATURE_OAUTH2, new String[] { "scope1 scope2" });

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. grant_type = client_credentials or password (chosen at random)
     * 2. OAuth request
     * 3. No scopes included in request
     * Expected result: STATUS_OK, no scopes included in the result
     */
    @Test
    public void validateAndHandle2LegsScope_ClientCredentialsOrPasswordGrantTypeMissingScopes() {

        // Behavior should be the same for the client_credentials and password grant types
        final String[] clientCredentialsArray = new String[] { OAuth20Constants.GRANT_TYPE_PASSWORD };

        try {
            grantTypeExpectations2Legs(clientCredentialsArray, clientGrantTypes, clientCredentialsArray);
            oidcRequestExpectation(null);
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(null));
                }
            });
            neverSetAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_OK);

            verify2LegsResult(result, true, OAuth20Constants.REQUEST_FEATURE_OAUTH2, new String[0]);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. grant_type = NOT client_credientials, jwt, or password
     * Expected result: STATUS_OK, no attributes included in the result
     */
    @Test
    public void validateAndHandle2LegsScope_NotNeededForGrantType() {
        // Behavior should be the same for all of these grant types
        final String[] grantTypes = getGrantTypesExcluding(OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS,
                OAuth20Constants.GRANT_TYPE_JWT,
                OAuth20Constants.GRANT_TYPE_PASSWORD);
        final String[] clientCredentialsArray = new String[] { grantTypes[0] };

        try {
            grantTypeExpectations2Legs(clientCredentialsArray, clientGrantTypes, clientCredentialsArray);
            neverSetAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_OK);

            verifyEmpty2LegsResult(result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. grant_type = Chosen at random
     * 2. Chosen grant type is not supported
     * Expected result: STATUS_FAILED with OAuth20InvalidGrantTypeException
     */
    @Test
    public void validateAndHandle2LegsScope_GrantTypeNotSupported() {

        final String requestedGrantType = OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS;

        // Provider supports all grant types except the requested grant type
        final String[] supportedGrantTypes = getGrantTypesExcluding(requestedGrantType);

        try {
            grantTypeExpectations2Legs(new String[] { requestedGrantType }, clientGrantTypes, supportedGrantTypes);
            neverSetAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_FAILED);

            invalidGrantTypeException(result, requestedGrantType);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /***************************************** Scope tests *****************************************/

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. Request contains duplicate scope parameters
     * 2. grant_type = jwt
     * 3. OAuth request
     * Expected result: STATUS_FAILED with OAuth20DuplicateParameterException
     */
    @Test
    public void validateAndHandle2LegsScope_OAuthDuplicateScopes() {

        final String[] multipleScopes = new String[] { "other", SCOPE };
        final String[] requestedGrantType = new String[] { OAuth20Constants.GRANT_TYPE_JWT };

        try {
            grantTypeExpectations2Legs(requestedGrantType, clientGrantTypes, requestedGrantType);
            oidcRequestExpectation(null);
            scopeExpectations2Legged(multipleScopes, null, null);
            neverSetAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_FAILED);

            duplicateParameterException(result, OAuth20Constants.SCOPE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. Request contains duplicate scope parameters
     * 2. grant_type = client_credentials
     * 3. OIDC request
     * Expected result: STATUS_FAILED with OAuth20DuplicateParameterException
     */
    @Test
    public void validateAndHandle2LegsScope_OIDCDuplicateScopes() {

        final String[] multipleScopes = new String[] { "other", SCOPE };
        final String[] requestedGrantType = new String[] { OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS };

        try {
            grantTypeExpectations2Legs(requestedGrantType, clientGrantTypes, requestedGrantType);
            oidcRequestExpectation("oidcreq");
            scopeExpectations2Legged(multipleScopes, null, null);
            neverSetAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_FAILED);

            duplicateParameterException(result, OAuth20Constants.SCOPE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. Requested scopes and registered scopes share no common values
     * Expected result: STATUS_FAILED with OAuth20InvalidScopeException
     */
    @Test
    public void validateAndHandle2LegsScope_NoCommonScopes() {

        final String[] requestedGrantType = new String[] { OAuth20Constants.GRANT_TYPE_PASSWORD };
        final String[] requestedScopes = new String[] { "other" };
        final String[] registeredScopes = new String[] { SCOPE };

        try {
            grantTypeExpectations2Legs(requestedGrantType, clientGrantTypes, requestedGrantType);
            oidcRequestExpectation("oidcreq");
            scopeExpectations2Legged(requestedScopes, SCOPE, null);
            neverSetAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_FAILED);

            invalidScopeException(result, registeredScopes, requestedScopes);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. Request is missing scope parameter
     * 2. grant_type = client_credentials or password
     * 3. OAuth request
     * Expected result: STATUS_OK with no scopes in the result
     */
    @Test
    public void validateAndHandle2LegsScope_OAuthMissingScopes() {
        // Behavior should be the same for client_credentials and password grant types
        final String[] requestedGrantType = new String[] { OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS };

        try {
            grantTypeExpectations2Legs(requestedGrantType, clientGrantTypes, requestedGrantType);
            oidcRequestExpectation(null);
            scopeExpectations2Legged(null, null, null);
            neverSetAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_OK);

            verify2LegsResult(result, true, OAuth20Constants.REQUEST_FEATURE_OAUTH2, new String[0]);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. Request is missing scope parameter
     * 2. grant_type = jwt
     * 3. OAuth request
     * Expected result: STATUS_FAILED with OAuth20InvalidScopeException
     */
    @Test
    public void validateAndHandle2LegsScope_OAuthMissingScopesJwtGrantType() {

        final String[] requestedGrantType = new String[] { OAuth20Constants.GRANT_TYPE_JWT };

        try {
            grantTypeExpectations2Legs(requestedGrantType, clientGrantTypes, requestedGrantType);
            oidcRequestExpectation(null);
            mock.checking(new Expectations() {
                {
                    exactly(2).of(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(null));
                    one(obc).getScope();
                    will(returnValue(null));
                }
            });
            neverSetAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_FAILED);

            invalidScopeException(result, null, null);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. Request is missing scope parameter
     * 2. grant_type = password
     * 3. OIDC request
     * Expected result: STATUS_FAILED with an OAuth20InvalidScopeException
     */
    @Test
    public void validateAndHandle2LegsScope_OIDCMissingScopes() {
        // Behavior should be the same for client_credentials, jwt, and password grant types
        final String[] requestedGrantType = new String[] { OAuth20Constants.GRANT_TYPE_PASSWORD };

        try {
            grantTypeExpectations2Legs(requestedGrantType, clientGrantTypes, requestedGrantType);
            oidcRequestExpectation("oidcreq");
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(null));
                }
            });
            neverSetAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_FAILED);

            missingScopeException(result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. preAuthzScope is not set
     * Expected result: STATUS_FAILED with InvalidGrantException
     */
    @Test
    public void validateAndHandle2LegsScope_NullPreAuthzScope() {

        try {
            grantTypeExpectations2Legs(new String[] { OAuth20Constants.GRANT_TYPE_JWT }, clientGrantTypes, new String[] { OAuth20Constants.GRANT_TYPE_JWT });
            oidcRequestExpectation("oidcreq");
            scopeExpectations2Legged(new String[] { SCOPE }, SCOPE, null);
            autoAuthzExpectations(AUTHZ_NAME, "false", false, null);
            neverSetAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_FAILED);

            OAuthException oe = result.getCause();
            assertTrue("Exception was not of expected type OAuth20Exception.", oe instanceof OAuth20Exception);
            OAuth20Exception ise = (OAuth20Exception) oe;
            assertEquals("Did not get the expected exception error.", OAuth20Exception.INVALID_SCOPE, ise.getError());
            assertEquals("CWWKS1416E: The token endpoint request failed because the client [" + CID
                    + "] is not autoAuthorized and it does not define the 'preAuthorizedScope' list in its configuration. No scopes can be authorized.", ise.getMessage());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. preAuthzScope does not match requested scope
     * Expected result: STATUS_FAILED with InvalidGrantException
     */
    @Test
    public void validateAndHandle2LegsScope_InvalidPreAuthzScope() {

        try {
            grantTypeExpectations2Legs(new String[] { OAuth20Constants.GRANT_TYPE_JWT }, clientGrantTypes, new String[] { OAuth20Constants.GRANT_TYPE_JWT });
            oidcRequestExpectation("oidcreq");
            scopeExpectations2Legged(new String[] { SCOPE }, SCOPE, INVALID_SCOPE);
            autoAuthzExpectations(AUTHZ_NAME, "false", false, null);
            neverSetAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_FAILED);

            OAuthException oe = result.getCause();
            assertTrue("Exception was not of expected type OAuth20Exception.", oe instanceof OAuth20Exception);
            OAuth20Exception ise = (OAuth20Exception) oe;
            assertEquals("Did not get the expected exception error.", OAuth20Exception.INVALID_SCOPE, ise.getError());
            assertEquals("CWWKS1415E: The token endpoint request failed because one of the scopes in the scope parameter of the request was not defined in the 'preAuthorizedScope' list of client ["
                    + CID + "].", ise.getMessage());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests validateAndHandle2LegsScope method with following conditions
     * 1. Request contains multiple scopes, one of which is neither registered nor preauthorized
     * 2. Multiple scopes registered
     * 3. Multiple scopes preauthorized
     * 4. Client is not auto authorized
     * Expected result: STATUS_OK with the subset of scopes from the request that are registered included in the result
     */
    @Test
    public void validateAndHandle2LegsScope_NonPreAuthzScopeInRequest() {
        // Behavior should be the same for client_credentials, password, and jwt grant types
        final String[] requestedGrantType = new String[] { OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS };

        try {
            grantTypeExpectations2Legs(requestedGrantType, clientGrantTypes, requestedGrantType);
            oidcRequestExpectation("oidcreq");
            scopeExpectations2Legged(new String[] { SCOPE + " scope1 scope2" }, SCOPE + " scope2", SCOPE + " scope2");
            autoAuthzExpectations(AUTHZ_NAME, "false", false, null);
            setAttributeExpectations();

            OAuthResult result = validateAndHandle2LegsScope(OAuthResult.STATUS_OK);

            verify2LegsResult(result, true, OAuth20Constants.REQUEST_FEATURE_OIDC, SCOPE, "scope2");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /******************************* isValidResponseTypeForAuthorizationCodeGrantType tests *******************************/

    /**
     * Tests isValidResponseTypeForAuthorizationCodeGrantType method with following conditions
     * 1. Both arguments are null
     * 2. Either argument is null
     * Expected result: false in all cases
     */
    @Test
    public void isValidResponseTypeForAuthorizationCodeGrantType_NullArgs() {
        ClientAuthorization ca = new ClientAuthorization();
        assertFalse("Null arguments should produce false.", ca.isValidResponseTypeForAuthorizationCodeGrantType(null, null));
        assertFalse("Null response type should produce false.", ca.isValidResponseTypeForAuthorizationCodeGrantType(null, OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE));
        assertFalse("Null grant type should produce false.", ca.isValidResponseTypeForAuthorizationCodeGrantType(OAuth20Constants.RESPONSE_TYPE_CODE, null));
    }

    /**
     * Tests isValidResponseTypeForAuthorizationCodeGrantType method with following conditions
     * 1. Grant type argument = "authorization_code"
     * 2. Response type argument is invalid
     * Expected result: false in all cases
     */
    @Test
    public void isValidResponseTypeForAuthorizationCodeGrantType_InvalidResponseType() {
        final String[] invalidRts = new String[] { "",
                OAuth20Constants.RESPONSE_TYPE_CODE.substring(1),
                "my" + OAuth20Constants.RESPONSE_TYPE_CODE,
                "my " + OAuth20Constants.RESPONSE_TYPE_CODE,
                OAuth20Constants.RESPONSE_TYPE_TOKEN,
                OIDCConstants.RESPONSE_TYPE_ID_TOKEN };

        for (String invalidResponseType : invalidRts) {
            ClientAuthorization ca = new ClientAuthorization();
            assertFalse(invalidResponseType + " response type should produce false.",
                    ca.isValidResponseTypeForAuthorizationCodeGrantType(invalidResponseType, OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE));
        }
    }

    /**
     * Tests isValidResponseTypeForAuthorizationCodeGrantType method with following conditions
     * 1. Grant type argument is invalid
     * 2. Response type argument = "code"
     * Expected result: false in all cases
     */
    @Test
    public void isValidResponseTypeForAuthorizationCodeGrantType_InvalidGrantType() {
        final String[] invalidGts = new String[] { "",
                OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE.substring(1),
                "my" + OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE,
                "my " + OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE,
                OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS,
                OAuth20Constants.GRANT_TYPE_IMPLICIT,
                OAuth20Constants.GRANT_TYPE_JWT,
                OAuth20Constants.GRANT_TYPE_PASSWORD,
                OAuth20Constants.GRANT_TYPE_REFRESH_TOKEN,
                OAuth20Constants.GRANT_TYPE_RESOURCE_OWNER };

        for (String invalidGrantType : invalidGts) {
            ClientAuthorization ca = new ClientAuthorization();
            assertFalse(invalidGrantType + " grant type should produce false.",
                    ca.isValidResponseTypeForAuthorizationCodeGrantType(OAuth20Constants.RESPONSE_TYPE_CODE, invalidGrantType));
        }
    }

    /**
     * Tests isValidResponseTypeForAuthorizationCodeGrantType method with following conditions
     * 1. Grant type argument = "authorization_code"
     * 2. Response type argument = "code"
     * Expected result: true
     */
    @Test
    public void isValidResponseTypeForAuthorizationCodeGrantType_Valid() {
        ClientAuthorization ca = new ClientAuthorization();
        assertTrue("Valid response type and grant type should produce true.",
                ca.isValidResponseTypeForAuthorizationCodeGrantType(OAuth20Constants.RESPONSE_TYPE_CODE, OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE));
    }

    /******************************* isValidResponseTypeForImplicitGrantType tests *******************************/

    /**
     * Tests isValidResponseTypeForImplicitGrantType method with following conditions
     * 1. Both arguments are null
     * 2. Either argument is null
     * Expected result: false in all cases
     */
    @Test
    public void isValidResponseTypeForImplicitGrantType_NullArgs() {
        ClientAuthorization ca = new ClientAuthorization();
        assertFalse("Null arguments should produce false.", ca.isValidResponseTypeForImplicitGrantType(null, null));
        assertFalse("Null response type should produce false.", ca.isValidResponseTypeForImplicitGrantType(null, OAuth20Constants.GRANT_TYPE_IMPLICIT));
        assertFalse("Null grant type should produce false.", ca.isValidResponseTypeForImplicitGrantType(OAuth20Constants.RESPONSE_TYPE_TOKEN, null));
        assertFalse("Null grant type should produce false.", ca.isValidResponseTypeForImplicitGrantType(OIDCConstants.RESPONSE_TYPE_ID_TOKEN, null));
    }

    /**
     * Tests isValidResponseTypeForImplicitGrantType method with following conditions
     * 1. Grant type argument = "implicit"
     * 2. Response type argument is invalid
     * Expected result: false in all cases
     */
    @Test
    public void isValidResponseTypeForImplicitGrantType_InvalidResponseType() {
        final String[] invalidRts = new String[] { "",
                OAuth20Constants.RESPONSE_TYPE_TOKEN.substring(1),
                "my" + OAuth20Constants.RESPONSE_TYPE_TOKEN,
                OIDCConstants.RESPONSE_TYPE_ID_TOKEN.substring(1),
                "my" + OIDCConstants.RESPONSE_TYPE_ID_TOKEN,
                OAuth20Constants.RESPONSE_TYPE_CODE };

        for (String invalidResponseType : invalidRts) {
            ClientAuthorization ca = new ClientAuthorization();
            assertFalse(invalidResponseType + " response type should produce false.",
                    ca.isValidResponseTypeForImplicitGrantType(invalidResponseType, OAuth20Constants.GRANT_TYPE_IMPLICIT));
        }
    }

    /**
     * Tests isValidResponseTypeForImplicitGrantType method with following conditions
     * 1. Grant type argument is invalid
     * 2. Response type argument = "token" or "id_token"
     * Expected result: false in all cases
     */
    @Test
    public void isValidResponseTypeForImplicitGrantType_InvalidGrantType() {
        final String[] invalidGts = new String[] { "",
                OAuth20Constants.GRANT_TYPE_IMPLICIT.substring(1),
                "my" + OAuth20Constants.GRANT_TYPE_IMPLICIT,
                "my " + OAuth20Constants.GRANT_TYPE_IMPLICIT,
                OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE,
                OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS,
                OAuth20Constants.GRANT_TYPE_JWT,
                OAuth20Constants.GRANT_TYPE_PASSWORD,
                OAuth20Constants.GRANT_TYPE_REFRESH_TOKEN,
                OAuth20Constants.GRANT_TYPE_RESOURCE_OWNER };

        for (String invalidGrantType : invalidGts) {
            ClientAuthorization ca = new ClientAuthorization();
            assertFalse(invalidGrantType + " grant type should produce false.", ca.isValidResponseTypeForImplicitGrantType(OAuth20Constants.RESPONSE_TYPE_TOKEN, invalidGrantType));
        }
        for (String invalidGrantType : invalidGts) {
            ClientAuthorization ca = new ClientAuthorization();
            assertFalse(invalidGrantType + " grant type should produce false.", ca.isValidResponseTypeForImplicitGrantType(OIDCConstants.RESPONSE_TYPE_ID_TOKEN, invalidGrantType));
        }
    }

    /**
     * Tests isValidResponseTypeForImplicitGrantType method with following conditions
     * 1. Grant type argument = "implicit"
     * 2. Response type argument = "token" or "id_token" or contains either
     * Expected result: true
     */
    @Test
    public void isValidResponseTypeForImplicitGrantType_Valid() {
        ClientAuthorization ca = new ClientAuthorization();
        assertTrue(ca.isValidResponseTypeForImplicitGrantType(OAuth20Constants.RESPONSE_TYPE_TOKEN, OAuth20Constants.GRANT_TYPE_IMPLICIT));
        assertTrue(ca.isValidResponseTypeForImplicitGrantType(OIDCConstants.RESPONSE_TYPE_ID_TOKEN, OAuth20Constants.GRANT_TYPE_IMPLICIT));
        assertTrue(ca.isValidResponseTypeForImplicitGrantType(OAuth20Constants.RESPONSE_TYPE_TOKEN + " extra", OAuth20Constants.GRANT_TYPE_IMPLICIT));
        assertTrue(ca.isValidResponseTypeForImplicitGrantType("extra other " + OAuth20Constants.RESPONSE_TYPE_TOKEN, OAuth20Constants.GRANT_TYPE_IMPLICIT));
        assertTrue(ca.isValidResponseTypeForImplicitGrantType(OIDCConstants.RESPONSE_TYPE_ID_TOKEN + " other", OAuth20Constants.GRANT_TYPE_IMPLICIT));
        assertTrue(ca.isValidResponseTypeForImplicitGrantType("extra " + OIDCConstants.RESPONSE_TYPE_ID_TOKEN + " other", OAuth20Constants.GRANT_TYPE_IMPLICIT));
    }

    /**************************************** getReducedScopes tests ****************************************/

    /**
     * Tests getReducedScopes method with following conditions
     * 1. no scope params
     * Expected result: null
     */
    @Test
    public void getReducedScopes_NullScopeParam() {

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(null));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            assertNull(ca.getReducedScopes(provider, request, CID, false));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests getReducedScopes method with following conditions
     * 1. No client found for default ID
     * Expected result: OAuth20InvalidClientException
     */
    @Test
    public void getReducedScopes_NoClient() {

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, null);
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(new String[] { "openid profile" }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            ca.getReducedScopes(provider, request, CID, false);
            fail("OAuth20InvalidClientException should be thrown.");

        } catch (OAuth20Exception oe) {
            invalidClientException(oe, CID);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests getReducedScopes method with following conditions
     * 1. Request includes invalid scope
     * Expected result: OAuth20BadParameterFormatException
     */
    @Test
    public void getReducedScopes_RequestIncludesInvalidScope() {

        final String invalidScope = "invalid\\scope";

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(new String[] { "openid " + invalidScope }));
                    one(obc).getScope();
                    will(returnValue(null));
                    one(obc).getPreAuthorizedScope();
                    will(returnValue(null));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            ca.getReducedScopes(provider, request, CID, false);
            fail("OAuth20BadParameterFormatException should be thrown.");

        } catch (OAuth20Exception oe) {
            badParameterException(oe, OAuth20Constants.SCOPE, invalidScope);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests getReducedScopes method with following conditions
     * 1. Request includes invalid scope
     * Expected result: OAuth20BadParameterFormatException
     */
    @Test
    public void getReducedScopes_DuplicateScope() {

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(new String[] { SCOPE, SCOPE }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            ca.getReducedScopes(provider, request, CID, false);
            fail("OAuth20DuplicateParameterException should be thrown.");

        } catch (OAuth20Exception oe) {
            duplicateParameterException(oe, OAuth20Constants.SCOPE);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests getReducedScopes method with following conditions
     * 1. Request includes a single empty scope
     * Expected result: Empty array
     */
    @Test
    public void getReducedScopes_EmptyScope() {

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(new String[] { "" }));
                    one(obc).getScope();
                    will(returnValue(SCOPE));
                    one(obc).getPreAuthorizedScope();
                    will(returnValue(null));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            String[] reducedScopes = ca.getReducedScopes(provider, request, CID, false);
            assertEquals("Found scopes in result when none should be present.", Arrays.toString(new String[0]), Arrays.toString(reducedScopes));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests getReducedScopes method with following conditions
     * 1. Request includes multiple scopes, including at least one that isn't registered
     * Expected result: Array of strings with only the requested scopes that are registered
     */
    @Test
    public void getReducedScopes_RequestIncludesExtraScopes() {

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(new String[] { SCOPE + " scope1 scope2" }));
                    one(obc).getScope();
                    will(returnValue(SCOPE + " scope1 scope3"));
                    one(obc).getPreAuthorizedScope();
                    will(returnValue(null));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            String[] reducedScopes = ca.getReducedScopes(provider, request, CID, false);
            assertEquals("Did not get the expected list of reduced scopes.", formatScopeStrings(SCOPE, "scope1"), formatScopeStrings(reducedScopes));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**************************************** urlsMatch tests ****************************************/

    /**
     * Tests urlsMatch method with following conditions
     * 1. urls are identical
     * Expected result: true
     */
    @Test
    public void urlsMatch_Match() {
        final String URL_A = "http://localhost/path";
        ClientAuthorization ca = new ClientAuthorization();
        assertTrue(ca.urlsMatch(URL_A, URL_A));
    }

    /**
     * Tests urlsMatch method with following conditions
     * 1. 1st url is null
     * Expected result: false
     */
    @Test
    public void urlsMatch_1stNull() {
        final String URL_A = "http://localhost/path";
        ClientAuthorization ca = new ClientAuthorization();
        assertFalse(ca.urlsMatch(null, URL_A));
    }

    /**
     * Tests urlsMatch method with following conditions
     * 1. 2nd url is null
     * Expected result: false
     */
    @Test
    public void urlsMatch_2ndNull() {
        final String URL_A = "http://localhost/path";
        ClientAuthorization ca = new ClientAuthorization();
        assertFalse(ca.urlsMatch(URL_A, null));
    }

    /**
     * Tests urlsMatch method with following conditions
     * 1. urls are not identical
     * Expected result: false
     */
    @Test
    public void urlsMatch_NotMatch() {
        final String URL_A = "http://localhost/path";
        final String URL_B = "https://localhost/path";
        ClientAuthorization ca = new ClientAuthorization();
        assertFalse(ca.urlsMatch(URL_A, URL_B));
    }

    /**
     * Tests urlsMatch method with following conditions
     * 1. urls are logically identical
     * Expected result: true
     */
    @Test
    public void urlsMatch_LogicalMatchTCP() {
        final String URL_A = "http://localhost/path";
        final String URL_B = "http://localhost:80/path";
        ClientAuthorization ca = new ClientAuthorization();
        assertTrue(ca.urlsMatch(URL_A, URL_B));
    }

    /**
     * Tests urlsMatch method with following conditions
     * 1. urls are not logically identical
     * Expected result: true
     */
    @Test
    public void urlsMatch_LogicalMisatchTCP() {
        final String URL_A = "http://localhost:80/path";
        final String URL_B = "http://localhost:8/path";
        ClientAuthorization ca = new ClientAuthorization();
        assertFalse(ca.urlsMatch(URL_A, URL_B));
    }

    /**
     * Tests urlsMatch method with following conditions
     * 1. urls are logically identical with ssl.
     * Expected result: true
     */
    @Test
    public void urlsMatch_LogicalMatchSSL() {
        final String URL_A = "https://localhost:443/path";
        final String URL_B = "https://localhost/path";
        ClientAuthorization ca = new ClientAuthorization();
        assertTrue(ca.urlsMatch(URL_A, URL_B));
    }

    /**
     * Tests urlsMatch method with following conditions
     * 1. urls are not logically identical with ssl.
     * Expected result: true
     */
    @Test
    public void urlsMatch_LogicalMismatchSSL() {
        final String URL_A = "https://localhost:44/path";
        final String URL_B = "https://localhost:443/path";
        ClientAuthorization ca = new ClientAuthorization();
        assertFalse(ca.urlsMatch(URL_A, URL_B));
    }

    /**************************************** isClientAutoAuthorized tests ****************************************/

    /**
     * Tests isClientAutoAuthorized method with following conditions
     * 1. autoauthz name is either null or empty
     * Expected result: false
     */
    @Test
    public void isClientAutoAuthorized_NoAutoAuthzName() {
        mock.checking(new Expectations() {
            {
                one(provider).getAutoAuthorizeParam();
                will(returnValue(null));
            }
        });

        ClientAuthorization ca = new ClientAuthorization();
        assertFalse(ca.isClientAutoAuthorized(provider, request, CID));
    }

    /**
     * Tests isClientAutoAuthorized method with following conditions
     * 1. isAutoAuthorize is set to false
     * 2. autoauthz parameter is null
     * Expected result: false
     */
    @Test
    public void isClientAutoAuthorized_AutoAuthzParamNull_isAutoAuthzFalse() {
        autoAuthzExpectations(AUTHZ_NAME, null, false, null);

        ClientAuthorization ca = new ClientAuthorization();
        assertFalse(ca.isClientAutoAuthorized(provider, request, CID));
    }

    /**
     * Tests isClientAutoAuthorized method with following conditions
     * 1. isAutoAuthorize is set to false
     * 2. autoauthz parameter is also false
     * Expected result: false
     */
    @Test
    public void isClientAutoAuthorized_AutoAuthzParamFalse_isAutoAuthzFalse() {
        autoAuthzExpectations(AUTHZ_NAME, "false", false, null);

        ClientAuthorization ca = new ClientAuthorization();
        assertFalse(ca.isClientAutoAuthorized(provider, request, CID));
    }

    /**
     * Tests isClientAutoAuthorized method with following conditions
     * 1. autoauthz parameter is true
     * 2. isAutoAuthorize is set to false
     * 3. Auto-authorized clients is set to single empty value
     * Expected result: true
     */
    @Test
    public void isClientAutoAuthorized_EmptyAutoAuthzClient() {
        mock.checking(new Expectations() {
            {
                one(request).getParameter(OAuth20Constants.CLIENT_ID);
                will(returnValue(""));
            }
        });
        autoAuthzExpectations(AUTHZ_NAME, "true", false, new String[] { "" });

        ClientAuthorization ca = new ClientAuthorization();
        assertTrue(ca.isClientAutoAuthorized(provider, request));
    }

    /**
     * Tests isClientAutoAuthorized method with following conditions
     * 1. autoauthz parameter is true
     * 2. isAutoAuthorize is set to false
     * 3. Auto-authorized clients is not set
     * Expected result: false
     */
    @Test
    public void isClientAutoAuthorized_NoAutoAuthzClient() {
        mock.checking(new Expectations() {
            {
                one(request).getParameter(OAuth20Constants.CLIENT_ID);
                will(returnValue(CID));
            }
        });
        autoAuthzExpectations(AUTHZ_NAME, "true", false, null);

        ClientAuthorization ca = new ClientAuthorization();
        assertFalse(ca.isClientAutoAuthorized(provider, request));
    }

    /**
     * Tests isClientAutoAuthorized method with following conditions
     * 1. autoauthz parameter is false
     * 2. isAutoAuthorize is set to true
     * 3. Auto-authorized clients list does not include the requested client_id
     * Expected result: false
     */
    @Test
    public void isClientAutoAuthorized_ClientNotAllowed() {
        mock.checking(new Expectations() {
            {
                one(request).getParameter(OAuth20Constants.CLIENT_ID);
                will(returnValue(CID));
            }
        });
        autoAuthzExpectations(AUTHZ_NAME, "false", true, new String[] { "other_client" });

        ClientAuthorization ca = new ClientAuthorization();
        assertFalse(ca.isClientAutoAuthorized(provider, request));
    }

    /**
     * Tests isClientAutoAuthorized method with following conditions
     * 1. autoauthz parameter is null
     * 2. isAutoAuthorize is set to true
     * 3. Auto-authorized clients list includes the requested client_id
     * Expected result: true
     */
    @Test
    public void isClientAutoAuthorized_ConfiguredAndMatch() {
        mock.checking(new Expectations() {
            {
                one(request).getParameter(OAuth20Constants.CLIENT_ID);
                will(returnValue(CID));
            }
        });
        // Value of the autoAuthzParam doesn't matter if isAutoAuthorize is true
        String autoAuthzParam = null;
        autoAuthzExpectations(AUTHZ_NAME, autoAuthzParam, true, CID_ARRAY);

        ClientAuthorization ca = new ClientAuthorization();
        assertTrue(ca.isClientAutoAuthorized(provider, request));
    }

    /**************************************** isScopeRegistered tests ****************************************/

    /**
     * Tests isScopeRegistered method with following conditions
     * 1. No registered scopes
     * 2. Empty registered scope set is not allowed
     * Expected result: false
     */
    @Test
    public void isScopeRegistered_NoScope() {

        try {
            mock.checking(new Expectations() {
                {
                    allowing(obc).getPreAuthorizedScope();
                    will(returnValue(SCOPE));
                    one(reducer).isNullEmptyScope();
                    will(returnValue(true));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            assertFalse(ca.isScopeRegistered(reducer, SCOPE, false));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests isScopeRegistered method with following conditions
     * 1. No registered scopes
     * 2. Empty registered scope set is allowed
     * Expected result: true
     */
    @Test
    public void isScopeRegistered_NoScopeEmptyRegisteredScopesAllowed() {

        try {
            mock.checking(new Expectations() {
                {
                    allowing(obc).getPreAuthorizedScope();
                    will(returnValue(SCOPE));
                    one(reducer).isNullEmptyScope();
                    will(returnValue(true));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            assertTrue(ca.isScopeRegistered(reducer, SCOPE, true));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests isScopeRegistered method with following conditions
     * 1. Scope is not registered with the reducer
     * Expected result: false
     */
    @Test
    public void isScopeRegistered_ScopeNotRegistered() {

        try {
            mock.checking(new Expectations() {
                {
                    allowing(obc).getPreAuthorizedScope();
                    will(returnValue(SCOPE));
                    one(reducer).isNullEmptyScope();
                    will(returnValue(false));
                    one(reducer).hasClientScope(SCOPE);
                    will(returnValue(false));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            assertFalse(ca.isScopeRegistered(reducer, SCOPE, false));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests isScopeRegistered method with following conditions
     * 1. Scope is registered with the reducer
     * Expected result: true
     */
    @Test
    public void isScopeRegistered_ScopeRegistered() {

        try {
            mock.checking(new Expectations() {
                {
                    allowing(obc).getPreAuthorizedScope();
                    will(returnValue(SCOPE));
                    one(reducer).isNullEmptyScope();
                    will(returnValue(false));
                    one(reducer).hasClientScope(SCOPE);
                    will(returnValue(true));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            assertTrue(ca.isScopeRegistered(reducer, SCOPE, false));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**************************************** isPreAuthorizedScope tests ****************************************/

    /**
     * Tests isPreAuthorizedScope method with following conditions
     * 1. Null scope set provided
     * Expected result: false
     */
    @Test
    public void isPreAuthorizedScope_NoScope() {

        try {
            ClientAuthorization ca = new ClientAuthorization();
            assertFalse(ca.isPreAuthorizedScope(provider, CID, null));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests isPreAuthorizedScope method with following conditions
     * 1. No client provider found
     * Expected result: false
     */
    @Test
    public void isPreAuthorizedScope_NoClientProvider() {

        try {
            mock.checking(new Expectations() {
                {
                    allowing(provider).getClientProvider();
                    will(returnValue(null));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            assertFalse(ca.isPreAuthorizedScope(provider, CID, new String[] { SCOPE }));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests isPreAuthorizedScope method with following conditions
     * 1. No client found
     * Expected result: OAuth20InvalidClientException
     */
    @Test
    public void isPreAuthorizedScope_InvalidClient() {

        try {
            mock.checking(new Expectations() {
                {
                    allowing(provider).getClientProvider();
                    will(returnValue(oocp));
                    allowing(oocp).get(CID);
                    will(returnValue(null));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            ca.isPreAuthorizedScope(provider, CID, new String[] { SCOPE });

            fail("OAuth20InvalidClientException should be thrown.");

        } catch (OAuth20Exception oe) {
            invalidClientException(oe, CID);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests isPreAuthorizedScope method with following conditions
     * 1. Null/Empty preauthorized scopes
     * Expected result: false
     */
    @Test
    public void isPreAuthorizedScope_NoPreAuthScope() {

        try {
            mock.checking(new Expectations() {
                {
                    allowing(provider).getClientProvider();
                    will(returnValue(oocp));
                    allowing(oocp).get(CID);
                    will(returnValue(obc));
                    allowing(obc).getScope();
                    will(returnValue(SCOPE));
                    allowing(obc).getPreAuthorizedScope();
                    will(returnValue(null));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            assertFalse(ca.isPreAuthorizedScope(provider, CID, new String[] { SCOPE }));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests isPreAuthorizedScope method with following conditions
     * 1. Registered scope is not preauthorized
     * Expected result: false
     */
    @Test
    public void isPreAuthorizedScope_NotPreauthorized() {

        try {
            mock.checking(new Expectations() {
                {
                    allowing(provider).getClientProvider();
                    will(returnValue(oocp));
                    allowing(oocp).get(CID);
                    will(returnValue(obc));
                    allowing(obc).getScope();
                    will(returnValue(SCOPE));
                    allowing(obc).getPreAuthorizedScope();
                    will(returnValue(INVALID_SCOPE));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            assertFalse(ca.isPreAuthorizedScope(provider, CID, new String[] { SCOPE }));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests isPreAuthorizedScope method with following conditions
     * 1. Single scope registered that's a substring of a preauthorized scope
     * 2. Single scope preauthorized
     * 3. Check the provided scope
     * Expected result: false
     */
    @Test
    public void isPreAuthorizedScope_Substring() {

        final String registeredScopes = SCOPE.substring(0, 1);

        try {
            mock.checking(new Expectations() {
                {
                    one(provider).getClientProvider();
                    will(returnValue(oocp));
                    one(oocp).get(CID);
                    will(returnValue(obc));
                    exactly(2).of(obc).getPreAuthorizedScope();
                    will(returnValue(SCOPE));
                    one(obc).getScope();
                    will(returnValue(registeredScopes));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            assertFalse(ca.isPreAuthorizedScope(provider, CID, new String[] { registeredScopes }));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Tests isPreAuthorizedScope method with following conditions
     * 1. Multiple scopes registered
     * 2. Multiple scopes preauthorized
     * 3. Every registered scope is preauthorized
     * Expected result: false
     */
    @Test
    public void isPreAuthorizedScope_Valid() {

        final String registeredScopes = SCOPE + " scope2 scope3";

        try {
            mock.checking(new Expectations() {
                {
                    one(provider).getClientProvider();
                    will(returnValue(oocp));
                    one(oocp).get(CID);
                    will(returnValue(obc));
                    exactly(2).of(obc).getPreAuthorizedScope();
                    will(returnValue(SCOPE + " scope1 scope2 scope3"));
                    one(obc).getScope();
                    will(returnValue(registeredScopes));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            assertTrue(ca.isPreAuthorizedScope(provider, CID, registeredScopes.split(" ")));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**************************************** checkForEmptyRegisteredScopeSet tests ****************************************/

    /**
     * Null client
     * Expected result: Nothing to check
     */
    @Test
    public void checkForEmptyRegisteredScopeSet_NoClient() {

        try {
            ClientAuthorization ca = new ClientAuthorization();
            ca.checkForEmptyRegisteredScopeSet(null, CID);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Null/Empty registered scope set
     * Expected result: OAuth20InvalidScopeException
     */
    @Test
    public void checkForEmptyRegisteredScopeSet_NoScopes() {

        try {
            mock.checking(new Expectations() {
                {
                    one(obc).getScope();
                    will(returnValue(""));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            ca.checkForEmptyRegisteredScopeSet(obc, CID);
            fail("OAuth20InvalidScopeException should be thrown.");

        } catch (OAuth20InvalidScopeException oe) {

            assertEquals("Did not get the expected exception error.", OAuth20Exception.INVALID_SCOPE, oe.getError());
            assertTrue("Did not get expected error message",
                    oe.getMessage().contains("CWOAU0068E") && oe.getMessage().contains("[" + CID + "]"));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Non-empty registered scope set
     * Expected result: Nothing to check
     */
    @Test
    public void checkForEmptyRegisteredScopeSet_NonEmptyScopes() {

        try {
            mock.checking(new Expectations() {
                {
                    one(obc).getScope();
                    will(returnValue(SCOPE));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            ca.checkForEmptyRegisteredScopeSet(obc, CID);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**************************************** checkForMissingScopeInTheRequest tests ****************************************/

    /**
     * No scope parameter in the request
     * Expected result: OAuth20InvalidScopeException
     */
    @Test
    public void checkForMissingScopeInTheRequest_MissingScope() {

        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(null));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            ca.checkForMissingScopeInTheRequest(request);
            fail("OAuth20InvalidScopeException should be thrown.");

        } catch (OAuth20InvalidScopeException ise) {
            missingScopeException(ise);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Request contains scope parameter
     * Expected result: Nothing to check
     */
    @Test
    public void checkForMissingScopeInTheRequest_ValidScope() {

        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(new String[] { SCOPE }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            ca.checkForMissingScopeInTheRequest(request);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Request contains multiple scope parameters
     * Expected result: Nothing to check
     */
    @Test
    public void checkForMissingScopeInTheRequest_MultipleScope() {

        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(new String[] { SCOPE, SCOPE }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            ca.checkForMissingScopeInTheRequest(request);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**************************************** getRequestedScopes tests ****************************************/

    /**
     * No scope parameter in the request
     * Expected result: Null
     */
    @Test
    public void getRequestedScopes_NoScope() {

        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(null));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            assertNull("Should get a null result for a null scope parameter.", ca.getRequestedScopes(request));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Multiple scope parameters in the request
     * Expected result: The first value in the scope array
     */
    @Test
    public void getRequestedScopes_MultipleScopes() {

        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(new String[] { SCOPE, "other" }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            assertEquals("Did not get expected scope value.", SCOPE, ca.getRequestedScopes(request));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * One scope parameter in the request
     * Expected result: The value of the scope parameter
     */
    @Test
    public void getRequestedScopes_SingleScope() {

        final String scopeValue = "hello world";

        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(new String[] { scopeValue }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            assertEquals("Did not get expected scope value.", scopeValue, ca.getRequestedScopes(request));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**************************************** validateGrantTypes tests ****************************************/

    /**
     * No grant_type parameter in request
     * response_type parameter is null or some unknown value
     * Grant type being used will end up being "unknown"
     * "unknown" is NOT within the reduced grant types set
     * Expected result: OAuth20InvalidGrantTypeException
     */
    @Test
    public void validateGrantTypes_NoRequestedGrantType_OtherResponseType_UnknownNotRegistered() {

        final String grantType = OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE;

        final JsonArray registeredGrantTypes = new JsonArray();
        registeredGrantTypes.add(new JsonPrimitive(grantType));

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(null));
                    one(obc).getGrantTypes();
                    will(returnValue(registeredGrantTypes));
                    one(provider).getGrantTypesAllowed();
                    will(returnValue(new String[] { grantType }));
                }
            });

            String responseTypeValue = "unknown value";

            ClientAuthorization ca = new ClientAuthorization();
            ca.validateGrantTypes(provider, request, CID, responseTypeValue);

            fail("OAuth20InvalidGrantTypeException should be thrown.");

        } catch (OAuthException oe) {
            invalidGrantTypeException(oe, GRANT_TYPE_UNKNOWN);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * No grant_type parameter in request
     * response_type parameter is null or some unknown value
     * Grant type being used will end up being "unknown"
     * "unknown" is within the reduced grant types set
     * Expected result: true
     */
    @Test
    public void validateGrantTypes_NoGrantTypeOrResponseType_UnknownRegistered() {

        final JsonArray registeredGrantTypes = new JsonArray();
        registeredGrantTypes.add(new JsonPrimitive(GRANT_TYPE_UNKNOWN));

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(null));
                    one(obc).getGrantTypes();
                    will(returnValue(registeredGrantTypes));
                    one(provider).getGrantTypesAllowed();
                    will(returnValue(new String[] { GRANT_TYPE_UNKNOWN }));
                }
            });

            String responseTypeValue = null;

            ClientAuthorization ca = new ClientAuthorization();
            assertTrue("Registered \"unknown\" grant type should produce true.", ca.validateGrantTypes(provider, request, CID, responseTypeValue));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * No grant_type parameter in request
     * response_type = code
     * authorization_code grant type is registered
     * Expected result: true
     */
    @Test
    public void validateGrantTypes_NoRequestedGrantType_ResponseTypeCode() {

        final String grantTypeValue = OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE;

        final JsonArray registeredGrantTypes = new JsonArray();
        registeredGrantTypes.add(new JsonPrimitive(grantTypeValue));

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(null));
                    one(obc).getGrantTypes();
                    will(returnValue(registeredGrantTypes));
                    one(provider).getGrantTypesAllowed();
                    will(returnValue(new String[] { grantTypeValue }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            assertTrue(ca.validateGrantTypes(provider, request, CID, OAuth20Constants.RESPONSE_TYPE_CODE));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * No grant_type parameter in request
     * response_type = token, id_token, or includes either
     * implicit grant type is registered
     * Expected result: true
     */
    @Test
    public void validateGrantTypes_NoRequestedGrantType_ResponseTypeTokenOrIdToken() {

        final String grantTypeValue = OAuth20Constants.GRANT_TYPE_IMPLICIT;

        final JsonArray registeredGrantTypes = new JsonArray();
        registeredGrantTypes.add(new JsonPrimitive(grantTypeValue));

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(null));
                    allowing(obc).getGrantTypes();
                    will(returnValue(registeredGrantTypes));
                    allowing(provider).getGrantTypesAllowed();
                    will(returnValue(new String[] { grantTypeValue }));
                }
            });

            String[] rtOptions = new String[] { OAuth20Constants.RESPONSE_TYPE_TOKEN,
                    OAuth20Constants.RESPONSE_TYPE_TOKEN + " other",
                    OIDCConstants.RESPONSE_TYPE_ID_TOKEN,
                    "other " + OIDCConstants.RESPONSE_TYPE_ID_TOKEN,
                    OAuth20Constants.RESPONSE_TYPE_TOKEN + " " + OIDCConstants.RESPONSE_TYPE_ID_TOKEN };
            for (String responseType : rtOptions) {
                System.out.println("Checking response type: [" + responseType + "]");

                ClientAuthorization ca = new ClientAuthorization();
                assertTrue("Response type " + responseType + " should produce true.", ca.validateGrantTypes(provider, request, CID, responseType));
            }

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * No grant_type parameter in request
     * response_type = "code token"
     * implicit grant type is registered
     * Expected result: true
     */
    @Test
    public void validateGrantTypes_NoRequestedGrantType_ResponseTypeCodeToken_ImplicitRegistered() {

        final String grantTypeValue = OAuth20Constants.GRANT_TYPE_IMPLICIT;

        final JsonArray registeredGrantTypes = new JsonArray();
        registeredGrantTypes.add(new JsonPrimitive(grantTypeValue));

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(null));
                    allowing(obc).getGrantTypes();
                    will(returnValue(registeredGrantTypes));
                    allowing(provider).getGrantTypesAllowed();
                    will(returnValue(new String[] { grantTypeValue }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            assertTrue(ca.validateGrantTypes(provider, request, CID, OAuth20Constants.RESPONSE_TYPE_CODE + " "
                    + OAuth20Constants.RESPONSE_TYPE_TOKEN));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * No grant_type parameter in request
     * response_type = "code token"
     * authorization_code grant type is registered
     * Runtime should expect the implicit grant type since "token" is included in the response type
     * Expected result: OAuth20InvalidGrantTypeException
     */
    @Test
    public void validateGrantTypes_NoRequestedGrantType_ResponseTypeCodeToken_AuthzCodeRegistered() {

        final String grantTypeValue = OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE;

        final JsonArray registeredGrantTypes = new JsonArray();
        registeredGrantTypes.add(new JsonPrimitive(grantTypeValue));

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(null));
                    allowing(obc).getGrantTypes();
                    will(returnValue(registeredGrantTypes));
                    allowing(provider).getGrantTypesAllowed();
                    will(returnValue(new String[] { grantTypeValue }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            ca.validateGrantTypes(provider, request, CID, "code token");

            fail("OAuth20InvalidGrantTypeException should be thrown.");

        } catch (OAuthException oe) {
            invalidGrantTypeException(oe, OAuth20Constants.GRANT_TYPE_IMPLICIT);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * No grant_type parameter in request
     * response_type = code
     * authorization_code grant type is registered
     * Multiple grant types are allowed, including authorization_code
     * Expected result: true
     */
    @Test
    public void validateGrantTypes_NoRequestedGrantType_ResponseTypeCode_AuthzCodeGrantTypeRegistered_MultipleGrantTypesAllowed() {

        final String grantTypeValue = OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE;

        final JsonArray registeredGrantTypes = new JsonArray();
        registeredGrantTypes.add(new JsonPrimitive(grantTypeValue));

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(null));
                    allowing(obc).getGrantTypes();
                    will(returnValue(registeredGrantTypes));
                    allowing(provider).getGrantTypesAllowed();
                    will(returnValue(clientGrantTypesList.toArray(new String[clientGrantTypesList.size()])));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            assertTrue(ca.validateGrantTypes(provider, request, CID, OAuth20Constants.RESPONSE_TYPE_CODE));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * No grant_type parameter in request
     * response_type = token, id_token, or includes either
     * Multiple grant types are registered, including implicit
     * Multiple grant types are allowed, including implicit
     * Expected result: true
     */
    @Test
    public void validateGrantTypes_NoRequestedGrantType_ResponseTypeTokenOrIdToken_MultipleGrantTypeRegistered_MultipleGrantTypesAllowed() {

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(null));
                    allowing(obc).getGrantTypes();
                    will(returnValue(clientGrantTypes));
                    allowing(provider).getGrantTypesAllowed();
                    will(returnValue(clientGrantTypesList.toArray(new String[clientGrantTypesList.size()])));
                }
            });

            String[] rtOptions = new String[] { OAuth20Constants.RESPONSE_TYPE_TOKEN,
                    OAuth20Constants.RESPONSE_TYPE_TOKEN + " other",
                    OIDCConstants.RESPONSE_TYPE_ID_TOKEN,
                    "other " + OIDCConstants.RESPONSE_TYPE_ID_TOKEN,
                    OAuth20Constants.RESPONSE_TYPE_TOKEN + " " + OIDCConstants.RESPONSE_TYPE_ID_TOKEN };

            for (String responseType : rtOptions) {
                System.out.println("Checking response type: [" + responseType + "]");

                ClientAuthorization ca = new ClientAuthorization();
                assertTrue("Response type " + responseType + " should produce true.", ca.validateGrantTypes(provider, request, CID, responseType));
            }

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * No grant_type parameter in request
     * response_type = Value that maps to a grant type that isn't registered
     * Multiple grant types are registered, but not the one matching the requested response type
     * Multiple grant types are allowed
     * Expected result: OAuth20InvalidGrantTypeException
     */
    @Test
    public void validateGrantTypes_NoRequestedGrantType_ResponseTypeNotRegistered_MultipleGrantTypeRegistered_MultipleGrantTypesAllowed() {

        String chosenGrantType = OAuth20Constants.GRANT_TYPE_PASSWORD;

        // Register all grant types except the randomly chosen one
        final JsonArray registeredGrantTypes = new JsonArray();
        for (int i = 0; i < clientGrantTypes.size(); i++) {
            String gt = clientGrantTypes.get(i).getAsString();
            if (!gt.equals(chosenGrantType)) {
                registeredGrantTypes.add(new JsonPrimitive(gt));
            }
        }

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(null));
                    allowing(obc).getGrantTypes();
                    will(returnValue(registeredGrantTypes));
                    allowing(provider).getGrantTypesAllowed();
                    will(returnValue(clientGrantTypesList.toArray(new String[clientGrantTypesList.size()])));
                }
            });

            // Choose a response type that would map to a grant type that isn't registered
            String responseType = "other";
            if (chosenGrantType.equals(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE)) {
                responseType = "code";
            } else if (chosenGrantType.equals(OAuth20Constants.GRANT_TYPE_IMPLICIT)) {
                responseType = OAuth20Constants.RESPONSE_TYPE_TOKEN;
            }

            ClientAuthorization ca = new ClientAuthorization();
            ca.validateGrantTypes(provider, request, CID, responseType);

            fail("OAuth20InvalidGrantTypeException should be thrown.");

        } catch (OAuthException oe) {
            if (!chosenGrantType.equals(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE)
                    && !chosenGrantType.equals(OAuth20Constants.GRANT_TYPE_IMPLICIT)) {
                chosenGrantType = GRANT_TYPE_UNKNOWN;
            }
            invalidGrantTypeException(oe, chosenGrantType);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Requested grant type = Single empty value
     * Reduced grant type set = Single value
     * Expected result: false
     */
    @Test
    public void validateGrantTypes_EmptyRequestedGrantType() {

        final String grantType = OAuth20Constants.GRANT_TYPE_IMPLICIT;

        final JsonArray registeredGrantTypes = new JsonArray();
        registeredGrantTypes.add(new JsonPrimitive(grantType));

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(new String[] { "" }));
                    one(obc).getGrantTypes();
                    will(returnValue(registeredGrantTypes));
                    one(provider).getGrantTypesAllowed();
                    will(returnValue(new String[] { grantType }));
                }
            });

            // Response type value doesn't matter since a requested grant type is present
            String responseTypeValue = "testRt";

            ClientAuthorization ca = new ClientAuthorization();
            assertFalse("An empty requested grant type should produce false, regardless of response type (response type: [" + responseTypeValue + "]).",
                    ca.validateGrantTypes(provider, request, CID, responseTypeValue));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Requested grant type = Single value
     * Reduced grant type set = Single value, not the same as the requested grant type
     * Expected result: false
     */
    @Test
    public void validateGrantTypes_SingleInvalidGrantType() {

        final String grantType = OAuth20Constants.GRANT_TYPE_JWT;
        String proposedReqGrantType = GRANT_TYPE_UNKNOWN;
        final String reqGrantType = proposedReqGrantType;

        final JsonArray registeredGrantTypes = new JsonArray();
        registeredGrantTypes.add(new JsonPrimitive(grantType));

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(new String[] { reqGrantType }));
                    one(obc).getGrantTypes();
                    will(returnValue(registeredGrantTypes));
                    one(provider).getGrantTypesAllowed();
                    will(returnValue(new String[] { grantType }));
                }
            });

            // Response type value doesn't matter since a requested grant type is present
            String responseTypeValue = null;

            ClientAuthorization ca = new ClientAuthorization();
            ca.validateGrantTypes(provider, request, CID, responseTypeValue);

            fail("OAuth20InvalidGrantTypeException should be thrown.");

        } catch (OAuthException oe) {
            // If the requested grant type contained spaces, the error message should only contain the first value
            invalidGrantTypeException(oe, reqGrantType.split(" ")[0]);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Requested grant type = Single value
     * Reduced grant types = Empty set
     * Expected result: true
     */
    @Test
    public void validateGrantTypes_SingleGrantType_EmptyReducedGrantTypes() {

        final String registeredGrantType = OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS;
        String proposedReqGrantType = OAuth20Constants.GRANT_TYPE_IMPLICIT;
        final String reqGrantType = proposedReqGrantType;

        final JsonArray registeredGrantTypes = new JsonArray();
        registeredGrantTypes.add(new JsonPrimitive(registeredGrantType));

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(new String[] { reqGrantType }));
                    one(obc).getGrantTypes();
                    will(returnValue(registeredGrantTypes));
                    one(provider).getGrantTypesAllowed();
                    will(returnValue(new String[] { "someOtherGrantType" }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            assertTrue(ca.validateGrantTypes(provider, request, CID, "doesntMatter"));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Requested grant type = Single value
     * Registered grant types = requested grant type
     * Allowed grant types = requested grant type
     * Expected result: true
     */
    @Test
    public void validateGrantTypes_ValidGrantTypeSpecified() {

        final String grantTypeValue = OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE;

        final JsonArray registeredGrantTypes = new JsonArray();
        registeredGrantTypes.add(new JsonPrimitive(grantTypeValue));

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(new String[] { grantTypeValue }));
                    one(obc).getGrantTypes();
                    will(returnValue(registeredGrantTypes));
                    one(provider).getGrantTypesAllowed();
                    will(returnValue(new String[] { grantTypeValue }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            assertTrue(ca.validateGrantTypes(provider, request, CID, null));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Requested grant type = Single value
     * Multiple grant types are registered, including the requested grant type
     * Multiple grant types are allowed, but none are the requested grant type
     * Expected result: OAuth20InvalidGrantTypeException
     */
    @Test
    public void validateGrantTypes_SingleRequestedGrantType_MultipleRegistered_MultipleAllowed_Invalid() {

        final String chosenGrantType = OAuth20Constants.GRANT_TYPE_PASSWORD;

        // Allow all grant types except the chosen one
        final List<String> grantTypesAllowedList = new ArrayList<String>(clientGrantTypesList);
        grantTypesAllowedList.remove(chosenGrantType);

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(new String[] { chosenGrantType }));
                    allowing(obc).getGrantTypes();
                    will(returnValue(clientGrantTypes));
                    allowing(provider).getGrantTypesAllowed();
                    will(returnValue(grantTypesAllowedList.toArray(new String[grantTypesAllowedList.size()])));
                }
            });

            // Response type value doesn't matter since a requested grant type is present
            String responseTypeValue = OAuth20Constants.RESPONSE_TYPE_TOKEN;

            ClientAuthorization ca = new ClientAuthorization();
            ca.validateGrantTypes(provider, request, CID, responseTypeValue);

            fail("OAuth20InvalidGrantTypeException should be thrown.");

        } catch (OAuthException oe) {
            invalidGrantTypeException(oe, chosenGrantType);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Requested grant type = Null or any other non-empty value
     * Response type = Null or any other value
     * Reduced grant types = Empty set
     * Expected result: true. If a non-empty value is set for the grant type, it is always valid if there are no reduced grant types.
     */
    @Test
    public void validateGrantTypes_NoReducedGrantTypes() {

        final String grantTypeValue = "testGt";

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue((grantTypeValue == null) ? null : new String[] { grantTypeValue }));
                    one(obc).getGrantTypes();
                    will(returnValue(new JsonArray()));
                    one(provider).getGrantTypesAllowed();
                    will(returnValue(null));
                }
            });

            String responseTypeValue = OAuth20Constants.RESPONSE_TYPE_CODE;

            ClientAuthorization ca = new ClientAuthorization();
            boolean result = ca.validateGrantTypes(provider, request, CID, responseTypeValue);

            assertTrue("Empty set of reduced grant types didn't produce true for grant type " + grantTypeValue + " and response type " + responseTypeValue + ".", result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Requested grant type = Multiple values provided
     * Expected result: OAuth20DuplicateParameterException
     */
    @Test
    public void validateGrantTypes_DuplicateGrantTypes() {

        final String grantTypeValue = OAuth20Constants.GRANT_TYPE_REFRESH_TOKEN;

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(new String[] { grantTypeValue, grantTypeValue }));
                }
            });

            String responseTypeValue = null;

            ClientAuthorization ca = new ClientAuthorization();
            ca.validateGrantTypes(provider, request, CID, responseTypeValue);

            fail("OAuth20DuplicateParameterException should be thrown.");

        } catch (OAuthException oe) {
            duplicateParameterException(oe, OAuth20Constants.GRANT_TYPE);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**************************************** getReducedGrantTypes tests ****************************************/

    /**
     * Null provider specified
     * Expected result: Empty set
     */
    @Test
    public void getReducedGrantTypes_NullProvider() {

        try {
            ClientAuthorization ca = new ClientAuthorization();
            Set<String> reducedGts = ca.getReducedGrantTypes(null, CID);

            assertEquals("Did not get matching grant type sets.", new HashSet<String>(), reducedGts);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * No client provider found
     * Expected result: Empty set
     */
    @Test
    public void getReducedGrantTypes_NullClientProvider() {

        final String grantType = OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS;

        try {
            getOidcOAuth20ClientExpectations(provider, null, null, null);
            mock.checking(new Expectations() {
                {
                    one(provider).getGrantTypesAllowed();
                    will(returnValue(new String[] { grantType }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            Set<String> reducedGts = ca.getReducedGrantTypes(provider, CID);

            assertEquals("Did not get matching grant type sets.", new HashSet<String>(), reducedGts);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * No client found
     * Expected result: Empty set
     */
    @Test
    public void getReducedGrantTypes_NullClient() {

        final String grantType = OAuth20Constants.GRANT_TYPE_IMPLICIT;

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, null);
            mock.checking(new Expectations() {
                {
                    one(provider).getGrantTypesAllowed();
                    will(returnValue(new String[] { grantType }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            Set<String> reducedGts = ca.getReducedGrantTypes(provider, CID);

            assertEquals("Did not get matching grant type sets.", new HashSet<String>(), reducedGts);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Null registered grant types
     * Expected result: Empty set
     */
    @Test
    public void getReducedGrantTypes_NullRegisteredGrantTypes() {

        final String grantType = OAuth20Constants.GRANT_TYPE_JWT;

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    one(obc).getGrantTypes();
                    will(returnValue(null));
                    one(provider).getGrantTypesAllowed();
                    will(returnValue(new String[] { grantType }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            Set<String> reducedGts = ca.getReducedGrantTypes(provider, CID);

            assertEquals("Did not get matching grant type sets.", new HashSet<String>(), reducedGts);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Null or an empty array of grant types allowed
     * Expected result: Empty set
     */
    @Test
    public void getReducedGrantTypes_NullOrNoGrantTypesAllowed() {

        final String grantType = OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE;

        final JsonArray grantTypes = new JsonArray();
        grantTypes.add(new JsonPrimitive(grantType));

        final String[] grantTypesAllowed = new String[0];

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    one(obc).getGrantTypes();
                    will(returnValue(grantTypes));
                    one(provider).getGrantTypesAllowed();
                    will(returnValue(grantTypesAllowed));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            Set<String> reducedGts = ca.getReducedGrantTypes(provider, CID);

            assertEquals("Did not get matching grant type sets.", new HashSet<String>(), reducedGts);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * One grant type registered
     * The same grant type is allowed
     * Expected result: A set with that grant type as its only value
     */
    @Test
    public void getReducedGrantTypes_SingleValid() {

        final String grantType = OAuth20Constants.GRANT_TYPE_PASSWORD;

        final JsonArray grantTypes = new JsonArray();
        grantTypes.add(new JsonPrimitive(grantType));

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    one(obc).getGrantTypes();
                    will(returnValue(grantTypes));
                    one(provider).getGrantTypesAllowed();
                    will(returnValue(new String[] { grantType }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            Set<String> reducedGts = ca.getReducedGrantTypes(provider, CID);

            Set<String> expectedGrantTypes = new HashSet<String>();
            expectedGrantTypes.add(grantType);

            assertEquals("Did not get matching grant type sets.", expectedGrantTypes, reducedGts);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * One grant type registered
     * Multiple grant types are allowed, including the registered grant type
     * Expected result: A set with the common grant type as its only value
     */
    @Test
    public void getReducedGrantTypes_SingleValidMultipleAllowed() {

        final String grantType = OAuth20Constants.GRANT_TYPE_REFRESH_TOKEN;

        final JsonArray grantTypes = new JsonArray();
        grantTypes.add(new JsonPrimitive(grantType));

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    one(obc).getGrantTypes();
                    will(returnValue(grantTypes));
                    one(provider).getGrantTypesAllowed();
                    will(returnValue(new String[] { grantType, "other", "test" }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            Set<String> reducedGts = ca.getReducedGrantTypes(provider, CID);

            Set<String> expectedGrantTypes = new HashSet<String>();
            expectedGrantTypes.add(grantType);

            assertEquals("Did not get matching grant type sets.", expectedGrantTypes, reducedGts);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Multiple grant types registered
     * One of the registered grant types is allowed
     * Expected result: A set with the common grant type as its only value
     */
    @Test
    public void getReducedGrantTypes_SingleValidMultipleRegistered() {

        final String grantType = OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS;

        final JsonArray grantTypes = new JsonArray();
        grantTypes.add(new JsonPrimitive(grantType));
        grantTypes.add(new JsonPrimitive("other"));
        grantTypes.add(new JsonPrimitive("jwt"));

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    one(obc).getGrantTypes();
                    will(returnValue(grantTypes));
                    one(provider).getGrantTypesAllowed();
                    will(returnValue(new String[] { grantType }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            Set<String> reducedGts = ca.getReducedGrantTypes(provider, CID);

            Set<String> expectedGrantTypes = new HashSet<String>();
            expectedGrantTypes.add(grantType);

            assertEquals("Did not get matching grant type sets.", expectedGrantTypes, reducedGts);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Multiple grant types registered
     * Multiple grant types are allowed, including a couple of the registered grant types
     * Expected result: A set with all of the common grant types
     */
    @Test
    public void getReducedGrantTypes_MultipleValid() {

        final String grantType = OAuth20Constants.GRANT_TYPE_JWT;

        final JsonArray grantTypes = new JsonArray();
        grantTypes.add(new JsonPrimitive(grantType));
        grantTypes.add(new JsonPrimitive("one"));
        grantTypes.add(new JsonPrimitive("three"));

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    one(obc).getGrantTypes();
                    will(returnValue(grantTypes));
                    one(provider).getGrantTypesAllowed();
                    will(returnValue(new String[] { grantType, "one", "two" }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            Set<String> reducedGts = ca.getReducedGrantTypes(provider, CID);

            Set<String> expectedGrantTypes = new HashSet<String>();
            expectedGrantTypes.add(grantType);
            expectedGrantTypes.add("one");

            assertEquals("Did not get matching grant type sets.", expectedGrantTypes, reducedGts);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Multiple grant types registered
     * Multiple grant types are allowed, none of which match any of the registered grant types
     * Expected result: Empty set
     */
    @Test
    public void getReducedGrantTypes_NoCommonGrantTypes() {

        final JsonArray grantTypes = new JsonArray();
        grantTypes.add(new JsonPrimitive("one"));
        grantTypes.add(new JsonPrimitive("three"));
        grantTypes.add(new JsonPrimitive("five"));

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    one(obc).getGrantTypes();
                    will(returnValue(grantTypes));
                    one(provider).getGrantTypesAllowed();
                    will(returnValue(new String[] { "two", "four", "six" }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            Set<String> reducedGts = ca.getReducedGrantTypes(provider, CID);

            assertEquals("Did not get matching grant type sets.", new HashSet<String>(), reducedGts);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**************************************** getUniqueArray tests ****************************************/

    @Test
    public void getUniqueArray_Null() {
        ClientAuthorization ca = new ClientAuthorization();
        String[] uniqueArray = ca.getUniqueArray(null);

        assertArrayEquals(new String[0], uniqueArray);
    }

    @Test
    public void getUniqueArray_Empty() {
        ClientAuthorization ca = new ClientAuthorization();
        String[] uniqueArray = ca.getUniqueArray(new String[0]);

        assertArrayEquals(new String[0], uniqueArray);
    }

    @Test
    public void getUniqueArray_SingleValue() {
        String[] expectedArray = new String[] { "test" };

        ClientAuthorization ca = new ClientAuthorization();
        String[] uniqueArray = ca.getUniqueArray(expectedArray);

        assertArrayEquals(expectedArray, uniqueArray);
    }

    @Test
    public void getUniqueArray_DuplicatedValues() {
        ClientAuthorization ca = new ClientAuthorization();
        String[] uniqueArray = ca.getUniqueArray(new String[] { "test", "one", "test", "one", "three" });

        assertArrayEquals(new String[] { "test", "one", "three" }, uniqueArray);
    }

    /**************************************** getRegisteredClientResponseTypes tests ****************************************/

    /**
     * Null provider passed to method
     * Expected result: Empty set
     */
    @Test
    public void getRegisteredClientResponseTypes_NullProvider() {
        try {
            ClientAuthorization ca = new ClientAuthorization();
            Set<String> registeredRts = ca.getRegisteredClientResponseTypes(null, CID);

            assertEquals("Did not get matching response type sets.", new HashSet<String>(), registeredRts);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * No client provider found
     * Expected result: Empty set
     */
    @Test
    public void getRegisteredClientResponseTypes_NullClientProvider() {
        try {
            getOidcOAuth20ClientExpectations(provider, null, null, null);

            ClientAuthorization ca = new ClientAuthorization();
            Set<String> registeredRts = ca.getRegisteredClientResponseTypes(provider, CID);

            assertEquals("Did not get matching response type sets.", new HashSet<String>(), registeredRts);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * No client found
     * Expected result: Empty set
     */
    @Test
    public void getRegisteredClientResponseTypes_NullClient() {
        try {
            getOidcOAuth20ClientExpectations(provider, oocp, null, null);

            ClientAuthorization ca = new ClientAuthorization();
            Set<String> registeredRts = ca.getRegisteredClientResponseTypes(provider, null);

            assertEquals("Did not get matching response type sets.", new HashSet<String>(), registeredRts);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Response types JsonArray is null
     * Expected result: Empty set
     */
    @Test
    public void getRegisteredClientResponseTypes_NullResponseTypes() {

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    one(obc).getResponseTypes();
                    will(returnValue(null));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            Set<String> registeredRts = ca.getRegisteredClientResponseTypes(provider, CID);

            assertEquals("Did not get matching response type sets.", new HashSet<String>(), registeredRts);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Single registered response type
     * Expected result: A set with that response type as its only value
     */
    @Test
    public void getRegisteredClientResponseTypes_SingleValue() {

        final JsonArray responseTypes = new JsonArray();
        responseTypes.add(new JsonPrimitive(OAuth20Constants.RESPONSE_TYPE_CODE));

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    one(obc).getResponseTypes();
                    will(returnValue(responseTypes));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            Set<String> registeredRts = ca.getRegisteredClientResponseTypes(provider, CID);

            HashSet<String> expectedRts = new HashSet<String>();
            expectedRts.add(OAuth20Constants.RESPONSE_TYPE_CODE);

            assertEquals("Did not get matching response type sets.", expectedRts, registeredRts);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Multiple response types registered, some possibly duplicates
     * Expected result: A set with the unique response types
     */
    @Test
    public void getRegisteredClientResponseTypes_MultipleValues() {

        final JsonArray responseTypes = new JsonArray();
        responseTypes.add(new JsonPrimitive(OIDCConstants.RESPONSE_TYPE_ID_TOKEN));
        responseTypes.add(new JsonPrimitive("two"));
        responseTypes.add(new JsonPrimitive("three and four"));
        responseTypes.add(new JsonPrimitive("two"));

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    one(obc).getResponseTypes();
                    will(returnValue(responseTypes));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            Set<String> registeredRts = ca.getRegisteredClientResponseTypes(provider, CID);

            HashSet<String> expectedRts = new HashSet<String>();
            expectedRts.add(OIDCConstants.RESPONSE_TYPE_ID_TOKEN);
            expectedRts.add("two");
            expectedRts.add("three and four");

            assertEquals("Did not get matching response type sets.", expectedRts, registeredRts);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**************************************** validateResponseTypeAndReturn tests ****************************************/

    /**
     * Requested response type = missing
     * Expected result: OAuth20MissingParameterException
     */
    @Test
    public void validateResponseTypeAndReturn_NoResponseType() {

        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.RESPONSE_TYPE);
                    will(returnValue(null));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            String responseType = ca.validateResponseTypeAndReturn(request, provider, CID);

            fail("OAuth20MissingParameterException should be thrown. Instead, got response type: " + responseType);

        } catch (OAuthException oe) {
            missingParameterException(oe, OAuth20Constants.RESPONSE_TYPE);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Requested response type = Multiple values
     * Expected result: OAuth20DuplicateParameterException
     */
    @Test
    public void validateResponseTypeAndReturn_DuplicateResponseType() {

        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.RESPONSE_TYPE);
                    will(returnValue(new String[] { "rt1", "rt2" }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            String responseType = ca.validateResponseTypeAndReturn(request, provider, CID);

            fail("OAuth20DuplicateParameterException should be thrown. Instead, got response type: " + responseType);

        } catch (OAuthException oe) {
            duplicateParameterException(oe, OAuth20Constants.RESPONSE_TYPE);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Requested response type = Single null value
     * Expected result: OAuth20MissingParameterException
     */
    @Test
    public void validateResponseTypeAndReturn_NullResponseType() {

        final String requestedResponseType = null;

        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.RESPONSE_TYPE);
                    will(returnValue(new String[] { requestedResponseType }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            String responseType = ca.validateResponseTypeAndReturn(request, provider, CID);

            fail("OAuth20MissingParameterException should be thrown. Instead, got response type: " + responseType);

        } catch (OAuthException oe) {
            missingParameterException(oe, OAuth20Constants.RESPONSE_TYPE);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Requested response type = Single empty value
     * Expected result: OAuth20MissingParameterException
     */
    @Test
    public void validateResponseTypeAndReturn_EmptyResponseType() {

        final String requestedResponseType = "";

        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.RESPONSE_TYPE);
                    will(returnValue(new String[] { requestedResponseType }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            String responseType = ca.validateResponseTypeAndReturn(request, provider, CID);

            fail("OAuth20MissingParameterException should be thrown. Instead, got response type: " + responseType);

        } catch (OAuthException oe) {
            missingParameterException(oe, OAuth20Constants.RESPONSE_TYPE);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Client provider not found
     * Expected result: OAuth20InvalidResponseTypeException
     */
    @Test
    public void validateResponseTypeAndReturn_NoClientProvider() {

        final String requestedResponseType = OAuth20Constants.RESPONSE_TYPE_CODE;

        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.RESPONSE_TYPE);
                    will(returnValue(new String[] { requestedResponseType }));
                }
            });
            getOidcOAuth20ClientExpectations(provider, null, null, null);

            ClientAuthorization ca = new ClientAuthorization();
            String responseType = ca.validateResponseTypeAndReturn(request, provider, CID);

            fail("OAuth20InvalidResponseTypeException should be thrown. Instead, got response type: " + responseType);

        } catch (OAuthException oe) {
            invalidResponseTypeException(oe, requestedResponseType);
        } catch (Throwable t) {
            System.out.println("Caught unexpected exception for requested response type: " + requestedResponseType);
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Client not found
     * Expected result: OAuth20InvalidResponseTypeException
     */
    @Test
    public void validateResponseTypeAndReturn_NoClient() {

        final String requestedResponseType = OAuth20Constants.RESPONSE_TYPE_TOKEN;

        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.RESPONSE_TYPE);
                    will(returnValue(new String[] { requestedResponseType }));
                }
            });
            getOidcOAuth20ClientExpectations(provider, oocp, CID, null);

            ClientAuthorization ca = new ClientAuthorization();
            String responseType = ca.validateResponseTypeAndReturn(request, provider, CID);

            fail("OAuth20InvalidResponseTypeException should be thrown. Instead, got response type: " + responseType);

        } catch (OAuthException oe) {
            invalidResponseTypeException(oe, requestedResponseType);
        } catch (Throwable t) {
            System.out.println("Caught unexpected exception for requested response type: " + requestedResponseType);
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * No response types are registered
     * Expected result: OAuth20InvalidResponseTypeException
     */
    @Test
    public void validateResponseTypeAndReturn_NoRegisteredResponseTypes() {

        final String requestedResponseType = OIDCConstants.RESPONSE_TYPE_ID_TOKEN;

        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.RESPONSE_TYPE);
                    will(returnValue(new String[] { requestedResponseType }));
                    one(obc).getResponseTypes();
                    will(returnValue(new JsonArray()));
                }
            });
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);

            ClientAuthorization ca = new ClientAuthorization();
            String responseType = ca.validateResponseTypeAndReturn(request, provider, CID);

            fail("OAuth20InvalidResponseTypeException should be thrown. Instead, got response type: " + responseType);

        } catch (OAuthException oe) {
            invalidResponseTypeException(oe, requestedResponseType);
        } catch (Throwable t) {
            System.out.println("Caught unexpected exception for requested response type: " + requestedResponseType);
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Requested response type = code, token, or id_token
     * All response types are registered
     * Expected result: Method returns requested response type
     */
    @Test
    public void validateResponseTypeAndReturn_Valid() {

        for (final String requestedResponseType : clientResponseTypesList) {

            System.out.println("requested response type: " + requestedResponseType);

            try {
                getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
                mock.checking(new Expectations() {
                    {
                        one(request).getParameterValues(OAuth20Constants.RESPONSE_TYPE);
                        will(returnValue(new String[] { requestedResponseType }));
                        one(obc).getResponseTypes();
                        will(returnValue(clientResponseTypes));
                    }
                });

                ClientAuthorization ca = new ClientAuthorization();
                String responseType = ca.validateResponseTypeAndReturn(request, provider, CID);

                assertEquals("Did not get expected response type.", requestedResponseType, responseType);

            } catch (Throwable t) {
                System.out.println("Caught unexpected exception for requested response type: " + requestedResponseType);
                outputMgr.failWithThrowable(name.getMethodName(), t);
            }
        }
    }

    /**
     * Requested response type = One of the valid client response types
     * Registered response types = All but the requested response type
     * Expected result: OAuth20InvalidResponseTypeException
     */
    @Test
    public void validateResponseTypeAndReturn_Invalid() {

        for (String currentResponseType : clientResponseTypesList) {

            final String requestedResponseType = currentResponseType;
            System.out.println("requested response type: " + requestedResponseType);

            final JsonArray registeredResponseTypes = new JsonArray();
            for (String rt : clientGrantTypesList) {
                if (!rt.equals(requestedResponseType)) {
                    // Register all of the response types except the requested type
                    registeredResponseTypes.add(new JsonPrimitive(rt));
                }
            }

            try {
                getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
                mock.checking(new Expectations() {
                    {
                        one(request).getParameterValues(OAuth20Constants.RESPONSE_TYPE);
                        will(returnValue(new String[] { requestedResponseType }));
                        one(obc).getResponseTypes();
                        will(returnValue(registeredResponseTypes));
                    }
                });

                ClientAuthorization ca = new ClientAuthorization();
                String responseType = ca.validateResponseTypeAndReturn(request, provider, CID);

                fail("OAuth20InvalidResponseTypeException should be thrown. Instead, got response type: " + responseType);

            } catch (OAuthException oe) {
                invalidResponseTypeException(oe, requestedResponseType);
            } catch (Throwable t) {
                System.out.println("Caught unexpected exception for requested response type: " + requestedResponseType);
                outputMgr.failWithThrowable(name.getMethodName(), t);
            }
        }
    }

    /**
     * Requested response type = Single non-empty value
     * Requested response type is not registered as itself, nor is it contained within another registered response type string
     * Registered response type = One of:
     * - [different response type],
     * - ["my" + requested response type],
     * - [requested response type + "my"],
     * - [different response type] ["my" + requested response type],
     * - [requested response type + "my"] [different response type]
     * Expected result: OAuth20InvalidResponseTypeException
     */
    @Test
    public void validateResponseTypeAndReturn_SingleRequestedResponseType_Invalid() {

        // Check all of the possible response type values, plus "other"
        List<String> requestedRtOptions = new ArrayList<String>(clientResponseTypesList);
        requestedRtOptions.add("other");

        for (int i = 0; i < requestedRtOptions.size(); i++) {
            final String requestedResponseType = requestedRtOptions.get(i);
            System.out.println("Requested response type: " + requestedResponseType);

            int differentResponseTypeIndex = i + 1;
            if (i == (requestedRtOptions.size() - 1)) {
                differentResponseTypeIndex = 0;
            }
            // Pick a different response type than the one being requested
            String differentResponseType = requestedRtOptions.get(differentResponseTypeIndex);

            String responseTypeWithPrefix = "my" + requestedResponseType;
            String responseTypeWithSuffix = requestedResponseType + "my";
            String multiRtDiffAndPrefix = differentResponseType + " " + responseTypeWithPrefix;
            String multiRtDiffAndSuffix = responseTypeWithSuffix + " " + differentResponseType;

            // Iterate through each of the invalid registered response type options
            String[] registeredRtOptions = new String[] { differentResponseType, responseTypeWithPrefix, responseTypeWithSuffix,
                    multiRtDiffAndPrefix, multiRtDiffAndSuffix };

            for (String registeredResponseType : registeredRtOptions) {
                System.out.println("Registered response type: " + registeredResponseType);

                final JsonArray registeredResponseTypes = new JsonArray();
                registeredResponseTypes.add(new JsonPrimitive(registeredResponseType));

                try {
                    getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
                    mock.checking(new Expectations() {
                        {
                            one(request).getParameterValues(OAuth20Constants.RESPONSE_TYPE);
                            will(returnValue(new String[] { requestedResponseType }));
                            one(obc).getResponseTypes();
                            will(returnValue(registeredResponseTypes));
                        }
                    });

                    ClientAuthorization ca = new ClientAuthorization();
                    String responseType = ca.validateResponseTypeAndReturn(request, provider, CID);

                    fail("OAuth20InvalidResponseTypeException should be thrown. Instead, got response type: " + responseType);

                } catch (OAuthException oe) {
                    invalidResponseTypeException(oe, requestedResponseType);
                } catch (Throwable t) {
                    System.out.println("Caught unexpected exception for requested response type: " + requestedResponseType);
                    outputMgr.failWithThrowable(name.getMethodName(), t);
                }
            }
        }
    }

    /**
     * Requested response type = Single non-empty value
     * Registered response type = One of:
     * - [requested response type],
     * - [requested response type] [requested response type],
     * - [requested response type] [different response type],
     * - [different response type] [requested response type],
     * - [requested response type] [different response type] [different response type],
     * - [different response type] [requested response type] [different response type],
     * - [different response type] [different response type] [requested response type],
     * Expected result: Method returns requested response type
     */
    @Test
    public void validateResponseTypeAndReturn_SingleRequestedResponseType_Valid() {

        // Check all of the possible response type values, plus "other"
        List<String> requestedRtOptions = new ArrayList<String>(clientResponseTypesList);
        requestedRtOptions.add("other");

        for (int i = 0; i < requestedRtOptions.size(); i++) {
            final String requestedResponseType = requestedRtOptions.get(i);
            System.out.println("Requested response type: " + requestedResponseType);

            int differentResponseTypeIndex = i + 1;
            if (i == (requestedRtOptions.size() - 1)) {
                differentResponseTypeIndex = 0;
            }
            // Pick a different response type than the one being requested
            String differentResponseType = requestedRtOptions.get(differentResponseTypeIndex);

            String duplicateReqResponseType = requestedResponseType + " " + requestedResponseType;
            String goodBadResponseType = requestedResponseType + " " + differentResponseType;
            String badGoodResponseType = differentResponseType + " " + requestedResponseType;
            String goodBadBadResponseType = requestedResponseType + " " + differentResponseType + " " + differentResponseType;
            String badGoodBadResponseType = differentResponseType + " " + requestedResponseType + " " + differentResponseType;
            String badBadGoodResponseType = differentResponseType + " " + differentResponseType + " " + requestedResponseType;

            // Iterate through each of the valid registered response type options
            String[] registeredRtOptions = new String[] { requestedResponseType, duplicateReqResponseType, goodBadResponseType,
                    badGoodResponseType, goodBadBadResponseType, badGoodBadResponseType, badBadGoodResponseType };

            for (String registeredResponseType : registeredRtOptions) {
                System.out.println("Registered response type: " + registeredResponseType);

                final JsonArray registeredResponseTypes = new JsonArray();
                registeredResponseTypes.add(new JsonPrimitive(registeredResponseType));

                try {
                    getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
                    mock.checking(new Expectations() {
                        {
                            one(request).getParameterValues(OAuth20Constants.RESPONSE_TYPE);
                            will(returnValue(new String[] { requestedResponseType }));
                            one(obc).getResponseTypes();
                            will(returnValue(registeredResponseTypes));
                        }
                    });

                    ClientAuthorization ca = new ClientAuthorization();
                    String responseType = ca.validateResponseTypeAndReturn(request, provider, CID);

                    assertEquals("Did not get expected response type.", requestedResponseType, responseType);

                } catch (Throwable t) {
                    System.out.println("Caught unexpected exception for requested response type: " + requestedResponseType);
                    outputMgr.failWithThrowable(name.getMethodName(), t);
                }
            }
        }
    }

    /**
     * Requested response type = Multiple non-empty values
     * Requested response types are not registered as themselves, nor are they contained within another registered response type string
     * Registered response type = One of:
     * - [different response type],
     * - [requested response type 1],
     * - [requested response type 2],
     * - ["my" + requested response type 1] [requested response type 2],
     * - [requested response type 1] [requested response type 2 + "my"],
     * - [requested response type 1] [different response type],
     * - [different response type] [requested response type 2] [different response type 2],
     * Expected result: OAuth20InvalidResponseTypeException
     */
    @Test
    public void validateResponseTypeAndReturn_MultipleRequestedResponseTypes_Invalid() {

        List<String> requestedRtOptions = new ArrayList<String>(clientResponseTypesList);
        requestedRtOptions.add("other");
        requestedRtOptions.add("yetOneMore");

        // Choose 2 different response types to request
        String requestedRt1 = requestedRtOptions.get(0);
        String requestedRt2 = requestedRtOptions.get(1);

        final String requestedResponseType = requestedRt1 + " " + requestedRt2;
        System.out.println("Requested response type: " + requestedResponseType);

        // Pick a different response type than any of the ones being requested
        String differentResponseType = requestedRtOptions.get(2);
        String responseTypeWithPrefix = "my" + requestedRt1;
        String responseTypeWithSuffix = requestedRt2 + "my";
        String mixedWithPrefix = responseTypeWithPrefix + " " + requestedRt2;
        String mixedWithSuffix = requestedRt1 + " " + responseTypeWithSuffix;
        String goodBad = requestedRt1 + " " + differentResponseType;
        String badGoodBad = "test " + requestedRt2 + " " + differentResponseType;

        // Iterate through each of the invalid registered response type options
        String[] registeredRtOptions = new String[] { differentResponseType, requestedRt1, requestedRt2,
                mixedWithPrefix, mixedWithSuffix, goodBad, badGoodBad };

        for (String registeredResponseType : registeredRtOptions) {
            System.out.println("Registered response type: " + registeredResponseType);

            final JsonArray registeredResponseTypes = new JsonArray();
            registeredResponseTypes.add(new JsonPrimitive(registeredResponseType));

            try {
                getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
                mock.checking(new Expectations() {
                    {
                        one(request).getParameterValues(OAuth20Constants.RESPONSE_TYPE);
                        will(returnValue(new String[] { requestedResponseType }));
                        one(obc).getResponseTypes();
                        will(returnValue(registeredResponseTypes));
                    }
                });

                ClientAuthorization ca = new ClientAuthorization();
                String responseType = ca.validateResponseTypeAndReturn(request, provider, CID);

                fail("OAuth20InvalidResponseTypeException should be thrown. Instead, got response type: " + responseType);

            } catch (OAuthException oe) {
                invalidResponseTypeException(oe, requestedResponseType);
            } catch (Throwable t) {
                System.out.println("Caught unexpected exception for requested response type: " + requestedResponseType);
                outputMgr.failWithThrowable(name.getMethodName(), t);
            }
        }
    }

    /**
     * Requested response type = Multiple non-empty values
     * Registered response type = One of:
     * - [requested response type 1] [requested response type 2],
     * - [requested response type 2] [requested response type 1],
     * - [requested response type 1] [requested response type 2] [different response type],
     * - [requested response type 1] [different response type] [requested response type 2] [requested response type 1],
     * Expected result: Method returns requested response type
     */
    @Test
    public void validateResponseTypeAndReturn_MultipleRequestedResponseTypes_Valid() {

        List<String> requestedRtOptions = new ArrayList<String>(clientResponseTypesList);
        requestedRtOptions.add("other");
        requestedRtOptions.add("yetOneMore");

        // Choose 2 different response types to request
        String requestedRt1 = requestedRtOptions.get(0);
        String requestedRt2 = requestedRtOptions.get(1);

        final String requestedResponseType = requestedRt1 + " " + requestedRt2;
        System.out.println("Requested response type: " + requestedResponseType);

        // Pick a different response type than any of the ones being requested
        String differentResponseType = requestedRtOptions.get(2);
        String good1Good2 = requestedRt1 + " " + requestedRt2;
        String good2Good1 = requestedRt2 + " " + requestedRt1;
        String good1Good2Diff = requestedRt1 + " " + requestedRt2 + " " + differentResponseType;
        String good1DiffGood2Good1 = requestedRt1 + " " + differentResponseType + " " + requestedRt2 + " " + requestedRt1;

        // Iterate through each of the valid registered response type options
        String[] registeredRtOptions = new String[] { good1Good2, good2Good1, good1Good2Diff, good1DiffGood2Good1 };

        for (String registeredResponseType : registeredRtOptions) {
            System.out.println("Registered response type: " + registeredResponseType);

            final JsonArray registeredResponseTypes = new JsonArray();
            registeredResponseTypes.add(new JsonPrimitive(registeredResponseType));

            try {
                getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
                mock.checking(new Expectations() {
                    {
                        one(request).getParameterValues(OAuth20Constants.RESPONSE_TYPE);
                        will(returnValue(new String[] { requestedResponseType }));
                        one(obc).getResponseTypes();
                        will(returnValue(registeredResponseTypes));
                    }
                });

                ClientAuthorization ca = new ClientAuthorization();
                String responseType = ca.validateResponseTypeAndReturn(request, provider, CID);

                assertEquals("Did not get expected response type.", requestedResponseType, responseType);

            } catch (Throwable t) {
                System.out.println("Caught unexpected exception for requested response type: " + requestedResponseType);
                outputMgr.failWithThrowable(name.getMethodName(), t);
            }
        }
    }

    /**
     * Requested response type = Multiple non-empty values
     * Registered response type = Multiple values including all of the requested response types across separate entries
     * Registered response types are one of the following sets:
     * - [requested response type 1], [requested response type 2]
     * - [requested response type 2], [requested response type 1]
     * - [requested response type 1] [requested response type 2], [other response type]
     * - [other response type], [requested response type 2] [requested response type 1]
     * - [requested response type 1], [other response type], [requested response type 2]
     * - [requested response type 1], [other response type], [other response type 2] [requested response type 2]
     * Expected result: Method returns requested response type
     */
    @Test
    public void validateResponseTypeAndReturn_MultipleRequestedResponseTypes_MultipleRegistered_Valid() {

        List<String> requestedRtOptions = new ArrayList<String>(clientResponseTypesList);
        requestedRtOptions.add("other");
        requestedRtOptions.add("yetOneMore");

        // Choose 2 different response types to request
        String requestedRt1 = requestedRtOptions.get(0);
        String requestedRt2 = requestedRtOptions.get(1);

        final String requestedResponseType = requestedRt1 + " " + requestedRt2;
        System.out.println("Requested response type: " + requestedResponseType);

        // Pick two different response types than any of the ones being requested
        String differentResponseType = requestedRtOptions.get(2);
        String differentResponseType2 = requestedRtOptions.get(3);

        // Create array options for each of the valid registered response type configurations
        JsonArray good1Good2 = new JsonArray();
        good1Good2.add(new JsonPrimitive(requestedRt1));
        good1Good2.add(new JsonPrimitive(requestedRt2));

        JsonArray good2Good1 = new JsonArray();
        good2Good1.add(new JsonPrimitive(requestedRt2));
        good2Good1.add(new JsonPrimitive(requestedRt1));

        JsonArray good1Good2Other = new JsonArray();
        good1Good2Other.add(new JsonPrimitive(requestedRt1 + " " + requestedRt2));
        good1Good2Other.add(new JsonPrimitive(differentResponseType));

        JsonArray otherGood2Good1 = new JsonArray();
        otherGood2Good1.add(new JsonPrimitive(differentResponseType));
        otherGood2Good1.add(new JsonPrimitive(requestedRt2 + " " + requestedRt1));

        JsonArray good1OtherGood2 = new JsonArray();
        good1OtherGood2.add(new JsonPrimitive(requestedRt1));
        good1OtherGood2.add(new JsonPrimitive(differentResponseType));
        good1OtherGood2.add(new JsonPrimitive(requestedRt2));

        JsonArray good1OtherOther2Good2 = new JsonArray();
        good1OtherOther2Good2.add(new JsonPrimitive(requestedRt1));
        good1OtherOther2Good2.add(new JsonPrimitive(differentResponseType));
        good1OtherOther2Good2.add(new JsonPrimitive(differentResponseType2 + " " + requestedRt2));

        // Iterate through each of the valid registered response type options
        JsonArray[] registeredRtOptions = new JsonArray[] { good1Good2, good2Good1, good1Good2Other, otherGood2Good1, good1OtherGood2, good1OtherOther2Good2 };

        for (final JsonArray registeredResponseTypes : registeredRtOptions) {

            try {
                getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
                mock.checking(new Expectations() {
                    {
                        one(request).getParameterValues(OAuth20Constants.RESPONSE_TYPE);
                        will(returnValue(new String[] { requestedResponseType }));
                        one(obc).getResponseTypes();
                        will(returnValue(registeredResponseTypes));
                    }
                });

                ClientAuthorization ca = new ClientAuthorization();
                String responseType = ca.validateResponseTypeAndReturn(request, provider, CID);

                assertEquals("Did not get expected response type.", requestedResponseType, responseType);

            } catch (Throwable t) {
                System.out.println("Caught unexpected exception for requested response type: " + requestedResponseType);
                outputMgr.failWithThrowable(name.getMethodName(), t);
            }
        }
    }

    /**************************************** getRequestedGrantType tests ****************************************/

    /**
     * grant_type = Single value
     * Expected result: Method returns requested grant type
     */
    @Test
    public void getRequestedGrantType_SingleValid() {

        final String requestedGrantType = "";

        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(new String[] { requestedGrantType }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            String grantType = ca.getRequestedGrantType(request, null);

            assertEquals("Did not get expected grant type.", requestedGrantType, grantType);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * grant_type = Multiple values
     * Expected result: OAuth20DuplicateParameterException
     */
    @Test
    public void getRequestedGrantType_Duplicate() {

        final String requestedGrantType = OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE;

        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(new String[] { requestedGrantType, "other" }));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            String grantType = ca.getRequestedGrantType(request, null);

            fail("OAuth20DuplicateParameterException should be thrown. Instead, got grant type: " + grantType);

        } catch (OAuthException oe) {
            duplicateParameterException(oe, OAuth20Constants.GRANT_TYPE);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * grant_type = Missing
     * Response type = null
     * Expected result: {@value #GRANT_TYPE_UNKNOWN}
     */
    @Test
    public void getRequestedGrantType_RequestedNull_ResponseTypeNull() {

        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(null));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            String grantType = ca.getRequestedGrantType(request, null);

            assertEquals("Did not get expected grant type.", GRANT_TYPE_UNKNOWN, grantType);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * grant_type = Missing
     * Response type = code
     * Expected result: "authorization_code"
     */
    @Test
    public void getRequestedGrantType_RequestedNull_ResponseTypeCode() {

        try {
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(null));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            String grantType = ca.getRequestedGrantType(request, OAuth20Constants.RESPONSE_TYPE_CODE);

            assertEquals("Did not get expected grant type.", OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE, grantType);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * grant_type = Missing
     * Response type = token, id_token, or includes either
     * Expected result: "implicit"
     */
    @Test
    public void getRequestedGrantType_RequestedNull_ResponseTypeTokenOrIdToken() {

        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(null));
                }
            });

            String[] responseTypeOptions = new String[] { OAuth20Constants.RESPONSE_TYPE_TOKEN,
                    OAuth20Constants.RESPONSE_TYPE_TOKEN + " " + OAuth20Constants.RESPONSE_TYPE_TOKEN,
                    OAuth20Constants.RESPONSE_TYPE_CODE + " " + OAuth20Constants.RESPONSE_TYPE_TOKEN,
                    OAuth20Constants.RESPONSE_TYPE_TOKEN + " other",
                    OIDCConstants.RESPONSE_TYPE_ID_TOKEN,
                    OIDCConstants.RESPONSE_TYPE_ID_TOKEN + " " + OIDCConstants.RESPONSE_TYPE_ID_TOKEN,
                    OIDCConstants.RESPONSE_TYPE_ID_TOKEN + " " + OAuth20Constants.RESPONSE_TYPE_CODE,
                    "other " + OIDCConstants.RESPONSE_TYPE_ID_TOKEN,
                    OAuth20Constants.RESPONSE_TYPE_TOKEN + " " + OIDCConstants.RESPONSE_TYPE_ID_TOKEN,
                    OIDCConstants.RESPONSE_TYPE_ID_TOKEN + " " + OAuth20Constants.RESPONSE_TYPE_TOKEN };

            for (String responseType : responseTypeOptions) {
                System.out.println("Running with response type: " + responseType);

                ClientAuthorization ca = new ClientAuthorization();
                String grantType = ca.getRequestedGrantType(request, responseType);

                assertEquals("Did not get expected grant type with response type: " + responseType + ".", OAuth20Constants.GRANT_TYPE_IMPLICIT, grantType);
            }

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * grant_type = Missing
     * Response type = Something other than code, token, or id_token, and includes neither token nor id_token
     * Expected result: {@value #GRANT_TYPE_UNKNOWN}
     */
    @Test
    public void getRequestedGrantType_RequestedNull_ResponseTypeOther() {

        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getParameterValues(OAuth20Constants.GRANT_TYPE);
                    will(returnValue(null));
                }
            });

            String[] responseTypeOptions = new String[] { "",
                    "testing",
                    OAuth20Constants.RESPONSE_TYPE_CODE + " other",
                    "other " + OAuth20Constants.RESPONSE_TYPE_CODE,
                    "my" + OAuth20Constants.RESPONSE_TYPE_TOKEN,
                    OIDCConstants.RESPONSE_TYPE_ID_TOKEN + "my" };

            for (String responseType : responseTypeOptions) {
                System.out.println("Running with response type: " + responseType);

                ClientAuthorization ca = new ClientAuthorization();
                String grantType = ca.getRequestedGrantType(request, responseType);

                assertEquals("Did not get expected grant type with response type: [" + responseType + "].", GRANT_TYPE_UNKNOWN, grantType);
            }

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**************************************** checkForEmptyScopeSetAfterConsent tests ****************************************/

    /**
     * Scope(s) provided in the request
     * Null provider specified
     * At least one attribute included in the initial result passed to the method
     * Expected result: Returned result is unchanged from the result passed to the method
     */
    @Test
    public void checkForEmptyScopeSetAfterConsent_ScopeProvided_NullProvider() {

        final String[] scopes = new String[] { SCOPE, "other" };

        // Create attribute list for initial result object
        String initialAttrName = "initialAttr";
        String initialAttrValue = "attrValue";
        AttributeList initialList = new AttributeList();
        initialList.setAttribute(initialAttrName, OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST, new String[] { initialAttrValue });

        OAuthResultImpl initialResult = new OAuthResultImpl(OAuthResult.STATUS_OK, initialList);

        try {
            ClientAuthorization ca = new ClientAuthorization();
            OAuthResult result = ca.checkForEmptyScopeSetAfterConsent(scopes, initialResult, request, null, CID);

            assertEquals("Did not receive the expected status code.", OAuthResult.STATUS_OK, result.getStatus());

            // Make sure the result still contains the attribute(s) set in the result passed in to the method
            List<Attribute> resultAttributes = result.getAttributeList().getAllAttributes();
            assertEquals("Did not find the expected number of attributes.", initialList.getAllAttributes().size(), resultAttributes.size());

            String[] resultAttrValue = result.getAttributeList().getAttributeValuesByName(initialAttrName);
            assertArrayEquals("Did not get the expected attribute value in the result.", new String[] { initialAttrValue }, resultAttrValue);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Scope(s) provided in the request
     * Client provider not found
     * Expected result: Returned result is unchanged from the result passed to the method
     */
    @Test
    public void checkForEmptyScopeSetAfterConsent_ScopeProvided_NullClientProvider() {

        final String[] scopes = new String[] { "" };
        OAuthResultImpl initialResult = new OAuthResultImpl(OAuthResult.STATUS_OK, new AttributeList());

        try {
            getOidcOAuth20ClientExpectations(provider, null, null, null);

            ClientAuthorization ca = new ClientAuthorization();
            OAuthResult result = ca.checkForEmptyScopeSetAfterConsent(scopes, initialResult, request, provider, CID);

            assertEquals("Did not receive the expected status code.", OAuthResult.STATUS_OK, result.getStatus());

            List<Attribute> allAttributes = result.getAttributeList().getAllAttributes();
            assertTrue("Found attributes in result when none should be present.", allAttributes.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Scope(s) provided in the request
     * Client not found
     * Expected result: Returned result is unchanged from the result passed to the method
     */
    @Test
    public void checkForEmptyScopeSetAfterConsent_ScopeProvided_NullClient() {

        final String[] scopes = new String[] { SCOPE };
        OAuthResultImpl initialResult = new OAuthResultImpl(OAuthResult.STATUS_OK, new AttributeList());

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, null);

            ClientAuthorization ca = new ClientAuthorization();
            OAuthResult result = ca.checkForEmptyScopeSetAfterConsent(scopes, initialResult, request, provider, CID);

            assertEquals("Did not receive the expected status code.", OAuthResult.STATUS_OK, result.getStatus());

            List<Attribute> allAttributes = result.getAttributeList().getAllAttributes();
            assertTrue("Found attributes in result when none should be present.", allAttributes.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Scope(s) provided
     * Expected result: Returned result is unchanged from the result passed to the method
     */
    @Test
    public void checkForEmptyScopeSetAfterConsent_Valid() {

        final String[] scopes = new String[] { SCOPE, "other" };
        OAuthResultImpl initialResult = new OAuthResultImpl(OAuthResult.STATUS_OK, new AttributeList());

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);

            ClientAuthorization ca = new ClientAuthorization();
            OAuthResult result = ca.checkForEmptyScopeSetAfterConsent(scopes, initialResult, request, provider, CID);

            assertEquals("Did not receive the expected status code.", OAuthResult.STATUS_OK, result.getStatus());

            List<Attribute> allAttributes = result.getAttributeList().getAllAttributes();
            assertTrue("Found attributes in result when none should be present.", allAttributes.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Scopes missing
     * Null result passed in to the method
     * Expected result: STATUS_FAILED with OAuth20InvalidScopeException
     */
    @Test
    public void checkForEmptyScopeSetAfterConsent_MissingScope_NoInitialResult() {

        final String[] scopes = null;

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(new String[] { SCOPE }));
                    one(obc).getScope();
                    will(returnValue(SCOPE));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            OAuthResult result = ca.checkForEmptyScopeSetAfterConsent(scopes, null, request, provider, CID);

            assertEquals("Did not receive the expected status code.", OAuthResult.STATUS_FAILED, result.getStatus());

            invalidScopeException(result, new String[] { SCOPE }, new String[] { SCOPE });

            // Make sure the result still contains the attribute(s) set in the result passed in to the method
            List<Attribute> resultAttributes = result.getAttributeList().getAllAttributes();
            assertTrue("Found attributes when none should be present.", resultAttributes.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**
     * Scopes missing
     * At least one attribute included in the initial result passed to the method
     * Expected result: STATUS_FAILED with OAuth20InvalidScopeException
     */
    @Test
    public void checkForEmptyScopeSetAfterConsent_MissingScope() {

        final String[] scopes = new String[0];

        // Create attribute list for initial result object
        String initialAttrName = "initialAttr";
        String initialAttrValue = "attrValue";
        AttributeList initialList = new AttributeList();
        initialList.setAttribute(initialAttrName, OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST, new String[] { initialAttrValue });

        OAuthResultImpl initialResult = new OAuthResultImpl(OAuthResult.STATUS_OK, initialList);

        try {
            getOidcOAuth20ClientExpectations(provider, oocp, CID, obc);
            mock.checking(new Expectations() {
                {
                    one(request).getParameterValues(OAuth20Constants.SCOPE);
                    will(returnValue(new String[] { SCOPE }));
                    one(obc).getScope();
                    will(returnValue(SCOPE));
                }
            });

            ClientAuthorization ca = new ClientAuthorization();
            OAuthResult result = ca.checkForEmptyScopeSetAfterConsent(scopes, initialResult, request, provider, CID);

            assertEquals("Did not receive the expected status code.", OAuthResult.STATUS_FAILED, result.getStatus());

            invalidScopeException(result, new String[] { SCOPE }, new String[] { SCOPE });

            // Make sure the result still contains the attribute(s) set in the result passed in to the method
            List<Attribute> resultAttributes = result.getAttributeList().getAllAttributes();
            assertEquals("Did not find the expected number of attributes.", initialList.getAllAttributes().size(), resultAttributes.size());

            String[] resultAttrValue = result.getAttributeList().getAttributeValuesByName(initialAttrName);
            assertArrayEquals("Did not get the expected attribute value in the result.", new String[] { initialAttrValue }, resultAttrValue);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(name.getMethodName(), t);
        }
    }

    /**************************************** end of tests ****************************************/

    /**
     * Calls the validateAuthorization() method and asserts that the provided status code matches the result's status code.
     *
     * @param expectedStatusCode
     * @return
     * @throws OidcServerException
     */
    private OAuthResult validateAuthorization(int expectedStatusCode) throws OidcServerException {
        ClientAuthorization ca = new ClientAuthorization();
        OAuthResult result = ca.validateAuthorization(provider, request, response);
        assertEquals("Did not receive the expected status code.", expectedStatusCode, result.getStatus());
        return result;
    }

    /**
     * Calls the validateAndHandle2LegsScope() method and asserts that the provided status code matches the result's status code.
     *
     * @param expectedStatusCode
     * @return
     */
    private OAuthResult validateAndHandle2LegsScope(int expectedStatusCode) {
        ClientAuthorization ca = new ClientAuthorization();
        OAuthResult result = ca.validateAndHandle2LegsScope(provider, request, response, CID);
        assertEquals("Did not receive the expected status code.", expectedStatusCode, result.getStatus());
        return result;
    }

    /**
     * Verifies that the expected feature and scopes are included in the result's attribute list.
     *
     * @param result
     * @param hasAttributes
     * @param expectedRequestFeature
     * @param expectedScopes
     */
    private void verify2LegsResult(OAuthResult result, boolean hasAttributes, String expectedRequestFeature, String... expectedScopes) {
        AttributeList attributeList = result.getAttributeList();

        if (!hasAttributes) {
            List<Attribute> allAttributes = attributeList.getAllAttributes();
            assertTrue("Found attributes in result when none should be present.", allAttributes.isEmpty());
        }

        String requestFeature = attributeList.getAttributeValueByName(OAuth20Constants.REQUEST_FEATURE);
        assertEquals("Did not get expected request feature attribute in the result.", expectedRequestFeature, requestFeature);

        String scopes[] = attributeList.getAttributeValuesByName(OAuth20Constants.SCOPE);
        if (expectedScopes == null || expectedScopes.length == 0) {
            assertNull("Found scopes in the result when none should be found: " + formatScopeStrings(scopes), scopes);
        } else {
            assertEquals("Did not get expected scopes in the result.", formatScopeStrings(expectedScopes), formatScopeStrings(scopes));
        }
    }

    /**
     * Asserts that no attributes are found in the provided result.
     *
     * @param result
     */
    private void verifyEmpty2LegsResult(OAuthResult result) {
        AttributeList attributeList = result.getAttributeList();
        List<Attribute> allAttributes = attributeList.getAllAttributes();
        assertTrue("Found attributes in result when none should be present.", allAttributes.isEmpty());
    }

    /**
     * Returns an array of client grant types excluding the ones specified as arguments.
     *
     * @param excludeGrantTypes
     * @return
     */
    private String[] getGrantTypesExcluding(String... excludeGrantTypes) {
        List<String> grantTypes = new ArrayList<String>(clientGrantTypesList);
        Set<String> excludeSet = new HashSet<String>();

        if (excludeGrantTypes != null) {
            for (String excludeGT : excludeGrantTypes) {
                excludeSet.add(excludeGT);
            }
        }
        grantTypes.removeAll(excludeSet);

        return grantTypes.toArray(new String[grantTypes.size()]);
    }

    /**
     * Verifies that the appropriate state value was included in the response. If a state was randomly chosen to be included,
     * it must be included in the response.
     *
     * @param result
     * @param stateArray
     */
    private void verifyState(OAuthResult result, String[] stateArray) {
        AttributeList attrList = result.getAttributeList();
        assertEquals("Did not get the expected state value.", (stateArray == null) ? null : stateArray[0], attrList.getAttributeValueByName(OAuth20Constants.STATE));
    }

    /**
     * Sorts the provided list of scopes and returns a normalized string. Because the order of scopes does not matter, this
     * normalizes scope lists without loss of precision.
     *
     * @param scopes
     * @return
     */
    private String formatScopeStrings(String... scopes) {
        List<String> sortedStrings = new ArrayList<String>();
        if (scopes == null) {
            return null;
        }
        for (String s : scopes) {
            sortedStrings.add(s);
        }
        Collections.sort(sortedStrings);

        String[] sortedArray = sortedStrings.toArray(new String[sortedStrings.size()]);
        String formattedResult = Arrays.toString(sortedArray).replace(",", "");
        System.out.println("Formatted strings: " + formattedResult);

        return formattedResult;
    }

}
