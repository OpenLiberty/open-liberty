/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 *
 */
@Component(service = JwtSSOTokenHelper.class, name = "JwtSSOTokenHelper", immediate = true, property = "service.vendor=IBM")
public class JwtSSOTokenHelper {

    private static final TraceComponent tc = Tr.register(JwtSSOTokenHelper.class);

    public static final String JSON_WEB_TOKEN_SSO_PROXY = "JwtSSOTokenProxy";
    protected final static AtomicServiceReference<JwtSSOTokenProxy> jwtSSOTokenProxyRef = new AtomicServiceReference<JwtSSOTokenProxy>(JSON_WEB_TOKEN_SSO_PROXY);

    static private boolean isJdk18Up = (JavaInfo.majorVersion() >= 8);

    @Reference(service = JwtSSOTokenProxy.class, name = JSON_WEB_TOKEN_SSO_PROXY, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setJwtSSOToken(ServiceReference<JwtSSOTokenProxy> ref) {
        if (isJavaVersionAtLeast18()) {
            jwtSSOTokenProxyRef.setReference(ref);
        }
    }

    protected void unsetJwtSSOToken(ServiceReference<JwtSSOTokenProxy> ref) {
        if (isJavaVersionAtLeast18()) {
            jwtSSOTokenProxyRef.unsetReference(ref);
        }

    }

    @org.osgi.service.component.annotations.Activate
    protected void activate(ComponentContext cc) {
        if (isJavaVersionAtLeast18()) {
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
        if (isJavaVersionAtLeast18()) {
            jwtSSOTokenProxyRef.deactivate(cc);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Jwt SSO token helper service is deactivated");
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Jwt SSO token helper service is activated");
        }
    }

    private static boolean isJavaVersionAtLeast18() {
        return isJdk18Up;
    }

    public static void createJwtSSOToken(Subject subject) {
        if (jwtSSOTokenProxyRef.getService() != null) {
            try {
                jwtSSOTokenProxyRef.getService().createJwtSSOToken(subject);
            } catch (WSSecurityException e) {
                String msg = Tr.formatMessage(tc, "warn_jwt_sso_token_service_error");
                Tr.error(tc, msg);
            }
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

    public static Subject handleJwtSSOToken(String jwtssotoken) throws WSSecurityException {
        if (jwtSSOTokenProxyRef.getService() != null) {
            return jwtSSOTokenProxyRef.getService().handleJwtSSOTokenValidation(null, jwtssotoken);
        }
        return null;

    }

    public static Subject handleJwtSSOToken(Subject subject, String jwtssotoken) throws WSSecurityException {
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

    public static void addCustomCacheKeyToJwtSSOToken(Subject subject, String cacheKeyValue) {
        if (jwtSSOTokenProxyRef.getService() != null) {
            jwtSSOTokenProxyRef.getService().addCustomCacheKeyToJwtSSOToken(subject, cacheKeyValue);
        }
    }

    public static boolean isJwtSSOTokenValid(Subject subject) {
        if (jwtSSOTokenProxyRef.getService() != null) {
            return jwtSSOTokenProxyRef.getService().isJwtSSOTokenValid(subject);
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

    public static boolean shouldFallbackToLtpaCookie() {
        if (jwtSSOTokenProxyRef.getService() != null) {
            return jwtSSOTokenProxyRef.getService().shouldFallbackToLtpaCookie();
        }
        return true;
    }

    public static String getJwtCookieName() {
        if (jwtSSOTokenProxyRef.getService() != null) {
            return jwtSSOTokenProxyRef.getService().getJwtCookieName();
        }
        return null;

    }
}
