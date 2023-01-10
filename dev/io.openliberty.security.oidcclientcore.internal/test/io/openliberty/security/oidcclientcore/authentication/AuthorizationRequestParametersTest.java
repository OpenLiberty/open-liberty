/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import test.common.SharedOutputManager;

public class AuthorizationRequestParametersTest {

    protected static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestName testName = new TestName();

    private final String authorizationEndpointUrl = "https://localhost:8020/oidc/op/authorize";
    private final String scope = "openid";
    private final String responseType = "code";
    private final String clientId = "oidcClientId";
    private final String redirectUri = "https://localhost:9020/oidc/rp/callback";
    private final String state = "statevalue";

    private AuthorizationRequestParameters parameters;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        parameters = new AuthorizationRequestParameters(authorizationEndpointUrl, scope, responseType, clientId, redirectUri, state);
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_buildRequestUrl_goldenPath() throws UnsupportedEncodingException {
        String requestUrl = parameters.buildRequestUrl();

        String expectedStart = authorizationEndpointUrl + "?";
        assertTrue("Built request URL [" + requestUrl + "] did not start with the expected value [" + expectedStart + "].", requestUrl.startsWith(expectedStart));

        String query = requestUrl.split("\\?")[1];
        String[] params = query.split("&");
        assertEquals("Query string [" + query + "] did not have the expected number of parameters.", 5, params.length);

        assertQueryContainsParameter(query, AuthorizationRequestParameters.SCOPE, scope);
        assertQueryContainsParameter(query, AuthorizationRequestParameters.RESPONSE_TYPE, responseType);
        assertQueryContainsParameter(query, AuthorizationRequestParameters.CLIEND_ID, clientId);
        assertQueryContainsParameter(query, AuthorizationRequestParameters.REDIRECT_URI, redirectUri);
        assertQueryContainsParameter(query, AuthorizationRequestParameters.STATE, state);
    }

    @Test
    public void test_buildRequestUrl_authzEndpointContainsQuery() throws UnsupportedEncodingException {
        String authzParam1Name = "extra_param";
        String authzParam1Value = "special !@#$%^&*()-_=+ chars";
        String authzParam2Name = "domain";
        String authzParam2Value = "example.com";
        String authorizationEndpointUrl = this.authorizationEndpointUrl + "?" + authzParam1Name + "=" + URLEncoder.encode(authzParam1Value, "UTF-8") + "&" + authzParam2Name + "="
                                          + authzParam2Value;

        parameters = new AuthorizationRequestParameters(authorizationEndpointUrl, scope, responseType, clientId, redirectUri, state);

        String requestUrl = parameters.buildRequestUrl();

        String expectedStart = this.authorizationEndpointUrl + "?";
        assertTrue("Built request URL [" + requestUrl + "] did not start with the expected value [" + expectedStart + "].", requestUrl.startsWith(expectedStart));

        String query = requestUrl.split("\\?")[1];
        String[] params = query.split("&");
        assertEquals("Query string [" + query + "] did not have the expected number of parameters.", 7, params.length);

        assertQueryContainsParameter(query, AuthorizationRequestParameters.SCOPE, scope);
        assertQueryContainsParameter(query, AuthorizationRequestParameters.RESPONSE_TYPE, responseType);
        assertQueryContainsParameter(query, AuthorizationRequestParameters.CLIEND_ID, clientId);
        assertQueryContainsParameter(query, AuthorizationRequestParameters.REDIRECT_URI, redirectUri);
        assertQueryContainsParameter(query, AuthorizationRequestParameters.STATE, state);
        assertQueryContainsParameter(query, authzParam1Name, authzParam1Value);
        assertQueryContainsParameter(query, authzParam2Name, authzParam2Value);
    }

    @Test
    public void test_buildRequestQueryString_noConditionalParams() throws UnsupportedEncodingException {
        String query = parameters.buildRequestQueryString();

        String[] params = query.split("&");
        assertEquals("Query string [" + query + "] did not have the expected number of parameters.", 5, params.length);

        assertQueryContainsParameter(query, AuthorizationRequestParameters.SCOPE, scope);
        assertQueryContainsParameter(query, AuthorizationRequestParameters.RESPONSE_TYPE, responseType);
        assertQueryContainsParameter(query, AuthorizationRequestParameters.CLIEND_ID, clientId);
        assertQueryContainsParameter(query, AuthorizationRequestParameters.REDIRECT_URI, redirectUri);
        assertQueryContainsParameter(query, AuthorizationRequestParameters.STATE, state);
    }

