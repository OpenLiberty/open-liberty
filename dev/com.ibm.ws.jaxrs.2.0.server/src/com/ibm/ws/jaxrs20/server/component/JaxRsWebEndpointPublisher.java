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
package com.ibm.ws.jaxrs20.server.component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.cxf.feature.Feature;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.jaxrs.server.IBMRestServlet;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.JaxRsConstants;
import com.ibm.ws.jaxrs20.api.EndpointPublisher;
import com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer;
import com.ibm.ws.jaxrs20.api.JaxRsProviderFactoryService;
import com.ibm.ws.jaxrs20.endpoint.JaxRsPublisherContext;
import com.ibm.ws.jaxrs20.endpoint.JaxRsWebEndpoint;
import com.ibm.ws.jaxrs20.metadata.EndpointInfo;
import com.ibm.ws.jaxrs20.server.JaxRsWebEndpointImpl;
import com.ibm.ws.jaxrs20.server.deprecated.JaxRsExtensionProcessor;
import com.ibm.ws.jaxrs20.server.internal.JaxRsServerConstants;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppNameSpaceCollaborator;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 * Web endpoint publisher supports to publish the web services into web container.
 */
@Component(name = "WebEndpointPublisher", immediate = true, property = { "service.vendor=IBM" }, configurationPolicy = ConfigurationPolicy.IGNORE)
public class JaxRsWebEndpointPublisher implements EndpointPublisher {
    private static final TraceComponent tc = Tr.register(JaxRsWebEndpointPublisher.class);

    private final AtomicServiceReference<JaxRsProviderFactoryService> jaxRsProviderFactoryServiceSR = new AtomicServiceReference<JaxRsProviderFactoryService>(JaxRsConstants.PROVIDERfACTORY_REFERENCE_NAME);
    private final Set<JaxRsFactoryBeanCustomizer> beanCustomizers = new HashSet<JaxRsFactoryBeanCustomizer>();
    private final List<Feature> features = new CopyOnWriteArrayList<Feature>();

    @Reference(name = "jaxRsFactoryBeanCustomizer",
               service = JaxRsFactoryBeanCustomizer.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC)
    protected void registerJaxRsFactoryBeanCustomizer(JaxRsFactoryBeanCustomizer beanCustomizer) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "registerJaxRsFactoryBeanCustomizer");
        }
        beanCustomizers.add(beanCustomizer);
    }

    protected void unregisterJaxRsFactoryBeanCustomizer(JaxRsFactoryBeanCustomizer beanCustomizer) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "registerJaxRsFactoryBeanCustomizer");
        }
        beanCustomizers.remove(beanCustomizer);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void setFeature(Feature feature) {
        features.add(feature);
    }

    protected void unsetFeature(Feature feature) {
        features.remove(feature);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        jaxRsProviderFactoryServiceSR.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        jaxRsProviderFactoryServiceSR.deactivate(cc);
    }

    @Reference(name = JaxRsConstants.PROVIDERfACTORY_REFERENCE_NAME, service = JaxRsProviderFactoryService.class, cardinality = ReferenceCardinality.MANDATORY)
    protected void setJaxRsProviderFactoryService(ServiceReference<JaxRsProviderFactoryService> ref) {
        jaxRsProviderFactoryServiceSR.setReference(ref);
    }

    protected void unsetJaxRsProviderFactoryService(ServiceReference<JaxRsProviderFactoryService> ref) {
        jaxRsProviderFactoryServiceSR.unsetReference(ref);
    }

    @Override
    public void publish(EndpointInfo endpointInfo, JaxRsPublisherContext context) {

        IServletContext servletContext = context.getAttribute(JaxRsServerConstants.SERVLET_CONTEXT, IServletContext.class);
        if (servletContext == null) {
            throw new IllegalStateException("Unable to publish the endpoint to web container due to null web app instance");
        }

        context.setAttribute(JaxRsServerConstants.BEAN_CUSTOMIZER, beanCustomizers);
        JaxRsWebEndpoint jaxRsWebEndpoint = new JaxRsWebEndpointImpl(endpointInfo, context, new ArrayList<Feature>(features));

        Container moduleContainer = context.getPublisherModuleContainer();

        WebAppConfig webAppConfig;
        try {
            webAppConfig = moduleContainer.adapt(WebAppConfig.class);
        } catch (UnableToAdaptException e) {
            throw new IllegalStateException(e);
        }

        // Create servlet instance and register it.
        Servlet restServlet = new IBMRestServlet(jaxRsWebEndpoint, (IWebAppNameSpaceCollaborator) context.getAttribute(JaxRsConstants.COLLABORATOR), jaxRsProviderFactoryServiceSR.getServiceWithException());

        /**
         * check if there is already a servlet config in web.xml,
         * if yes, then just replace the servlet to the IＢＭ rest servlet
         * if no, create a servlet config with the IBM rest servlet
         */
        IServletConfig sconfig = webAppConfig.getServletInfo(endpointInfo.getServletName());

        if (sconfig == null) {
            // No servlet config specified in web.xml
            try {
                JaxRsExtensionProcessor processor = new JaxRsExtensionProcessor(servletContext);
                IServletConfig info = processor.createConfig(endpointInfo.getServletName());
                info.setServletName(endpointInfo.getServletName());
                info.setAsyncSupported(true);
                info.setServlet(restServlet);
                info.setServletClass(restServlet.getClass());
                webAppConfig.addServletInfo(endpointInfo.getServletName(), info);

                // If no existing mapping, add it according to ApplicationPath value
                // E.g.
                // <servlet>
                //   <servlet-name>com.ibm.sample.jaxrs.DemoApplication</servlet-name>
                // </servlet>
                if (info.getMappings() == null || info.getMappings().size() == 0) {
                    info.setServletContext(servletContext);
                    if (endpointInfo.getServletMappingUrl() != null) {
                        info.addMapping(endpointInfo.getServletMappingUrl());
                    } else {
                        info.addMapping(endpointInfo.getAppPath());
                    }
                }
            } catch (ServletException e) {
                // Dynamically adding new servlet failed. throw exception
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Servlet Exception when create a new servlet info:", e);
                }
            }

        } else {

            sconfig.setAsyncSupported(true);
            sconfig.setServlet(restServlet);
            sconfig.setServletClass(restServlet.getClass());
            if (sconfig.getServletWrapper() != null) {
                sconfig.getServletWrapper().setTarget(restServlet);
            }

            // If no existing mapping, add it according to ApplicationPath value
            // E.g.
            // <servlet>
            //   <servlet-name>sample2</servlet-name>
            //   <servlet-class>com.ibm.ws.jaxrs20.webcontainer.LibertyJaxRsServlet</servlet-class>
            //   <init-param>
            //     <param-name>javax.ws.rs.Application</param-name>
            //     <param-value>com.ibm.sample.jaxrs.DemoApplication</param-value>
            //   </init-param>
            // </servlet>
            if (sconfig.getMappings() == null || sconfig.getMappings().size() == 0) {
                sconfig.setServletContext(servletContext);
                sconfig.addMapping(endpointInfo.getAppPath());
            }
        }

        // Set endpoint address
        if (endpointInfo.getServletMappingUrl() != null) {
            jaxRsWebEndpoint.setEndpointInfoAddress(endpointInfo.getServletMappingUrl());
        } else if (endpointInfo.getAppPath() != null) {
            jaxRsWebEndpoint.setEndpointInfoAddress(endpointInfo.getAppPath());
        }

    }

    @Override
    public String getType() {
        return JaxRsConstants.WEB_ENDPOINT_PUBLISHER_TYPE;
    }

}
