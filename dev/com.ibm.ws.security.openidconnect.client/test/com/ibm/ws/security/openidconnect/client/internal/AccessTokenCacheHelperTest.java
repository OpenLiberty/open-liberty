/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.common.structures.SingleTableCache;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import test.common.SharedOutputManager;

public class AccessTokenCacheHelperTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.openidconnect*=all");
    @Rule
    public TestRule managerRule = outputMgr;

    private final OidcClientConfig clientConfig = mockery.mock(OidcClientConfig.class);
    private final ProviderAuthenticationResult authnResult = mockery.mock(ProviderAuthenticationResult.class);

    private static final String ACCESS_TOKEN = "access_token";
    private static final String HTTPS_URL = "https://localhost:8020/root";
    private static final String GOOD_USER = "user";

    private final AccessTokenCacheHelper cacheHelper = new AccessTokenCacheHelper();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.trace("*=all=disabled");
        outputMgr.restoreStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
    }

    @After
    public void tearDown() {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_getCachedTokenAuthenticationResult_tokenReuse_false() {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getTokenReuse();
                will(returnValue(false));
            }
        });

        ProviderAuthenticationResult result = cacheHelper.getCachedTokenAuthenticationResult(clientConfig, ACCESS_TOKEN);
        assertNull("Result should have been null but wasn't.", result);
    }

    @Test
    public void test_getCachedTokenAuthenticationResult_cacheEmpty() {
        SingleTableCache cache = getCache();
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getTokenReuse();
                will(returnValue(true));
                one(clientConfig).getCache();
                will(returnValue(cache));
            }
        });

        ProviderAuthenticationResult result = cacheHelper.getCachedTokenAuthenticationResult(clientConfig, ACCESS_TOKEN);
        assertNull("Result should have been null but wasn't.", result);
    }

    @Test
    public void test_getCachedTokenAuthenticationResult_nothingCachedForToken() {
        SingleTableCache cache = getCache();
        ProviderAuthenticationResult cachedResult = new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK);
        cache.put("someToken", cachedResult);
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getTokenReuse();
                will(returnValue(true));
                one(clientConfig).getCache();
                will(returnValue(cache));
            }
        });

        ProviderAuthenticationResult result = cacheHelper.getCachedTokenAuthenticationResult(clientConfig, ACCESS_TOKEN);
        assertNull("Result should have been null but wasn't.", result);
    }

    @Test
    public void test_getCachedTokenAuthenticationResult_resultAlreadyCached() {
        SingleTableCache cache = getCache();
        ProviderAuthenticationResult cachedResult = createProviderAuthenticationResult(System.currentTimeMillis());
        cache.put(ACCESS_TOKEN, cachedResult);
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getTokenReuse();
                will(returnValue(true));
                one(clientConfig).getCache();
                will(returnValue(cache));
                one(clientConfig).getClockSkewInSeconds();
                will(returnValue(300L));
            }
        });

        ProviderAuthenticationResult result = cacheHelper.getCachedTokenAuthenticationResult(clientConfig, ACCESS_TOKEN);
        assertNotNull("Result should not have been null but was.", result);
        assertEquals("Cached result did not match expected object.", cachedResult.getStatus(), result.getStatus());
        assertEquals("Cached result did not match expected object.", cachedResult.getHttpStatusCode(), result.getHttpStatusCode());
    }

    @Test
    public void test_cacheTokenAuthenticationResult_emptyCache() {
        SingleTableCache cache = getCache();
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getCache();
                will(returnValue(cache));
            }
        });

        ProviderAuthenticationResult resultToCache = new ProviderAuthenticationResult(AuthResult.OAUTH_CHALLENGE, HttpServletResponse.SC_EXPECTATION_FAILED);
        cacheHelper.cacheTokenAuthenticationResult(clientConfig, ACCESS_TOKEN, resultToCache);

        ProviderAuthenticationResult cachedResult = (ProviderAuthenticationResult) cache.get(ACCESS_TOKEN);
        assertNotNull("Result should not have been null but was.", cachedResult);
        assertEquals("Cached result did not match expected object.", resultToCache.getStatus(), cachedResult.getStatus());
        assertEquals("Cached result did not match expected object.", resultToCache.getHttpStatusCode(), cachedResult.getHttpStatusCode());
    }

    @Test
    public void test_cacheTokenAuthenticationResult_nonEmptyCache_newResult() {
        SingleTableCache cache = getCache();
        // Create an existing cache entry for a different key
        ProviderAuthenticationResult cachedResult = new ProviderAuthenticationResult(AuthResult.RETURN, HttpServletResponse.SC_CONFLICT);
        cache.put("otherEntry", cachedResult);
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getCache();
                will(returnValue(cache));
            }
        });

        ProviderAuthenticationResult resultToCache = new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK);
        cacheHelper.cacheTokenAuthenticationResult(clientConfig, ACCESS_TOKEN, resultToCache);

        ProviderAuthenticationResult newCachedResult = (ProviderAuthenticationResult) cache.get(ACCESS_TOKEN);
        assertNotNull("Result should not have been null but was.", newCachedResult);
        assertEquals("Cached result did not match expected object.", resultToCache.getStatus(), newCachedResult.getStatus());
        assertEquals("Cached result did not match expected object.", resultToCache.getHttpStatusCode(), newCachedResult.getHttpStatusCode());
    }

    @Test
    public void test_cacheTokenAuthenticationResult_entryAlreadyExists() {
        SingleTableCache cache = getCache();
        ProviderAuthenticationResult cachedResult = new ProviderAuthenticationResult(AuthResult.RETURN, HttpServletResponse.SC_CONFLICT);
        cache.put(ACCESS_TOKEN, cachedResult);
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getCache();
                will(returnValue(cache));
            }
        });

        ProviderAuthenticationResult resultToCache = new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK);
        cacheHelper.cacheTokenAuthenticationResult(clientConfig, ACCESS_TOKEN, resultToCache);

        ProviderAuthenticationResult newCachedResult = (ProviderAuthenticationResult) cache.get(ACCESS_TOKEN);
        assertNotNull("Result should not have been null but was.", newCachedResult);
        assertEquals("Cached result did not match expected object.", resultToCache.getStatus(), newCachedResult.getStatus());
        assertEquals("Cached result did not match expected object.", resultToCache.getHttpStatusCode(), newCachedResult.getHttpStatusCode());
    }

    @Test
    public void test_isTokenInCachedResultExpired_nullCustomProperties() {
        mockery.checking(new Expectations() {
            {
                one(authnResult).getCustomProperties();
                will(returnValue(null));
            }
        });

        boolean result = cacheHelper.isTokenInCachedResultExpired(authnResult, clientConfig);
        assertTrue("Authentication result without custom properties should have been considered as an expired token.", result);
    }

    @Test
    public void test_isTokenInCachedResultExpired_emptyCustomProperties() {
        final Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        mockery.checking(new Expectations() {
            {
                one(authnResult).getCustomProperties();
                will(returnValue(customProperties));
            }
        });

        boolean result = cacheHelper.isTokenInCachedResultExpired(authnResult, clientConfig);
        assertTrue("Authentication result with an empty set of custom properties should have been considered as an expired token.", result);
    }

    @Test
    public void test_isTokenInCachedResultExpired_zeroExp() {
        long expValue = 0;
        final Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        Map<String, Object> tokenInfoMap = new HashMap<String, Object>();
        tokenInfoMap.put("exp", expValue);
        customProperties.put(Constants.ACCESS_TOKEN_INFO, tokenInfoMap);

        mockery.checking(new Expectations() {
            {
                one(authnResult).getCustomProperties();
                will(returnValue(customProperties));
                one(clientConfig).getClockSkewInSeconds();
                will(returnValue(300L));
            }
        });

        boolean result = cacheHelper.isTokenInCachedResultExpired(authnResult, clientConfig);
        assertTrue("A token expiration time of 0 should have been considered as an expired token.", result);
    }

    @Test
    public void test_isTokenInCachedResultExpired_expLarge() {
        long expValue = System.currentTimeMillis();
        final Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        Map<String, Object> tokenInfoMap = new HashMap<String, Object>();
        tokenInfoMap.put("exp", expValue);
        customProperties.put(Constants.ACCESS_TOKEN_INFO, tokenInfoMap);

        mockery.checking(new Expectations() {
            {
                one(authnResult).getCustomProperties();
                will(returnValue(customProperties));
                one(clientConfig).getClockSkewInSeconds();
                will(returnValue(300L));
            }
        });

        boolean result = cacheHelper.isTokenInCachedResultExpired(authnResult, clientConfig);
        assertFalse("A token expiration time of " + expValue + " compared to current time should not have been considered as an expired token.", result);
    }

    @Test
    public void test_getTokenExpirationFromCustomProperties_nullCustomProperties() {
        long result = cacheHelper.getTokenExpirationFromCustomProperties(null);
        assertEquals("Did not get expected expiration time from the custom properties.", 0, result);
    }

    @Test
    public void test_getTokenExpirationFromCustomProperties_emptyCustomProperties() {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        long result = cacheHelper.getTokenExpirationFromCustomProperties(customProperties);
        assertEquals("Did not get expected expiration time from the custom properties.", 0, result);
    }

    @Test
    public void test_getTokenExpirationFromCustomProperties_missingTokenInfo() {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        customProperties.put("one", 1);
        customProperties.put("two", "two");

        long result = cacheHelper.getTokenExpirationFromCustomProperties(customProperties);
        assertEquals("Did not get expected expiration time from the custom properties.", 0, result);
    }

    @Test
    public void test_getTokenExpirationFromCustomProperties_tokenInfoEntryWrongType() {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        customProperties.put(Constants.ACCESS_TOKEN_INFO, "wrong type");

        long result = cacheHelper.getTokenExpirationFromCustomProperties(customProperties);
        assertEquals("Did not get expected expiration time from the custom properties.", 0, result);
    }

    @Test
    public void test_getTokenExpirationFromCustomProperties_tokenInfoEmpty() {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        Map<String, Object> tokenInfoMap = new HashMap<String, Object>();
        customProperties.put(Constants.ACCESS_TOKEN_INFO, tokenInfoMap);

        long result = cacheHelper.getTokenExpirationFromCustomProperties(customProperties);
        assertEquals("Did not get expected expiration time from the custom properties.", 0, result);
    }

    @Test
    public void test_getTokenExpirationFromCustomProperties_tokenInfoMissingExp() {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        Map<String, Object> tokenInfoMap = new HashMap<String, Object>();
        tokenInfoMap.put("one", "one");
        customProperties.put(Constants.ACCESS_TOKEN_INFO, tokenInfoMap);

        long result = cacheHelper.getTokenExpirationFromCustomProperties(customProperties);
        assertEquals("Did not get expected expiration time from the custom properties.", 0, result);
    }

    @Test
    public void test_getTokenExpirationFromCustomProperties_expWrongType() {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        Map<String, Object> tokenInfoMap = new HashMap<String, Object>();
        tokenInfoMap.put("exp", "wrong type");
        customProperties.put(Constants.ACCESS_TOKEN_INFO, tokenInfoMap);

        long result = cacheHelper.getTokenExpirationFromCustomProperties(customProperties);
        assertEquals("Did not get expected expiration time from the custom properties.", 0, result);
    }

    @Test
    public void test_getTokenExpirationFromCustomProperties_expNegative() {
        long expValue = -42;
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        Map<String, Object> tokenInfoMap = new HashMap<String, Object>();
        tokenInfoMap.put("exp", expValue);
        customProperties.put(Constants.ACCESS_TOKEN_INFO, tokenInfoMap);

        long result = cacheHelper.getTokenExpirationFromCustomProperties(customProperties);
        assertEquals("Did not get expected expiration time from the custom properties.", expValue, result);
    }

    @Test
    public void test_getTokenExpirationFromCustomProperties_expNormal() {
        long expValue = 300;
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        Map<String, Object> tokenInfoMap = new HashMap<String, Object>();
        tokenInfoMap.put("exp", expValue);
        customProperties.put(Constants.ACCESS_TOKEN_INFO, tokenInfoMap);

        long result = cacheHelper.getTokenExpirationFromCustomProperties(customProperties);
        assertEquals("Did not get expected expiration time from the custom properties.", expValue, result);
    }

    private SingleTableCache getCache() {
        return new SingleTableCache(1000 * 60);
    }

    private ProviderAuthenticationResult createProviderAuthenticationResult(long expTime) {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        Map<String, Object> accessTokenInfo = new HashMap<String, Object>();
        accessTokenInfo.put("exp", expTime);
        customProperties.put(Constants.ACCESS_TOKEN_INFO, accessTokenInfo);
        ProviderAuthenticationResult result = new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK, GOOD_USER, new Subject(), customProperties, HTTPS_URL);
        return result;
    }

}