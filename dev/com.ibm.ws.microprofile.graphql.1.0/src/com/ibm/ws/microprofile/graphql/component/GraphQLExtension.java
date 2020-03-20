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
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;

import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.idl.SchemaPrinter;

import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.mapping.TypeMapperRegistry;
import io.leangen.graphql.metadata.strategy.query.AnnotatedResolverBuilder;
import io.leangen.graphql.metadata.strategy.value.jsonb.JsonbValueMapperFactory;

import org.eclipse.microprofile.graphql.Enum;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Input;
import org.eclipse.microprofile.graphql.Type;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIServiceUtils;
import com.ibm.ws.microprofile.graphql.internal.MPDateTimeScalarMapperExtension;
import com.ibm.ws.microprofile.graphql.internal.MPDefaultInclusionStrategy;
import com.ibm.ws.microprofile.graphql.internal.MPOperationBuilder;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.cdi.extension.WebSphereCDIExtension;
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

        if (MetricsInterceptor.metrics != null && metricsEnabled) {
            AnnotatedType<MetricsInterceptor> metricsInterceptorType = beanManager.createAnnotatedType(MetricsInterceptor.class);
            beforeBeanDiscovery.addAnnotatedType(metricsInterceptorType, CDIServiceUtils.getAnnotatedTypeIdentifier(metricsInterceptorType, this.getClass()));
        }
    }

    public <X> void detectGraphQLComponent(@Observes ProcessBean<X> event) {
        Annotated annotated = event.getAnnotated();
        Bean<?> bean = event.getBean();
        if (annotated.isAnnotationPresent(GraphQLApi.class)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "adding GraphQLApi bean: " + bean);
            }
            addBeanToMMDMap(bean, graphQLComponents);
        }
    }

    private static void addBeanToMMDMap(Bean<?> bean, Map<ModuleMetaData, Set<Bean<?>>> map) {
        //TODO: add interceptor(s) set via DS - i.e. metric capturing interceptor
        ModuleMetaData mmd = getModuleMetaData();
        map.computeIfAbsent(mmd, k -> {
            return new HashSet<>();
        });
        map.get(mmd).add(bean);
    }

    static GraphQLSchema createSchema(BeanManager beanManager) {
        ModuleMetaData mmd = getModuleMetaData();
        Set<Bean<?>> beans = graphQLComponents.get(mmd);
        if (beans == null || beans.size() < 1) {
            return null;
        }

        JsonbValueMapperFactory jsonbValueMapperFactory = JsonbValueMapperFactory.instance();
        GraphQLSchemaGenerator schemaGen = new GraphQLSchemaGenerator()
                        .withResolverBuilders(new AnnotatedResolverBuilder())
                        .withValueMapperFactory(jsonbValueMapperFactory)
                        .withInclusionStrategy(new MPDefaultInclusionStrategy())
                        .withResolverInterceptors(new PartialResultsResolverInterceptor())
                        .withOutputConverters(new DataFetcherResultOutputConverter())
                        .withOperationBuilder(new MPOperationBuilder())
                        .withScalarMapperExtensions(new MPDateTimeScalarMapperExtension());

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

    static ModuleMetaData getModuleMetaData() {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        ModuleMetaData mmd = cmd.getModuleMetaData();
        return mmd;
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
