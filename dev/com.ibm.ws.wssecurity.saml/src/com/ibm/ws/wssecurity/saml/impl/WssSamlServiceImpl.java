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
package com.ibm.ws.wssecurity.saml.impl;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.sso.common.SsoService;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorHelper;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

public class WssSamlServiceImpl implements SsoService {
    protected static final TraceComponent tc = Tr.register(WssSamlServiceImpl.class,
                                                           WssSamlConstants.TR_GROUP,
                                                           WssSamlConstants.TR_RESOURCE_BUNDLE);

    protected static final String KEY_SERVICE_PID = "service.pid";
    public static final String KEY_SSO_SERVICE = "ssoService";
    static final String KEY_SECURITY_SERVICE = "securityService";

    protected final ConcurrentServiceReferenceMap<String, SsoService> ssoServiceRefs =
                    new ConcurrentServiceReferenceMap<String, SsoService>(KEY_SSO_SERVICE);
    protected final AtomicServiceReference<SecurityService> securityServiceRef =
                    new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);

    protected void setSsoService(ServiceReference<SsoService> reference) {
        String type = (String) reference.getProperty(KEY_TYPE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "WSS Saml setSsoService type=" + type);
        }
        synchronized (ssoServiceRefs) {
            ssoServiceRefs.putReference(type, reference);
        }
    }

    protected void unsetSsoService(ServiceReference<SsoService> reference) {
        String type = (String) reference.getProperty(KEY_TYPE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "WSS Saml unsetSsoService type=" + type);
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

    public void activate(ComponentContext cc) {
        ssoServiceRefs.activate(cc);
        securityServiceRef.activate(cc);
        ServiceUtils.setAuthHelper(new WebProviderAuthenticatorHelper(securityServiceRef));
        ServiceUtils.setCommonSsoService(ssoServiceRefs);
        ServiceUtils.setSecurityServiceRef(securityServiceRef);
    }

    public void deactivate(ComponentContext cc) {
        ssoServiceRefs.deactivate(cc);
        securityServiceRef.deactivate(cc);
    }

    @Override
    public Map<String, Object> handleRequest(String requestName,
                                             Map<String, Object> requestContext) throws Exception {
        // temporary implement for testing only
        Map<String, Object> result = new HashMap<String, Object>();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "WSS Saml handleRequest requestName:" + requestName +
                         "requestContext:" + requestContext);
        }
        result.putAll(requestContext);
        return result;
    }

}
