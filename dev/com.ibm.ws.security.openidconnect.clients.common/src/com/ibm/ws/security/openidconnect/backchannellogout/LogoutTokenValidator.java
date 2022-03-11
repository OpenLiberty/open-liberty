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

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.openidconnect.client.jose4j.util.Jose4jUtil;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
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
            // TODO;
            // 3. Validate the iss, aud, and iat Claims in the same way they are validated in ID Tokens
            // 4. Verify that the Logout Token contains a sub Claim, a sid Claim, or both.
            // 5. Verify that the Logout Token contains an events Claim whose value is JSON object containing the member name http://schemas.openid.net/event/backchannel-logout.
            // 6. Verify that the Logout Token does not contain a nonce Claim.

        } catch (Exception e) {
            String errorMsg = Tr.formatMessage(tc, "BACKCHANNEL_LOGOUT_TOKEN_ERROR", new Object[] { e });
            throw new BackchannelLogoutException(errorMsg, e);
        }
        return null;
    }

}
