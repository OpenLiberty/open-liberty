/*
 * IBM Confidential
 * 
 * OCO Source Materials
 * 
 * Copyright IBM Corp. 2017
 * 
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.security.social.internal.utils;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TokenCacheTest {

    private Cache cache;

    @Before
    public void setUp() throws Exception {
        cache = new Cache(50000, 600000L); 
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void putByAccessToken() {
        String accessToken = "12345";
        String socialLoginConfigurationId = "123";
        CacheToken cacheToken = new CacheToken(accessToken, socialLoginConfigurationId);
        cache.put(accessToken, cacheToken);
        CacheToken tokenFromCache = (CacheToken) cache.get(accessToken);
        assertSame("The token must be cached.", cacheToken, tokenFromCache);
    }

}
