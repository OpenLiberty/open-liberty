/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws23.client.security;

import java.io.IOException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLSocketFactory;
import javax.xml.namespace.QName;

import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.metadata.ConfigProperties;
import com.ibm.ws.jaxws.metadata.PortComponentRefInfo;
import com.ibm.ws.jaxws.metadata.WebServiceRefInfo;
import com.ibm.ws.jaxws.security.JaxWsSecurityConfigurationService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.jaxws.JaxWsConstants;
import com.ibm.ws.jaxws.metadata.ConfigProperties;
import com.ibm.ws.jaxws.metadata.PortComponentRefInfo;
import com.ibm.ws.jaxws.metadata.WebServiceRefInfo;
import com.ibm.ws.jaxws.security.JaxWsSecurityConfigurationService;

/**
 * Used to set the SSL config on the Client side conduit. This removes the need to modify the
 * HttpConduit directly through an extended LibertyHttpConduit 
 */
public class LibertyJaxWsClientSecurityOutInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final TraceComponent tc = Tr.register(LibertyJaxWsClientSecurityOutInterceptor.class);

    private static final QName CXF_TRANSPORT_URI_RESOLVER_QNAME = new QName("http://cxf.apache.org", "TransportURIResolver");
    private static final AtomicReference<AtomicServiceReference<JaxWsSecurityConfigurationService>> securityConfigSR = new AtomicReference<AtomicServiceReference<JaxWsSecurityConfigurationService>>();



    private final JaxWsSecurityConfigurationService securityConfigService;
    private static final String HTTPS_SCHEMA = "https";
    private final Set<ConfigProperties> configPropertiesSet;
    private final EndpointInfo endpointInfo;

    protected final WebServiceRefInfo wsrInfo;
    
    // Used for SSL check
    private boolean isSecured = false;
    private String address;

    /**
     * @param endpointInfo 
     * @param phase
     */
    public LibertyJaxWsClientSecurityOutInterceptor(WebServiceRefInfo wsrInfo, JaxWsSecurityConfigurationService securityConfigService,
                                               Set<ConfigProperties> configPropertiesSet, EndpointInfo endpointInfo) {
        super(Phase.PREPARE_SEND);
        this.wsrInfo = wsrInfo;
        this.configPropertiesSet = configPropertiesSet;
        this.securityConfigService = securityConfigService;
        // Does this actually make sense to set the endpointInfo only in the constructor?
        this.endpointInfo = endpointInfo;
      
    }

    @Override
    public void handleMessage(Message message) throws Fault {

            Conduit conduit = message.getExchange().getConduit(message);
            //open the auto redirect when load wsdl, and close auto redirect when process soap message in default.
            //users can open the auto redirect for soap message with ibm-ws-bnd.xml
            if (conduit != null) {
                HTTPClientPolicy clientPolicy = ((HTTPConduit) conduit).getClient();

                clientPolicy.setAutoRedirect(CXF_TRANSPORT_URI_RESOLVER_QNAME.equals(endpointInfo.getName()));
            }

            // Set defaultSSLConfig for this HTTP Conduit, in case it is needed when retrieve WSDL from HTTPS URL
            AtomicServiceReference<JaxWsSecurityConfigurationService> secConfigSR = securityConfigSR.get();
            JaxWsSecurityConfigurationService securityConfigService = secConfigSR == null ? null : secConfigSR.getService();

            customizeClientSecurity(message, securityConfigService);
    }
    
    protected void customizeClientSecurity(Message message, JaxWsSecurityConfigurationService securityConfigService) {
        QName portName = getPortQName(message);

        // configure the basic-auth
        if (null == securityConfigService) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The JaxWsSecurityConfigurationService is unavailable");
            }
            return;
        }

        //TODO now only consider https, can add if support other protocols in future 
        //let's check protocols first, we need prepare their check list for configuration

        //SSL check
        boolean isSecured = false;
        String address = (String) message.get(Message.ENDPOINT_ADDRESS);
        isSecured = address == null ? false : address.startsWith(HTTPS_SCHEMA);

        //process unmanaged service
        if (null == wsrInfo) {

            //if unmanaged service uses SSL,config default SSL
            if (isSecured) {
                securityConfigService.configClientSSL(message.getExchange().getConduit(message), null, null);
            }

            return;
        }

        //process managed service
        PortComponentRefInfo portRefInfo = wsrInfo.getPortComponentRefInfo(portName);

        if (null == portRefInfo) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Could not find the port component: " + portName + " for WebServiceRef: " + wsrInfo.getJndiName());
            }
            // try to use the server default ssl configuration, and disableCNcheck is true
            if (isSecured) {
                securityConfigService.configClientSSL(message.getExchange().getConduit(message), null, null);
            }

        } else {
            String userName = portRefInfo.getUserName();
            ProtectedString password = portRefInfo.getPassword();

            securityConfigService.configBasicAuth(message.getExchange().getConduit(message), userName, password);

            // configure the ssl
            if (isSecured) {
                securityConfigService.configClientSSL(message.getExchange().getConduit(message), portRefInfo.getSSLRef(), portRefInfo.getKeyAlias());
            }
        }

    }
    

    /**
     * build the qname with the given, and make sure the namespace is ended with "/" if specified.
     * 
     * @param portNameSpace
     * @param portLocalName
     * @return
     */
    public static QName buildQName(String namespace, String localName)
    {
        String namespaceURI = namespace;
        if (!isEmpty(namespace) && !namespace.trim().endsWith("/"))
        {
            namespaceURI += "/";
        }

        return new QName(namespaceURI, localName);
    }


    private QName getPortQName(Message message) {
        Object wsdlPort = message.getExchange().get(Message.WSDL_PORT);
        String namespace = "";
        String localName = "";
        if (null != wsdlPort && wsdlPort instanceof QName) {
            namespace = ((QName) wsdlPort).getNamespaceURI();
            localName = ((QName) wsdlPort).getLocalPart();
            return buildQName(namespace, localName);
        }
        return null;
    }

    /**
     * Check whether the target string is empty
     * 
     * @param str
     * @return true if the string is null or the length is zero after trimming spaces.
     */
    public static boolean isEmpty(String str) {
        if (str == null || str.isEmpty()) {
            return true;
        }

        int len = str.length();
        for (int x = 0; x < len; ++x) {
            if (str.charAt(x) > ' ') {
                return false;
            }
        }

        return true;
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