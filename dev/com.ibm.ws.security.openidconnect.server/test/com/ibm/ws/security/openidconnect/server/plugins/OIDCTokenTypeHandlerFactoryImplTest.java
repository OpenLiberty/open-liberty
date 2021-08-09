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

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.oauth.core.api.OAuthComponentInstance;
import com.ibm.oauth.core.internal.oauth20.OAuth20ComponentInternal;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;
import com.ibm.oauth.core.internal.oauth20.tokentype.OAuth20TokenTypeHandler;

public class OIDCTokenTypeHandlerFactoryImplTest {
    private static SharedOutputManager outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    final OAuth20ComponentInternal oa20ci = mock.mock(OAuth20ComponentInternal.class, "oa20ci");
    final OAuthComponentInstance oaci = mock.mock(OAuthComponentInstance.class, "oaci");
    final OAuth20ConfigProvider config = mock.mock(OAuth20ConfigProvider.class, "config");
    final OAuth20TokenTypeHandler oa20tth = mock.mock(OAuth20TokenTypeHandler.class, "oa20tth");

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
            OIDCTokenTypeHandlerFactory oidcTokenTypeHandlerFactory = new OIDCTokenTypeHandlerFactory();
            assertNotNull("Can not instantiate an oidcTokenTypehandlerCode", oidcTokenTypeHandlerFactory);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetHandler() {
        final String methodName = "testGetHandler";
        try {
            OIDCTokenTypeHandlerFactory oidcTokenTypeHandlerFactory = new OIDCTokenTypeHandlerFactory();
            mock.checking(new Expectations() {
                {
                    one(oa20ci).getParentComponentInstance();
                    will(returnValue(oaci));
                    one(oaci).getInstanceId();
                    will(returnValue("myOaId1"));
                    one(oa20ci).get20Configuration();
                    will(returnValue(config));
                    one(config).getTokenTypeHandler();
                    will(returnValue(oa20tth));
                    one(oa20ci).getParentComponentInstance();
                    will(returnValue(oaci));
                    one(oaci).getInstanceId();
                    will(returnValue("myOaId1"));

                }
            });
            OAuth20TokenTypeHandler result = OIDCTokenTypeHandlerFactory.getHandler(oa20ci);
            assertNotNull("Can not instantiate an oidcTokenTypehandlerCode", oidcTokenTypeHandlerFactory);
            assertNotNull("Did not get instantiate an OAuth20TokenTypeHandler", result);
            assertEquals("It's supposed to get " + oa20tth + " but get " + result, oa20tth, result);
            OAuth20TokenTypeHandler result1 = OIDCTokenTypeHandlerFactory.getHandler(oa20ci); // do it again to get it from cache
            assertEquals("It's supposed to get " + oa20tth + " again but get " + result1, oa20tth, result1);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetIDTokenHandler() {
        final String methodName = "testGetIDTokenHandler";
        try {
            OIDCTokenTypeHandlerFactory oidcTokenTypeHandlerFactory = new OIDCTokenTypeHandlerFactory();
            mock.checking(new Expectations() {
                {
                    one(oa20ci).getParentComponentInstance();
                    will(returnValue(oaci));
                    one(oaci).getInstanceId();
                    will(returnValue("myOaId2"));
                    one(oa20ci).get20Configuration();
                    will(returnValue(config));
                    one(config).getIDTokenTypeHandler();
                    will(returnValue(oa20tth));
                    one(oa20ci).getParentComponentInstance();
                    will(returnValue(oaci));
                    one(oaci).getInstanceId();
                    will(returnValue("myOaId2"));
                }
            });
            OAuth20TokenTypeHandler result = OIDCTokenTypeHandlerFactory.getIDTokenHandler(oa20ci);
            assertNotNull("Can not instantiate an oidcTokenTypehandlerCode", oidcTokenTypeHandlerFactory);
            assertNotNull("Did not get instantiate an OAuth20TokenTypeHandler", result);
            assertEquals("It's supposed to get " + oa20tth + " but get " + result, oa20tth, result);
            OAuth20TokenTypeHandler result1 = OIDCTokenTypeHandlerFactory.getIDTokenHandler(oa20ci); // do it again to get it from cache
            assertEquals("It's supposed to get " + oa20tth + " again but get " + result1, oa20tth, result1);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
