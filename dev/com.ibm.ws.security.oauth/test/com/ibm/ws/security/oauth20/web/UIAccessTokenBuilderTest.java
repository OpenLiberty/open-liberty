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

import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;

import test.common.SharedOutputManager;

/**
 *
 */
public class UIAccessTokenBuilderTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule outputRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final OAuth20Provider provider = mock.mock(OAuth20Provider.class);

    private final OidcOAuth20ClientProvider ocp = mock.mock(OidcOAuth20ClientProvider.class);
    private final OidcBaseClient obc = mock.mock(OidcBaseClient.class);

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void after() {
        mock.assertIsSatisfied();
    }

    @Test
    // check that preauthorized scopes of client id are correctly obtained from client config
    public void testGetScopes() {
        final String clientId = "testClientId";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(provider).getClientProvider();
                    will(returnValue(ocp));
                    allowing(provider).getComponent();
                    will(returnValue(null));
                    allowing(ocp).get(clientId);
                    will(returnValue(obc));
                    allowing(obc).getPreAuthorizedScope();
                    will(returnValue("a    b c"));
                }
            });
            // testing: String scopes = _provider.getClientProvider().get(clientId).getPreAuthorizedScope();
            UIAccessTokenBuilder uitb = new UIAccessTokenBuilder(provider, null);
            String[] result = uitb.getScopes(clientId);

            assertTrue("wrong array size: " + result.length, result.length == 3);
            assertTrue("wrong array contents: " + result, result[0].equals("a") && result[1].equals("b") && result[2].equals("c"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testScopesPassedToTokenFactory", t);
        }
    }

    @Test
    // check that a call to getTokenAttributesMap calls getScopes so scopes are included in token
    public void testScopesPassedToTokenFactory() {
        final String clientId = "testClientId";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(provider).getClientProvider();
                    will(returnValue(ocp));
                    allowing(provider).getComponent();
                    will(returnValue(null));
                    allowing(ocp).get(clientId);
                    will(returnValue(obc));
                    allowing(obc).getPreAuthorizedScope();
                    will(returnValue("a b testscope"));
                }
            });

            UIAccessTokenBuilder uitb = new UIAccessTokenBuilder(provider, null);
            Map<String, String[]> result = uitb.getTokenAttributesMap(new OAuth20TokenFactory(null), clientId, "user");
            String scopeUsed = result.get(OAuth20Constants.SCOPE)[2];
            assertTrue("did not get expected scope, expected testscope and got: " + scopeUsed, scopeUsed.equals("testscope"));

        } catch (Throwable t) {
            outputMgr.failWithThrowable("testScopesPassedToTokenFactory", t);
        }
    }
}