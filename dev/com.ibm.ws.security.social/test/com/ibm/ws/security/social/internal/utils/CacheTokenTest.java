/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
