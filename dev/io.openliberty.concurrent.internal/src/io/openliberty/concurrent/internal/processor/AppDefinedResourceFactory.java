/*******************************************************************************
 * Copyright (c) 2013,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.concurrent.internal.processor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.resource.ResourceFactoryBuilder;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

import io.openliberty.concurrent.internal.qualified.QualifiedResourceFactory;

/**
 * AppDefinedResourceFactory is a Future-like wrapper for an application defined resource factory.
 * When createResource is invoked on the AppDefinedResourceFactory,
 * it waits, if necessary for ConfigurationAdmin to finish creating the resource factory, and then
 * delegates createResource to that resource factory.
 */
public class AppDefinedResourceFactory implements QualifiedResourceFactory {
    private static final TraceComponent tc = Tr.register(AppDefinedResourceFactory.class);

    /**
     * Name of the application that defines this resource.
     */
    private final String appName;

    /**
     * ResourceFactoryBuilder instance that created this resource factory.
     */
    private final ResourceFactoryBuilder builder;

    /**
     * Filter for a ContextService that this resource factory depends upon.
     * Null if the resource factory is itself a ContextService.
     */
    private final String contextSvcFilter;

    /**
     * JNDI name of a ContextService that this resource factory depends upon.
     * Null if the resource factory is itself a ContextService.
     */
    private final String contextSvcJndiName;

    /**
     * The class loader of the application artifact that defines the
     * resource definition.
     *
     * @return the class loader.
     */
    private final ClassLoader declaringClassLoader;

    /**
     * Metadata of the application component that defines the
     * resource definition if defined by a component.
     *
     * @return the metadata.
     */
    private final MetaData declaringMetadata;

    /**
     * Unique identifier for the resource factory.
     */
    private final String id;

    /**
     * JNDI name for the resource factory.
     */
    private final String jndiName;

    /**
     * Instances of qualifier annotations that are specified on the resource definition,
     * or if none are specified then the value is the empty set.
     */
    private final Set<Annotation> qualifiers;

    /**
     * Service tracker for this resource factory.
     */
    private final ServiceTracker<ResourceFactory, ResourceFactory> tracker;

    /**
     * Construct a Future-like wrapper for an application-defined resource factory.
     *
     * @param builder              the resource factory builder
     * @param bundleContext        the bundle context
     * @param appName              name of the application that defines the resource factory
     * @param id                   unique identifier for the resource factory
     * @param jndiName             JNDI name of the resource factory
     * @param filter               filter for the resource factory
     * @param contextSvcJndiName   JNDI name of the context service that this resource depends on. Otherwise null.
     * @param contextSvcFilter     filter for the context service that this resource depends on. Otherwise null.
     * @param declaringMetadata    metadata of the application artifact that defines the resource definition.
     * @param declaringClassLoader class loader of the application artifact that defines the resource definition.
     * @param qualifierNames       names of qualifier annotation classes from the resource definition. Null indicates none.
     * @throws InvalidSyntaxException if the filter has incorrect syntax
     */
    AppDefinedResourceFactory(ResourceFactoryBuilder builder, BundleContext bundleContext, String appName, //
                              String id, String jndiName, String filter, //
                              String contextSvcJndiName, String contextSvcFilter,
                              MetaData declaringMetadata, ClassLoader declaringClassLoader,
                              List<String> qualifierNames) throws ClassNotFoundException, InvalidSyntaxException {
        this.appName = appName;
        this.builder = builder;
        this.id = id;
        this.jndiName = jndiName;
        this.contextSvcFilter = contextSvcFilter;
        this.contextSvcJndiName = contextSvcJndiName;
        this.declaringClassLoader = declaringClassLoader;
        this.declaringMetadata = declaringMetadata;

        if (qualifierNames == null) {
            qualifiers = Collections.emptySet();
        } else {
            qualifiers = new LinkedHashSet<Annotation>();

            for (String qualifierClassName : qualifierNames) {
                Class<?> qualifierClass = declaringClassLoader.loadClass(qualifierClassName);
                if (!qualifierClass.isInterface())
                    throw new IllegalArgumentException("The " + qualifierClassName + " class is not a valid qualifier class" +
                                                       " because it is not an annotation."); // TODO NLS
                qualifiers.add(Annotation.class.cast(Proxy.newProxyInstance(declaringClassLoader,
                                                                            new Class<?>[] { Annotation.class, qualifierClass },
                                                                            new QualifierProxy(qualifierClass))));
            }
        }

        // The resource factory is activated asynchronously. ServiceTracker is used to wait for it when we need it.
        tracker = new ServiceTracker<ResourceFactory, ResourceFactory>(bundleContext, bundleContext.createFilter(filter), null);
        tracker.open();
    }

