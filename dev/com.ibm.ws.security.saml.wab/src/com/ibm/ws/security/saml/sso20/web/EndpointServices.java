/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.web;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoHandler;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.impl.HandlerFactory;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

public class EndpointServices {
    private static TraceComponent tc = Tr.register(EndpointServices.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    public static final String KEY_SECURITY_SERVICE = "securityService";
    public static final String KEY_ID = "id";
    public static final String KEY_SAML_SERVICE = "samlService";
    private final ConcurrentServiceReferenceMap<String, SsoSamlService> samlServiceRef = new ConcurrentServiceReferenceMap<String, SsoSamlService>(KEY_SAML_SERVICE);
    protected final AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);
    @SuppressWarnings("unused")
    private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";

    protected void setSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.setReference(reference);
    }

    protected void unsetSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.unsetReference(reference);
    }

    @Trivial
    protected void setSamlService(ServiceReference<SsoSamlService> ref) {
        synchronized (samlServiceRef) {
            samlServiceRef.putReference((String) ref.getProperty(KEY_ID), ref);
        }
    }

    @Trivial
    protected void unsetSamlService(ServiceReference<SsoSamlService> ref) {
        synchronized (samlServiceRef) {
            samlServiceRef.removeReference((String) ref.getProperty(KEY_ID), ref);
        }
    }

    protected void activate(ComponentContext cc) {
        securityServiceRef.activate(cc);
        samlServiceRef.activate(cc);
        Tr.info(tc, "SAML20_ENDPOINT_SERVICE_ACTIVATED");
    }

    protected void deactivate(ComponentContext cc) {
        securityServiceRef.deactivate(cc);
        samlServiceRef.deactivate(cc);
    }

    protected void handleSamlRequest(HttpServletRequest request,
                                     HttpServletResponse response) throws SamlException {
        SsoRequest samlRequest = (SsoRequest) request.getAttribute(Constants.ATTRIBUTE_SAML20_REQUEST);
        if (samlRequest != null) {
            handleSamlRequest(request, response, samlRequest);
        }
    }

    /**
     * @param request
     * @param response
     * @param servletContext
     * @param samlRequest
     * @throws SamlException
     */
    private void handleSamlRequest(HttpServletRequest request, HttpServletResponse response, SsoRequest samlRequest) throws SamlException {
        SsoSamlService samlService = getSsoSamlService(response, samlRequest);
        if (samlService != null) {
            SsoConfig ssoConfig = samlService.getConfig();
            if (ssoConfig != null) {
                String reqUrl = request.getRequestURL().toString();
                if (!checkHttpsRequirement(ssoConfig, reqUrl)) {
                    throw new SamlException("SAML20_EP_PROTOCOL_NOT_HTTPS", null, new Object[] { reqUrl });
                } else {
                    Map<String, Object> parameters = getParameterMap(samlService);
                    SsoHandler samlHandler = HandlerFactory.getHandlerInstance(samlRequest); //AcsHandler
                    samlHandler.handleRequest(request, response, samlRequest, parameters);
                }
            }
        }
    }

    boolean checkHttpsRequirement(SsoConfig ssoConfig, String urlStr) {
        boolean metHttpsRequirement = true;
        if (ssoConfig.isHttpsRequired()) {
            if (urlStr != null && !urlStr.startsWith("https")) {
                metHttpsRequirement = false;
            }
        }
        return metHttpsRequirement;
    }

    /**
     * @param samlRequest
     * @param samlService
     * @return
     */
    private Map<String, Object> getParameterMap(SsoSamlService samlService) {
        HashMap<String, Object> results = new HashMap<String, Object>();
        results.put(Constants.KEY_SAML_SERVICE, samlService);
        results.put(Constants.KEY_SECURITY_SERVICE, securityServiceRef.getService());
        return results;
    }

    private SsoSamlService getSsoSamlService(HttpServletResponse response, SsoRequest samlRequest) throws SamlException {
        SsoSamlService service = samlServiceRef.getService(samlRequest.getProviderName());
        if (service == null || !service.isEnabled()) { // if no ssoService or the ssoService is disabled
            service = null; // even it has an service, as long as it is disable. It is the same as null
            throw new SamlException("SAML20_NO_SUCH_ACS_PROVIDER", null, new Object[] { samlRequest.getProviderName() });
        } else {
            samlRequest.setSsoSamlService(service); // for error handling
        }
        return service;
    }
}
