/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal.utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.webcontainer.internalRuntimeExport.srt.IPrivateRequestAttributes;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 * Utility class to generate hash code
 */
public class RequestUtil {
    private static final TraceComponent tc = Tr.register(RequestUtil.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    static public final String KEY_SOCIAL_LOGIN_CONFIG = "socialLoginConfig";
    static ConcurrentServiceReferenceMap<String, SocialLoginConfig> socialLoginConfigRef = new ConcurrentServiceReferenceMap<String, SocialLoginConfig>(KEY_SOCIAL_LOGIN_CONFIG);

    public static void setSocialLoginConfigRef(ConcurrentServiceReferenceMap<String, SocialLoginConfig> socialLoginConfigRef) {
        RequestUtil.socialLoginConfigRef = socialLoginConfigRef;
    }

    // Method for unit testing.
    public static SocialLoginConfig getSocialLoginConfig(String key) {
        return RequestUtil.socialLoginConfigRef.getService(key);
    }

    public static String getCtxRootUrl(HttpServletRequest req, String samlCtxPath) {
        // TODO this will need to handle the redirect host and sp due to the firewall or proxy issue
        String hostName = req.getServerName();
        Integer httpsPort = getRedirectPortFromRequest(req);
        if (httpsPort == null && req.isSecure()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The redirect SSL port is null from request. Trying to get http port");
            }
            int port = req.getServerPort();
            String httpSchema = ((javax.servlet.ServletRequest) req).getScheme();
            // return whatever in the req
            return httpSchema + "://" + hostName + (port > 0 && port != 443 ? ":" + port : "") + samlCtxPath;
        } else {
            return "https://" + hostName + (httpsPort == null ? "" : ":" + httpsPort) + samlCtxPath;
        }
    }

    static protected Integer getRedirectPortFromRequest(HttpServletRequest req) {
        HttpServletRequest sr = getWrappedServletRequestObject(req);
        if (sr instanceof IPrivateRequestAttributes) {
            return (Integer) ((IPrivateRequestAttributes) sr).getPrivateAttribute("SecurityRedirectPort");
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getRedirectUrl called for non-IPrivateRequestAttributes object", req);
            }
            return null;
        }

    }

    /**
     * Drill down through any possible HttpServletRequestWrapper objects.
     *
     * @param sr
     * @return
     */
    static HttpServletRequest getWrappedServletRequestObject(HttpServletRequest sr) {
        if (sr instanceof HttpServletRequestWrapper) {
            HttpServletRequestWrapper w = (HttpServletRequestWrapper) sr;
            // make sure we drill all the way down to an
            // SRTServletRequest...there
            // may be multiple proxied objects
            sr = (HttpServletRequest) w.getRequest();
            while (sr instanceof HttpServletRequestWrapper)
                sr = (HttpServletRequest) ((HttpServletRequestWrapper) sr).getRequest();
        }
        return sr;
    }
}
