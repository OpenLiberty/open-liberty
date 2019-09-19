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
package com.ibm.ws.microprofile.graphql.component;

import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaPrinter;

import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.metadata.strategy.query.AnnotatedResolverBuilder;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIServiceUtils;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.microprofile.graphql.internal.MPDefaultInclusionStrategy;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.logging.Introspector;


@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "api.classes=org.eclipse.microprofile.graphql.GraphQLApi",
                        "bean.defining.annotations=org.eclipse.microprofile.graphql.GraphQLApi",
                        "service.vendor=IBM" })
public class GraphQLExtension implements Extension, WebSphereCDIExtension, Introspector {
    private final static TraceComponent tc = Tr.register(GraphQLExtension.class);
    private final static boolean metricsEnabled = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
        return "true".equalsIgnoreCase(System.getProperty("com.ibm.ws.microprofile.graphql.enable.metrics", "true"));
    });

    private static final Map<ModuleMetaData, Set<Bean<?>>> graphQLComponents = new WeakHashMap<>();
    private static final Map<ModuleMetaData, GraphQLSchema> graphQLSchemas = new WeakHashMap<>();

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        //register the interceptor binding and in the interceptor itself
        AnnotatedType<GraphQLApi> bindingType = beanManager.createAnnotatedType(GraphQLApi.class);
        beforeBeanDiscovery.addInterceptorBinding(bindingType);
        AnnotatedType<TracingInterceptor> interceptorType = beanManager.createAnnotatedType(TracingInterceptor.class);
        beforeBeanDiscovery.addAnnotatedType(interceptorType, CDIServiceUtils.getAnnotatedTypeIdentifier(interceptorType, this.getClass()));

        if (canLoad("com.ibm.ws.microprofile.metrics.cdi.producer.MetricRegistryFactory") && metricsEnabled) {
            AnnotatedType<MetricsInterceptor> metricsInterceptorType = beanManager.createAnnotatedType(MetricsInterceptor.class);
            beforeBeanDiscovery.addAnnotatedType(metricsInterceptorType, CDIServiceUtils.getAnnotatedTypeIdentifier(metricsInterceptorType, this.getClass()));
        }
    }

    public <X> void detectGraphQLComponent(@Observes ProcessBean<X> event) {
        if (event.getAnnotated().isAnnotationPresent(GraphQLApi.class)) {
            Bean<?> bean = event.getBean();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "adding GraphQLApi bean: " + bean);
            }
            //TODO: add interceptor(s) set via DS - i.e. metric capturing interceptor
            ModuleMetaData mmd = getModuleMetaData();
            graphQLComponents.computeIfAbsent(mmd, k -> {
                return new HashSet<>();
            });
            graphQLComponents.get(mmd).add(bean);
        }
    }

    static GraphQLSchema createSchema(BeanManager beanManager) {
        ModuleMetaData mmd = getModuleMetaData();
        Set<Bean<?>> beans = graphQLComponents.get(getModuleMetaData());
        if (beans == null || beans.size() < 1) {
            return null;
        }

        GraphQLSchemaGenerator schemaGen = new GraphQLSchemaGenerator()
                        .withResolverBuilders(new AnnotatedResolverBuilder())
                        .withValueMapperFactory(new JacksonValueMapperFactory())
                        .withInclusionStrategy(new MPDefaultInclusionStrategy())
                        .withResolverInterceptors(new PartialResultsResolverInterceptor())
                        .withOutputConverters(new DataFetcherResultOutputConverter());

        for (Bean<?> bean : beans) {
            schemaGen.withOperationsFromSingleton(
                beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean)), 
                bean.getBeanClass());
        }

        GraphQLSchema schema = schemaGen.generate();
        synchronized (graphQLSchemas) {
            graphQLSchemas.put(mmd, schema);
        }
        return schema;
    }

//    static Set<Bean<?>> getGraphQLComponents() {
//        Set<Bean<?>> set = graphQLComponents.get(getModuleMetaData());
//        return set == null ? Collections.emptySet() : set;
//    }

    static ModuleMetaData getModuleMetaData() {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        ModuleMetaData mmd = cmd.getModuleMetaData();
        return mmd;
    }

    private boolean canLoad(String className) {
        try {
            return Class.forName(className) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    // Introspector methods:
    @Override
    public String getIntrospectorName() {
        return "GraphQLIntrospector";
    }

    @Override
    public String getIntrospectorDescription() {
        return "Per-app dump of GraphQL schemas and endpoints";
    }

    @Override
    public void introspect(PrintWriter out) throws Exception {
        out.println("Schemas:");
        synchronized (graphQLSchemas) {
            graphQLSchemas.forEach((k, v) -> {
                out.println("*  Module: " + k.getJ2EEName());
                out.println();
                out.println(new SchemaPrinter().print(v));
            });
        }
    }
}
