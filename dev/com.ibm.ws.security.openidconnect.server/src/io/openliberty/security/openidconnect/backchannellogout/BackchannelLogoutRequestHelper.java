/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.openidconnect.backchannellogout;

import org.jose4j.jwt.consumer.JwtContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.openidconnect.client.jose4j.util.Jose4jUtil;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;
import com.ibm.wsspi.ssl.SSLSupport;

@Component
public class BackchannelLogoutRequestHelper {

    private static SSLSupport SSL_SUPPORT = null;

    private OAuth20Provider oauth20provider;
    private OidcServerConfig oidcServerConfig;

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
    public BackchannelLogoutRequestHelper() {
    }

    public BackchannelLogoutRequestHelper(OAuth20Provider oauth20provider, OidcServerConfig oidcServerConfig) {
        this.oauth20provider = oauth20provider;
        this.oidcServerConfig = oidcServerConfig;
        jose4jUtil = new Jose4jUtil(SSL_SUPPORT);
    }

    /**
     * Uses the provided ID token string to build a Logout Token and sends that Logout Token in back-channel logout requests to
     * all of the necessary RPs.
     *
     * @param idTokenString
     */
    public void sendBackchannelLogoutRequests(String idTokenString) {
        try {
            JwtContext jwtContext = Jose4jUtil.parseJwtWithoutValidation(idTokenString);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
        // TODO
//        JwtContext jwtContext = jose4jUtil.validateJwtStructureAndGetContext(idTokenString, config);
//        JwtClaims claims = jose4jUtil.validateJwsSignature(jwtContext, config);
    }

}
