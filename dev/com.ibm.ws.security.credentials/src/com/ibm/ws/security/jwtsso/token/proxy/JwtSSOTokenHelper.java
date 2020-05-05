/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso.token.proxy;

import java.util.Map;

import javax.security.auth.Subject;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.WSLoginFailedException;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 *
 */
@Component(service = JwtSSOTokenHelper.class, name = "JwtSSOTokenHelper", immediate = true, property = "service.vendor=IBM")
public class JwtSSOTokenHelper {

    private static final TraceComponent tc = Tr.register(JwtSSOTokenHelper.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public static final String JSON_WEB_TOKEN_SSO_PROXY = "JwtSSOTokenProxy";
    protected final static AtomicServiceReference<JwtSSOTokenProxy> jwtSSOTokenProxyRef = new AtomicServiceReference<JwtSSOTokenProxy>(JSON_WEB_TOKEN_SSO_PROXY);

    static private boolean isJavaVersionAtLeast18 = (JavaInfo.majorVersion() >= 8);

    @Reference(service = JwtSSOTokenProxy.class, name = JSON_WEB_TOKEN_SSO_PROXY, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setJwtSSOToken(ServiceReference<JwtSSOTokenProxy> ref) {
        if (isJavaVersionAtLeast18) {
            jwtSSOTokenProxyRef.setReference(ref);
        }
    }

    protected void unsetJwtSSOToken(ServiceReference<JwtSSOTokenProxy> ref) {
        if (isJavaVersionAtLeast18) {
            jwtSSOTokenProxyRef.unsetReference(ref);
        }

    }

    @org.osgi.service.component.annotations.Activate
    protected void activate(ComponentContext cc) {
        if (isJavaVersionAtLeast18) {
            jwtSSOTokenProxyRef.activate(cc);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Jwt SSO token helper service is activated");
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Jwt SSO token helper service is activated");
        }
    }

    @org.osgi.service.component.annotations.Modified
    protected void modified(Map<String, Object> props) {}

    @org.osgi.service.component.annotations.Deactivate
    protected void deactivate(ComponentContext cc) {
        if (isJavaVersionAtLeast18) {
            jwtSSOTokenProxyRef.deactivate(cc);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Jwt SSO token helper service is deactivated");
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Jwt SSO token helper service is activated");
        }
    }

    public static void createJwtSSOToken(Subject subject) throws WSLoginFailedException {
        if (jwtSSOTokenProxyRef.getService() != null) {
            jwtSSOTokenProxyRef.getService().createJwtSSOToken(subject);
        }
    }

    /**
     * @param subject
     */
    public static String getJwtSSOToken(Subject subject) {
        if (jwtSSOTokenProxyRef.getService() != null) {
            return jwtSSOTokenProxyRef.getService().getJwtSSOToken(subject);
        }
        return null;

    }

    public static Subject handleJwtSSOToken(String jwtssotoken) throws WSLoginFailedException {
        return handleJwtSSOToken(null, jwtssotoken);
    }

    public static Subject handleJwtSSOToken(Subject subject, String jwtssotoken) throws WSLoginFailedException {
        if (jwtSSOTokenProxyRef.getService() != null) {
            return jwtSSOTokenProxyRef.getService().handleJwtSSOTokenValidation(subject, jwtssotoken);
        }
        return null;

    }

    public static String getCustomCacheKeyFromJwtSSOToken(String encodedjwt) {
        if (jwtSSOTokenProxyRef.getService() != null) {
            return jwtSSOTokenProxyRef.getService().getCustomCacheKeyFromJwtSSOToken(encodedjwt);
        }
        return null;
    }

    public static String getCacheKeyForJwtSSOToken(Subject subject, String encodedjwt) {
        if (jwtSSOTokenProxyRef.getService() != null) {
            return jwtSSOTokenProxyRef.getService().getCacheKeyForJwtSSOToken(subject, encodedjwt);
        }
        return null;
    }

    public static void addAttributesToJwtSSOToken(Subject subject) throws WSLoginFailedException {
        if (jwtSSOTokenProxyRef.getService() != null) {
            jwtSSOTokenProxyRef.getService().addAttributesToJwtSSOToken(subject);
        }
    }

    public static boolean isSubjectValid(Subject subject) {
        if (jwtSSOTokenProxyRef.getService() != null) {
            return jwtSSOTokenProxyRef.getService().isSubjectValid(subject);
        }
        return false;
    }

    public static boolean shouldSetJwtCookiePathToWebAppContext() {
        if (jwtSSOTokenProxyRef.getService() != null) {
            return jwtSSOTokenProxyRef.getService().shouldSetJwtCookiePathToWebAppContext();
        }
        return false;

    }

    public static boolean shouldAlsoIncludeLtpaCookie() {
        if (jwtSSOTokenProxyRef.getService() != null) {
            return jwtSSOTokenProxyRef.getService().shouldAlsoIncludeLtpaCookie();
        }
        return true;
    }

    public static boolean shouldUseLtpaIfJwtAbsent() {
        if (jwtSSOTokenProxyRef.getService() != null) {
            return jwtSSOTokenProxyRef.getService().shouldUseLtpaIfJwtAbsent();
        }
        return true;
    }

    public static String getJwtCookieName() {
        if (jwtSSOTokenProxyRef.getService() != null) {
            return jwtSSOTokenProxyRef.getService().getJwtCookieName();
        }
        return null;

    }

    public static boolean isCookieSecured() {
        if (jwtSSOTokenProxyRef.getService() != null) {
            return jwtSSOTokenProxyRef.getService().isCookieSecured();
        }
        return true;

    }

    public static long getValidTimeInMinutes() {
        if (jwtSSOTokenProxyRef.getService() != null) {
            return jwtSSOTokenProxyRef.getService().getValidTimeInMinutes();
        }
        return 0;

    }

    public static boolean isDisableJwtCookie() {
        if (jwtSSOTokenProxyRef.getService() != null) {
            return jwtSSOTokenProxyRef.getService().isDisableJwtCookie();
        }
        return true;

    }
}
