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
package com.ibm.ws.jaxws.endpoint;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.handler.Handler;

import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.WSDLGetInterceptor;
import org.apache.cxf.frontend.WSDLGetUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.servlet.BaseUrlHelper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.JaxWsConstants;
import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.ws.jaxws.metadata.JaxWsModuleMetaData;
import com.ibm.ws.jaxws.support.JaxWsInstanceManager;
import com.ibm.ws.jaxws.support.JaxWsInstanceManager.InterceptException;
import com.ibm.ws.jaxws.support.LibertyJaxWsCompatibleWSDLGetInterceptor;
import com.ibm.ws.jaxws.utils.JaxWsUtils;
import com.ibm.ws.jaxws.utils.StringUtils;

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
        if (!wsdlLocationEmpty && wsdlUrl == null)
        {
            wsdlLocationExisted = false;
            OASISCatalogManager catalogManager = jaxWsModuleMetaData.getServerMetaData().getServerBus().getExtension(OASISCatalogManager.class);
            String resolvedLocation = null;
            if (catalogManager != null)
            {

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
            if (resolvedLocation != null)
            {
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

    protected void customizeLoggingInOutIntercetptor(EndpointInfo libertyEndpointInfo) {
        Map<String, String> endpointProperties = libertyEndpointInfo.getEndpointProperties();
        if (null != endpointProperties && Boolean.valueOf(endpointProperties.get(JaxWsConstants.ENABLE_lOGGINGINOUTINTERCEPTOR))) {
            List<Interceptor<? extends Message>> inInterceptors = server.getEndpoint().getInInterceptors();
            inInterceptors.add(new LoggingInInterceptor());
            List<Interceptor<? extends Message>> outInterceptors = server.getEndpoint().getOutInterceptors();
            outInterceptors.add(new LoggingOutInterceptor());
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
    public void invoke(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {
            updateDestination(request);
            destination.invoke(servletConfig, servletConfig.getServletContext(), request, response);
        } catch (IOException e) {
            throw new ServletException(e);
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

        Tr.info(tc, "@TJJ in updateDestination");
        String ad = destination.getEndpointInfo().getAddress();
        if (ad != null && ad.startsWith(HTTP_PREFIX)) {
            Tr.info(tc, "@TJJ ad != null && ad.startsWith(HTTP_PREFIX) ad = " + ad );
            return;
        }

        synchronized (destination) {
            ad = destination.getEndpointInfo().getAddress();
            if (ad == null
                && destination.getAddress() != null
                && destination.getAddress().getAddress() != null) {
                ad = destination.getAddress().getAddress().getValue();
                if (ad == null) {
                    Tr.info(tc, "@TJJ ad == nul ad = " + ad );
                    ad = "/";
                }
            }

            if (ad != null && !ad.startsWith(HTTP_PREFIX)) {
                String base = getBaseURL(request);
                if (disableAddressUpdates) {
                    
                    request.setAttribute("org.apache.cxf.transport.endpoint.address",
                                         base + ad);
                } else {
                    if(destination.getEndpointInfo().getAddress() == null) {
                      
                        BaseUrlHelper.setAddress(destination, base + ad);
                    }
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