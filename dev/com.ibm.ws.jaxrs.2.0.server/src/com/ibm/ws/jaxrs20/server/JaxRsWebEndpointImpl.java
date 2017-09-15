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
package com.ibm.ws.jaxrs20.server;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.ws.rs.core.Application;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer;
import com.ibm.ws.jaxrs20.api.JaxRsProviderFactoryService;
import com.ibm.ws.jaxrs20.endpoint.AbstractJaxRsWebEndpoint;
import com.ibm.ws.jaxrs20.endpoint.JaxRsPublisherContext;
import com.ibm.ws.jaxrs20.metadata.EndpointInfo;
import com.ibm.ws.jaxrs20.metadata.ProviderResourceInfo;
import com.ibm.ws.jaxrs20.metadata.ProviderResourceInfo.RuntimeType;
import com.ibm.ws.jaxrs20.server.internal.JaxRsServerConstants;

/**
 * JaxWsWebEndpoint for an POJO Web service endpoint
 */
public class JaxRsWebEndpointImpl extends AbstractJaxRsWebEndpoint {

    private Server server = null;
    private final Set<JaxRsFactoryBeanCustomizer> beanCustomizers;
    private final static TraceComponent tc = Tr.register(JaxRsWebEndpointImpl.class);
    private final List<Feature> features;

    @SuppressWarnings("unchecked")
    public JaxRsWebEndpointImpl(EndpointInfo endpointInfo, JaxRsPublisherContext context, List<Feature> features) {
        super(endpointInfo, context.getModuleMetaData());
        beanCustomizers = (Set<JaxRsFactoryBeanCustomizer>) context.getAttribute(JaxRsServerConstants.BEAN_CUSTOMIZER);
        this.features = features;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(ServletConfig servletConfig, JaxRsProviderFactoryService providerFactoryService) throws ServletException {
        super.init(servletConfig, providerFactoryService);

        LibertyJaxRsServerFactoryBean jaxRsServerFactory = new LibertyJaxRsServerFactoryBean(endpointInfo, jaxRsModuleMetaData, beanCustomizers, servletConfig, providerFactoryService);
        if (features != null && !features.isEmpty()) {
            jaxRsServerFactory.setFeatures(features);
        }
        jaxRsServerFactory.doInit();

        ClassLoader origClassLoader = jaxRsServerFactory.getBus().getExtension(ClassLoader.class);
        jaxRsServerFactory.getBus().setExtension(null, ClassLoader.class);
        server = jaxRsServerFactory.create();
        jaxRsServerFactory.getBus().setExtension(origClassLoader, ClassLoader.class);

        configureEndpointInfoProperties(endpointInfo, server.getEndpoint().getEndpointInfo());

        server.start();
        destination = (AbstractHTTPDestination) server.getDestination();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {

        /**
         * call @PreDestroy for POJO resource & provider(default constructor)
         */
        Set<ProviderResourceInfo> singletonProviderAndPathInfos = endpointInfo.getSingletonProviderAndPathInfos();
        for (ProviderResourceInfo o : singletonProviderAndPathInfos) {

            if (o.getRuntimeType() == RuntimeType.POJO) {
                Method preDestoryMethod = ResourceUtils.findPreDestroyMethod(o.getProviderResourceClass());
                InjectionUtils.invokeLifeCycleMethod(o.getObject(), preDestoryMethod);
            }

        }

        /**
         * call @PreDestroy for provider(param constructor)
         */
        Set<ProviderResourceInfo> perRequestProviderAndPathInfos = endpointInfo.getPerRequestProviderAndPathInfos();
        for (ProviderResourceInfo o : perRequestProviderAndPathInfos) {

            if (o.getRuntimeType() == RuntimeType.POJO && o.isJaxRsProvider() == true) {
                Method preDestoryMethod = ResourceUtils.findPreDestroyMethod(o.getProviderResourceClass());
                InjectionUtils.invokeLifeCycleMethod(o.getObject(), preDestoryMethod);
            }

        }

        //call preDestroy method for non-customized Applications
        if (!endpointInfo.isCustomizedApp()) {
            Application app = endpointInfo.getApp();
            Method preDestoryMethod = ResourceUtils.findPreDestroyMethod(app.getClass());
            InjectionUtils.invokeLifeCycleMethod(app, preDestoryMethod);

        } else {

            for (JaxRsFactoryBeanCustomizer beanCustomizer : beanCustomizers) {
                beanCustomizer.destroyApplicationScopeResources(jaxRsModuleMetaData);
            }
        }

        if (server != null) {
            server.destroy();
        }
        if (features != null) {
            features.clear();
        }
    }

}
