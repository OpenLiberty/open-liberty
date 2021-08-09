/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wssecurity.token;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.sso.common.SsoService;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorHelper;
import com.ibm.ws.wssecurity.caller.AssertionToSubject;
import com.ibm.ws.wssecurity.internal.WSSecurityConstants;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.saml2.UserCredentialResolver;

public class WSSecurityTokenServiceImpl implements SsoService {
    protected static final TraceComponent tc = Tr.register(WSSecurityTokenServiceImpl.class,
                                                           WSSecurityConstants.TR_GROUP,
                                                           WSSecurityConstants.TR_RESOURCE_BUNDLE);

    static final String KEY_SERVICE_PID = "service.pid";
    static final String KEY_SSO_SERVICE = "ssoService";
    static final String KEY_USER_RESOLVER = "userResolver";
    static final String KEY_SECURITY_SERVICE = "securityService";

    protected final ConcurrentServiceReferenceMap<String, SsoService> ssoServiceRefs =
                    new ConcurrentServiceReferenceMap<String, SsoService>(KEY_SSO_SERVICE);
    protected final ConcurrentServiceReferenceMap<String, UserCredentialResolver> userResolverRef =
                    new ConcurrentServiceReferenceMap<String, UserCredentialResolver>(KEY_USER_RESOLVER);
    protected AtomicServiceReference<SecurityService> securityServiceRef =
                    new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);

    protected void setSsoService(ServiceReference<SsoService> reference) {
        String type = (String) reference.getProperty(KEY_TYPE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "WSSecurity setSsoService type=" + type);
        }
        synchronized (ssoServiceRefs) {
            ssoServiceRefs.putReference(type, reference);
        }
    }

    protected void unsetSsoService(ServiceReference<SsoService> reference) {
        String type = (String) reference.getProperty(KEY_TYPE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "WSSecurity unsetSsoService type=" + type);
        }
        synchronized (ssoServiceRefs) {
            ssoServiceRefs.removeReference(type, reference);
        }
    }

    protected void setSecurityService(ServiceReference<SecurityService> ref) {
        securityServiceRef.setReference(ref);
    }

    protected void unsetSecurityService(ServiceReference<SecurityService> ref) {
        securityServiceRef.unsetReference(ref);
    }

    protected void setUserResolver(ServiceReference<UserCredentialResolver> ref) {
        String serviceId = (String) ref.getProperty(KEY_SERVICE_PID);
        synchronized (userResolverRef) {
            userResolverRef.putReference(serviceId, ref);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " setUserResolver id:" + serviceId);
        }
    }

    protected void updatedUserResolver(ServiceReference<UserCredentialResolver> ref) {
        String serviceId = (String) ref.getProperty(KEY_SERVICE_PID);
        synchronized (userResolverRef) {
            userResolverRef.putReference(serviceId, ref);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " updateUserResolver id:" + serviceId);
        }
    }

    protected void unsetUserResolver(ServiceReference<UserCredentialResolver> ref) {
        String serviceId = (String) ref.getProperty(KEY_SERVICE_PID);
        synchronized (userResolverRef) {
            userResolverRef.removeReference(serviceId, ref);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " unsetUserResolverRef id:" + serviceId);
        }
    }

    public void activate(ComponentContext cc) {
        ssoServiceRefs.activate(cc);
        userResolverRef.activate(cc);
        securityServiceRef.activate(cc);
        AssertionToSubject.setActivatedUserResolverRef(userResolverRef);
        TokenUtils.setAuthHelper(new WebProviderAuthenticatorHelper(securityServiceRef));
        TokenUtils.setCommonSsoService(ssoServiceRefs);
        TokenUtils.setSecurityServiceRef(securityServiceRef);
    }

    public void deactivate(ComponentContext cc) {
        ssoServiceRefs.deactivate(cc);
        userResolverRef.deactivate(cc);
        securityServiceRef.deactivate(cc);
    }

    @Override
    public Map<String, Object> handleRequest(String requestName,
                                             Map<String, Object> requestContext) throws Exception {
        Map<String, Object> result = new HashMap<String, Object>();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "WSSecurity handleRequest requestName:" + requestName +
                         "requestContext:" + requestContext);
        }
        // temporary implementation
        if (requestContext != null) {
            result.putAll(requestContext);
        }
        return result;
    }

}