    /**
     * @see com.ibm.wsspi.resource.ResourceFactory#createResource(com.ibm.wsspi.resource.ResourceInfo)
     */
    @Override
    public Object createResource(ResourceInfo info) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "createResource", info);

        Object resource;

        try {
            ResourceFactory factory = tracker.waitForService(TimeUnit.MINUTES.toMillis(1));
            if (factory == null) {
                // Determine if ManagedExecutorService or ManagedScheduledExecutorService or ManagedThreadFactory from the id
                int start = id.lastIndexOf("]/managed") + 3;
                String type = 'M' + id.substring(start, id.indexOf('[', start));

                // Determine if the cause of this resource being unavailable is a ContextService that it depends on,
                if (contextSvcFilter != null) {
                    Bundle bundle = FrameworkUtil.getBundle(getClass());
                    BundleContext bundleContext = ContextServiceDefinitionProvider.priv.getBundleContext(bundle);
                    Collection<ServiceReference<ResourceFactory>> refs = ContextServiceDefinitionProvider.priv.getServiceReferences(bundleContext, ResourceFactory.class,
                                                                                                                                    contextSvcFilter);
                    if (refs.isEmpty())
                        throw new IllegalStateException(Tr.formatMessage(tc, "CWWKC1201.contextsvc.inaccessible",
                                                                         type + " " + jndiName,
                                                                         appName,
                                                                         contextSvcJndiName));
                }
                throw new IllegalStateException(Tr.formatMessage(tc, "CWWKC1200.resource.unavailable",
                                                                 type + " " + jndiName,
                                                                 appName));
            }
            resource = factory.createResource(info);
        } catch (Exception x) {
            FFDCFilter.processException(x, getClass().getName(), "129", this);
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "createResource", x);
            throw x;
        } catch (Error x) {
            FFDCFilter.processException(x, getClass().getName(), "134", this);
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "createResource", x);
            throw x;
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "createResource", resource);
        return resource;
    }

    /**
     * @see com.ibm.ws.resource.ResourceFactory#createResource(com.ibm.ws.resource.ResourceRefInfo)
     */
    @Override
    @Trivial
    public Object createResource(ResourceRefInfo ref) throws Exception {
        return createResource((ResourceInfo) ref);
    }

    /**
     * Destroy this application-defined resource by removing its configuration
     * and the configuration of all other services that were created for it.
     *
     * @throws Exception if an error occurs.
     */
    @Override
    public void destroy() throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "destroy", id);

        tracker.close();

        StringBuilder filter = new StringBuilder(FilterUtils.createPropertyFilter("id", id));
        filter.insert(filter.length() - 1, '*');

        builder.removeExistingConfigurations(filter.toString());

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "destroy");
    }

    /**
     * Returns the class loader of the application artifact that defines the
     * resource definition.
     *
     * @return the class loader
     */
    @Override
    public ClassLoader getDeclaringClassLoader() {
        return declaringClassLoader;
    }

    /**
     * Obtains the metadata of the application artifact that
     * defines the resource definition.
     *
     * @return component metadata.
     */
    @Override
    public MetaData getDeclaringMetadata() {
        return declaringMetadata;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public void modify(Map<String, Object> props) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    @Trivial
    public String toString() {
        return new StringBuilder("AppDefinedResourceFactory@") //
                        .append(Integer.toHexString(hashCode())) //
                        .append(':').append(id) //
                        .toString();
    }
}
