/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.endpoint;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;

import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.InvalidCharsetException;
import org.apache.cxf.transport.servlet.BaseUrlHelper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.JaxRsRuntimeException;
import com.ibm.ws.jaxrs20.api.JaxRsProviderFactoryService;
import com.ibm.ws.jaxrs20.metadata.EndpointInfo;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleMetaData;
//import javax.xml.ws.handler.Handler;
//import org.apache.cxf.frontend.WSDLGetInterceptor;
//import org.apache.cxf.frontend.WSDLGetUtils;
//import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
//import com.ibm.ws.jaxrs.support.LibertyJaxRsCompatibleWSDLGetInterceptor;

public abstract class AbstractJaxRsWebEndpoint implements JaxRsWebEndpoint {

    private static final TraceComponent tc = Tr.register(AbstractJaxRsWebEndpoint.class);

//    public static final String DISABLE_ADDRESS_UPDATES = "disable-address-updates";

//    public static final String BASE_ADDRESS = "base-address";

    private static final String HTTP_PREFIX = "http";

    private static final String SET_JAXB_VALIDATION_EVENT_HANDLER = "set-jaxb-validation-event-handler";

    protected final EndpointInfo endpointInfo;

    protected final JaxRsModuleMetaData jaxRsModuleMetaData;

    protected ServletConfig servletConfig;

    protected AbstractHTTPDestination destination;

//    protected boolean disableAddressUpdates = true;

//
//    protected String forcedBaseAddress;

