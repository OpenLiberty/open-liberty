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

import org.junit.Before;
import org.junit.Test;

public class CacheTokenTest {
    
    CacheToken cacheToken;
    private static final String ACCESS_TOKEN = "12345";
    private static final String SOCIAL_LOGIN_CONFIGURATION_ID = "123";
    private static final String ID_TOKEN = "123.456.789";

    @Before
    public void setUp() throws Exception {
        cacheToken = new CacheToken(ACCESS_TOKEN, SOCIAL_LOGIN_CONFIGURATION_ID);
        cacheToken.setIdToken(ID_TOKEN);
    }

    @Test
    public void test() {
        assertEquals("The access token must be set in the cache token.", ACCESS_TOKEN, cacheToken.getAccessToken());
        assertEquals("The social login configuration id must be set in the cache token.", SOCIAL_LOGIN_CONFIGURATION_ID, cacheToken.getSocialLoginConfigurationId());
        assertEquals("The IdToken must be set in the cache token.", ID_TOKEN, cacheToken.getIdToken());
    }

}
