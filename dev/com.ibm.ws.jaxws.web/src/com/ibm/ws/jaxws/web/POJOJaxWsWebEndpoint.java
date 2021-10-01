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
package com.ibm.ws.jaxws.web;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.xml.ws.Binding;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.ImplBeanCustomizer;
import com.ibm.ws.jaxws.endpoint.AbstractJaxWsWebEndpoint;
import com.ibm.ws.jaxws.endpoint.JaxWsPublisherContext;
import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.ws.jaxws.support.JaxWsInstanceManager;
import com.ibm.ws.jaxws.support.JaxWsInstanceManager.InterceptException;
import com.ibm.ws.jaxws.support.LibertyJaxWsImplementorInfo;
import com.ibm.ws.jaxws.support.LibertyJaxWsServerFactoryBean;
import com.ibm.ws.jaxws.support.LibertyJaxWsServiceFactoryBean;
import com.ibm.wsspi.adaptable.module.Container;

/**
 * JaxWsWebEndpoint for an POJO Web service endpoint
 */
public class POJOJaxWsWebEndpoint extends AbstractJaxWsWebEndpoint {

    private final static TraceComponent tc = Tr.register(POJOJaxWsWebEndpoint.class);

    private final JaxWsPublisherContext publisherContext;

    public POJOJaxWsWebEndpoint(EndpointInfo endpointInfo, JaxWsPublisherContext context) {
        super(endpointInfo, context.getModuleMetaData());
        this.publisherContext = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        Class<?> implBeanClass = null;
        LibertyJaxWsImplementorInfo implInfo = null;
        try {
            implBeanClass = jaxWsModuleMetaData.getModuleInfo().getClassLoader().loadClass(endpointInfo.getImplBeanClassName());

            implInfo = new LibertyJaxWsImplementorInfo(implBeanClass, endpointInfo, publisherContext);
        } catch (ClassNotFoundException e) {
            throw new ServletException(e);
        }

        Bus serverBus = jaxWsModuleMetaData.getServerMetaData().getServerBus();

        Object implementor = null;
        try {
            ImplBeanCustomizer implBeanCustomizer = (ImplBeanCustomizer) publisherContext.getAttribute("ImplBeanCustomizer");
            if (implBeanCustomizer != null) {
                Container container = jaxWsModuleMetaData.getModuleContainer();
                implementor = implBeanCustomizer.onPrepareImplBean(implBeanClass, container);
            }
            if (implementor == null) {
                implementor = jaxWsModuleMetaData.getJaxWsInstanceManager().createInstance(implBeanClass);
            }
        } catch (InstantiationException e) {
            throw new ServletException(e);
        } catch (IllegalAccessException e) {
            throw new ServletException(e);
        } catch (InterceptException e) {
            throw new ServletException(e);
        }

        Invoker jaxWsMethodInvoker = new POJOJAXWSMethodInvoker(implementor);

        JaxWsServiceFactoryBean serviceFactory = new LibertyJaxWsServiceFactoryBean(implInfo, publisherContext);

        LibertyJaxWsServerFactoryBean jaxWsServerFactory = new LibertyJaxWsServerFactoryBean(serviceFactory);

        serviceFactory.setBus(serverBus);
        serviceFactory.setInvoker(jaxWsMethodInvoker);
        serviceFactory.setFeatures(jaxWsServerFactory.getFeatures());
        serviceFactory.create();

        jaxWsServerFactory.setBus(serverBus);
        jaxWsServerFactory.setAddress(endpointInfo.getAddress(0));
        jaxWsServerFactory.setStart(false);
        jaxWsServerFactory.setServiceBean(implementor);
        jaxWsServerFactory.setInvoker(jaxWsMethodInvoker);

        server = jaxWsServerFactory.create();
        configureEndpointInfoProperties(endpointInfo, server.getEndpoint().getEndpointInfo());

        // Get binding instance
        Binding binding = ((JaxWsEndpointImpl) server.getEndpoint()).getJaxwsBinding();
        // Bind handlers to server
        binding.setHandlerChain(jaxWsServerFactory.createHandlers(endpointInfo, jaxWsModuleMetaData.getJaxWsInstanceManager()));

        // Customize WSDL Get Interceptor.
        customizeWSDLGetInterceptor(implBeanClass);
        enableLogging(endpointInfo);

        server.start();

        this.destination = (AbstractHTTPDestination) server.getDestination();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        //preDestroy service object
        invokePOJOPreDestroy();
        //preDestroy handlers
        this.invokePreDestroy();
        //destroy server
        this.server.destroy();
    }

    /**
     * invokePOJOPreDestroy
     */
    private void invokePOJOPreDestroy() {
        POJOJAXWSMethodInvoker tempInvoker = (POJOJAXWSMethodInvoker) server.getEndpoint().getService().getInvoker();
        Object instance = tempInvoker.getServiceObject();
        JaxWsInstanceManager jaxWsInstanceManager = jaxWsModuleMetaData.getJaxWsInstanceManager();

        try {
            jaxWsInstanceManager.destroyInstance(instance);
        } catch (InterceptException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "invoke POJO service PreDestroy fails:" + e.toString());
            }
        }

    }
}
