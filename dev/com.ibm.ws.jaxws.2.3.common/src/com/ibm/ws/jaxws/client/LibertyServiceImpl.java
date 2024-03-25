/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.client;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.WebServiceFeature;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.jaxws.ServiceImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.feature.Feature;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.JaxWsConstants;
import com.ibm.ws.jaxws.internal.WebServiceConfigConstants;
import com.ibm.ws.jaxws.metadata.ConfigProperties;
import com.ibm.ws.jaxws.metadata.PortComponentRefInfo;
import com.ibm.ws.jaxws.metadata.WebServiceFeatureInfo;
import com.ibm.ws.jaxws.metadata.WebServiceRefInfo;
import com.ibm.ws.jaxws.security.JaxWsSecurityConfigurationService;
import com.ibm.ws.jaxws.support.LibertyLoggingInInterceptor;
import com.ibm.ws.jaxws.support.LibertyLoggingOutInterceptor;
import com.ibm.ws.jaxws23.client.security.LibertyJaxWsClientSecurityOutInterceptor;
import com.ibm.ws.kernel.productinfo.ProductInfo;

/**
 * All the Web Service ports and dispatches are created via the class
 */
public class LibertyServiceImpl extends ServiceImpl {

    private static final TraceComponent tc = Tr.register(LibertyServiceImpl.class);

    /**
     * The map contains service pid to property prefix entry.
     */
    private static final Map<String, String> servicePidToPropertyPrefixMap = new HashMap<String, String>();

    private final WebServiceRefInfo wsrInfo;

    private final JaxWsSecurityConfigurationService securityConfigService;
   
    /**
     * The map contains the port QName to port properties entry
     */
    private final Map<QName, Set<ConfigProperties>> servicePropertiesMap = new HashMap<QName, Set<ConfigProperties>>();

    static {
        // Add more if need other service pid to property prefix
        servicePidToPropertyPrefixMap.put(JaxWsConstants.HTTP_CONDUITS_SERVICE_FACTORY_PID, JaxWsConstants.HTTP_CONDUIT_PREFIX);
    }
    
    // Flag tells us if the message for a call to a beta method has been issued
    private static boolean issuedBetaMessage = false;
    

    /**
     * @param bus
     * @param url
     * @param name
     * @param clazz
     * @param features
     */
    public LibertyServiceImpl(JaxWsSecurityConfigurationService securityConfigService, WebServiceRefInfo wsrInfo,
                              Bus bus, URL url, QName name, Class<?> clazz, WebServiceFeature... features) {
        
        super(bus, url, name, clazz, features);

        this.securityConfigService = securityConfigService;
        this.wsrInfo = wsrInfo;
        
        if (null != wsrInfo) { 
            try {
                //Configure each port's properties defined in the custom binding files.
                configureClientProperties();
                
            } catch (IOException e) {
                throw new Fault(e);
            }
        } 
        
    }

    @Override
    protected <T> T createPort(QName portName, EndpointReferenceType epr, Class<T> serviceEndpointInterface,
                               WebServiceFeature... features) {

        if (features == null || features.length == 0) {
            features = getWebServiceFeaturesOnPortComponentRef(serviceEndpointInterface);
        }

        T clientProxy = super.createPort(portName, epr, serviceEndpointInterface, features);

        Client client = ClientProxy.getClient(clientProxy);

        configureCustomizeBinding(client, portName);
        
        if (!ProductInfo.getBetaEdition()) {           

            // Because we have to apply webService and webServiceClient configuration to existing code
            // we need to not apply it when not in beta. That means
            // we can't throw the normal UnsupportedOperationException when not in beta
            // so we just return the clientProxy as was done originally at this point in the code.
            return clientProxy;
        } else {
            // Running beta exception, issue message if we haven't already issued one for this class
            if (!issuedBetaMessage) {
                Tr.info(tc, "BETA: A webServiceClient configuration beta method has been invoked for the class " + this.getClass().getName() + " for the first time.");
                issuedBetaMessage = !issuedBetaMessage;
            }
        }
		
        configureWebServiceClientProperties(client);

        return clientProxy;
    }

