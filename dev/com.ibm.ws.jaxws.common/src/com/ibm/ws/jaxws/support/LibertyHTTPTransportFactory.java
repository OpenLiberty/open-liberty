/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.support;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.namespace.QName;

import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import com.ibm.ws.jaxws.security.JaxWsSecurityConfigurationService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * LibertyHTTPTransportFactory provides Liberty extension for CXF internal HTTPTransportFactory, it provides the extra functions below :
 * a. Enable auto redirect function while loading WSDL file, as WebSphere full profile will send a redirect response while accessing WSDL with ?wsdl
 * b. create our LibertyHTTPConduit so that we can set the TCCL when run the handleResponseInternal asynchronously
 */
public class LibertyHTTPTransportFactory extends HTTPTransportFactory {

    private static final QName CXF_TRANSPORT_URI_RESOLVER_QNAME = new QName("http://cxf.apache.org", "TransportURIResolver");
    private static final AtomicReference<AtomicServiceReference<JaxWsSecurityConfigurationService>> securityConfigSR = new AtomicReference<AtomicServiceReference<JaxWsSecurityConfigurationService>>();

    /**
     * set the auto-redirect to true
     */
    @Override
    public Conduit getConduit(EndpointInfo endpointInfo, EndpointReferenceType target) throws IOException {
        //create our LibertyHTTPConduit so that we can set the TCCL when run the handleResponseInternal asynchronously
        LibertyHTTPConduit conduit = null;
        try {
            conduit = AccessController.doPrivileged(new PrivilegedExceptionAction<LibertyHTTPConduit>() {
                @Override
                public LibertyHTTPConduit run() throws IOException {
                    return new LibertyHTTPConduit(bus, endpointInfo, target);
                }
            });
        } catch (PrivilegedActionException pae) {
            throw (IOException) pae.getException();
        }

        //following are copied from the super class.
        //Spring configure the conduit.  
        String address = conduit.getAddress();
        if (address != null && address.indexOf('?') != -1) {
            address = address.substring(0, address.indexOf('?'));
        }
        HTTPConduitConfigurer c1 = bus.getExtension(HTTPConduitConfigurer.class);
        if (c1 != null) {
            c1.configure(conduit.getBeanName(), address, conduit);
        }
        configure(conduit, conduit.getBeanName(), address);
        conduit.finalizeConfig();
        //copy end.

        //open the auto redirect when load wsdl, and close auto redirect when process soap message in default.
        //users can open the auto redirect for soap message with ibm-ws-bnd.xml
        if (conduit != null) {
            HTTPClientPolicy clientPolicy = conduit.getClient();

            clientPolicy.setAutoRedirect(CXF_TRANSPORT_URI_RESOLVER_QNAME.equals(endpointInfo.getName()));
        }

        // Set defaultSSLConfig for this HTTP Conduit, in case it is needed when retrieve WSDL from HTTPS URL
        AtomicServiceReference<JaxWsSecurityConfigurationService> secConfigSR = securityConfigSR.get();
        JaxWsSecurityConfigurationService securityConfigService = secConfigSR == null ? null : secConfigSR.getService();
        if (null != securityConfigService) {
            // set null values for sslRef and certAlias so the default one will be used
            securityConfigService.configClientSSL(conduit, null, null);
        }

        return conduit;
    }

    /**
     * Set the security configuration service
     * 
     * @param securityConfigService the securityConfigService to set
     */
    public static void setSecurityConfigService(AtomicServiceReference<JaxWsSecurityConfigurationService> serviceRefer) {
        securityConfigSR.set(serviceRefer);
    }
}
