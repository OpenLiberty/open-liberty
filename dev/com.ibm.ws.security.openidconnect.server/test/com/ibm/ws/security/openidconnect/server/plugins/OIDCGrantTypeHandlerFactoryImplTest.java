/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.plugins;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.oauth.core.api.error.OAuthConfigurationException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidGrantTypeException;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;
import com.ibm.oauth.core.internal.oauth20.granttype.OAuth20GrantTypeHandler;
import com.ibm.oauth.core.internal.oauth20.granttype.impl.OAuth20GrantTypeHandlerClientCredentialsImpl;
import com.ibm.oauth.core.internal.oauth20.granttype.impl.OAuth20GrantTypeHandlerResourceOwnerCredentialsImpl;

public class OIDCGrantTypeHandlerFactoryImplTest {
    private static SharedOutputManager outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    final OAuth20ConfigProvider oa20cp = mock.mock(OAuth20ConfigProvider.class, "oa20cp");

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
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
            OIDCGrantTypeHandlerFactoryImpl oidcGrantTypeHandlerFactoryImpl = new OIDCGrantTypeHandlerFactoryImpl();
            assertNotNull("Can not instantiate an oidcGrantTypehandlerCodeImpl", oidcGrantTypeHandlerFactoryImpl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHandlerBadGrantType() {
        final String methodName = "testGetHandlerBadGrantType";
        try {
            OIDCGrantTypeHandlerFactoryImpl oidcGrantTypeHandlerFactoryImpl = new OIDCGrantTypeHandlerFactoryImpl();
            assertNotNull("Can not instantiate an oidcGrantTypehandlerCodeImpl", oidcGrantTypeHandlerFactoryImpl);
            oidcGrantTypeHandlerFactoryImpl.getHandler(null, "BadGrantType", oa20cp);
            fail("Expect to get an OAuth20InvalidGrantTypeException but not");
        } catch (OAuth20InvalidGrantTypeException e) {
            // This is what we expected
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHandlerGrantTypeNotAllowed() {
        final String methodName = "testGetHandlerGrantTypeNotAllowed";
        try {
            OIDCGrantTypeHandlerFactoryImpl oidcGrantTypeHandlerFactoryImpl = new OIDCGrantTypeHandlerFactoryImpl();
            assertNotNull("Can not instantiate an oidcGrantTypehandlerCodeImpl", oidcGrantTypeHandlerFactoryImpl);
            final String grantType = "authorization_code";
            mock.checking(new Expectations() {
                {
                    one(oa20cp).isGrantTypeAllowed(grantType);
                    will(returnValue(false));
                }
            });
            oidcGrantTypeHandlerFactoryImpl.getHandler(null, "authorization_code", oa20cp);
            fail("Expect to get an OAuthConfigurationException but not");
        } catch (OAuthConfigurationException e) {
            // This is what we expected
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHandlerGrantTypeAuthorizationCode() {
        final String methodName = "testGetHandlerGrantTypeAuthorizationCode";
        try {
            OIDCGrantTypeHandlerFactoryImpl oidcGrantTypeHandlerFactoryImpl = new OIDCGrantTypeHandlerFactoryImpl();
            assertNotNull("Can not instantiate an oidcGrantTypehandlerCodeImpl", oidcGrantTypeHandlerFactoryImpl);
            final String grantType1 = OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE;
            mock.checking(new Expectations() {
                {
                    one(oa20cp).isGrantTypeAllowed(grantType1);
                    will(returnValue(true));
                }
            });
            OAuth20GrantTypeHandler oa20gth = oidcGrantTypeHandlerFactoryImpl.getHandler(null, grantType1, oa20cp);
            assertTrue("Expect to get OIDCGrantTypeHandlerCodeImpl but get " + oa20gth.getClass().getName(),
                       oa20gth instanceof OIDCGrantTypeHandlerCodeImpl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHandlerGrantTypeClientCredentials() {
        final String methodName = "testGetHandlerGrantTypeClientCredentials";
        try {
            OIDCGrantTypeHandlerFactoryImpl oidcGrantTypeHandlerFactoryImpl = new OIDCGrantTypeHandlerFactoryImpl();
            assertNotNull("Can not instantiate an oidcGrantTypehandlerCodeImpl", oidcGrantTypeHandlerFactoryImpl);
            final String grantType2 = OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS;
            mock.checking(new Expectations() {
                {
                    one(oa20cp).isGrantTypeAllowed(grantType2);
                    will(returnValue(true));
                }
            });
            OAuth20GrantTypeHandler oa20gth = oidcGrantTypeHandlerFactoryImpl.getHandler(null, grantType2, oa20cp);
            assertTrue("Expect to get OIDCGrantTypeHandlerCodeImpl but get " + oa20gth.getClass().getName(),
                       oa20gth instanceof OAuth20GrantTypeHandlerClientCredentialsImpl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHandlerGrantTypeResourceOwnerCredentials() {
        final String methodName = "testGetHandlerGrantTypeResourceOwnerCredentials";
        try {
            OIDCGrantTypeHandlerFactoryImpl oidcGrantTypeHandlerFactoryImpl = new OIDCGrantTypeHandlerFactoryImpl();
            assertNotNull("Can not instantiate an oidcGrantTypehandlerCodeImpl", oidcGrantTypeHandlerFactoryImpl);
            final String grantType3 = OAuth20Constants.GRANT_TYPE_PASSWORD;
            mock.checking(new Expectations() {
                {
                    one(oa20cp).isGrantTypeAllowed(grantType3);
                    will(returnValue(true));
                }
            });
            OAuth20GrantTypeHandler oa20gth = oidcGrantTypeHandlerFactoryImpl.getHandler(null, grantType3, oa20cp);
            assertTrue("Expect to get OIDCGrantTypeHandlerCodeImpl but get " + oa20gth.getClass().getName(),
                       oa20gth instanceof OAuth20GrantTypeHandlerResourceOwnerCredentialsImpl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHandlerGrantTypeRefreshToken() {
        final String methodName = "testGetHandlerGrantTypeRefreshToken";
        try {
            OIDCGrantTypeHandlerFactoryImpl oidcGrantTypeHandlerFactoryImpl = new OIDCGrantTypeHandlerFactoryImpl();
            assertNotNull("Can not instantiate an oidcGrantTypehandlerCodeImpl", oidcGrantTypeHandlerFactoryImpl);
            final String grantType4 = OAuth20Constants.GRANT_TYPE_REFRESH_TOKEN;
            mock.checking(new Expectations() {
                {
                    one(oa20cp).isGrantTypeAllowed(grantType4);
                    will(returnValue(true));
                }
            });
            OAuth20GrantTypeHandler oa20gth = oidcGrantTypeHandlerFactoryImpl.getHandler(null, grantType4, oa20cp);
            assertTrue("Expect to get OIDCGrantTypeHandlerCodeImpl but get " + oa20gth.getClass().getName(),
                       oa20gth instanceof OIDCGrantTypeHandlerRefreshImpl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHandlerGrantTypeImplicit() {
        final String methodName = "testGetHandlerGrantTypeImplicit";
        try {
            OIDCGrantTypeHandlerFactoryImpl oidcGrantTypeHandlerFactoryImpl = new OIDCGrantTypeHandlerFactoryImpl();
            assertNotNull("Can not instantiate an oidcGrantTypehandlerCodeImpl", oidcGrantTypeHandlerFactoryImpl);
            final String grantType4 = OAuth20Constants.GRANT_TYPE_IMPLICIT;
            mock.checking(new Expectations() {
                {
                    one(oa20cp).isGrantTypeAllowed(grantType4);
                    will(returnValue(true));
                }
            });
            OAuth20GrantTypeHandler oa20gth = oidcGrantTypeHandlerFactoryImpl.getHandler(null, grantType4, oa20cp);
            fail("Expect to get an OAuth20InvalidGrantTypeException but not");
        } catch (OAuth20InvalidGrantTypeException e) {
            // this is what we expected
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}
