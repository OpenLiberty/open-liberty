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
package com.ibm.ws.security.jwt.sso.token.utils;

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

    public static final String JSON_WEB_TOKEN_SSO = "jwtSSOToken";
    protected final static AtomicServiceReference<JwtSSOToken> jwtSSOTokenUtilRef = new AtomicServiceReference<JwtSSOToken>(JSON_WEB_TOKEN_SSO);

    static private boolean isJdk18Up = (JavaInfo.majorVersion() >= 8);

    @Reference(service = JwtSSOToken.class, name = JSON_WEB_TOKEN_SSO, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setJwtSSOToken(ServiceReference<JwtSSOToken> ref) {
        if (isJavaVersionAtLeast18()) {
            jwtSSOTokenUtilRef.setReference(ref);
        }
    }

    protected void unsetJwtSSOToken(ServiceReference<JwtSSOToken> ref) {
        if (isJavaVersionAtLeast18()) {
            jwtSSOTokenUtilRef.unsetReference(ref);
        }

    }

    @org.osgi.service.component.annotations.Activate
    protected void activate(ComponentContext cc) {
        if (isJavaVersionAtLeast18()) {
            jwtSSOTokenUtilRef.activate(cc);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Jwt SSO token service is activated");
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
            jwtSSOTokenUtilRef.deactivate(cc);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Jwt SSO token service is deactivated");
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
        if (jwtSSOTokenUtilRef.getService() != null) {
            try {
                jwtSSOTokenUtilRef.getService().createJwtSSOToken(subject);
            } catch (WSSecurityException e) {
                // TODO Auto-generated catch block
                // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
                // e.printStackTrace();
                String msg = Tr.formatMessage(tc, "warn_jwt_sso_token_service_error");
                Tr.error(tc, msg);
            }
        }

    }

    /**
     * @param subject
     */
    public static String getJwtSSOToken(Subject subject) {
        // TODO Auto-generated method stub
        if (jwtSSOTokenUtilRef.getService() != null) {
            return jwtSSOTokenUtilRef.getService().getJwtSSOToken(subject);
        }
        return null;

    }

    public static Subject handleJwtSSOToken(String jwtssotoken) {
        if (jwtSSOTokenUtilRef.getService() != null) {
            return jwtSSOTokenUtilRef.getService().handleJwtSSOToken(jwtssotoken);
        }
        return null;

    }

}
