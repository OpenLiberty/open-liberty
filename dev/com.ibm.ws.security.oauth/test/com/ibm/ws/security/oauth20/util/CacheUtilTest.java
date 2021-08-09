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
package com.ibm.ws.security.oauth20.util;

import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.token.impl.OAuth20AuthorizationGrantRefreshImpl;
import com.ibm.oauth.core.test.base.BaseCache;
import com.ibm.oauth.core.test.base.BaseConfig;
import com.ibm.oauth.core.test.base.BaseTestCase;
import com.ibm.ws.security.oauth20.plugins.OAuth20BearerTokenImpl;
import com.ibm.ws.security.oauth20.plugins.OAuth20TokenImpl;

/**
 *
 */
public class CacheUtilTest extends BaseTestCase {

    public void testGetRefreshTokenFromAccessToken() {
        BaseConfig config = new BaseConfig();

        initializeOAuthFramework(config);

        String username = "testuser";
        String clientId = "key";
        String redirectUri = "http://localhost:9080/oauth/client.jsp";

        String[] scope = new String[] { "scope1", "scope2" };

        String stateId = "uniqueStateId";

        BaseCache cache = new BaseCache();
        cache.init(config);
        OAuth20Token refreshtoken = new OAuth20AuthorizationGrantRefreshImpl(
                "refreshtokenString",
                "componentId",
                clientId, username, redirectUri, "state", scope,
                Integer.parseInt("160"), null);
        cache.add(refreshtoken.getId(), refreshtoken, refreshtoken.getLifetimeSeconds());
        OAuth20Token accesstoken = new OAuth20BearerTokenImpl("accesstokenString", "componentId",
                clientId, username, redirectUri, "state", stateId, scope,
                Integer.parseInt("60"), null, OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE);
        ((OAuth20TokenImpl) accesstoken).setRefreshTokenKey(refreshtoken.getId());
        cache.add(accesstoken.getId(), accesstoken, accesstoken.getLifetimeSeconds());
        CacheUtil cacheUtil = new CacheUtil(cache);
        OAuth20Token token = cacheUtil.getRefreshToken(accesstoken);
        String refreshId = cacheUtil.getRefreshTokenId(accesstoken);
        assertEquals("refreshId should be refreshtokenString", refreshId, refreshtoken.getId());
        assertEquals("refresh token id and id of the refreshtoken from accesstoken should match", refreshtoken.getId(), token.getId());

    }

    // This test belongs in the OIDC server code
    // public void testGetRefreshTokenFromIDToken() {
    // BaseConfig config = new BaseConfig();
    //
    // initializeOAuthFramework(config);
    //
    // String username = "testuser";
    // String clientId = "key";
    // String redirectUri = "http://localhost:9080/oauth/client.jsp";
    //
    // String stateId = "uniqueStateId";
    //
    // String[] scope = new String[] { "scope1", "scope2" };
    //
    // BaseCache cache = new BaseCache();
    // cache.init(config);
    // OAuth20Token refreshtoken = new OAuth20AuthorizationGrantRefreshImpl(
    // "refreshtokenString",
    // "componentId",
    // clientId, username, redirectUri, "state", scope,
    // Integer.parseInt("160"), null);
    // cache.add(refreshtoken.getId(), refreshtoken, refreshtoken.getLifetimeSeconds());
    // OAuth20Token accesstoken = new OAuth20BearerTokenImpl("accesstokenString", "componentId",
    // clientId, username, redirectUri, "state", stateId, scope,
    // Integer.parseInt("60"), null, OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE);
    // ((OAuth20TokenImpl) accesstoken).setRefreshTokenKey(refreshtoken.getId());
    // cache.add(accesstoken.getId(), accesstoken, accesstoken.getLifetimeSeconds());
    // OAuth20Token idtoken = new IDTokenImpl("idtokenkey", "idTokenString", "componentId",
    // clientId, username, redirectUri, "state", scope,
    // Integer.parseInt("60"), null, OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE);
    // ((OAuth20TokenImpl) idtoken).setAccessTokenKey(accesstoken.getId());
    // cache.add(idtoken.getId(), idtoken, idtoken.getLifetimeSeconds());
    // CacheUtil cacheUtil = new CacheUtil(cache);
    // OAuth20Token token = cacheUtil.getRefreshToken(idtoken);
    // String accessId = cacheUtil.getAccessTokenId(idtoken);
    // assertEquals("accessId should be accesstokenString", accessId, accesstoken.getId());
    // assertEquals("refresh token id and id of the refreshtoken from idtoken should match", refreshtoken.getId(), token.getId());
    //
    // }
}