    public AbstractJaxRsWebEndpoint(EndpointInfo endpointInfo, JaxRsModuleMetaData jaxWsModuleMetaData) {
        this.endpointInfo = endpointInfo;
        this.jaxRsModuleMetaData = jaxWsModuleMetaData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(ServletConfig servletConfig, JaxRsProviderFactoryService jaxRsProviderFactoryService) throws ServletException {
        this.servletConfig = servletConfig;

//        String isDisableAddressUpdates = servletConfig.getInitParameter(DISABLE_ADDRESS_UPDATES);
//        if (!StringUtils.isEmpty(isDisableAddressUpdates)) {
//            this.disableAddressUpdates = Boolean.valueOf(isDisableAddressUpdates);
//        }

//        String isForcedBaseAddress = servletConfig.getInitParameter(BASE_ADDRESS);
//        if (!StringUtils.isEmpty(isForcedBaseAddress)) {
//            this.forcedBaseAddress = isForcedBaseAddress;
//        }
    }

    /**
     * Customize WSDL Intercepter when no need to generate WSDL.
     */
//    protected void customizeWSDLGetInterceptor(Class<?> implBeanClass) {
//        String wsdlLocation = endpointInfo.getWsdlLocation();
//        URL wsdlUrl = JaxWsUtils.resolve(wsdlLocation, jaxWsModuleMetaData.getModuleContainer());
//        boolean wsdlLocationExisted = true;
//        boolean wsdlLocationEmpty = StringUtils.isEmpty(wsdlLocation);
//        if (!wsdlLocationEmpty && wsdlUrl == null) {
//            wsdlLocationExisted = false;
//        }
//        if (!wsdlLocationEmpty && wsdlLocationExisted) {
//            return;
//        }
//        String protocolBinding = endpointInfo.getProtocolBinding();
//        if (!JaxWsUtils.isWSDLGenerationSupported(protocolBinding) || !wsdlLocationExisted) {
//            List<Interceptor<? extends Message>> inInterceptors = server.getEndpoint().getInInterceptors();
//            inInterceptors.remove(WSDLGetInterceptor.INSTANCE);
//            inInterceptors.add(new LibertyJaxWsCompatibleWSDLGetInterceptor(implBeanClass.getCanonicalName(), wsdlLocation, wsdlLocationExisted));
//        }
//    }

//    protected void customizeLoggingInOutIntercetptor(EndpointInfo libertyEndpointInfo) {
//        Map<String, String> endpointProperties = libertyEndpointInfo.getEndpointProperties();
//        if (null != endpointProperties && Boolean.valueOf(endpointProperties.get(JaxRsConstants.ENABLE_lOGGINGINOUTINTERCEPTOR))) {
//            List<Interceptor<? extends Message>> inInterceptors = server.getEndpoint().getInInterceptors();
//            inInterceptors.add(new LoggingInInterceptor());
//            List<Interceptor<? extends Message>> outInterceptors = server.getEndpoint().getOutInterceptors();
//            outInterceptors.add(new LoggingOutInterceptor());
//        }
//    }

    public AbstractHTTPDestination getDestination() {
        return this.destination;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invoke(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        if (destination == null)
        {
            response.setStatus(404);
            return;
        }
        try {
            updateDestination(request);
            destination.invoke(servletConfig, servletConfig.getServletContext(), request, response);
        } catch (IOException e) {
            if (e instanceof InvalidCharsetException) {
                throw new BadRequestException(e);
            }
            throw new ServletException(e);
        } catch (JaxRsRuntimeException ex) {
            throw new ServletException(ex.getCause());
        }

    }

//    @SuppressWarnings("rawtypes")
//    protected void invokePreDestroy() {
//        // preDestroy handlers
//        List<Handler> handlers = ((JaxWsEndpointImpl) server.getEndpoint()).getJaxwsBinding().getHandlerChain();
//        JaxRsInstanceManager jaxWsInstanceManager = jaxRsModuleMetaData.getJaxRsInstanceManager();
//        for (Handler handler : handlers) {
//            try {
//                jaxWsInstanceManager.destroyInstance(handler);
//            } catch (InterceptException e) {
//                Tr.warning(tc, "warn.invoke.handler.predestory", handler.getClass().getName(), e.getMessage());
//            }
//        }
//    }

    /**
     * Configure common endpoint properties
     *
     * @param endpointInfo
     */
    protected void configureEndpointInfoProperties(EndpointInfo libertyEndpointInfo, org.apache.cxf.service.model.EndpointInfo cxfEndpointInfo) {

        //Disable jaxb validation event handler, as IBM FastPath does not support this, which will finally fallback to RI unmarshall
        cxfEndpointInfo.setProperty(SET_JAXB_VALIDATION_EVENT_HANDLER, false);

//        //Set autoRewriteSoapAddressForAllServices with true by default, which will override all the services in the target WSDL file
//        cxfEndpointInfo.setProperty(WSDLGetUtils.AUTO_REWRITE_ADDRESS_ALL, true);

        //Set WSDL_DESCRIPTION property
//        try {
//            String wsdlLocation = libertyEndpointInfo.getWsdlLocation();
//            if (wsdlLocation != null && !(wsdlLocation.isEmpty())) {
//                URI wsdlDescription = new URI(wsdlLocation);
//                cxfEndpointInfo.setProperty("URI", wsdlDescription);
//            }
//        } catch (URISyntaxException e) {
//            //donothing
//        }
        Map<String, String> endpointProperties = libertyEndpointInfo.getEndpointProperties();
        if (endpointProperties != null && !endpointProperties.isEmpty()) {
            for (Entry<String, String> entry : endpointProperties.entrySet()) {
                cxfEndpointInfo.setProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    protected void updateDestination(HttpServletRequest request) {

        String base = getBaseURL(request);
        synchronized (destination) {   //moved up for OLGH3669

            String ad = null;
            if (destination.getAddress() != null
                && destination.getAddress().getAddress() != null) {
                ad = destination.getAddress().getAddress().getValue();
                if (ad == null) {
                    ad = "/";
                }
            }

            if (ad != null && !ad.startsWith(HTTP_PREFIX)) {
                String combined = "";
                if (!base.endsWith("/") && !ad.startsWith("/")) {
                    combined = base + "/" + ad;
                }
                else {
                    combined = base + ad;
                }

                /**
                 * only bind the real address once
                 */
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "EndpointInfo address = " + destination.getEndpointInfo().getAddress());
                    Tr.debug(tc, "Base + address = " + combined);
                }

                BaseUrlHelper.setAddress(destination, combined);
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
//        if (forcedBaseAddress != null) {
//            return forcedBaseAddress;
//        }

        String reqPrefix = request.getRequestURL().toString();
        //Liberty code change start
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "";
        }
        //Liberty code change end

        //fix for CXF-898
        if (!"/".equals(pathInfo) || reqPrefix.endsWith("/")) {
            StringBuilder sb = new StringBuilder();
            // request.getScheme(), request.getLocalName() and request.getLocalPort()
            // should be marginally cheaper - provided request.getLocalName() does
            // return the actual name used in request URI as opposed to localhost
            // consistently across the Servlet stacks

            //Liberty code change start
            String[] uri = HttpUtils.parseURI(reqPrefix, true);
            if (uri == null) {
                throw new IllegalArgumentException(reqPrefix + " contains illegal arguments");
            }
            sb.append(uri[0]).append("://").append(uri[1]);
            //Liberty code change end

            //No servletPath will be appended, as in Liberty, each endpoint will be served by one servlet instance
            sb.append(request.getContextPath());

            reqPrefix = sb.toString();
        }
        return reqPrefix;
    }

    @Override
    public void setEndpointInfoAddress(String add) {

        // Remove "*" from ending
        if (add.endsWith("/*")) {
            add = add.substring(0, (add.length() - 1));
        }

        // Add leading "/" if does not exist
        if (!add.startsWith("/")) {
            add = "/" + add;
        }

        this.endpointInfo.setAddress(add);
    };

}