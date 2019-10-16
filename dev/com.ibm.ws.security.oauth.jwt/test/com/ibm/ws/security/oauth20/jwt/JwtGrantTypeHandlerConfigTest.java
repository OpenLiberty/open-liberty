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
package com.ibm.ws.security.oauth20.jwt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.PublicKey;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.util.BoundedCommonCache;

public class JwtGrantTypeHandlerConfigTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;
    // tokenString
    final String tokenString = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOi8vaGFybW9uaWM6ODAxMSIsIm5vbmNlIjoibXlOb25jZU15Tm9uY2UiLCJpYXQiOjEzODkyODYxNzAsInN1YiI6InRlc3R1c2VyIiwiZXhwIjoxMzg5Mjg5NzcwLCJhdWQiOiJjbGllbnQwMSJ9.iuqcj3SNSeos38St61fCU9alkExIsgVjVTdQfKilhrM";

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    // final AttributeList attributeList = mock.mock(AttributeList.class, "attributeList");
    final OAuth20Provider mockConfig = mock.mock(OAuth20Provider.class, "mockConfig");
    final SecurityService mockSecurityService = mock.mock(SecurityService.class, "mockSecurityService");
    final PublicKey mockPublicKey = mock.mock(PublicKey.class, "mockPublicKey");

    final String providerId = "testProviderId";

    final String clientId = "client03";
    final String client_secret = "client03pwd";
    final String signatureAlgorithm = "HS256";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        //outputMgr.captureStreams();
        outputMgr.trace("*=all");
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
        //outputMgr.restoreStreams();
        outputMgr.trace("*=all=disabled");
    }

    @Test
    public void testGetMethods() {
        final String methodName = "testGetMethods";
        try {
            JwtGrantTypeHandlerConfig configTool = new JwtGrantTypeHandlerConfig(providerId, mockConfig);
            mock.checking(new Expectations() {
                {
                    one(mockConfig).getSecurityService();
                    will(returnValue(mockSecurityService));
                    one(mockConfig).getJwtClockSkew();
                    will(returnValue(300L));
                    one(mockConfig).getJwtMaxJtiCacheSize();
                    will(returnValue(100000L));
                    one(mockConfig).getJwtTokenMaxLifetime();
                    will(returnValue(7200L));
                    one(mockConfig).getJwtIatRequired();
                    will(returnValue(true));
                    one(mockConfig).getJwtMaxJtiCacheSize();
                    will(returnValue(1000L));
                    one(mockConfig).getJwtMaxJtiCacheSize();
                    will(returnValue(1500L));
                    one(mockConfig).isAutoAuthorize();
                    will(returnValue(true));
                    one(mockConfig).getAutoAuthorizeClients();
                    will(returnValue(new String[] { "client03" }));
                    one(mockConfig).getAutoAuthorizeClients();
                    will(returnValue(new String[] { "client03" }));
                }
            });
            String strProviderId = configTool.getProviderId();
            assertEquals("The providerId is not " + providerId, strProviderId, providerId);

            SecurityService securityService = configTool.getSecurityService();
            assertEquals("Did not get the right SecurityService", securityService, mockSecurityService);

            long jwtClockSkew = configTool.getJwtClockSkew();
            assertEquals("Did not get the right jwtClockSkew", jwtClockSkew, 300L);

            long jwtMaxJtiCacheSize = configTool.getJwtMaxJtiCacheSize();
            assertEquals("Did not get the right jwtMaxJtiCacheSize", jwtMaxJtiCacheSize, 100000L);

            long jwtTokenMaxLifetime = configTool.getJwtTokenMaxLifetime();
            assertEquals("Did not get the right jwtTokenMaxLifetime()", jwtTokenMaxLifetime, 7200L);

            boolean jwtIatRequired = configTool.isJwtIatRequired();
            assertTrue("jwtIatRequired is not true", jwtIatRequired);

            BoundedCommonCache<String> cache = configTool.getJtiCache();
            assertEquals("cache size is not 1000L", cache.getCapacity(), 1000L);

            BoundedCommonCache<String> cache1 = configTool.getJtiCache();
            assertEquals("cache size is not 1500L", cache1.getCapacity(), 1500L);
            assertTrue("cache changed. It is not the same instance", cache1 == cache);

            boolean bAutoAuthorize = configTool.isAutoAuthorize();
            assertTrue("boolean isAutoAuthorize() in not true", bAutoAuthorize);

            boolean bAutoAuthorizeClient = configTool.isAutoAuthorizeClient("client03");
            assertTrue("client03 is a clinet but it says it's not", bAutoAuthorizeClient);

            boolean bAutoAuthorizeClient4 = configTool.isAutoAuthorizeClient("client04");
            assertFalse("client03 is a clinet but it says it's not", bAutoAuthorizeClient4);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
