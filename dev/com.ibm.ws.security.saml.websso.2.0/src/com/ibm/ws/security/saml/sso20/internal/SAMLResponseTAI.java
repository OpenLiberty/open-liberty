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

import java.util.Map;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WebTrustAssociationException;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.internal.utils.HttpRequestInfo;
import com.ibm.ws.security.saml.sso20.internal.utils.RequestUtil;
import com.ibm.ws.security.saml.sso20.internal.utils.UserData;
import com.ibm.ws.security.saml.sso20.rs.SamlInboundService;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

public class SAMLResponseTAI extends SAMLRequestTAI {
    public static final TraceComponent tc = Tr.register(SAMLResponseTAI.class,
                                                        TraceConstants.TRACE_GROUP,
                                                        TraceConstants.MESSAGE_BUNDLE);

    static ConcurrentServiceReferenceMap<String, SsoSamlService> respSsoSamlServiceRef = new ConcurrentServiceReferenceMap<String, SsoSamlService>(KEY_SSO_SAML_SERVICE);
    static SAMLRequestTAI activatedRequestTAI = null;
    static SamlInboundService activatedSamlInboundService = null;

    static void setActivatedRequestTai(SAMLRequestTAI activatedRequestTai) {
        activatedRequestTAI = activatedRequestTai;
    }

    public static void setActivatedInboundService(SamlInboundService inboundService) {
        activatedSamlInboundService = inboundService;
        SAMLRequestTAI.setActivatedInboundService(inboundService);
    }

    static void setTheActivatedSsoSamlServiceRef(ConcurrentServiceReferenceMap<String, SsoSamlService> activatedSsoSamlServiceRef) {
        respSsoSamlServiceRef = activatedSsoSamlServiceRef;
    }

    @Override
    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {

    }

    @Override
    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> props) {
        // do nothing for now. SAMLRequestTAI handles SsoSamlServices and AuthnFilter
    }

    @Override
    @Deactivate
    protected void deactivate(ComponentContext cc) {

    }

    /*
     *
     */
    @Override
    public TAIResult negotiateValidateandEstablishTrust(HttpServletRequest req, HttpServletResponse resp) throws WebTrustAssociationFailedException {
        TAIResult taiResult = handleErrorIfAnyAlready(req, resp);
        if (taiResult != null)
            return taiResult;
        String providerId = null;
        // It needs to have an acs cookie or the SP disableLtpaCookie
        SsoRequest samlRequest = (SsoRequest) req.getAttribute(Constants.ATTRIBUTE_SAML20_REQUEST);
        SsoSamlService service = samlRequest.getSsoSamlService();
        if (service.isInboundPropagation()) {
            // very tiny change and can-be-ignore change that the acrivatedSamlInboundService is null
            return activatedSamlInboundService.negotiateValidateandEstablishTrust(req, resp, service);
        } else {
            try {
                HttpRequestInfo.restoreSavedParametersIfAny(req, resp, samlRequest);
            } catch (SamlException e) {
                throw new WebTrustAssociationFailedException(e.getMessage());
            }
            UserData userData = samlRequest.getUserData();
            providerId = samlRequest.getProviderName();
            if (userData != null) { // came from acs cookie. see handleWithCookie()
                Authenticator samlAuthenticator = new Authenticator(service, userData);
                return samlAuthenticator.authenticate(req, resp);
            }
            // handling 174880 SpCookie if disableLtpaCookie
            if (samlRequest.isDisableLtpaCookie()) {
                // find a valid spCookie and get its subject, otherwise call SAMLRequestTAI directly
                SpCookieRetriver spCookieRetriver = new SpCookieRetriver(authCacheServiceRef.getService(), req, samlRequest);
                Subject subject = spCookieRetriver.getSubjectFromSpCookie();
                boolean bSubjectValid = validateSubject(subject, req, resp, samlRequest);

                if (!bSubjectValid) {
                    // remove the subject from cache since it's not valid
                    spCookieRetriver.removeSubject();
                    // either the sp cookie is not created or sp cookie expires
                    // let's call SAMLRequestTAI to bypass the SSO/LTPA Cookie handling 174880
                    samlRequest.setType(Constants.EndpointType.REQUEST);
                    if (activatedRequestTAI != null) {
                        return activatedRequestTAI.negotiateValidateandEstablishTrust(req, resp);
                    } else {
                        // activatedRequestTAI is null...
                        // This is unlikely to happen, unless dynamic updates got troubles
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "There is no activatedRequestTAI available!! (" + providerId + ")");
                        }
                    }
                } else {
                    // let's create the subject again 174880
                    return TAIResult.create(HttpServletResponse.SC_OK,
                                            RequestUtil.getUserName(subject),
                                            subject);
                }
            }
        }

        // This should not happen, unless dynamic got troubles
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "The SP service is not available or can not handle the request!!" + providerId);
        }
        //Tr.error(tc, "SAML20_NO_SP_FOUND_INTERNAL_ERROR", cookieProviderId);
        String errMsg = SamlException.formatMessage("SAML20_AUTHENTICATION_FAIL",
                                                    "CWWKS5063E: SAML Exception: The SAML service provider (SP) failed to process the authentication request.",
                                                    new Object[] { providerId });
        throw new WebTrustAssociationFailedException(errMsg);
    }

    /*
     */
    @Override
    public boolean isTargetInterceptor(HttpServletRequest request) throws WebTrustAssociationException {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "isTargetInterceptor()");
        }
        if (RequestUtil.isUnprotectedUrlForSaml(request))
            return false;

        // Let's get the qualified SP first (174880)
        if (findSpSpecificFirst(request, respSsoSamlServiceRef, Constants.EndpointType.RESPONSE)) {
            IExtendedRequest req = (IExtendedRequest) request;
            SsoRequest samlRequest = (SsoRequest) req.getAttribute(Constants.ATTRIBUTE_SAML20_REQUEST);
            if (samlRequest != null) {
                if (samlRequest.isInboundPropagation()) {
                    // rsSaml does not need to check the cookie
                    return true;
                } else {
                    // samlWebSso20
                    samlRequest.setLocationAdminRef(locationAdminRef); // locationAdminRef is for SAMLResponseTAI
                    // check if AcsCookie exist
                    // Set the request attribute
                    if (handledWithCookie(req, samlRequest)) {
                        return true;
                    }
                    if (samlRequest.isDisableLtpaCookie()) {
                        // find a valid spCookie or call SAMLRequestTAI directly
                        return true;
                    }
                }
            }
        } else {
            Object exception = request.getAttribute(Constants.SAML_SAMLEXCEPTION_FOUND);
            if (exception != null)
                return true; // need to do error handling later
        }

        // No acs cookie and using ltpa cookie(disableLtpaCookie=false)
        return false;
    }

    boolean handledWithCookie(IExtendedRequest req, SsoRequest samlRequest) {
        if (RequestUtil.isUnprocessedAcsCookiePresent(respSsoSamlServiceRef, req, samlRequest)) {
            // Let check the if the acs cookie is in our cache
            String spProviderId = samlRequest.getProviderName();
            Cache cache = RequestUtil.getAcsCookieCacheForProvider(respSsoSamlServiceRef, spProviderId);
            String acsCookieValue = RequestUtil.getAcsCookieValueFromRequest(req, spProviderId);
            UserData userData = (UserData) cache.get(acsCookieValue);
            if (userData != null) {
                samlRequest.setUserData(userData);
                cache.remove(acsCookieValue); // the acs cookie can only be used once
                return true; // handle the request when it has the cached userData
            } else {
                return false;
            }
        }
        return false;
    }

}
