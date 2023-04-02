/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package userinfo.servlets;

import java.util.List;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;

import io.openliberty.security.jakartasec.fat.utils.Constants;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.servlet.annotation.WebServlet;

/**
 *
 * Mock userinfo servlet created to test OpenIdAuthenticationMechanismDefinition scope and scopeExpression.
 * This servlet takes the scope claim from the access token and returns userinfo claims based on the scopes available.
 * Only a subset of profile scope claims are added for brevity.
 *
 * The tests can be found in io.openliberty.security.jakartasec.fat.config.tests.ConfigurationScopeTests.java
 *
 */
@WebServlet("/JsonUserInfoScopeServlet")
public class JsonUserInfoScopeServlet extends JsonUserInfoServlet {

    private static final long serialVersionUID = -145839375682343L;

    @Override
    protected JsonObject getMinimumClaims(String accessToken) {
        JsonObjectBuilder claims = Json.createObjectBuilder();
        try {
            List<String> scopes = getScopes(accessToken);
            if (scopes.contains(Constants.PROFILE_SCOPE)) {
                claims.add(OpenIdConstant.NAME, "Test User");
                claims.add(OpenIdConstant.PREFERRED_USERNAME, "testuser");
                claims.add(OpenIdConstant.BIRTHDATE, "01-01-1970");
                claims.add(OpenIdConstant.LOCALE, "en-CA");
            }
            if (scopes.contains(Constants.EMAIL_SCOPE)) {
                claims.add(OpenIdConstant.EMAIL, "test@test.er");
                claims.add(OpenIdConstant.EMAIL_VERIFIED, "true");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return claims.build();
    }

    private List<String> getScopes(String accessToken) throws InvalidJwtException, MalformedClaimException {
        JwtConsumer jwtConsumer = new JwtConsumerBuilder().setSkipAllValidators().setDisableRequireSignature().setSkipSignatureVerification().build();
        JwtContext jwtContext = jwtConsumer.process(accessToken);
        JwtClaims jwtClaims = jwtContext.getJwtClaims();
        return jwtClaims.getStringListClaimValue(OpenIdConstant.SCOPE);
    }

}