    @Test
    public void test_buildRequestQueryString_withConditionalParams() throws UnsupportedEncodingException {
        String paramNameBase = "paramName";
        String paramValueBase = "paramValue";
        parameters.addParameter(paramNameBase + "1", paramValueBase + "1");
        parameters.addParameter(paramNameBase + "2", paramValueBase + "2");
        parameters.addParameter(paramNameBase + "3", paramValueBase + "3");

        String query = parameters.buildRequestQueryString();

        String[] params = query.split("&");
        assertEquals("Query string [" + query + "] did not have the expected number of parameters.", 8, params.length);

        assertQueryContainsParameter(query, AuthorizationRequestParameters.SCOPE, scope);
        assertQueryContainsParameter(query, AuthorizationRequestParameters.RESPONSE_TYPE, responseType);
        assertQueryContainsParameter(query, AuthorizationRequestParameters.CLIEND_ID, clientId);
        assertQueryContainsParameter(query, AuthorizationRequestParameters.REDIRECT_URI, redirectUri);
        assertQueryContainsParameter(query, AuthorizationRequestParameters.STATE, state);
        assertQueryContainsParameter(query, paramNameBase + "1", paramValueBase + "1");
        assertQueryContainsParameter(query, paramNameBase + "2", paramValueBase + "2");
        assertQueryContainsParameter(query, paramNameBase + "3", paramValueBase + "3");
    }

    @Test
    public void test_buildQueryWithRequiredParameters() throws UnsupportedEncodingException {
        String query = parameters.buildQueryWithRequiredParameters();

        String[] params = query.split("&");
        assertEquals("Query string [" + query + "] did not have the expected number of parameters.", 5, params.length);

        assertQueryContainsParameter(query, AuthorizationRequestParameters.SCOPE, scope);
        assertQueryContainsParameter(query, AuthorizationRequestParameters.RESPONSE_TYPE, responseType);
        assertQueryContainsParameter(query, AuthorizationRequestParameters.CLIEND_ID, clientId);
        assertQueryContainsParameter(query, AuthorizationRequestParameters.REDIRECT_URI, redirectUri);
        assertQueryContainsParameter(query, AuthorizationRequestParameters.STATE, state);
    }

    @Test
    public void test_appendConditionalParametersToQuery_noConditionalParams() {
        String query = "start";

        String result = parameters.appendConditionalParametersToQuery(query);
        assertEquals("Query string should not have been modified.", query, result);
    }

    @Test
    public void test_appendConditionalParametersToQuery_nullName() {
        String query = "start";
        String paramName = null;
        String paramValue = "paramValue";
        parameters.addParameter(paramName, paramValue);

        String result = parameters.appendConditionalParametersToQuery(query);
        assertEquals("Query string should not have been modified.", query, result);
    }

