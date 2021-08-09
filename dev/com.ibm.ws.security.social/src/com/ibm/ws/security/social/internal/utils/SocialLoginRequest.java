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
package com.ibm.ws.security.social.internal.utils;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.web.RequestFilter;

/*
 * Store the data for a httpServletRequest session
 *
 * Initialize when a session starts and
 * discard after it ends
 */
public class SocialLoginRequest {
    private static TraceComponent tc = Tr.register(SocialLoginRequest.class,
            TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);
    // social login URL.
    protected SocialLoginConfig socialLoginConfig = null;
    protected String providerName; // providerId

    String socialUrlType = null;

    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected List<SocialLoginConfig> socialLoginConfigs = null;

    // Called by the RequestFilter (socialLogin filter)
    public SocialLoginRequest(SocialLoginConfig socialLoginConfig, String socialUrlType, HttpServletRequest request, HttpServletResponse response) {
        this.socialLoginConfig = socialLoginConfig;
        this.providerName = socialLoginConfig.getUniqueId();
        this.request = request;
        this.response = response;
        this.socialUrlType = socialUrlType;
    }

    // Called by the RequestFilter (socialLogin filter)
    public SocialLoginRequest(String socialUrlType, HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
        this.socialUrlType = socialUrlType;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "request type:" + socialUrlType);
            Tr.debug(tc, "request url:" + getRequestUrl());
        }
    }

    public String getProviderName() {
        return providerName;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * @return
     */
    public SocialLoginConfig getSocialLoginConfig() {
        return socialLoginConfig;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SocialLoginRequest [provider:").append(this.providerName).append(" request:").append(this.request).append("]");
        return sb.toString();
    }

    public String getRequestUrl() {
        java.lang.StringBuffer sb = request.getRequestURL();
        return sb.toString();
    }

    public String getUrlType() {
        return socialUrlType;
    }

    public boolean isLogout() {
        return socialUrlType.equals(RequestFilter.LOGOUT);
    }

    public boolean isWellknownConfig() {
        return socialUrlType.equals(RequestFilter.WELLKNOWN_CONFIG);
    }

    public boolean isUnknown() {
        return socialUrlType.equals(RequestFilter.UNKNOWN);
    }

    /**
     * @return
     */
    public boolean isRedirect() {
        return socialUrlType.equals(RequestFilter.REDIRECT);
    }
}
