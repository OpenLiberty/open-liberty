/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.logout;

import static org.junit.Assert.assertEquals;

import java.net.URLEncoder;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.oidcclientcore.JakartaOIDCConstants;
import io.openliberty.security.oidcclientcore.client.LogoutConfig;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import test.common.SharedOutputManager;

public class RPInitiatedLogoutStrategyTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final OidcClientConfig oidcClientConfig = mockery.mock(OidcClientConfig.class);
    private final LogoutConfig logoutConfig = mockery.mock(LogoutConfig.class);

    private static final String CLIENT_ID = "myClientId";
    private static final String END_SESSION_ENDPOINT = "https://localhost/oidc/op/end_session";
    private static final String ID_TOKEN_STRING = "xxx.yyy.zzz";
    private static final String POST_LOGOUT_REDIRECT_URI = "https://localhost/rp/post_logout";

    RPInitiatedLogoutStrategy logoutStrategy;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getLogoutConfig();
                will(returnValue(logoutConfig));
            }
        });
        logoutStrategy = new RPInitiatedLogoutStrategy(oidcClientConfig, END_SESSION_ENDPOINT, ID_TOKEN_STRING);
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_buildEndSessionUrl_noLogoutConfig_noIdString() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getLogoutConfig();
                will(returnValue(null));
                one(oidcClientConfig).getClientId();
                will(returnValue(CLIENT_ID));
            }
        });
        logoutStrategy = new RPInitiatedLogoutStrategy(oidcClientConfig, END_SESSION_ENDPOINT, null);

        String expectedResult = END_SESSION_ENDPOINT + "?client_id=" + CLIENT_ID;

        String result = logoutStrategy.buildEndSessionUrl();
        assertEquals("End session URL did not match expected value.", expectedResult, result);
    }

    @Test
    public void test_buildEndSessionUrl_noLogoutConfig_withIdString() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getLogoutConfig();
                will(returnValue(null));
                one(oidcClientConfig).getClientId();
                will(returnValue(CLIENT_ID));
            }
        });
        logoutStrategy = new RPInitiatedLogoutStrategy(oidcClientConfig, END_SESSION_ENDPOINT, ID_TOKEN_STRING);

        String expectedResult = END_SESSION_ENDPOINT + "?client_id=" + CLIENT_ID;

        String result = logoutStrategy.buildEndSessionUrl();
        assertEquals("End session URL did not match expected value.", expectedResult, result);
    }

    @Test
    public void test_buildEndSessionUrl_withLogoutConfig_noRedirectUri_withIdString() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getClientId();
                will(returnValue(CLIENT_ID));
                one(logoutConfig).getRedirectURI();
                will(returnValue(null));
            }
        });

        String expectedResult = END_SESSION_ENDPOINT + "?client_id=" + CLIENT_ID;

        String result = logoutStrategy.buildEndSessionUrl();
        assertEquals("End session URL did not match expected value.", expectedResult, result);
    }

    @Test
    public void test_buildEndSessionUrl_withLogoutConfig_withRedirectUri_withIdString() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(oidcClientConfig).getClientId();
                will(returnValue(CLIENT_ID));
                one(logoutConfig).getRedirectURI();
                will(returnValue(POST_LOGOUT_REDIRECT_URI));
            }
        });

        String expectedResult = END_SESSION_ENDPOINT + "?client_id=" + CLIENT_ID + "&" + JakartaOIDCConstants.POST_LOGOUT_REDIRECT_URI + "="
                                + URLEncoder.encode(POST_LOGOUT_REDIRECT_URI, "UTF-8");

        String result = logoutStrategy.buildEndSessionUrl();
        assertEquals("End session URL did not match expected value.", expectedResult, result);
    }

    @Test
    public void test_appendParameter_nullValue() throws Exception {
        String queryString = "";
        String parameterValue = null;
        String result = logoutStrategy.appendParameter(queryString, "name", parameterValue);
        assertEquals("Query string should not have changed.", queryString, result);
    }

    @Test
    public void test_appendParameter_emptyValue() throws Exception {
        String queryString = "";
        String parameterValue = "";
        String result = logoutStrategy.appendParameter(queryString, "name", parameterValue);
        assertEquals("Query string should not have changed.", queryString, result);
    }

    @Test
    public void test_appendParameter_simpleValue() throws Exception {
        String queryString = "";
        String parameterValue = "simple_string";
        String result = logoutStrategy.appendParameter(queryString, "name", parameterValue);

        String expectedResult = "name=" + parameterValue;
        assertEquals("Query string did not match expected value.", expectedResult, result);
    }

    @Test
    public void test_appendParameter_complexValue() throws Exception {
        String queryString = "";
        String parameterValue = "start !@#$%^&*()-= end";
        String result = logoutStrategy.appendParameter(queryString, "name", parameterValue);

        String expectedResult = "name=" + URLEncoder.encode(parameterValue, "UTF-8");
        assertEquals("Query string did not match expected value.", expectedResult, result);
    }

}
