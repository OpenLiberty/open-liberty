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
package com.ibm.ws.security.saml.sso20.slo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.web.CommonWebConstants;
import com.ibm.ws.security.common.web.WebUtils;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl;

public class SLOPostLogoutHandler {

    private static TraceComponent tc = Tr.register(SLOPostLogoutHandler.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public static final String PARAM_LOGOUT_STATUS = "logout_status";

    private HttpServletRequest request = null;
    private SsoConfig config = null;
    private BasicMessageContext<?, ?> messageContext = null;
    private SLOMessageContextUtils msgContextUtils = null;

    public SLOPostLogoutHandler(HttpServletRequest request, SsoConfig config, BasicMessageContext<?, ?> msgCtx) {
        this.request = request;
        this.config = config;
        this.messageContext = msgCtx;
        this.msgContextUtils = new SLOMessageContextUtils(messageContext);
    }

    public void sendToPostLogoutPage(HttpServletResponse response) throws IOException {
        if (isValidPostLogoutRedirectUrlConfigured()) {
            redirectToCustomPostLogoutPage(response);
        } else {
            generateDefaultPostLogoutPage(response);
        }
    }

    boolean isValidPostLogoutRedirectUrlConfigured() {
        String postLogoutUrl = getAndValidatePostLogoutRedirectUrl();
        return (postLogoutUrl != null);
    }

    void redirectToCustomPostLogoutPage(HttpServletResponse response) throws IOException {
        String postLogoutUrl = getAndValidatePostLogoutRedirectUrl();
        if (postLogoutUrl == null) {
            Tr.debug(tc, "Somehow the redirect URL [{0}] is no longer valid, so will redirect to default post logout page", postLogoutUrl);
            generateDefaultPostLogoutPage(response);
            return;
        }
        response.sendRedirect(postLogoutUrl + "?" + getCustomPostLogoutQueryString());
    }

    String getCustomPostLogoutQueryString() {
        String query = PARAM_LOGOUT_STATUS + "=";
        try {
            String statusCode = getStatusCodeForQueryString();
            query += URLEncoder.encode(statusCode, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Do nothing - UTF-8 will be supported
        }
        return query;
    }

    String getStatusCodeForQueryString() {
        String statusCode = msgContextUtils.getSloStatusCode();
        if (statusCode == null || statusCode.isEmpty()) {
            statusCode = SLOMessageContextUtils.STATUS_UNKNOWN;
        }
        return statusCode;
    }

    String getAndValidatePostLogoutRedirectUrl() {
        String postLogoutUrl = config.getPostLogoutRedirectUrl();
        if (postLogoutUrl != null) {
            // logout url specified in saml config trumps formLogoutExitPage
            if (WebUtils.validateUriFormat(postLogoutUrl, CommonWebConstants.VALID_URI_PATH_CHARS + "+")) {
                return postLogoutUrl;
            } else {
                Tr.error(tc, "SAML20_POST_LOGOUT_URL_NOT_VALID", new Object[] { postLogoutUrl, SsoConfigImpl.KEY_postLogoutRedirectUrl, config.getProviderId() });
            }
        }
        // if FormLogoutExtensionProcessor was in play and had a logout page for us to use, retrieve it now.
        postLogoutUrl = null;
        String formLogoutExitPage = messageContext.getCachedRequestInfo().getFormLogoutExitPage();
        if (formLogoutExitPage != null) {
            postLogoutUrl = formLogoutExitPage;
        }
        return postLogoutUrl;
    }

    void generateDefaultPostLogoutPage(HttpServletResponse response) throws IOException {
        SLOPostLogoutPageBuilder pageBuilder = getPostLogoutPageBuilder();
        pageBuilder.writeDefaultLogoutPage(response);
    }

    SLOPostLogoutPageBuilder getPostLogoutPageBuilder() {
        return new SLOPostLogoutPageBuilder(request, messageContext);
    }

}
