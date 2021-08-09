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
package com.ibm.ws.jaxws.webcontainer;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.Servlet;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.jaxws.ImplBeanCustomizer;
import com.ibm.ws.jaxws.JaxWsConstants;
import com.ibm.ws.jaxws.endpoint.EndpointPublisher;
import com.ibm.ws.jaxws.endpoint.JaxWsEndpointConfigurator;
import com.ibm.ws.jaxws.endpoint.JaxWsEndpointConfiguratorManager;
import com.ibm.ws.jaxws.endpoint.JaxWsPublisherContext;
import com.ibm.ws.jaxws.endpoint.JaxWsWebEndpoint;
import com.ibm.ws.jaxws.metadata.EndpointInfo;
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
public class WebEndpointPublisher implements EndpointPublisher {

    protected final AtomicServiceReference<JaxWsEndpointConfiguratorManager> jaxWsEndpointConfiguratorManagerRef = new AtomicServiceReference<JaxWsEndpointConfiguratorManager>("jaxWsEndpointConfiguratorManager");

    private final Set<ImplBeanCustomizer> beanCustomizers = new HashSet<ImplBeanCustomizer>();

    public void setJaxWsEndpointConfiguratorManager(ServiceReference<JaxWsEndpointConfiguratorManager> ref) {
        jaxWsEndpointConfiguratorManagerRef.setReference(ref);
    }

    public void unsetJaxWsEndpointConfiguratorManager(ServiceReference<JaxWsEndpointConfiguratorManager> ref) {
        jaxWsEndpointConfiguratorManagerRef.setReference(null);
    }

    protected void activate(ComponentContext cc) {
        jaxWsEndpointConfiguratorManagerRef.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        jaxWsEndpointConfiguratorManagerRef.deactivate(cc);
    }

    protected void registerImplBeanCustomizer(ImplBeanCustomizer beanCustomizer) {
        beanCustomizers.add(beanCustomizer);
    }

    protected void unregisterImplBeanCustomizer(ImplBeanCustomizer beanCustomizer) {
        beanCustomizers.remove(beanCustomizer);
    }

    @Override
    public void publish(EndpointInfo endpointInfo, JaxWsPublisherContext context) {

        JaxWsEndpointConfiguratorManager jaxWsEndpointConfiguratorManager = jaxWsEndpointConfiguratorManagerRef.getServiceWithException();
        JaxWsEndpointConfigurator jaxWsEndpointConfigurator = jaxWsEndpointConfiguratorManager.getJaxWsEndpointConfigurator(endpointInfo.getEndpointType());

        if (jaxWsEndpointConfigurator == null) {
            throw new IllegalStateException("Unsupport endpoint type " + endpointInfo.getEndpointType());
        }

        Container moduleContainer = context.getPublisherModuleContainer();

        WebAppConfig webAppConfig;
        try {
            webAppConfig = moduleContainer.adapt(WebAppConfig.class);
        } catch (UnableToAdaptException e) {
            throw new IllegalStateException(e);
        }

        IServletContext servletContext = context.getAttribute(JaxWsWebContainerConstants.SERVLET_CONTEXT, IServletContext.class);
        if (servletContext == null) {
            throw new IllegalStateException("Unable to publish the endpoint to web container due to null web app instance");
        }

        for (ImplBeanCustomizer implBeanCustomizer : beanCustomizers)
        {
            if (implBeanCustomizer != null)
            {
                context.setAttribute("ImplBeanCustomizer", implBeanCustomizer);
                break;
            }
        }

        JaxWsWebEndpoint jaxWsWebEndpoint = jaxWsEndpointConfigurator.createWebEndpoint(endpointInfo, context);

        if (jaxWsWebEndpoint == null) {
            return;
        }

        IWebAppNameSpaceCollaborator nameSpaceCollaborator = context.getAttribute(JaxWsWebContainerConstants.NAMESPACE_COLLABORATOR, IWebAppNameSpaceCollaborator.class);
        Boolean useNameSpaceCollaboratorPropertyValue = jaxWsEndpointConfigurator.getEndpointProperty(JaxWsEndpointConfigurator.USE_NAMESPACE_COLLABORATOR, Boolean.class);
        boolean useNameSpaceCollaborator = useNameSpaceCollaboratorPropertyValue == null ? false : useNameSpaceCollaboratorPropertyValue.booleanValue();

        if (endpointInfo.isConfiguredInWebXml()) {
            String servletName = endpointInfo.getServletName();
            IServletConfig sconfig = webAppConfig.getServletInfo(servletName);
            Servlet newServlet = new LibertyJaxWsServlet(jaxWsWebEndpoint, useNameSpaceCollaborator ? nameSpaceCollaborator : null);
            sconfig.setClassName(newServlet.getClass().getName());
            sconfig.setServlet(newServlet);
            sconfig.setServletClass(newServlet.getClass());
            if (sconfig.getServletWrapper() != null) {
                sconfig.getServletWrapper().setTarget(newServlet);
            }
            if (sconfig.getMappings() == null || sconfig.getMappings().size() == 0) {
                sconfig.setServletContext(servletContext);
                sconfig.addMapping(endpointInfo.getAddresses());
            }
        } else {
            String servletName = normalizeServletName(endpointInfo.getImplBeanClassName(), webAppConfig);
            servletContext.addServlet(servletName, new LibertyJaxWsServlet(jaxWsWebEndpoint, useNameSpaceCollaborator ? nameSpaceCollaborator : null));
            for (String address : endpointInfo.getAddresses()) {
                webAppConfig.addServletMapping(servletName, address);
            }
        }
    }

    @Override
    public String getType() {
        return JaxWsConstants.WEB_ENDPOINT_PUBLISHER_TYPE;
    }

    private String normalizeServletName(String servletName, WebAppConfig webAppConfig) {
        if (webAppConfig.getServletInfo(servletName) == null) {
            return servletName;
        }

        StringBuilder servletNameBuilder = new StringBuilder(servletName);
        do {
            servletNameBuilder.append("_0");
        } while (webAppConfig.getServletInfo(servletNameBuilder.toString()) != null);

        return servletNameBuilder.toString();
    }
}
