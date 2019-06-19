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
import java.lang.annotation.Annotation;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
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

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIServiceUtils;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
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

    private static final Map<J2EEName, Set<Bean<?>>> graphQLComponents = new WeakHashMap<>();
    private static final Map<J2EEName, GraphQLSchema> graphQLSchemas = new WeakHashMap<>();
    private static final Map<J2EEName, Boolean> containsSecAnnotations = new WeakHashMap<>();

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        //register the interceptor binding and in the interceptor itself
        AnnotatedType<GraphQLApi> bindingType = beanManager.createAnnotatedType(GraphQLApi.class);
        beforeBeanDiscovery.addInterceptorBinding(bindingType);
        AnnotatedType<TracingInterceptor> interceptorType = beanManager.createAnnotatedType(TracingInterceptor.class);
        beforeBeanDiscovery.addAnnotatedType(interceptorType, CDIServiceUtils.getAnnotatedTypeIdentifier(interceptorType, this.getClass()));

        // only add the auth interceptor is security is enabled and app contains a class-level security annotation
        if (canLoad("com.ibm.ws.security.javaeesec.AuthContext")) {
            AnnotatedType<AuthInterceptor> authInterceptorType = beanManager.createAnnotatedType(AuthInterceptor.class);
            beforeBeanDiscovery.addAnnotatedType(authInterceptorType, CDIServiceUtils.getAnnotatedTypeIdentifier(interceptorType, this.getClass()));
        }

        // only add the metrics collection interceptor if metrics is enabled
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

            J2EEName mmdName = getModuleMetaDataName();
            graphQLComponents.computeIfAbsent(mmdName, k -> {
                return new HashSet<>();
            });
            graphQLComponents.get(mmdName).add(bean);

            if (containsSecurityAnnotations(bean.getBeanClass())) {
                containsSecAnnotations.put(mmdName, true);
            }
        }
    }

    static GraphQLSchema createSchema(BeanManager beanManager) {
        J2EEName mmdName = getModuleMetaDataName();
        Set<Bean<?>> beans = graphQLComponents.get(mmdName);
        if (beans == null || beans.size() < 1) {
            return null;
        }

        GraphQLSchemaGenerator schemaGen = new GraphQLSchemaGenerator()
                        .withResolverBuilders(new AnnotatedResolverBuilder())
                        .withValueMapperFactory(new JacksonValueMapperFactory());

        for (Bean<?> bean : beans) {
            schemaGen.withOperationsFromSingleton(
                beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean)), 
                bean.getBeanClass());
        }

        GraphQLSchema schema = schemaGen.generate();
        synchronized (graphQLSchemas) {
            graphQLSchemas.put(mmdName, schema);
        }
        return schema;
    }

    static J2EEName getModuleMetaDataName() {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        ModuleMetaData mmd = cmd.getModuleMetaData();
        return mmd.getJ2EEName();
    }

    static void forEachComponentInApp(Consumer<Class<?>> consumer) {
        getBeans().stream()
                  .map(bean -> { return bean.getBeanClass();})
                  .forEach(consumer);
    }

    static boolean appContainsSecurityAnnotations() {
        return containsSecAnnotations.get(getModuleMetaDataName());
    }

    private static boolean containsSecurityAnnotations(Class<?> cls) {
        for (Annotation anno : cls.getAnnotations()) {
            Class<?> annoType = anno.annotationType();
            if (annoType == DeclareRoles.class ||
                annoType == DenyAll.class ||
                annoType == PermitAll.class ||
                annoType == RolesAllowed.class) {
                return true;
            }
        }
        return false;
    }

    private static Set<Bean<?>> getBeans() {
        J2EEName mmdName = getModuleMetaDataName();
        Set<Bean<?>> beans = graphQLComponents.get(mmdName);
        if (beans == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No GraphQL components found for module " + mmdName);
            }
            return Collections.emptySet();
        }
        return beans;
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
                out.println("*  Module: " + k);
                out.println();
                out.println(new SchemaPrinter().print(v));
            });
        }
    }
}