    @Override
    public <T> Dispatch<T> createDispatch(QName portName, Class<T> type, JAXBContext context, Mode mode,
                                          WebServiceFeature... features) {

        DispatchImpl<T> dispatch = (DispatchImpl<T>) super.createDispatch(portName, type, context, mode, features);
        Client client = dispatch.getClient();

        configureCustomizeBinding(client, portName);
		
		if (!ProductInfo.getBetaEdition()) {           

            // Because we have to apply webService and webServiceClient configuration to existing code
            // we need to not apply it when not in beta. That means
            // we can't throw the normal UnsupportedOperationException when not in beta
            // so we just return the clientProxy as was done originally at this point in the code.
            return dispatch;
        } else {
            // Running beta exception, issue message if we haven't already issued one for this class
            if (!issuedBetaMessage) {
                Tr.info(tc, "BETA: A webServiceClient configuration beta method has been invoked for the class " + this.getClass().getName() + " for the first time.");
                issuedBetaMessage = !issuedBetaMessage;
            }
        }
		
        configureWebServiceClientProperties(client); 
		
        return dispatch;
    }
    
    /**
     *  This method is used to apply webServiceClient configuration to the Client instance
     *  Given that all of the current configuration is meant to be applied to inbound response processing
     *  This method simply sets a new instances of the LibertyWebServiceClientInInterceptor to the InterceptorChain.
     * @param client
     * @param map
     */
    private void configureWebServiceClientProperties(Client client) {
        // add the a new instance of LibertyWebServiceClientInInterceptor
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Adding the LibertyWebServiceClintInInterceptor ");
        }
        client.getInInterceptors().add(new LibertyWebServiceClientInInterceptor());
        
    }


    /**
     * Add the LibertyCustomizeBindingOutInterceptor in the out interceptor chain.
     * 
     * @param client
     * @param portName
     */
    protected void configureCustomizeBinding(Client client, QName portName) {
        //put all properties defined in ibm-ws-bnd.xml into the client request context
        Map<String, Object> requestContext = client.getRequestContext();
        if (null != requestContext && null != wsrInfo) {
            PortComponentRefInfo portRefInfo = wsrInfo.getPortComponentRefInfo(portName);

            Map<String, String> wsrProps = wsrInfo.getProperties();
            Map<String, String> portProps = (null != portRefInfo) ? portRefInfo.getProperties() : null;

            if (null != wsrProps) {
                requestContext.putAll(wsrProps);
            }

            if (null != portProps) {
                requestContext.putAll(portProps);
            }
            
            if (null != wsrProps && Boolean.valueOf(wsrProps.get(JaxWsConstants.ENABLE_lOGGINGINOUTINTERCEPTOR))) {

                Bus bus = this.getBus();
                if(bus != null) {               
                    
                    // Get all the Features enabled on the CXF BUS
                    Collection<Feature> featureList = bus.getFeatures();
                    
                    if( !featureList.contains(LoggingFeature.class)) {
                        // Create a new LogginFeature instance
                        LoggingFeature loggingFeature = new LoggingFeature();

                        // Add new LoggingFeature instance to Feature list and set it back on the Bus
                        if (!featureList.contains(loggingFeature)) {
                            loggingFeature.setPrettyLogging(true);
                            loggingFeature.initialize(bus);
                            featureList.add(loggingFeature);
                            bus.setFeatures(featureList);
                        }
                    }
                }

            }
        }

        Set<ConfigProperties> configPropsSet = servicePropertiesMap.get(portName);
        client.getOutInterceptors().add(new LibertyCustomizeBindingOutInterceptor(wsrInfo, securityConfigService, configPropsSet));

        client.getOutInterceptors().add(new LibertyJaxWsClientSecurityOutInterceptor(wsrInfo, securityConfigService, configPropsSet, client.getEndpoint().getEndpointInfo()));
        //need to add an interceptor to clean up HTTPTransportActivator.sorted & props via calling HTTPTransportActivator. deleted(String)
        //Memory Leak fix for 130985
        client.getOutInterceptors().add(new LibertyCustomizeBindingOutEndingInterceptor(wsrInfo));

    }

    /**
     * Gets an array of features possibly enabled on client's
     * port-component-ref in the deployment descriptor
     */
    private WebServiceFeature[] getWebServiceFeaturesOnPortComponentRef(Class serviceEndpointInterface) {

        WebServiceFeature[] returnArray = {};

        if (serviceEndpointInterface != null && wsrInfo != null) {
            String seiName = serviceEndpointInterface.getName();
            PortComponentRefInfo pcr = wsrInfo.getPortComponentRefInfo(seiName);

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "SEI name = " + seiName);
                Tr.debug(tc, "PortComponentRefInfo found = " + pcr);
            }

            if (pcr != null) {
                List<WebServiceFeatureInfo> featureInfoList = pcr.getWebServiceFeatureInfos();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "List of WebServiceFeatureInfo from PortComponentRefInfo = " + featureInfoList);
                }
                if (!featureInfoList.isEmpty()) {
                    returnArray = new WebServiceFeature[featureInfoList.size()];
                    for (int i = 0; i < featureInfoList.size(); i++) {
                        WebServiceFeatureInfo featureInfo = featureInfoList.get(i);
                        WebServiceFeature wsf = featureInfo.getWebServiceFeature();
                        returnArray[i] = wsf;
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Added WebServiceFeature " + wsf);
                        }
                    }
                }
            }
        }

        return returnArray;
    }

    /**
     * configure the http conduit properties defined in the custom binding file.
     */
    private void configureClientProperties() throws IOException {
        Map<String, String> serviceRefProps = wsrInfo.getProperties();
        Iterator<QName> iterator = this.getPorts();//wsrInfo.getBindingPortComponentRefInfoList();

        while (null != iterator && iterator.hasNext()) {
            QName portQName = iterator.next();
            PortComponentRefInfo portInfo = wsrInfo.getPortComponentRefInfo(portQName);
            Map<String, String> portProps = (null != portInfo) ? portInfo.getProperties() : null;

            if ((null != serviceRefProps || null != portProps)) {
                prepareProperties(portQName, serviceRefProps, portProps);
            }
        }
    }

    /**
     * merge the serviceRef properties and port properties, and update the merged properties in the configAdmin service.
     * 
     * @param configAdmin
     * @param serviceRefProps
     * @param portProps
     * @return
     * @throws IOException
     */
    private void prepareProperties(QName portQName, Map<String, String> serviceRefProps, Map<String, String> portProps) throws IOException {
        // Merge the properties form port and service.
        Map<String, String> allProperties = new HashMap<String, String>();

        if (null != serviceRefProps) {
            allProperties.putAll(serviceRefProps);
        }

        if (null != portProps) {
            allProperties.putAll(portProps);
        }

        for (Map.Entry<String, String> entry : servicePidToPropertyPrefixMap.entrySet()) {

            String serviceFactoryPid = entry.getKey();
            String prefix = entry.getValue();

            // Extract the properties according to different property prefix,
            // update the extracted properties by corresponding factory service.
            Map<String, String> extractProps = extract(prefix, allProperties);

            // Put the port QName and the properties into the servicePropertiesMap
            ConfigProperties configProps = new ConfigProperties(serviceFactoryPid, extractProps);
            Set<ConfigProperties> configSet = servicePropertiesMap.get(portQName);
            if (null == configSet) {
                configSet = new HashSet<ConfigProperties>();
                servicePropertiesMap.put(portQName, configSet);
            }
            if (configSet.contains(configProps)) {
                // re-add the config props
                configSet.remove(configProps);
                configSet.add(configProps);
            } else {
                configSet.add(configProps);
            }
        }
    }

    /**
     * Extract the properties according to the property prefix.
     * 
     * @param propertyPrefix
     * @param properties
     * @param removeProps if is true, will remove the properties that have be extracted.
     * @return
     */
    protected Map<String, String> extract(String propertyPrefix, Map<String, String> properties, boolean removeProps) {

        if (null == properties || properties.isEmpty()) {
            return Collections.<String, String> emptyMap();
        }

        Map<String, String> extractProps = new HashMap<String, String>();

        Iterator<Map.Entry<String, String>> propIter = properties.entrySet().iterator();
        while (propIter.hasNext()) {
            Map.Entry<String, String> propEntry = propIter.next();
            if (null != propEntry.getKey() && propEntry.getKey().startsWith(propertyPrefix)) {
                String propKey = propEntry.getKey().substring(propertyPrefix.length());
                extractProps.put(propKey, propEntry.getValue());
                if (removeProps) {
                    propIter.remove();
                }
            }
        }
        return extractProps;
    }

    /**
     * Extract the properties according to the property prefix, will remove the extracted properties from original properties.
     * 
     * @param propertyPrefix
     * @param properties
     * @return
     */
    protected Map<String, String> extract(String propertyPrefix, Map<String, String> properties) {
        return extract(propertyPrefix, properties, true);
    }

}