    @Test
    public void test_appendConditionalParametersToQuery_emptyName() {
        String query = "start";
        String paramName = "";
        String paramValue = "paramValue";
        parameters.addParameter(paramName, paramValue);

        String result = parameters.appendConditionalParametersToQuery(query);
        String expectedResult = query + "&" + paramName + "=" + paramValue;
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_appendConditionalParametersToQuery_nullValue() {
        String query = "start";
        String paramName = "paramName";
        String paramValue = null;
        parameters.addParameter(paramName, paramValue);

        String result = parameters.appendConditionalParametersToQuery(query);
        String expectedResult = query + "&" + paramName;
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_appendConditionalParametersToQuery_emptyValue() {
        String query = "start";
        String paramName = "paramName";
        String paramValue = "";
        parameters.addParameter(paramName, paramValue);

        String result = parameters.appendConditionalParametersToQuery(query);
        String expectedResult = query + "&" + paramName + "=" + paramValue;
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_appendConditionalParametersToQuery_multipleParams() {
        String query = "start";
        String paramNameBase = "paramName";
        String paramValueBase = "paramValue";
        parameters.addParameter(paramNameBase + "1", paramValueBase + "1");
        parameters.addParameter(paramNameBase + "2", paramValueBase + "2");
        parameters.addParameter(paramNameBase + "3", paramValueBase + "3");

        String result = parameters.appendConditionalParametersToQuery(query);
        String expectedResult = query + "&" + paramNameBase + "1" + "=" + paramValueBase + "1" + "&" + paramNameBase + "2" + "=" + paramValueBase + "2" + "&" + paramNameBase + "3"
                                + "=" + paramValueBase + "3";
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_appendParameterToQuery_nameNull() {
        String query = "start";
        String parameterName = null;
        String parameterValue = "value";

        String result = parameters.appendParameterToQuery(query, parameterName, parameterValue);
        assertEquals("Query string should not have been modified.", query, result);
    }

    @Test
    public void test_appendParameterToQuery_nameEmpty() {
        String query = "start";
        String parameterName = "";
        String parameterValue = "value";

        String result = parameters.appendParameterToQuery(query, parameterName, parameterValue);
        String expected = query + "&" + parameterName + "=" + parameterValue;
        assertEquals(expected, result);
    }

    @Test
    public void test_appendParameterToQuery_nameContainsSpecialCharacters() {
        String query = "start";
        String parameterName = "special=&,?/:chars";
        String parameterValue = "value";

        String result = parameters.appendParameterToQuery(query, parameterName, parameterValue);
        String expected = query + "&" + "special%3D%26%2C%3F%2F%3Achars" + "=" + parameterValue;
        assertEquals(expected, result);
    }

    @Test
    public void test_appendParameterToQuery_valueNull() {
        String query = "start";
        String parameterName = "name";
        String parameterValue = null;

        String result = parameters.appendParameterToQuery(query, parameterName, parameterValue);
        String expected = query + "&" + parameterName;
        assertEquals(expected, result);
    }

    @Test
    public void test_appendParameterToQuery_valueEmpty() {
        String query = "start";
        String parameterName = "name";
        String parameterValue = "";

        String result = parameters.appendParameterToQuery(query, parameterName, parameterValue);
        String expected = query + "&" + parameterName + "=" + parameterValue;
        assertEquals(expected, result);
    }

    @Test
    public void test_appendParameterToQuery_valueContainsSpecialCharacters() {
        String query = "start";
        String parameterName = "name";
        String parameterValue = "special=&,?/:chars";

        String result = parameters.appendParameterToQuery(query, parameterName, parameterValue);
        String expected = query + "&" + parameterName + "=" + "special%3D%26%2C%3F%2F%3Achars";
        assertEquals(expected, result);
    }

    @Test
    public void test_appendParameterToQuery_queryNull() {
        String query = null;
        String parameterName = "name";
        String parameterValue = "value";

        String result = parameters.appendParameterToQuery(query, parameterName, parameterValue);
        String expected = parameterName + "=" + parameterValue;
        assertEquals(expected, result);
    }

    @Test
    public void test_appendParameterToQuery_queryEmpty() {
        String query = "";
        String parameterName = "name";
        String parameterValue = "value";

        String result = parameters.appendParameterToQuery(query, parameterName, parameterValue);
        String expected = parameterName + "=" + parameterValue;
        assertEquals(expected, result);
    }

    @Test
    public void test_appendParameterToQuery_queryNotEmpty() {
        String query = "start";
        String parameterName = "name";
        String parameterValue = "value";

        String result = parameters.appendParameterToQuery(query, parameterName, parameterValue);
        String expected = query + "&" + parameterName + "=" + parameterValue;
        assertEquals(expected, result);
    }

    @Test
    public void test_appendParameterToQuery_queryContainsOtherParameters() {
        String query = "value=1&other_value=some+other+value";
        String parameterName = "name";
        String parameterValue = "value";

        String result = parameters.appendParameterToQuery(query, parameterName, parameterValue);
        String expected = query + "&" + parameterName + "=" + parameterValue;
        assertEquals(expected, result);
    }

    private void assertQueryContainsParameter(String query, String paramName, String paramValue) throws UnsupportedEncodingException {
        Pattern expectedPattern = Pattern.compile("(^|.+&)" + Pattern.quote(paramName + "=" + URLEncoder.encode(paramValue, "UTF-8")) + "(&.+|$)");
        assertTrue("Did not find pattern " + expectedPattern.pattern() + " in query [" + query + "].", Pattern.matches(expectedPattern.pattern(), query));
    }

}
