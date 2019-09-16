/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.oauth.core.api.OAuthComponentInstance;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20ComponentInternal;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;
import com.ibm.oauth.core.internal.oauth20.tokentype.OAuth20TokenTypeHandler;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;

import test.common.SharedOutputManager;

public class IDTokenfactoryTest {
    private static SharedOutputManager outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    protected final OAuth20ComponentInternal oa2ci = mock.mock(OAuth20ComponentInternal.class, "oa2ci");
    protected final OAuthComponentInstance oaci = mock.mock(OAuthComponentInstance.class, "oaci");
    protected final OAuth20ConfigProvider oa2cp = mock.mock(OAuth20ConfigProvider.class, "oa2cp");
    protected final OAuth20TokenTypeHandler oa2tth = mock.mock(OAuth20TokenTypeHandler.class, "oa2tth");
    protected final OidcOAuth20ClientProvider oa2clientprovider = mock.mock(OidcOAuth20ClientProvider.class, "oa2clientprovider");
    protected final OidcBaseClient oidcBaseClient = mock.mock(OidcBaseClient.class, "oidcBaseClient");
    protected final OAuth20Token oa20token = mock.mock(OAuth20Token.class, "oa20token");

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @Test
    public void testConstructor() {
        final String methodName = "testConstructor";
        try {
            IDTokenFactory idtf = new IDTokenFactory(oa2ci);
            assertNotNull("There must be a IDTokenFactory", idtf);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testCreateIDToken_noOpenid() {
        final String methodName = "testCreateIDToken_noOpenid";
        try {
            Map<String, String[]> tokenMap = new HashMap<String, String[]>();
            tokenMap.put(OAuth20Constants.SCOPE, new String[] { "profile", "no_openid" });

            IDTokenFactory idtf = new IDTokenFactory(oa2ci);
            OAuth20Token oa2t = idtf.createIDToken(tokenMap);
            assertNull("we expected an ID_OKEN but none returned", oa2t);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testCreateIDToken_openid() {
        final String methodName = "testCreateIDToken_openid";
        try {
            Map<String, String[]> tokenMap = new HashMap<String, String[]>();
            tokenMap.put(OAuth20Constants.SCOPE, new String[] { "profile", "openid" });
            tokenMap.put(OAuth20Constants.LIFETIME, new String[] { "550" });
            tokenMap.put(OAuth20Constants.CLIENT_ID, new String[] { "client01" });
            mock.checking(new Expectations() {
                {
                    allowing(oa2ci).getParentComponentInstance();
                    will(returnValue(oaci));
                    allowing(oaci).getInstanceId();
                    will(returnValue("unitTestSampleId"));
                    allowing(oa2ci).get20Configuration();
                    will(returnValue(oa2cp));
                    one(oa2cp).getTokenLifetimeSeconds();
                    will(returnValue(600)); // 10 minutes
                    one(oa2cp).getAccessTokenLength();
                    will(returnValue(30));
                    one(oa2cp).getIDTokenTypeHandler();
                    will(returnValue(oa2tth));
                    one(oa2cp).getClientProvider();
                    will(returnValue(oa2clientprovider));
                    one(oa2clientprovider).get("client01");
                    will(returnValue(oidcBaseClient));
                    one(oidcBaseClient).getClientSecret();
                    will(returnValue("password"));
                    one(oa2tth).createToken(with(any(Map.class)));
                    will(returnValue(oa20token));
//                    one(oa20token).isPersistent();
//                    will(returnValue(false));
                }
            });

            IDTokenFactory idtf = new IDTokenFactory(oa2ci);
            OAuth20Token oa2t = idtf.createIDToken(tokenMap);
            assertNotNull("We are expecting an id_token(Oauth20Token but did not get it", oa2t);
            assertEquals("did not get back thye mocked id_token", oa2t, oa20token);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}
