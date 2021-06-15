/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.ws.security.common.web.WebSSOUtils;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.sp.Solicited;
import com.ibm.ws.security.saml.sso20.sp.Unsolicited;
import com.ibm.wsspi.security.tai.TAIResult;

/**
 * 
 */
public class Initiator {
    public static final TraceComponent tc = Tr.register(Initiator.class,
                                                        TraceConstants.TRACE_GROUP,
                                                        TraceConstants.MESSAGE_BUNDLE);

    SsoSamlService ssoService = null;
    WebSSOUtils webssoUtils = new WebSSOUtils();

    /**
     * @param service
     */
    public Initiator(SsoSamlService service) {
        ssoService = service;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Solicited(" + service.getProviderId() + ")");
        }
    }

    /**
     * When LoginPageURL is set in the SP configuration,
     * the SAML requests are processed by the LoginPageURL. (not common cases)
     * 
     * Otherwise, we create the AuthnRequest along with the SAMLRequests.
     * (This is common.)
     * 
     * @param req
     * @param resp
     * @return
     * @throws WebTrustAssociationFailedException
     * @throws SamlException
     */
    public TAIResult forwardRequestToSamlIdp(HttpServletRequest req, HttpServletResponse resp) throws WebTrustAssociationFailedException, SamlException {
        SsoConfig samlConfig = ssoService.getConfig();

        boolean createSession = samlConfig.createSession();
        if (createSession) {
            try {
                req.getSession(true);
            } catch (Exception e) {
                //ignore it. Session exists
            }
        }
        //webssoUtils.savePostParameters(req);

        String loginPageUrl = samlConfig.getLoginPageURL();
        if (loginPageUrl != null && !loginPageUrl.isEmpty()) {
            // If loginPageURL, then it's SP_unsolicited
            // The rest of process is very similar to idp_initiated
            Unsolicited spUnsolicited = new Unsolicited(ssoService);
            return spUnsolicited.sendRequestToLoginPageUrl(req, resp, loginPageUrl);
        } else {
            // If no loginPageURL, then it's SP_solicited
            // AuthnRequest is requred in this case
            Solicited spSolicited = new Solicited(ssoService);
            return spSolicited.sendAuthRequestToIdp(req, resp);
        }
    }

}
