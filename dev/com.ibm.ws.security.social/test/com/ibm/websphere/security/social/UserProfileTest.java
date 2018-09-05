/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.social;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.social.internal.utils.ClientConstants;

public class UserProfileTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private static final String ACCESS_TOKEN = "12345";
    private static final String REFRESH_TOKEN = "67890";
    private static final long EXPIRES_IN = 5177064;
    private static final String SOCIAL_MEDIA_NAME = "facebookLogin";
    private static final String SCOPE = "email user_friends public_profile user_about_me";
    private static final String ENCRYPTED_TOKEN = "54321";
    private static final String ACCESS_TOKEN_ALIAS = "ABCDE";
    private static final String ID_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2xvY2FsaG9zdDo4MDIwL29pZGMvZW5kcG9pbnQvT1AiLCJub25jZSI6InYxemc1T1o5dlhQNWgwbEVpWXMxIiwiaWF0IjoxNDU1OTAxODU4LCJzdWIiOiJ1c2VyMSIsImV4cCI6MTQ1NTkwOTA1OCwiYXVkIjoicnAiLCJyZWFsbU5hbWUiOiJPcEJhc2ljUmVhbG0iLCJ1bmlxdWVTZWN1cml0eU5hbWUiOiJ1c2VyMSIsImF0X2hhc2giOiIwSGJ6aFc0OWJoRVAyYjNTVkhmZUdnIn0.VJNknPRe0BhzfMA4MpQIEeVczaHYiMzPiBYejp72zIs";

    private UserProfile userProfile;
    private JwtToken jwtToken;
    private JwtToken jwtAccessToken;
    private Claims jwtClaimsMap;
    private Map<String, Object> customProperties;
    private static final String USERINFO = 
    "{\"sub\":\"testuser\",\"iss\":\"https:\\/\\/localhost:29443\\/oidc\\/endpoint\\/OP\\\",\"name\":\"testuser\"}";

    @Before
    public void setUp() {
        jwtToken = mockery.mock(JwtToken.class, "jwtToken");
        jwtAccessToken = mockery.mock(JwtToken.class, "jwtAccessToken");
        jwtClaimsMap = mockery.mock(Claims.class, "jwtClaimsMap");
        setupUserProfile(jwtToken);
    }

    private void setupUserProfile(final JwtToken jwtToken) {
        final Claims jwtClaims = mockery.mock(Claims.class);
        mockery.checking(new Expectations() {
            {
                allowing(jwtToken).getClaims();
                will(returnValue(jwtClaims));
            }
        });

        customProperties = new Hashtable<String, Object>();
        customProperties.put(ClientConstants.ACCESS_TOKEN, ACCESS_TOKEN);
        customProperties.put(ClientConstants.REFRESH_TOKEN, REFRESH_TOKEN);
        customProperties.put("expires_in", EXPIRES_IN);
        customProperties.put("social_media", SOCIAL_MEDIA_NAME);
        customProperties.put(ClientConstants.SCOPE, SCOPE);
        customProperties.put("encrypted_token", ENCRYPTED_TOKEN);
        customProperties.put("accessTokenAlias", ACCESS_TOKEN_ALIAS);
        userProfile = new UserProfile(jwtToken, customProperties, jwtClaims, USERINFO);
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
    }

    @Test
    public void getClaimSet() {
        Set<Claims> claimSet = userProfile.getClaimSet();

        // There is currently one Claims object
        assertEquals("The claim set in the UserProfile must contain the claims from the JwtToken.", claimSet.iterator().next(), jwtToken.getClaims());
    }

    @Test
    public void getClaims() {
        Claims claims = userProfile.getClaims();

        assertEquals("The claims in the UserProfile must be the same as the claims in the Subject's JwtToken credential.", jwtToken.getClaims(), claims);
        assertNull("There must not be an IdToken as JWT in the UserProfile.", userProfile.getIdToken());
    }

    @Test
    public void getAccessToken() {
        assertEquals("The access_token must be set in the UserProfile.", ACCESS_TOKEN, userProfile.getAccessToken());
    }

    @Test
    public void getRefreshToken() {
        assertEquals("The refresh_token must be set in the UserProfile.", REFRESH_TOKEN, userProfile.getRefreshToken());
    }

    @Test
    public void getAccessTokenLifeTime() {
        assertEquals("The access_token lifetime must be set in the UserProfile.", EXPIRES_IN, userProfile.getAccessTokenLifeTime());
    }

    @Test
    public void getSocialMediaName() {
        assertEquals("The social media name must be set in the UserProfile.", SOCIAL_MEDIA_NAME, userProfile.getSocialMediaName());
    }
    
    @Test
    public void getScopes() {
        assertEquals("The scope must be set in the UserProfile.", SCOPE, userProfile.getScopes());
    }
    
//    @Test
//    public void getJwtAccessToken() {
//        assertEquals("The access token as JWT must be set in the UserProfile.", jwtAccessToken, userProfile.getJwtAccessToken());
//    }
    
    @Test
    public void getIdToken() {
        customProperties.put(ClientConstants.ID_TOKEN, ID_TOKEN);
        userProfile = new UserProfile(jwtToken, customProperties, jwtClaimsMap);
        assertEquals("The IdToken as JWT must be set in the UserProfile.", jwtToken, userProfile.getIdToken());
    }

    @Test
    public void getEncryptedAccessToken() {
        assertEquals("The encrypted access token must be set in the UserProfile.", ENCRYPTED_TOKEN, userProfile.getEncryptedAccessToken());
    }
    
    @Test
    public void getAccessTokenAlias() {
        assertEquals("The access token alias must be set in the UserProfile.", ACCESS_TOKEN_ALIAS, userProfile.getAccessTokenAlias());
    }
    
    @Test
    public void getUserInfo(){
        assertEquals("userinfo must match what was used in constructor.", USERINFO, userProfile.getUserInfo());
    }
    
}
