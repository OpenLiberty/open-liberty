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

package com.ibm.ws.security.saml;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.saml.Constants.EndpointType;
import com.ibm.ws.security.saml.sso20.internal.utils.RequestUtil;
import com.ibm.ws.security.saml.sso20.internal.utils.UserData;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/*
 * Store the data for a httpServletRequest session
 *
 * Initialize when a session starts and
 * discard after it ends
 */
public class SsoRequest {

    // Called by the RequestFilter (SAML filter)
    public SsoRequest(String providerName, EndpointType type, HttpServletRequest request, Constants.SamlSsoVersion samlVersion) {
        this.providerName = providerName;
        this.type = type;
        this.request = request;
        this.samlVersion = samlVersion;
    }

    // Called when we found an SsoSamlService
    public SsoRequest(String providerName, EndpointType type, HttpServletRequest request, Constants.SamlSsoVersion samlVersion, SsoSamlService ssoSamlService) {
        this(providerName, type, request, samlVersion);
        setSsoSamlService(ssoSamlService);
    }

    protected Constants.EndpointType type;

    protected String providerName; // providerId
    protected HttpServletRequest request;
    protected Constants.SamlSsoVersion samlVersion;

    protected SsoSamlService ssoSamlService;
    protected SsoConfig ssoConfig;
    protected UserData userData = null;
    protected AtomicServiceReference<WsLocationAdmin> locationAdminRef; // in use for the SpCookie name
    @Sensitive
    protected String spCookieValue = null; // in use for the SpCookie

    boolean inboundPropagation = false;

    /**
     * @return the inboundPropagation
     */
    public boolean isInboundPropagation() {
        return inboundPropagation;
    }

    /**
     * @param inboundPropagation the inboundPropagation to set
     */
    public void setInboundPropagation(boolean inboundPropagation) {
        this.inboundPropagation = inboundPropagation;
    }

    /**
     * @return the userData
     */
    public UserData getUserData() {
        return userData;
    }

    /**
     * @param userData the userData to set
     */
    public void setUserData(UserData userData) {
        this.userData = userData;
    }

    public Constants.EndpointType getType() {
        return type;
    }

    public String getProviderName() {
        return providerName;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public Constants.SamlSsoVersion getSamlVersion() {
        return samlVersion;
    }

    public SsoSamlService getSsoSamlService() {
        return ssoSamlService;
    }

    public SsoConfig getSsoConfig() {
        return ssoConfig;
    }

    public void setSsoSamlService(SsoSamlService ssoSamlService) {
        this.ssoSamlService = ssoSamlService;
        this.ssoConfig = ssoSamlService.getConfig();
        inboundPropagation = ssoSamlService.isInboundPropagation();
    }

    /**
     * @param resp
     */
    public void createSpCookieIfDisableLtpa(HttpServletRequest req, HttpServletResponse resp) {
        if (isDisableLtpaCookie()) {
            String spCookieName = getSpCookieName();
            String spCookieValue = getSpCookieValue();
            if (spCookieName != null && spCookieValue != null) {
                RequestUtil.createCookie(req,
                                         resp,
                                         spCookieName,
                                         spCookieValue);
            }
        }
    }

    /**
     * @param type the type to set
     */
    public void setType(Constants.EndpointType type) {
        this.type = type;
    }

    /**
     * @return the spCookieValue
     */
    @Sensitive
    public String getSpCookieValue() {
        return spCookieValue;
    }

    /**
     * @param spCookieValue the spCookieValue to set
     */
    public void setSpCookieValue(@Sensitive String spCookieValue) {
        this.spCookieValue = spCookieValue;
    }

    /**
     * @param locationAdminRef the locationAdminRef to set
     */
    public void setLocationAdminRef(AtomicServiceReference<WsLocationAdmin> locationAdminRef) {
        this.locationAdminRef = locationAdminRef;
    }

    public String getSpCookieName() {
        WsLocationAdmin locationAdmin = locationAdminRef.getService();
        return ssoConfig.getSpCookieName(locationAdmin);
    }

    public boolean isDisableLtpaCookie() {
        return ssoConfig.isDisableLtpaCookie();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SamlRequest [provider:").append(this.providerName).append(" type:").append(this.type).append(" request:").append(this.request).append("]").append(" userData="
                                                                                                                                                                     + (userData != null));
        return sb.toString();
    }

}
