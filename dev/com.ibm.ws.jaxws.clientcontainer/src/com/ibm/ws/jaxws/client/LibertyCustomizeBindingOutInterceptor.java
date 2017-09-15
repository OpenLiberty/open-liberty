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
package com.ibm.ws.jaxws.client;

import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
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
 * Using the class to process the content in ibm-ws-bnd.xml file at client's side, such as url override, properties and security related stuff.
 */
public class LibertyCustomizeBindingOutInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final TraceComponent tc = Tr.register(LibertyCustomizeBindingOutInterceptor.class);

    private static final String HTTPS_SCHEMA = "https";

    private final JaxWsSecurityConfigurationService securityConfigService;
    private final Set<ConfigProperties> configPropertiesSet;

    protected final WebServiceRefInfo wsrInfo;

    public LibertyCustomizeBindingOutInterceptor(WebServiceRefInfo wsrInfo, JaxWsSecurityConfigurationService securityConfigService,
                                                 Set<ConfigProperties> configPropertiesSet) {
        super(Phase.PREPARE_SEND);
        this.wsrInfo = wsrInfo;
        this.securityConfigService = securityConfigService;
        this.configPropertiesSet = configPropertiesSet;
    }

    /**
     * 1 Override the port address by customized binding file
     * 2 Configure the properties in customized binding file.
     * 3 Configure the basic-auth, SSL, client-cert in customzed binding file
     * 
     * NOTICE: DO NOT change the order.
     */
    @Override
    public void handleMessage(@Sensitive Message message) throws Fault {

        //if no wsrinfo, we will not override the port address & client prop
        if (wsrInfo != null) {
            customizePortAddress(message);
            customizeClientProperties(message);
        }

        customizeClientSecurity(message);

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
     * remove the blank characters in the left and right for a given value.
     * 
     * @param value
     * @return
     */
    public final static String trim(String value)
    {
        String result = null;
        if (null != value)
        {
            result = value.trim();
        }

        return result;
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

    protected void customizeClientSecurity(Message message) {
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
     * Customize the client properties.
     * 
     * @param message
     */
    protected void customizeClientProperties(Message message) {
        if (null == configPropertiesSet) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "There are no client properties.");
            }
            return;
        }

        Bus bus = message.getExchange().getBus();
        if (null == bus) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The bus is null");
            }
            return;
        }

        for (ConfigProperties configProps : configPropertiesSet) {
            if (JaxWsConstants.HTTP_CONDUITS_SERVICE_FACTORY_PID.equals(configProps.getFactoryPid())) {
                customizeHttpConduitProperties(message, bus, configProps);
            }

        }
    }

    /**
     * Customize the port address
     * 
     * @param message
     */
    protected void customizePortAddress(Message message) {
        String address = null;
        PortComponentRefInfo portInfo = null;

        if (null != wsrInfo) {
            QName portQName = getPortQName(message);
            if (null != portQName) {
                portInfo = wsrInfo.getPortComponentRefInfo(portQName);
                address = (null != portInfo && null != portInfo.getAddress()) ? portInfo.getAddress() : wsrInfo.getDefaultPortAddress();
            }
        }

        if (null != address) {
            //change the endpoint address if there is a valid one.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The endpoint address is overriden by " + address);
            }
            message.put(Message.ENDPOINT_ADDRESS, address);
        }
    }

    private void customizeHttpConduitProperties(Message message, Bus bus, ConfigProperties configProps) {
        Conduit conduit = message.getExchange().getConduit(message);
        HTTPConduitConfigurer conduitConfigurer = bus.getExtension(HTTPConduitConfigurer.class);

        if (conduitConfigurer != null && conduit instanceof HTTPConduit) {
            HTTPConduit httpConduit = (HTTPConduit) conduit;
            String address = (String) message.get(Message.ENDPOINT_ADDRESS);
            if (conduitConfigurer instanceof ManagedServiceFactory) {
                String portQNameStr = getPortQName(message).toString();
                try {
                    ((ManagedServiceFactory) conduitConfigurer).updated(portQNameStr, configProps.getProperties());
                    conduitConfigurer.configure(portQNameStr, address, httpConduit);
                } catch (ConfigurationException e) {
                    throw new Fault(e);
                }
            }
        }
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
}
