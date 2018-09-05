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


import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtToken;


/**
 * This API represents the user's access_token and profile upon user authenticated by social media.
 *
 * @author IBM Corporation
 *
 * @version 1.0
 *
 * @since 1.0
 *
 * @ibm-api
 */
public class UserProfile {

    // TODO: Should this be a clone instead of the actual Subject's JwtToken?
    private final JwtToken jwtToken;
    private final Map<String, Object> customProperties;
    private final Set<Claims> claimSet;
    private final String userInfo;  // only present in oidc clients

    public UserProfile(JwtToken jwtToken, Map<String, Object> customProperties, Claims claims) {
        this(jwtToken, customProperties, claims, null);
    }    
    
    public UserProfile(JwtToken jwtToken, Map<String, Object> customProperties, Claims claims, String userInfo){
        this.jwtToken = jwtToken;
        this.customProperties = customProperties;
        claimSet = new HashSet<Claims>();
        claimSet.add(claims);
        this.userInfo = userInfo;
    }

    /**
     * @return user data from social media.
     */
    public Set<Claims> getClaimSet() {
        Set<Claims> claimSetToReturn = new HashSet<Claims>();
        claimSetToReturn.addAll(claimSet);
        return claimSetToReturn;
    }

    /**
     * @return user data from social media.
     */
    public Claims getClaims() {
        // TODO: return Claims clone to avoid modification.
        Iterator<Claims> it = claimSet.iterator();
        if (it.hasNext()) {
            return it.next();
        }
        return null;
    }

    /**
     * @return access token used for user API call.
     */
    public String getAccessToken() {
        return (String) customProperties.get("access_token");
    }

    /**
     * @return refresh token.
     */
    public String getRefreshToken() {
        return (String) customProperties.get("refresh_token");
    }

    /**
     * @return access_token life time.
     */
    public long getAccessTokenLifeTime() {
        Long expiresIn = (Long) customProperties.get("expires_in");
        if (expiresIn == null) {
            return 0;
        }
        return expiresIn;
    }

    /**
     * @return social media name.
     */
    public String getSocialMediaName() {
        return (String) customProperties.get("social_media");
    }

    /**
     * @return authorized scope.
     */
    public String getScopes() {
        return (String) customProperties.get("scope");
    }

    /**
     * @return IdToken as JWT.
     */
    public JwtToken getIdToken() {
        String id_token = (String) customProperties.get("id_token");
        if (id_token != null && !id_token.trim().isEmpty()) {
            return jwtToken; // The jwtToken was built from the id_token if there was one.
        }
        return null;
    }

    /**
     * @return encrypted access token.
     */
    public String getEncryptedAccessToken() {
        return (String) customProperties.get("encrypted_token");
    }

    /**
     * @return access token alias that can resolve access_token.
     */
    public String getAccessTokenAlias() {
        return (String) customProperties.get("accessTokenAlias");
    }
    
    /**
     * return userInfo information from an OpenIdConnect provider's userInfo
     * endpoint for the authenticated user.    
     *
     * @return the userInfo JSON as a string or null if the info is not available. 
     */
    public String getUserInfo()  {
        return userInfo;
    }
}
