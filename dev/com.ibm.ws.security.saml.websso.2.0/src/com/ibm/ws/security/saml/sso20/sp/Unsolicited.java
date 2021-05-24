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
package com.ibm.ws.security.saml.sso20.sp;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.internal.utils.ForwardRequestInfo;
import com.ibm.ws.security.saml.sso20.internal.utils.HttpRequestInfo;
import com.ibm.ws.security.saml.sso20.internal.utils.InitialRequestUtil;
import com.ibm.ws.security.saml.sso20.internal.utils.RequestUtil;
import com.ibm.ws.security.saml.sso20.internal.utils.SamlUtil;
import com.ibm.wsspi.security.tai.TAIResult;

/**
 * http://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf
 * section 3.4
 */
public class Unsolicited {
    public static final TraceComponent tc = Tr.register(Unsolicited.class,
                                                        TraceConstants.TRACE_GROUP,
                                                        TraceConstants.MESSAGE_BUNDLE);

    SsoSamlService ssoService = null;
    InitialRequestUtil irUtil = new InitialRequestUtil();
    /**
     * @param service
     */
    public Unsolicited(SsoSamlService service) {
        ssoService = service;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Unsolicited(" + service.getProviderId() + ")");
        }
    }

    /**
     * @param req
     * @param resp
     * @return
     * @throws WebTrustAssociationFailedException
     * @throws SamlException
     */
    public TAIResult sendRequestToLoginPageUrl(HttpServletRequest req,
                                               HttpServletResponse resp,
                                               String loginPageUrl)
                    throws WebTrustAssociationFailedException, SamlException {
        String decodedLoginPageUrl;
        try {
            decodedLoginPageUrl = URLDecoder.decode(loginPageUrl, Constants.UTF8);
        } catch (UnsupportedEncodingException e) {
            throw new SamlException(e); // let SamlException handle this unexpected error
        }
        String targetId = SamlUtil.generateRandom(); // no need to Base64 encode
        HttpRequestInfo cachingRequestInfo = new HttpRequestInfo(req);
        RequestUtil.cacheRequestInfo(targetId, ssoService, cachingRequestInfo);
        irUtil.handleSerializingInitialRequest(req, resp, Constants.IDP_INITAL, targetId, cachingRequestInfo, ssoService);
        TAIResult result = redirectToUserDefinedLoginPageURL(req, resp, targetId, decodedLoginPageUrl, cachingRequestInfo);
        return result;
    }



    /**
     * @param req
     * @param resp
     * @param targetId
     * @param idpUrl
     * @return
     */
    TAIResult redirectToUserDefinedLoginPageURL(HttpServletRequest req,
                                                HttpServletResponse resp,
                                                String targetId,
                                                String idpUrl,
                                                HttpRequestInfo cachingRequestInfo)
                    throws WebTrustAssociationFailedException {
        String target = Constants.IDP_INITAL + targetId; // Target

        try {
            resp.setStatus(javax.servlet.http.HttpServletResponse.SC_OK);
            // May need to handle the encoding of user defined login page url 
            // such as: 
            // https://ws-rhel4-7.austin.ibm.com:9443/sps/FvtIdp1Fed/saml20/logininitial
            //     ?RequestBinding=HTTPPost
            //      &PartnerId=https%3A%2F%2Fnc135008.tivlab.austin.ibm.com%3A9443%2Fsamlsps%2Facs
            //      &Target=https%3A%2F%2Fnc135008.tivlab.austin.ibm.com%3A9443%2Ffimivt%2Fprotected%2Fivtlanding.jsp
            //      &NameIdFormat=email
            // or https://localhost:8020/samlclient/idpClient.jsp

            ForwardRequestInfo requestInfo = new ForwardRequestInfo(idpUrl, "");
            requestInfo.setFragmentCookieId(cachingRequestInfo.getFragmentCookieId());
            requestInfo.redirectGetRequest(req,
                                           resp,
                                           Constants.COOKIE_WAS_REQUEST,
                                           target,
                                           true);
        } catch (SamlException e) {
            WebTrustAssociationFailedException wtafe = new WebTrustAssociationFailedException(e.getMessage());
            wtafe.initCause(e);
            throw wtafe;
        }

        // expect to return a form to redirect to the idp by the browser
        // due to admin center intercepting 403, send 401.
        //return TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
        return TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED); 
    }
}
