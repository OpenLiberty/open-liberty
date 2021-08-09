/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import com.google.gson.JsonArray;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;

import test.common.SharedOutputManager;

public class TokenExchangeTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule managerRule = outputMgr;
    @Rule
    public TestName testNameRule = new TestName();

    private String testName = null;

    private final Mockery mockery = new Mockery();
    private final OAuth20Provider provider = mockery.mock(OAuth20Provider.class);
    private final OidcOAuth20ClientProvider clientProvider = mockery.mock(OidcOAuth20ClientProvider.class);

    private final String clientId = "my client id";
    private final String clientSecret = "my client secret";
    private final String clientName = "my client name";
    private final String componentId = "my component id";
    private final JsonArray redirectUris = new JsonArray();

    private TokenExchange exchange = new TokenExchange();
    private final OidcBaseClient client = new OidcBaseClient(clientId, clientSecret, redirectUris, clientName, componentId, true);

    @Before
    public void before() {
        testName = testNameRule.getMethodName();
        System.out.println("Entering test: " + testName);
        exchange = new TokenExchange();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName);
        mockery.assertIsSatisfied();
    }

    /*********************************************** clientIdAndSecretValid ***********************************************/

    @Test
    public void test_clientIdAndSecretValid_clientProviderMissing() {
        String[] clientIdAndSecret = new String[] { clientId, clientSecret };
        mockery.checking(new Expectations() {
            {
                one(provider).getClientProvider();
                will(returnValue(null));
            }
        });
        boolean result = exchange.clientIdAndSecretValid(provider, clientIdAndSecret);
        assertFalse("Missing client provider should not have been considered valid.", result);
    }

    @Test
    public void test_clientIdAndSecretValid_noMatchingClient() throws Exception {
        String[] clientIdAndSecret = new String[] { clientId, clientSecret };
        mockery.checking(new Expectations() {
            {
                one(provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).get(clientId);
                will(returnValue(null));
            }
        });
        boolean result = exchange.clientIdAndSecretValid(provider, clientIdAndSecret);
        assertFalse("No matching client in the client provider should not have been considered valid.", result);
    }

    @Test
    public void test_clientIdAndSecretValid_nullClientId_noMatchingClient() throws Exception {
        String[] clientIdAndSecret = new String[] { null, clientSecret };
        mockery.checking(new Expectations() {
            {
                one(provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).get(null);
                will(returnValue(null));
            }
        });
        boolean result = exchange.clientIdAndSecretValid(provider, clientIdAndSecret);
        assertFalse("Null client ID with no matching client should not have been considered valid.", result);
    }

    @Test
    public void test_clientIdAndSecretValid_nullClientSecret() throws Exception {
        String[] clientIdAndSecret = new String[] { clientId, null };
        mockery.checking(new Expectations() {
            {
                one(provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).get(clientId);
                will(returnValue(client));
            }
        });
        boolean result = exchange.clientIdAndSecretValid(provider, clientIdAndSecret);
        assertFalse("Null client secret should not have been considered valid.", result);
    }

    @Test
    public void test_clientIdAndSecretValid_mismatchedClientSecret() throws Exception {
        String[] clientIdAndSecret = new String[] { clientId, "some other secret" };
        mockery.checking(new Expectations() {
            {
                one(provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).get(clientId);
                will(returnValue(client));
            }
        });
        boolean result = exchange.clientIdAndSecretValid(provider, clientIdAndSecret);
        assertFalse("Non-matching client secret should not have been considered valid.", result);
    }

    @Test
    public void test_clientIdAndSecretValid_clientHasNullClientSecret() throws Exception {
        String[] clientIdAndSecret = new String[] { clientId, clientSecret };
        final OidcBaseClient client = new OidcBaseClient(clientId, null, redirectUris, clientName, componentId, true);
        mockery.checking(new Expectations() {
            {
                one(provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).get(clientId);
                will(returnValue(client));
            }
        });
        boolean result = exchange.clientIdAndSecretValid(provider, clientIdAndSecret);
        assertFalse("Client with a null secret should not have been considered valid.", result);
    }

    @Test
    public void test_clientIdAndSecretValid_matchingClientSecret() throws Exception {
        String[] clientIdAndSecret = new String[] { clientId, clientSecret };
        mockery.checking(new Expectations() {
            {
                one(provider).getClientProvider();
                will(returnValue(clientProvider));
                one(clientProvider).get(clientId);
                will(returnValue(client));
            }
        });
        boolean result = exchange.clientIdAndSecretValid(provider, clientIdAndSecret);
        assertTrue("Matching client id and secret should have been considered valid.", result);
    }

}
