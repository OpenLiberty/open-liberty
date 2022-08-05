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
package com.ibm.ws.jaxws.ejb;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.xml.ws.Binding;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.JAXWSMethodDispatcher;
import org.apache.cxf.jaxws.handler.logical.LogicalHandlerInInterceptor;
import org.apache.cxf.jaxws.handler.soap.SOAPHandlerInterceptor;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.osgi.EJBContainer;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxws.endpoint.AbstractJaxWsWebEndpoint;
import com.ibm.ws.jaxws.endpoint.JaxWsPublisherContext;
import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.ws.jaxws.support.LibertyJaxWsImplementorInfo;
import com.ibm.ws.jaxws.support.LibertyJaxWsServerFactoryBean;
import com.ibm.ws.jaxws.support.LibertyJaxWsServiceFactoryBean;

/**
 * JaxWsWebEndpoint for an EJB based Web service endpoint
 */
@SuppressWarnings("deprecation")
public class EJBJaxWsWebEndpoint extends AbstractJaxWsWebEndpoint {

    private static final TraceComponent tc = Tr.register(EJBJaxWsWebEndpoint.class);

    private final EJBContainer ejbContainer;

    private final JaxWsPublisherContext publisherContext;

    private LibertyJaxWsServerFactoryBean jaxWsServerFactory;

    public EJBJaxWsWebEndpoint(EndpointInfo endpointInfo, JaxWsPublisherContext context, EJBContainer ejbContainer) {
        super(endpointInfo, context.getModuleMetaData());
        this.ejbContainer = ejbContainer;
        this.publisherContext = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        Class<?> implBeanClass;
        LibertyJaxWsImplementorInfo implInfo = null;
        try {
            implBeanClass = jaxWsModuleMetaData.getModuleInfo().getClassLoader().loadClass(endpointInfo.getImplBeanClassName());
            implInfo = new LibertyJaxWsImplementorInfo(implBeanClass, endpointInfo, publisherContext);
        } catch (ClassNotFoundException e) {
            throw new ServletException(e);
        }

        Bus serverBus = jaxWsModuleMetaData.getServerMetaData().getServerBus();

        JaxWsServiceFactoryBean serviceFactory = new LibertyJaxWsServiceFactoryBean(implInfo, publisherContext);
        jaxWsServerFactory = new LibertyJaxWsServerFactoryBean(serviceFactory);

        Invoker ejbMethodInvoker = new EJBMethodInvoker();

        serviceFactory.setBus(serverBus);
        serviceFactory.setInvoker(ejbMethodInvoker);
        serviceFactory.setFeatures(jaxWsServerFactory.getFeatures());
        serviceFactory.create();

        Service service = serviceFactory.create();

        jaxWsServerFactory.setBus(serverBus);
        jaxWsServerFactory.setAddress(endpointInfo.getAddress(0));
        jaxWsServerFactory.setStart(false);
        jaxWsServerFactory.setServiceBean(implBeanClass);
        jaxWsServerFactory.setInvoker(ejbMethodInvoker);

        server = jaxWsServerFactory.create();

        //Config the server side properties
        configureEndpointInfoProperties(endpointInfo, server.getEndpoint().getEndpointInfo());

        List<Interceptor<? extends Message>> inInterceptors = server.getEndpoint().getInInterceptors();
        List<Interceptor<? extends Message>> outInterceptors = server.getEndpoint().getOutInterceptors();

        //Initialize EJBMethodInvoker
        J2EEName j2EEName = jaxWsModuleMetaData.getServerMetaData().getEndpointJ2EEName(endpointInfo.getPortLink());
        if (implInfo.isWebServiceProvider()) {
            //add ejb pre-invoke interceptor
            EJBPreInvokeInterceptor ejbPreInvokeInterceptor = new EJBPreInvokeInterceptor(j2EEName, implBeanClass, ejbContainer, null);
            ejbPreInvokeInterceptor.setEjbJaxWsWebEndpoint(this);

            inInterceptors.add(ejbPreInvokeInterceptor);
            outInterceptors.add(new EJBPostInvokeInterceptor());
        } else {
            if (service == null) {
                service = serviceFactory.getService();
            }
            org.apache.cxf.service.model.EndpointInfo cxfEndpointInfo = service.getEndpointInfo(endpointInfo.getWsdlPort());

            String methodDispatcherClassName = getMethodDispatcherClassNameFromClassLoader();

            JAXWSMethodDispatcher methodDispatcher = (JAXWSMethodDispatcher) service.get(methodDispatcherClassName);
            List<Method> methods = new ArrayList<Method>(cxfEndpointInfo.getBinding().getOperations().size());
            for (BindingOperationInfo bindingOperationInfo : cxfEndpointInfo.getBinding().getOperations()) {
                Method method = methodDispatcher.getMethod(bindingOperationInfo);
                if (method != null) {
                    methods.add(method);
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unable to get method for binding operation info " + bindingOperationInfo.getName());
                    }
                }
            }
            //add ejb pre-invoke interceptor
            EJBPreInvokeInterceptor ejbPreInvokeInterceptor = new EJBPreInvokeInterceptor(j2EEName, implBeanClass, ejbContainer, methods);
            ejbPreInvokeInterceptor.setEjbJaxWsWebEndpoint(this);

            inInterceptors.add(ejbPreInvokeInterceptor);

            outInterceptors.add(new EJBPostInvokeInterceptor());
        }

        // Delay the handler creation in EJBPreInvokeInterceptor, so not like POJOJaxWsWebEndpoint create handler here.

        // Customize WSDL Get Interceptor.
        customizeWSDLGetInterceptor(implBeanClass);
        enableLogging(endpointInfo);

        server.start();

        this.destination = (AbstractHTTPDestination) server.getDestination();

    }

    /*
     * TODO: Investigate a better way to check versions for jaxws-2.2/jaxws-2.3.
     * Because of a package name change in CXF for the MethodDispatcher class, we need to look up what class name to use
     * in order to keep the EJBJaxWsWebEndpoint common between jaxws-2.2 and jaxws-2.3
     */
    @FFDCIgnore(ClassNotFoundException.class)
    private String getMethodDispatcherClassNameFromClassLoader() {
        Class<?> MethodDispatcherClass = null;
        try {
            MethodDispatcherClass = Class.forName("org.apache.cxf.frontend.MethodDispatcher");
        } catch (ClassNotFoundException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to load org.apache.cxf.frontend.MethodDispatcher");
            }
            try {
                MethodDispatcherClass = Class.forName("org.apache.cxf.service.invoker.MethodDispatcher");
            } catch (ClassNotFoundException e1) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to load org.apache.cxf.service.invoker.MethodDispatcher ");
                }
            }
        }
        return MethodDispatcherClass.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        super.destroy();
    }

    public void initializeHandlers(SoapMessage message) {
        // Get binding instanc
        Binding binding = ((JaxWsEndpointImpl) server.getEndpoint()).getJaxwsBinding();
        // Bind handlers to server
        binding.setHandlerChain(jaxWsServerFactory.createHandlers(endpointInfo, jaxWsModuleMetaData.getJaxWsInstanceManager()));

        // this method is called from EJBPreInvokeInterceptor, and only called when the first time invoke the ejb based web service.
        // because we just do the setHandlerChain, so the handler interceptor is not added to the interceptors chain of the first invoke.
        for (Interceptor<? extends Message> iterceptor : server.getEndpoint().getInInterceptors()) {
            if (iterceptor instanceof SOAPHandlerInterceptor || iterceptor instanceof LogicalHandlerInInterceptor) {
                message.getInterceptorChain().add(iterceptor);
            }
        }
    }
}
