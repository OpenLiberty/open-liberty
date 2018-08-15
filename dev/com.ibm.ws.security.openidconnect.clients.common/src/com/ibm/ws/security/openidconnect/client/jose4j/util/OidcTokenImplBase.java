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
package com.ibm.ws.security.openidconnect.client.jose4j.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jose4j.jwt.JwtClaims;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
// import com.ibm.websphere.security.openidconnect.token.IdToken;
import com.ibm.ws.security.openidconnect.clients.common.TraceConstants;
import com.ibm.ws.security.openidconnect.common.Constants;

public class OidcTokenImplBase {
    private static final TraceComponent tc = Tr.register(OidcTokenImplBase.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    protected static final String CLIENT_ID = "azp2";

    private static final String BEARER = "Bearer";
    JwtClaims jwtClaims = null; // this is not serializable
    String access_token = null;
    String refresh_token = null;
    String client_id = null;
    String tokenTypeNoSpace = null;

    public OidcTokenImplBase(JwtClaims jwtClaims, String access_token, String refresh_token, String client_id, String tokenTypeNoSpace) {
        this.jwtClaims = jwtClaims;
        this.access_token = access_token;
        this.refresh_token = refresh_token;
        this.client_id = client_id;
        if (tokenTypeNoSpace == null) {
            tokenTypeNoSpace = Constants.TOKEN_TYPE_ID_TOKEN;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Provided token type is null; defaulting to " + tokenTypeNoSpace);
            }
        }
        this.tokenTypeNoSpace = tokenTypeNoSpace;
    }

    public String getTokenTypeNoSpace() {
        return this.tokenTypeNoSpace;
    }

    public JwtClaims getJwtClaims() {
        return this.jwtClaims;
    }

    public String getJwtId() {
        if (this.jwtClaims == null) {
            return null;
        }
        try {
            return this.jwtClaims.getJwtId();
        } catch (Exception e) {
            return null;
        }
    }

    public String getType() {
        return BEARER;
    }

    public String getIssuer() {
        if (this.jwtClaims == null) {
            return null;
        }
        try {
            return this.jwtClaims.getIssuer();
        } catch (Exception e) {
            return null;
        }
    }

    public String getSubject() {
        if (this.jwtClaims == null) {
            return null;
        }
        try {
            return this.jwtClaims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> getAudience() {
        if (this.jwtClaims == null) {
            return null;
        }
        try {
            return this.jwtClaims.getAudience();
        } catch (Exception e) {
            return null;
        }
    }

    public String getClientId() {
        return this.client_id;
    }

    public long getExpirationTimeSeconds() {
        if (this.jwtClaims == null) {
            return 0;
        }
        try {
            return this.jwtClaims.getExpirationTime().getValue();
        } catch (Exception e) {
            return 0;
        }
    }

    public long getNotBeforeTimeSeconds() {
        if (this.jwtClaims == null) {
            return 0;
        }
        try {
            return this.jwtClaims.getNotBefore().getValue();
        } catch (Exception e) {
            return 0;
        }
    }

    public long getIssuedAtTimeSeconds() {
        if (this.jwtClaims == null) {
            return 0;
        }
        try {
            return this.jwtClaims.getIssuedAt().getValue();
        } catch (Exception e) {
            return 0;
        }
    }

    public long getAuthorizationTimeSeconds() {
        return 0;
    }

    public String getNonce() {
        if (this.jwtClaims == null) {
            return null;
        }
        try {
            return this.jwtClaims.getStringClaimValue("nonce");
        } catch (Exception e) {
            return null;
        }
    }

    public String getAccessTokenHash() {
        if (this.jwtClaims == null) {
            return null;
        }
        try {
            return this.jwtClaims.getStringClaimValue("at_hash");
        } catch (Exception e) {
            return null;
        }
    }

    public String getClassReference() {
        return null;
    }

    public List<String> getMethodsReferences() {
        return null;
    }

    public String getAuthorizedParty() {
        return null;
    }

    public Object getClaim(String key) {
        if (this.jwtClaims == null) {
            return null;
        }
        try {
            return this.jwtClaims.getClaimValue(key);
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Object> getAllClaims() {
        if (this.jwtClaims == null) {
            return null;
        }
        return new HashMap<String, Object>(this.jwtClaims.getClaimsMap());
    }

    public String getAccessToken() {
        return this.access_token;
    }

    public String getRefreshToken() {
        return this.refresh_token;
    }

    public String getAllClaimsAsJson() {
        try {
            if (this.jwtClaims == null) {
                return null;
            }
            return this.jwtClaims.toJson();
        } catch (Exception e) {
            return null;
        }
    }

    //
    // Example: Private Credential: IDToken:{iss=https://localhost:8999/oidc/endpoint/OidcConfigSample, sub=testuser, aud=client01, exp=1391718615, iat=1391715015, at_hash=Xdplqpld3TOjqA0FSf7zqw}
    //

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(tokenTypeNoSpace);

        String claimsString = getAllClaimsAsJson();
        if (claimsString == null) {
            return sb.toString();
        }
        sb.append(":").append(claimsString);
        return sb.toString();
    }

}
