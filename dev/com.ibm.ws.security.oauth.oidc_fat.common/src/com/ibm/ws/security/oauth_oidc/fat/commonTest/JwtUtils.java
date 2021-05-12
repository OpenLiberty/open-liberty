/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;

public class JwtUtils {

    public static final long OLD_IAT = 1467300000L;
    public static final long OLD_EXP = 1467307200L;
    public static final String DEFAULT_AT_HASH = "MKPUyhph8IuIc5raqfxs8Q";

    /**
     * Creates and returns a valid JWT string with the "iss" and "exp" claims set to times well in the past.
     */
    public String buildOldJwtString(TestSettings settings) throws Exception {
        return buildOldJwtString(settings, Constants.HEADER_DEFAULT_KEY_ID);
    }

    /**
     * Creates and returns a valid JWT string with the "iss" and "exp" claims set to times well in the past.
     */
    public String buildOldJwtString(TestSettings settings, String kid) throws Exception {
        if (kid == null) {
            kid = Constants.HEADER_DEFAULT_KEY_ID;
        }
        JWTToken jsonToken = new JWTToken(settings.getIssuer(), kid, settings);
        jsonToken.setPayloadProp("iat", OLD_IAT);
        jsonToken.setPayloadProp("exp", OLD_EXP);
        jsonToken.setPayloadProp("sub", settings.getAdminUser());
        jsonToken.setPayloadProp("realmName", settings.getRealm());
        jsonToken.setPayloadProp("uniqueSecurityName", settings.getAdminUser());
        jsonToken.setPayloadProp("at_hash", DEFAULT_AT_HASH);
        return jsonToken.getJWTTokenString();
    }

    /**
     * Invokes the JWKS URI endpoint specified in the TestSettings object in order to obtain a valid "kid" claim that refers to
     * one of the JWKs that the OP has generated. This allows us, for instance, to build an old JWT and give it a kid that matches
     * a valid JWK, which means the RP will find a matching key when it is performing token validation.
     */
    public String getValidJwksKid(TestSettings settings) throws Exception {
        String method = "getValidJwksKid";
        if (Constants.X509_CERT.equals(settings.getRsCertType())) {
            Log.info(getClass(), method, "The test is not using JWK as the cert type, so returning null as the JWK kid");
            return null;
        }
        String jwksString = getJwksStringFromOp(settings);
        Log.info(getClass(), method, "Got JWKs: " + jwksString);
        JsonObject jwks = Json.createReader(new StringReader(jwksString)).readObject();
        JsonArray jwkArray = jwks.getJsonArray("keys");
        JsonObject firstJwk = jwkArray.getJsonObject(0);
        String firstJwkKid = firstJwk.getString("kid");
        Log.info(getClass(), method, "Returning the \"kid\" value from the first JWK in the returned keys array: " + firstJwkKid);
        return firstJwkKid;
    }

    private String getJwksStringFromOp(TestSettings settings) throws Exception {
        WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        Page jwkEndpointResponse = webClient.getPage(settings.getJwkEndpt());
        webClient.close();
        if (jwkEndpointResponse == null) {
            throw new Exception("Failed to obtain JWKs from the JWK endpoint of the OP; the returned content was null.");
        }
        return jwkEndpointResponse.getWebResponse().getContentAsString();
    }

}
