/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.client.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.security.Principal;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

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
import com.ibm.ws.security.openidconnect.client.jose4j.OidcTokenImpl;
import com.ibm.ws.security.openidconnect.client.jose4j.util.OidcTokenImplBase;
import com.ibm.ws.security.openidconnect.clients.common.Constants;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.wsspi.security.token.AttributeNameConstants;

import test.common.SharedOutputManager;

public class AccessTokenCacheHelperTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.openidconnect*=all");
    @Rule
    public TestRule managerRule = outputMgr;

    private final OidcClientConfig clientConfig = mockery.mock(OidcClientConfig.class);
    private final ProviderAuthenticationResult authnResult = mockery.mock(ProviderAuthenticationResult.class);
    private final Principal principal = mockery.mock(Principal.class);

    private static final String CONFIG_ID = "myOidcConfigId";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String HTTPS_URL = "https://localhost:8020/root";
    private static final String GOOD_USER = "user";

    private final AccessTokenCacheHelper cacheHelper = new AccessTokenCacheHelper();

    @BeforeClass
    public static void setUpBeforeClass() {
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        outputMgr.trace("*=all=disabled");
        outputMgr.restoreStreams();
    }

    @Before
    public void setUp() {
        System.out.println("Entering test: " + testName.getMethodName());
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getId();
                will(returnValue(CONFIG_ID));
            }
        });
    }

    @After
    public void tearDown() {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_getCachedTokenAuthenticationResult_cacheDisabled() {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getAccessTokenCacheEnabled();
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
                one(clientConfig).getAccessTokenCacheEnabled();
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
        AccessTokenCacheKey cacheKey = cacheHelper.getCacheKey("someToken", CONFIG_ID);
        cache.put(cacheKey, new AccessTokenCacheValue("unqiue id", cachedResult));
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getAccessTokenCacheEnabled();
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
        AccessTokenCacheKey cacheKey = cacheHelper.getCacheKey(ACCESS_TOKEN, CONFIG_ID);
        cache.put(cacheKey, new AccessTokenCacheValue("unique id", cachedResult));
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getAccessTokenCacheEnabled();
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
    public void test_getCachedTokenAuthenticationResult_insertsUniqueIDIfMissing() {
        SingleTableCache cache = getCache();
        String uniqueID = "unique id";
        ProviderAuthenticationResult cachedResult = createProviderAuthenticationResult(System.currentTimeMillis());
        AccessTokenCacheKey cacheKey = cacheHelper.getCacheKey(ACCESS_TOKEN, CONFIG_ID);
        cache.put(cacheKey, new AccessTokenCacheValue(uniqueID, cachedResult));
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getAccessTokenCacheEnabled();
                will(returnValue(true));
                one(clientConfig).getCache();
                will(returnValue(cache));
                one(clientConfig).getClockSkewInSeconds();
                will(returnValue(300L));
            }
        });

        ProviderAuthenticationResult result = cacheHelper.getCachedTokenAuthenticationResult(clientConfig, ACCESS_TOKEN);
        String cachedUniqueID = (String) result.getCustomProperties().get(AttributeNameConstants.WSCREDENTIAL_UNIQUEID);
        assertNotNull("Unique ID should have been inserted into cached result.", cachedUniqueID);
        assertEquals("Cached unique ID did not match expected unique ID.", uniqueID, cachedUniqueID);
    }

    @Test
    public void test_cacheTokenAuthenticationResult_emptyCache() {
        SingleTableCache cache = getCache();

        cacheEnabledExpectations(cache);

        ProviderAuthenticationResult resultToCache = new ProviderAuthenticationResult(AuthResult.OAUTH_CHALLENGE, HttpServletResponse.SC_EXPECTATION_FAILED);
        cacheHelper.cacheTokenAuthenticationResult(clientConfig, ACCESS_TOKEN, resultToCache);

        AccessTokenCacheKey cacheKey = cacheHelper.getCacheKey(ACCESS_TOKEN, CONFIG_ID);

        AccessTokenCacheValue cacheEntry = (AccessTokenCacheValue) cache.get(cacheKey);
        assertNotNull("Cache entry should not have been null but was.", cacheEntry);

        ProviderAuthenticationResult cachedResult = cacheEntry.getResult();
        assertNotNull("Result should not have been null but was.", cachedResult);
        assertEquals("Cached result did not match expected object.", resultToCache.getStatus(), cachedResult.getStatus());
        assertEquals("Cached result did not match expected object.", resultToCache.getHttpStatusCode(), cachedResult.getHttpStatusCode());
    }

    @Test
    public void test_cacheTokenAuthenticationResult_nonEmptyCache_newResult() {
        SingleTableCache cache = getCache();
        // Create an existing cache entry for a different key
        ProviderAuthenticationResult cachedResult = new ProviderAuthenticationResult(AuthResult.RETURN, HttpServletResponse.SC_CONFLICT);
        AccessTokenCacheKey cacheKey = cacheHelper.getCacheKey("otherEntry", CONFIG_ID);
        cache.put(cacheKey, new AccessTokenCacheValue("unique id", cachedResult));

        cacheEnabledExpectations(cache);

        ProviderAuthenticationResult resultToCache = new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK);
        cacheHelper.cacheTokenAuthenticationResult(clientConfig, ACCESS_TOKEN, resultToCache);

        cacheKey = cacheHelper.getCacheKey(ACCESS_TOKEN, CONFIG_ID);

        AccessTokenCacheValue cacheEntry = (AccessTokenCacheValue) cache.get(cacheKey);
        assertNotNull("Cache entry should not have been null but was.", cacheEntry);

        ProviderAuthenticationResult newCachedResult = cacheEntry.getResult();
        assertNotNull("Result should not have been null but was.", newCachedResult);
        assertEquals("Cached result did not match expected object.", resultToCache.getStatus(), newCachedResult.getStatus());
        assertEquals("Cached result did not match expected object.", resultToCache.getHttpStatusCode(), newCachedResult.getHttpStatusCode());
    }

    @Test
    public void test_cacheTokenAuthenticationResult_entryAlreadyExists() {
        SingleTableCache cache = getCache();
        ProviderAuthenticationResult cachedResult = new ProviderAuthenticationResult(AuthResult.RETURN, HttpServletResponse.SC_CONFLICT);
        AccessTokenCacheKey cacheKey = cacheHelper.getCacheKey(ACCESS_TOKEN, CONFIG_ID);
        cache.put(cacheKey, new AccessTokenCacheValue("unique id", cachedResult));

        cacheEnabledExpectations(cache);

        ProviderAuthenticationResult resultToCache = new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK);
        cacheHelper.cacheTokenAuthenticationResult(clientConfig, ACCESS_TOKEN, resultToCache);

        AccessTokenCacheValue cacheEntry = (AccessTokenCacheValue) cache.get(cacheKey);
        assertNotNull("Cache entry should not have been null but was.", cacheEntry);

        ProviderAuthenticationResult newCachedResult = cacheEntry.getResult();
        assertNotNull("Result should not have been null but was.", newCachedResult);
        assertEquals("Cached result did not match expected object.", resultToCache.getStatus(), newCachedResult.getStatus());
        assertEquals("Cached result did not match expected object.", resultToCache.getHttpStatusCode(), newCachedResult.getHttpStatusCode());
    }

    @Test
    public void test_cacheTokenAuthenticationResult_cachesUniqueID() {
        SingleTableCache cache = getCache();

        cacheEnabledExpectations(cache);

        String uniqueID = "unique id";
        ProviderAuthenticationResult resultToCache = createProviderAuthenticationResult(System.currentTimeMillis());
        resultToCache.getCustomProperties().put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, uniqueID);
        cacheHelper.cacheTokenAuthenticationResult(clientConfig, ACCESS_TOKEN, resultToCache);

        AccessTokenCacheKey cacheKey = cacheHelper.getCacheKey(ACCESS_TOKEN, CONFIG_ID);

        AccessTokenCacheValue cacheEntry = (AccessTokenCacheValue) cache.get(cacheKey);
        assertNotNull("Cache entry should not have been null but was.", cacheEntry);
        assertEquals("Unique ID in cache entry should have matched unique ID in custom properties.", uniqueID, cacheEntry.getUniqueID());
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
    public void test_isTokenInCachedResultExpired_missingExp() {
        final Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        Map<String, Object> tokenInfoMap = new HashMap<String, Object>();
        tokenInfoMap.put("one", 1);
        tokenInfoMap.put("two", "two");
        customProperties.put(Constants.ACCESS_TOKEN_INFO, tokenInfoMap);

        mockery.checking(new Expectations() {
            {
                one(authnResult).getCustomProperties();
                will(returnValue(customProperties));
            }
        });

        boolean result = cacheHelper.isTokenInCachedResultExpired(authnResult, clientConfig);
        assertFalse("A token without an expiration time should not have been considered as an expired token.", result);
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
    public void test_doesPropertyExistInAccessTokenInfo_nullCustomProperties() {
        boolean result = cacheHelper.doesPropertyExistInAccessTokenInfo("exp", null);
        assertFalse("Custom property should not exist when the properties set is null.", result);
    }

    @Test
    public void test_doesPropertyExistInAccessTokenInfo_emptyCustomProperties() {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        boolean result = cacheHelper.doesPropertyExistInAccessTokenInfo("exp", customProperties);
        assertFalse("Custom property should not exist when the properties set is empty.", result);
    }

    @Test
    public void test_doesPropertyExistInAccessTokenInfo_missingTokenInfo() {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        customProperties.put("one", 1);
        customProperties.put("two", "two");

        boolean result = cacheHelper.doesPropertyExistInAccessTokenInfo("exp", customProperties);
        assertFalse("Custom property should not exist when the properties set is null.", result);
    }

    @Test
    public void test_doesPropertyExistInAccessTokenInfo_tokenInfoEntryWrongType() {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        customProperties.put(Constants.ACCESS_TOKEN_INFO, "wrong type");

        boolean result = cacheHelper.doesPropertyExistInAccessTokenInfo("exp", customProperties);
        assertFalse("Custom property should not exist when the properties set does not contain access token info.", result);
    }

    @Test
    public void test_doesPropertyExistInAccessTokenInfo_tokenInfoEmpty() {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        Map<String, Object> tokenInfoMap = new HashMap<String, Object>();
        customProperties.put(Constants.ACCESS_TOKEN_INFO, tokenInfoMap);

        boolean result = cacheHelper.doesPropertyExistInAccessTokenInfo("exp", customProperties);
        assertFalse("Custom property should not exist when the access token info is empty.", result);
    }

    @Test
    public void test_doesPropertyExistInAccessTokenInfo_tokenInfoMissingProp() {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        Map<String, Object> tokenInfoMap = new HashMap<String, Object>();
        tokenInfoMap.put("one", "one");
        customProperties.put(Constants.ACCESS_TOKEN_INFO, tokenInfoMap);

        boolean result = cacheHelper.doesPropertyExistInAccessTokenInfo("exp", customProperties);
        assertFalse("Custom property should not exist when its key does not exist in access token info.", result);
    }

    @Test
    public void test_doesPropertyExistInAccessTokenInfo_propNull() {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        Map<String, Object> tokenInfoMap = new HashMap<String, Object>();
        tokenInfoMap.put("exp", null);
        customProperties.put(Constants.ACCESS_TOKEN_INFO, tokenInfoMap);

        boolean result = cacheHelper.doesPropertyExistInAccessTokenInfo("exp", customProperties);
        assertFalse("Custom property should not exist when its value is null.", result);
    }

    @Test
    public void test_doesPropertyExistInAccessTokenInfo_propExists() {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        Map<String, Object> tokenInfoMap = new HashMap<String, Object>();
        tokenInfoMap.put("exp", 300);
        customProperties.put(Constants.ACCESS_TOKEN_INFO, tokenInfoMap);

        boolean result = cacheHelper.doesPropertyExistInAccessTokenInfo("exp", customProperties);
        assertTrue("Custom property should exist when it is found in the access token info and its value is not null.", result);
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
    public void test_getTokenExpirationFromCustomProperties_expNull() {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        Map<String, Object> tokenInfoMap = new HashMap<String, Object>();
        tokenInfoMap.put("exp", null);
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

    @Test
    public void test_recreateSubject_cachedSubjectNull() {
        Subject result = cacheHelper.recreateSubject(null);
        assertEquals("Null cached subject should return a new subject.", new Subject(), result);
    }

    @Test
    public void test_recreateSubject_cachedSubjectEmpty() {
        Subject cachedSubject = new Subject();

        Subject result = cacheHelper.recreateSubject(cachedSubject);
        assertEquals("Empty cached subject should return a new subject.", new Subject(), result);
    }

    @Test
    public void test_recreateSubject_cachedSubjectWithOidcTokenImpl() {
        Subject cachedSubject = new Subject();
        Set<Object> creds = cachedSubject.getPrivateCredentials();
        OidcTokenImplBase oidcTokenBase = new OidcTokenImplBase(null, null, null, null, null);
        OidcTokenImpl oidcToken = new OidcTokenImpl(oidcTokenBase);
        creds.add(oidcToken);

        Subject result = cacheHelper.recreateSubject(cachedSubject);
        assertFalse("OidcTokenImpl in cached subject should be copied into new subject.", result.getPrivateCredentials(OidcTokenImpl.class).isEmpty());
    }

    @Test
    public void test_recreateSubject_cachedSubjectWithHashtable() {
        Subject cachedSubject = new Subject();
        Set<Object> creds = cachedSubject.getPrivateCredentials();
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        customProperties.put("one", "one");
        customProperties.put("two", "two");
        creds.add(customProperties);

        Subject result = cacheHelper.recreateSubject(cachedSubject);
        assertFalse("Hashtable in cached subject should be copied into new subject.", result.getPrivateCredentials(Hashtable.class).isEmpty());
    }

    @Test
    public void test_recreateSubject_cachedSubjectWithOidcTokenImplAndHashtable() {
        Subject cachedSubject = new Subject();
        Set<Object> creds = cachedSubject.getPrivateCredentials();
        OidcTokenImplBase oidcTokenBase = new OidcTokenImplBase(null, null, null, null, null);
        OidcTokenImpl oidcToken = new OidcTokenImpl(oidcTokenBase);
        creds.add(oidcToken);
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        customProperties.put("one", "one");
        customProperties.put("two", "two");
        creds.add(customProperties);

        Subject result = cacheHelper.recreateSubject(cachedSubject);
        boolean oidcTokenImplEmpty = result.getPrivateCredentials(OidcTokenImpl.class).isEmpty();
        boolean hashtableEmpty = result.getPrivateCredentials(Hashtable.class).isEmpty();
        assertFalse("OidcTokenImpl and Hashtable in cached subject should be copied into new subject.", oidcTokenImplEmpty || hashtableEmpty);
    }

    @Test
    public void test_recreateSubject_cachedSubjectWithWrongCredType() {
        Subject cachedSubject = new Subject();
        Set<Object> creds = cachedSubject.getPrivateCredentials();
        creds.add("one");

        Subject result = cacheHelper.recreateSubject(cachedSubject);
        assertTrue("Only OidcTokenImpl's and Hashtables should be copied into new subject.", result.getPrivateCredentials(String.class).isEmpty());
    }

    @Test
    public void test_recreateSubject_cachedSubjectWithOidcTokenImplBase() {
        Subject cachedSubject = new Subject();
        Set<Object> creds = cachedSubject.getPrivateCredentials();
        OidcTokenImplBase oidcTokenBase = new OidcTokenImplBase(null, null, null, null, null);
        creds.add(oidcTokenBase);

        Subject result = cacheHelper.recreateSubject(cachedSubject);
        assertTrue("OidcTokenImplBase should not be copied into new subject.", result.getPrivateCredentials(OidcTokenImplBase.class).isEmpty());
    }

    @Test
    public void test_recreateSubject_cachedSubjectWithPrincipal() {
        Subject cachedSubject = new Subject();
        cachedSubject.getPrincipals().add(principal);

        Subject result = cacheHelper.recreateSubject(cachedSubject);
        assertTrue("Cached principal should not be copied into new subject.", result.getPrincipals().isEmpty());
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

    private void cacheEnabledExpectations(SingleTableCache cache) {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getAccessTokenCacheEnabled();
                will(returnValue(true));
                one(clientConfig).getCache();
                will(returnValue(cache));
                one(clientConfig).getClockSkew();
                will(returnValue(0L));
            }
        });
    }

}