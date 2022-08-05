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
package com.ibm.ws.wsat.webservice.client;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.HTTPConduit;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.client.LibertyCustomizeBindingOutInterceptor;
import com.ibm.ws.jaxws.security.JaxWsSecurityConfigurationService;
import com.ibm.ws.jaxws.wsat.components.WSATConfigService;
import com.ibm.wsspi.ssl.SSLSupport;

/**
 *
 */
public class SSLClientInterceptor extends AbstractPhaseInterceptor<Message> {

    final TraceComponent tc = Tr.register(
                                          SSLClientInterceptor.class, "WSAT", null);

    /**
     * @param phase
     */
    public SSLClientInterceptor() {
        super(Phase.PREPARE_SEND);
        getAfter().add(LibertyCustomizeBindingOutInterceptor.class.getName());
    }

    public WSATConfigService getConfigService() {
        BundleContext context = FrameworkUtil.getBundle(WSATConfigService.class)
                        .getBundleContext();
        ServiceReference<WSATConfigService> serviceRef = context.getServiceReference(WSATConfigService.class);
        if (serviceRef == null)
            return null;
        return context.getService(serviceRef);
    }

    public SSLSupport getSSLSupportService() {
        BundleContext context = FrameworkUtil.getBundle(SSLSupport.class).getBundleContext();
        ServiceReference<SSLSupport> serviceRef = context.getServiceReference(SSLSupport.class);
        if (serviceRef == null)
            return null;
        return context.getService(serviceRef);
    }

    public JaxWsSecurityConfigurationService getJaxWsSecurityService() {
        BundleContext context = FrameworkUtil.getBundle(JaxWsSecurityConfigurationService.class)
                        .getBundleContext();
        ServiceReference<JaxWsSecurityConfigurationService> serviceRef = context.getServiceReference(JaxWsSecurityConfigurationService.class);
        if (serviceRef == null) {
            throw new RuntimeException("JaxWsSecurity Service is not available, please make sure you have enabled appSecurity-2.0 feature and configured correct SSL setting");
        }
        return context.getService(serviceRef);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.interceptor.Interceptor#handleMessage(org.apache.cxf.message.Message)
     */
    @Override
    public void handleMessage(Message message) throws Fault {
        Conduit conduit = message.getExchange().getConduit(message);
        SSLSupport sslService = getSSLSupportService();
        if (sslService == null) {
            throw new Fault("SSL support service is not available", tc.getLogger());
        }
        if (conduit instanceof HTTPConduit) {
            HTTPConduit httpConduit = (HTTPConduit) conduit;
            String sslId = getConfigService().getSSLReferenceId();
            boolean exist = sslService.getJSSEHelper().doesSSLConfigExist(sslId);
            if (!exist) {
                throw new Fault("SSL Reference ID [" + sslId + "] not exist", tc.getLogger());
            } else {
                try {
                    JaxWsSecurityConfigurationService service = getJaxWsSecurityService();
                    if (service == null) {
                        throw new Fault("JaxWsSecurity Service is not available, please make sure you have enabled appSecurity-2.0 feature and configured correct SSL setting", tc.getLogger());
                    }
                    service.configClientSSL(httpConduit, sslId, null);
                } catch (Exception e) {
                    throw new Fault(e);
                }
            }
        }
    }
}
