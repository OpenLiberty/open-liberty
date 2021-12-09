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

package com.ibm.ws.security.saml.impl;

import java.util.HashMap;
import java.util.Map;

import org.opensaml.saml.saml2.core.Assertion;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.token.Saml20TokenImpl;
import com.ibm.ws.security.sso.common.SsoService;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

public class Saml20SsoServiceImpl implements SsoService {
    private static TraceComponent tc = Tr.register(Saml20SsoServiceImpl.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);

    public static final String KEY_SSO_SERVICE = "ssoService";
    private static final ConcurrentServiceReferenceMap<String, SsoService> ssoServiceRefs =
                    new ConcurrentServiceReferenceMap<String, SsoService>(KEY_SSO_SERVICE);

    public void setSsoService(ServiceReference<SsoService> reference) {
        String type = (String) reference.getProperty(KEY_TYPE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "saml20 setSsoService type=" + type);
        }
        synchronized (ssoServiceRefs) {
            ssoServiceRefs.putReference(type, reference);
        }
    }

    public void unsetSsoService(ServiceReference<SsoService> reference) {
        String type = (String) reference.getProperty(KEY_TYPE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "saml20 unsetSsoService type=" + type);
        }
        synchronized (ssoServiceRefs) {
            ssoServiceRefs.removeReference(type, reference);
        }
    }

    protected void activate(ComponentContext cc) {
        ssoServiceRefs.activate(cc);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "SsoService activated");
        }
    }

    protected void deactivate(ComponentContext cc) {
        ssoServiceRefs.deactivate(cc);
    }

    @Override
    public Map<String, Object> handleRequest(String requestName,
                                             Map<String, Object> requestContext) throws Exception {
        Map<String, Object> result = new HashMap<String, Object>();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "saml20 handleRequest requestName:" + requestName +
                         "requestContext:" + requestContext);
        }
        if (WSSEC_SAML_ASSERTION.equals(requestName)) {
            Assertion assertion = null;
            if ((assertion = (Assertion) requestContext.get(WSSEC_SAML_ASSERTION)) != null) {
                result = handleSamlAssertion(assertion);
            }
        }
        return result;
    }

    /**
     * @param assertion
     * @return
     * @throws SamlException
     */
    private Map<String, Object> handleSamlAssertion(Assertion assertion) throws SamlException {
        Saml20Token token = new Saml20TokenImpl(assertion);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(SAML_SSO_TOKEN, token);
        return result;
    }

}
