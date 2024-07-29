/*******************************************************************************
 * Copyright (c) 2019,2024 IBM Corporation and others.
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
package com.ibm.ws.jaxws.endpoint;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.Handler;

import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.WSDLGetInterceptor;
import org.apache.cxf.frontend.WSDLGetUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.servlet.BaseUrlHelper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.JaxWsConstants;
import com.ibm.ws.jaxws.internal.WebServiceConfigConstants;

import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.feature.Feature;

import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.ws.jaxws.metadata.JaxWsModuleMetaData;
import com.ibm.ws.jaxws.support.JaxWsInstanceManager;
import com.ibm.ws.jaxws.support.JaxWsInstanceManager.InterceptException;
import com.ibm.ws.jaxws.support.LibertyJaxWsCompatibleWSDLGetInterceptor;
import com.ibm.ws.jaxws.support.LibertyLoggingInInterceptor;
import com.ibm.ws.jaxws.support.LibertyLoggingOutInterceptor;
import com.ibm.ws.jaxws.utils.JaxWsUtils;
import com.ibm.ws.jaxws.utils.StringUtils;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.service.util.JavaInfo;

import io.openliberty.jaxws.jaxb.IgnoreUnexpectedElementValidationEventHandler;

import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.Bus;

public abstract class AbstractJaxWsWebEndpoint implements JaxWsWebEndpoint {

    private static final TraceComponent tc = Tr.register(AbstractJaxWsWebEndpoint.class);

    public static final String DISABLE_ADDRESS_UPDATES = "disable-address-updates";

    public static final String BASE_ADDRESS = "base-address";

    private static final String HTTP_PREFIX = "http";

    private static final String SET_JAXB_VALIDATION_EVENT_HANDLER = "set-jaxb-validation-event-handler";

    protected final EndpointInfo endpointInfo;

    protected final JaxWsModuleMetaData jaxWsModuleMetaData;

    protected Server server;

    protected ServletConfig servletConfig;

    protected AbstractHTTPDestination destination;

    protected boolean disableAddressUpdates;

    protected String forcedBaseAddress;    
    
    // Flag tells us if the message for a call to a beta method has been issued
    private static boolean issuedBetaMessage = false;

    public AbstractJaxWsWebEndpoint(EndpointInfo endpointInfo, JaxWsModuleMetaData jaxWsModuleMetaData) {
        this.endpointInfo = endpointInfo;
        this.jaxWsModuleMetaData = jaxWsModuleMetaData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        this.servletConfig = servletConfig;

        String isDisableAddressUpdates = servletConfig.getInitParameter(DISABLE_ADDRESS_UPDATES);
        if (!StringUtils.isEmpty(isDisableAddressUpdates)) {
            this.disableAddressUpdates = Boolean.valueOf(isDisableAddressUpdates);
        }

        String isForcedBaseAddress = servletConfig.getInitParameter(BASE_ADDRESS);
        if (!StringUtils.isEmpty(isForcedBaseAddress)) {
            this.forcedBaseAddress = isForcedBaseAddress;
        }
    }

    /**
     * Customize WSDL Intercepter when no need to generate WSDL.
     */
    protected void customizeWSDLGetInterceptor(Class<?> implBeanClass) {
        String wsdlLocation = endpointInfo.getWsdlLocation();
        URL wsdlUrl = JaxWsUtils.resolve(wsdlLocation, jaxWsModuleMetaData.getModuleContainer());
        boolean wsdlLocationExisted = true;
        boolean wsdlLocationEmpty = StringUtils.isEmpty(wsdlLocation);
        //check whether there is jax-ws-catalog enabled
        if (!wsdlLocationEmpty && wsdlUrl == null) {
            wsdlLocationExisted = false;
            OASISCatalogManager catalogManager = jaxWsModuleMetaData.getServerMetaData().getServerBus().getExtension(OASISCatalogManager.class);
            String resolvedLocation = null;
            if (catalogManager != null) {

                try {
                    resolvedLocation = catalogManager.resolveSystem(wsdlLocation);

                    if (resolvedLocation == null) {
                        resolvedLocation = catalogManager.resolveURI(wsdlLocation);
                    }
                } catch (MalformedURLException e) {
                    //do nothing
                } catch (IOException e) {
                    //do nothing
                }

            }
            if (resolvedLocation != null) {
                wsdlLocationExisted = true;
            }

        }

        if (!wsdlLocationEmpty && wsdlLocationExisted) {
            return;
        }
        String protocolBinding = endpointInfo.getProtocolBinding();
        if (!JaxWsUtils.isWSDLGenerationSupported(protocolBinding) || !wsdlLocationExisted) {
            List<Interceptor<? extends Message>> inInterceptors = server.getEndpoint().getInInterceptors();
            inInterceptors.remove(WSDLGetInterceptor.INSTANCE);
            inInterceptors.add(new LibertyJaxWsCompatibleWSDLGetInterceptor(implBeanClass.getCanonicalName(), wsdlLocation, wsdlLocationExisted));
        }
    }

    protected void enableLogging(EndpointInfo libertyEndpointInfo) {
        Map<String, String> endpointProperties = libertyEndpointInfo.getEndpointProperties();

        if (null != endpointProperties && Boolean.valueOf(endpointProperties.get(JaxWsConstants.ENABLE_lOGGINGINOUTINTERCEPTOR))) {
            // If we're here we know this property is set in the config and is true. We enable pretty logging of the SOAP Message
            // by LibertyLoggingIn(Out)Interceptors
            // TODO Create a way of enabling and disabling logging for individual endpoints. 
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, JaxWsConstants.ENABLE_lOGGINGINOUTINTERCEPTOR
                             + " has been enabled, enabling SOAP Message Logging with the LibertyLoggingInInterceptor and LibertyLoggingOutInterceptor");
            }
            
            if(this.jaxWsModuleMetaData != null) {
   
                if( jaxWsModuleMetaData.getServerMetaData().getServerBus() != null ) {

                    Bus bus = jaxWsModuleMetaData.getServerMetaData().getServerBus();
                
                    Collection<Feature> featureList = bus.getFeatures();
                
                    if( !featureList.contains(LoggingFeature.class)) {
                        LoggingFeature loggingFeature = new LoggingFeature();

                    
                        if (!featureList.contains(loggingFeature)) {
                            loggingFeature.setPrettyLogging(true);
                            loggingFeature.initialize(bus);
                            featureList.add(loggingFeature);
                            bus.setFeatures(featureList);
                        }
                        
                    }
                } else if ( jaxWsModuleMetaData.getClientMetaData() != null ) {
                    Bus bus = jaxWsModuleMetaData.getClientMetaData().getClientBus();
                    
                    Collection<Feature> featureList = bus.getFeatures();
                    
                    if(!featureList.contains(LoggingFeature.class)) {
                        LoggingFeature loggingFeature = new LoggingFeature();

                        
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
    }

    public AbstractHTTPDestination getDestination() {
        return this.destination;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        invokePreDestroy();
        server.destroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invoke(final HttpServletRequest request, final HttpServletResponse response) throws ServletException {
        try {
            updateDestination(request);
            final HttpServletRequest req = request;
            final HttpServletResponse resp = response;
            
            if (!ProductInfo.getBetaEdition()) {           

                try {
                    AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                        @Override
                        public Void run() throws IOException {
                            destination.invoke(servletConfig, servletConfig.getServletContext(), req, resp);
                            return null;
                        }
                    });
                } catch (PrivilegedActionException pae) {
                    throw (IOException) pae.getException();
                }
            } else {
                // Running beta exception, issue message if we haven't already issued one for this class
                if (!issuedBetaMessage) {
                    Tr.debug(tc, "BETA: A webService configuration beta method has been invoked for the class " + this.getClass().getName() + " for the first time.");
                    issuedBetaMessage = !issuedBetaMessage;
                }
                
                configureWebServicesConfig();
                
                try {
                    AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                        @Override
                        public Void run() throws IOException {
                            destination.invoke(servletConfig, servletConfig.getServletContext(), req, resp);
                            return null;
                        }
                    });
                } catch (PrivilegedActionException pae) {
                    throw (IOException) pae.getException();
                }
            }
                    


        } catch (IOException e) {
            throw new ServletException(e);
        }
    }

    /**
     * 
     */
    private void configureWebServicesConfig() {
        
        boolean debug = tc.isDebugEnabled();
        
        // Use CXF's EndpointInfo to set the configuration properties
        org.apache.cxf.service.model.EndpointInfo cxfEndpointInfo = destination.getEndpointInfo();
        
        // Get the portName from the Liberty's EndpointInfo
        QName portNameQname = endpointInfo.getWsdlPort();
        String portName = portNameQname.getLocalPart();
        
        if (debug) {
            Tr.debug(tc, portName + " will be used to find webService Configuration");
        }
        
        Object enableSchemaValidation = null;

        Object ignoreUnexpectedElements = null;

        // if portName != null, try to get the values from configuration using it
        if (portName != null) {
            // if portName != null, try to get enableSchemaValidation value from configuration, if it's == null try it to get the default configuration value
            if(WebServicesConfigHolder.getEnableSchemaValidation(portName) != null) {
                
                enableSchemaValidation = WebServicesConfigHolder.getEnableSchemaValidation(portName);
                
            } else if (WebServicesConfigHolder.getEnableSchemaValidation(WebServiceConfigConstants.DEFAULT_PROP) != null) {
                
                enableSchemaValidation = WebServicesConfigHolder.getEnableSchemaValidation(WebServiceConfigConstants.DEFAULT_PROP);
                
            }
            

            // if portName != null, try to get ignoreUnexpectedElements value from configuration, if it's == null try to get the default configuration value
            if(WebServicesConfigHolder.getIgnoreUnexpectedElements(portName) != null) {
                
                ignoreUnexpectedElements = WebServicesConfigHolder.getIgnoreUnexpectedElements(portName);
                
            } else if (WebServicesConfigHolder.getIgnoreUnexpectedElements(WebServiceConfigConstants.DEFAULT_PROP) != null) {
                
                ignoreUnexpectedElements = WebServicesConfigHolder.getIgnoreUnexpectedElements(WebServiceConfigConstants.DEFAULT_PROP);
                
            }

            
        } else {
            // if portName == null then try to get the global configuration values, if its not set keep values null
            enableSchemaValidation = (WebServicesConfigHolder.getEnableSchemaValidation(WebServiceConfigConstants.DEFAULT_PROP) != null) ? WebServicesConfigHolder.getEnableSchemaValidation(WebServiceConfigConstants.DEFAULT_PROP) : null;

            ignoreUnexpectedElements = (WebServicesConfigHolder.getIgnoreUnexpectedElements(WebServiceConfigConstants.DEFAULT_PROP) != null) ? WebServicesConfigHolder.getIgnoreUnexpectedElements(WebServiceConfigConstants.DEFAULT_PROP) : null;            

        }
        
        
        if ((enableSchemaValidation == null && ignoreUnexpectedElements == null)) {
            if (debug) {
                Tr.debug(tc, "No webService configuration found. returning.");
            }
            return;
        }
        
        Map<String, String> endpointInfoProperties = endpointInfo.getEndpointProperties();

        // Enable or disable schema validation as long as property is non-null
        if ( enableSchemaValidation != null) {
            cxfEndpointInfo.setProperty("schema-validation-enabled",  enableSchemaValidation);

            if (debug) {
                Tr.debug(tc, "Set schema-validation-enabled to " + enableSchemaValidation);

            }
        } else {

            if (debug) {
                Tr.debug(tc, "enableSchemaValdiation was null, not configuring schema-validation-enabled on the Web Service Endpoint");

            }
        }
       

        // Set ignoreUnexpectedElements if true
        if (ignoreUnexpectedElements != null && (boolean) ignoreUnexpectedElements == true) {
           
            cxfEndpointInfo.setProperty(JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER, ignoreUnexpectedElements);
            
            // Set our custom validation event handler
            IgnoreUnexpectedElementValidationEventHandler unexpectedElementValidationEventHandler = new IgnoreUnexpectedElementValidationEventHandler();
            cxfEndpointInfo.setProperty(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER, unexpectedElementValidationEventHandler); 

            if (debug) {
                Tr.debug(tc, "Set JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER to  " + ignoreUnexpectedElements);
            }

        } else {
            if (debug) {
                Tr.debug(tc, "ignoreUnexpectedElements was " + ignoreUnexpectedElements + " not configuring ignoreUnexpectedElements on the Web Service Endpoint");

            }
        }
        
    }

    @SuppressWarnings("rawtypes")
    protected void invokePreDestroy() {
        // preDestroy handlers
        List<Handler> handlers = ((JaxWsEndpointImpl) server.getEndpoint()).getJaxwsBinding().getHandlerChain();
        JaxWsInstanceManager jaxWsInstanceManager = jaxWsModuleMetaData.getJaxWsInstanceManager();
        for (Handler handler : handlers) {
            try {
                jaxWsInstanceManager.destroyInstance(handler);
            } catch (InterceptException e) {
                Tr.warning(tc, "warn.invoke.handler.predestory", handler.getClass().getName(), e.getMessage());
            }
        }
    }

    /**
     * Configure common endpoint properties
     *
     * @param endpointInfo
     */
    protected void configureEndpointInfoProperties(EndpointInfo libertyEndpointInfo, org.apache.cxf.service.model.EndpointInfo cxfEndpointInfo) {

        //Disable jaxb validation event handler, as IBM FastPath does not support this, which will finally fallback to RI unmarshall
        cxfEndpointInfo.setProperty(SET_JAXB_VALIDATION_EVENT_HANDLER, false);

        //Set autoRewriteSoapAddressForAllServices with true by default, which will override all the services in the target WSDL file
        cxfEndpointInfo.setProperty(WSDLGetUtils.AUTO_REWRITE_ADDRESS_ALL, true);

        //Set WSDL_DESCRIPTION property
        try {
            String wsdlLocation = libertyEndpointInfo.getWsdlLocation();
            if (wsdlLocation != null && !(wsdlLocation.isEmpty())) {
                URI wsdlDescription = new URI(wsdlLocation);
                cxfEndpointInfo.setProperty("URI", wsdlDescription);
            }
        } catch (URISyntaxException e) {
            //donothing
        }
        Map<String, String> endpointProperties = libertyEndpointInfo.getEndpointProperties();
        if (endpointProperties != null && !endpointProperties.isEmpty()) {
            for (Entry<String, String> entry : endpointProperties.entrySet()) {
                cxfEndpointInfo.setProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    protected void updateDestination(HttpServletRequest request) {
        String ad = destination.getEndpointInfo().getAddress();
        if (ad != null && ad.startsWith(HTTP_PREFIX)) {
            return;
        }

        synchronized (destination) {
            ad = destination.getEndpointInfo().getAddress();
            if (ad == null
                && destination.getAddress() != null
                && destination.getAddress().getAddress() != null) {
                ad = destination.getAddress().getAddress().getValue();
                if (ad == null) {
                    ad = "/";
                }
            }

            if (ad != null && !ad.startsWith(HTTP_PREFIX)) {
                String base = getBaseURL(request);
                if (disableAddressUpdates) {

                    request.setAttribute("org.apache.cxf.transport.endpoint.address",
                                         base + ad);
                } else {
                    // Only set the address if the Endpoint URL hasn't already been set
                    if (destination.getEndpointInfo().getAddress() == null)
                        BaseUrlHelper.setAddress(destination, base + ad);
                }
            }
        }
    }

    /**
     * Calculate the base URL based on the HttpServletRequest instance
     *
     * @param request
     * @return
     */
    protected String getBaseURL(HttpServletRequest request) {
        if (forcedBaseAddress != null) {
            return forcedBaseAddress;
        }

        String reqPrefix = request.getRequestURL().toString();
        String pathInfo = request.getPathInfo() == null ? "" : request.getPathInfo();
        //fix for CXF-898
        if (!"/".equals(pathInfo) || reqPrefix.endsWith("/")) {
            StringBuilder sb = new StringBuilder();
            // request.getScheme(), request.getLocalName() and request.getLocalPort()
            // should be marginally cheaper - provided request.getLocalName() does
            // return the actual name used in request URI as opposed to localhost
            // consistently across the Servlet stacks

            URI uri = URI.create(reqPrefix);
            sb.append(uri.getScheme()).append("://").append(uri.getRawAuthority());

            //No servletPath will be appended, as in Liberty, each endpoint will be served by one servlet instance
            sb.append(request.getContextPath());

            reqPrefix = sb.toString();
        }
        return reqPrefix;
    }
}
