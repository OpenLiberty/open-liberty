/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.service.impl;

import java.util.Map;

import javax.xml.soap.SOAPException;

import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.jaxws.wsat.components.WSATConfigService;
import com.ibm.ws.wsat.service.Handler;
import com.ibm.wsspi.http.VirtualHost;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 *
 */
@Component(name = "com.ibm.ws.wsat.service.wsatconfigservice",
           immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = { "service.vendor=IBM" })
public class WSATConfigServiceImpl implements WSATConfigService {

    private static final TraceComponent TC = Tr.register(WSATConfigServiceImpl.class);
    private static final String SSLEnabled = "sslEnabled";
    private static final String clientAuthRef = "clientAuth";
    private static final String SSLRef = "sslRef";
    private static final String proxyRef = "externalURLPrefix";
    private static final String WSATContextRoot = "/ibm/wsatservice";

    private static final String HTTPCONFIGSERVICE_REFERENCE_NAME = "httpOptions";
    private static final String WSATHANDLERSERVICE_REFERENCE_NAME = "handler";

    private static final AtomicServiceReference<VirtualHost> httpOptions = new AtomicServiceReference<VirtualHost>(
                    HTTPCONFIGSERVICE_REFERENCE_NAME);
    private static final AtomicServiceReference<Handler> handlerService = new AtomicServiceReference<Handler>(WSATHANDLERSERVICE_REFERENCE_NAME);

    private boolean enabled;
    private String sslId;
    private String proxy;
    private boolean clientAuth;

    @Reference(name = WSATHANDLERSERVICE_REFERENCE_NAME, service = Handler.class)
    protected void setHandlerService(ServiceReference<Handler> ref) {
        handlerService.setReference(ref);
    }

    @Reference(name = HTTPCONFIGSERVICE_REFERENCE_NAME, service = VirtualHost.class,
               target = "(&(enabled=true)(id=default_host))")
    protected void setHttpOptions(ServiceReference<VirtualHost> ref) {
        httpOptions.setReference(ref);
    }

    protected void unsetHttpOptions(ServiceReference<VirtualHost> ref) {
        httpOptions.unsetReference(ref);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsat.service.WSATConfigService#isGlobalEnabled()
     */
    @Override
    public boolean isSSLEnabled() {
        return enabled;
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> properties) throws SOAPException {
        modified(cc, properties);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        httpOptions.deactivate(cc);

    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> properties) throws SOAPException {
        httpOptions.activate(cc);
        handlerService.activate(cc);

        enabled = (Boolean) properties.get(SSLEnabled);
        sslId = (String) properties.get(SSLRef);
        proxy = (String) properties.get(proxyRef);
        clientAuth = (Boolean) properties.get(clientAuthRef);
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "SSLEnabled = [{0}], SSLRefId = [{1}], proxy = [{2}], clientAuth = [{3}]", enabled, sslId, proxy, clientAuth);
        }
        if (enabled) {
            Tr.info(TC, "WSAT_SECURITY_CWLIB0206", sslId);
        }
        if (proxy != null && !proxy.equals("")) {
            Tr.info(TC, "WSAT_PROXY_CWLIB0207", proxy);

            if (enabled) {
                if (!proxy.startsWith("https://")) {
                    Tr.error(TC, "WSAT_PROXY_CWLIB0211", proxy);
                }
            } else {
                if (!proxy.startsWith("http://") && !proxy.startsWith("https://")) {
                    Tr.error(TC, "WSAT_PROXY_CWLIB0210", proxy);
                }
            }
        }

        String regHost = resolveHost()
                         + "/"
                         + Constants.COORDINATION_REGISTRATION_ENDPOINT;
        String coorHost = resolveHost()
                          + "/"
                          + Constants.COORDINATION_ENDPOINT;
        String partHost = resolveHost()
                          + "/"
                          + Constants.PARTICIPANT_ENDPOINT;

        EndpointReferenceType localCoorEpr = createEpr(coorHost);
        EndpointReferenceType localRegEpr = createEpr(regHost);
        EndpointReferenceType localPartEpr = createEpr(partHost);

        //set into HandlerService will always self coor...
        handlerService.getService().setCoordinatorEndpoint(localCoorEpr);
        handlerService.getService().setRegistrationEndpoint(localRegEpr);
        handlerService.getService().setParticipantEndpoint(localPartEpr);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.jaxws.wsat.components.WSATConfigService#getSSLReferenceId()
     */
    @Override
    public String getSSLReferenceId() {
        return sslId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.jaxws.wsat.components.WSATConfigService#getWSATUrl(boolean)
     */
    @Override
    public String getWSATUrl() {
        if (proxy != null && proxy.length() > 0)
            return proxy + WSATContextRoot;
        else
            return httpOptions.getService().getUrlString(WSATContextRoot, enabled);
    }

    private String resolveHost() {
        String host = "";
        if (TraceComponent.isAnyTracingEnabled() && TC.isDebugEnabled())
            Tr.debug(
                     TC,
                     "resolveHost",
                     "Checking if enable SSL for WS-AT",
                     enabled);
        host = getWSATUrl();
        if (TraceComponent.isAnyTracingEnabled() && TC.isDebugEnabled())
            Tr.debug(
                     TC,
                     "resolveHost",
                     "Checking which url is using for WS-AT",
                     host);
        return host;
    }

    private EndpointReferenceType createEpr(String hostname) throws SOAPException {
        EndpointReferenceType epr = new EndpointReferenceType();
        AttributedURIType uri = new AttributedURIType();
        uri.setValue(hostname);
        epr.setAddress(uri);
        ReferenceParametersType para = new ReferenceParametersType();
        epr.setReferenceParameters(para);
        return epr;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.jaxws.wsat.components.WSATConfigService#isClientAuthEnabled()
     */
    @Override
    public boolean isClientAuthEnabled() {
        return clientAuth;
    }
}
