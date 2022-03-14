/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.backchannellogout;

import java.util.ArrayList;
import java.util.List;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.JwtContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.openidconnect.client.jose4j.util.Jose4jUtil;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OIDCClientAuthenticatorUtil;
import com.ibm.ws.security.openidconnect.jose4j.Jose4jValidator;
import com.ibm.wsspi.ssl.SSLSupport;

@Component(name = "com.ibm.ws.security.openidconnect.backchannellogout.LogoutTokenValidator", service = {}, property = { "service.vendor=IBM" })
public class LogoutTokenValidator {

    private static TraceComponent tc = Tr.register(LogoutTokenValidator.class);

    private static SSLSupport SSL_SUPPORT = null;

    private ConvergedClientConfig config = null;
    private Jose4jUtil jose4jUtil = null;

    @Reference
    protected void setSslSupport(SSLSupport sslSupport) {
        SSL_SUPPORT = sslSupport;
    }

    protected void unsetSslSupport() {
        SSL_SUPPORT = null;
    }

    /**
     * Do not use; needed for this to be a valid @Component object.
     */
    public LogoutTokenValidator() {
    }

    public LogoutTokenValidator(ConvergedClientConfig config) {
        this.config = config;
        jose4jUtil = new Jose4jUtil(SSL_SUPPORT);
    }

    /**
     * Valides an OIDC back-channel logout token per https://openid.net/specs/openid-connect-backchannel-1_0.html.
     */
    @FFDCIgnore(Exception.class)
    public JwtToken validateToken(String logoutTokenString) throws BackchannelLogoutException {
        try {
            JwtContext jwtContext = jose4jUtil.validateJwtStructureAndGetContext(logoutTokenString, config);
            JwtClaims claims = jose4jUtil.validateJwsSignature(jwtContext, config);

            verifyAllRequiredClaimsArePresent(claims);

            Jose4jValidator validator = getJose4jValidator();
            // 3. Validate the iss, aud, and iat Claims in the same way they are validated in ID Tokens
            validator.verifyIssForIdToken(claims.getIssuer());
            validator.verifyAudForIdToken(claims.getAudience());
            validator.verifyIatAndExpClaims(claims);
            // TODO;
            // 4. Verify that the Logout Token contains a sub Claim, a sid Claim, or both.
            // 5. Verify that the Logout Token contains an events Claim whose value is JSON object containing the member name http://schemas.openid.net/event/backchannel-logout.
            // 6. Verify that the Logout Token does not contain a nonce Claim.
            // 7. Optionally verify that another Logout Token with the same jti value has not been recently received.
            // 8. Optionally verify that the iss Logout Token Claim matches the iss Claim in an ID Token issued for the current session or a recent session of this RP with the OP.
            // 9. Optionally verify that any sub Logout Token Claim matches the sub Claim in an ID Token issued for the current session or a recent session of this RP with the OP.
            // 10. Optionally verify that any sid Logout Token Claim matches the sid Claim in an ID Token issued for the current session or a recent session of this RP with the OP.

        } catch (Exception e) {
            String errorMsg = Tr.formatMessage(tc, "BACKCHANNEL_LOGOUT_TOKEN_ERROR", new Object[] { e });
            throw new BackchannelLogoutException(errorMsg, e);
        }
        return null;
    }

    void verifyAllRequiredClaimsArePresent(JwtClaims claims) throws MalformedClaimException, BackchannelLogoutException {
        List<String> missingClaims = new ArrayList<>();
        String iss = claims.getIssuer();
        if (iss == null) {
            missingClaims.add(Claims.ISSUER);
        }
        List<String> aud = claims.getAudience();
        if (aud == null || aud.isEmpty()) {
            missingClaims.add(Claims.AUDIENCE);
        }
        NumericDate iat = claims.getIssuedAt();
        if (iat == null) {
            missingClaims.add(Claims.ISSUED_AT);
        }
        String jti = claims.getJwtId();
        if (jti == null) {
            missingClaims.add(Claims.ID);
        }
        Object events = claims.getClaimValue("events");
        if (events == null) {
            missingClaims.add("events");
        }
        if (!missingClaims.isEmpty()) {
            String errorMsg = Tr.formatMessage(tc, "LOGOUT_TOKEN_MISSING_CLAIMS", new Object[] { missingClaims });
            throw new BackchannelLogoutException(errorMsg);
        }
    }

    Jose4jValidator getJose4jValidator() {
        return new Jose4jValidator(null, config.getClockSkewInSeconds(), new OIDCClientAuthenticatorUtil().getIssuerIdentifier(config), config.getClientId(), config.getSignatureAlgorithm(), null);
    }

}
