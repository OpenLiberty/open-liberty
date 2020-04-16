/*******************************************************************************
 * Copyright (c) 2013,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.context.service.serializable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.container.service.metadata.extended.MetaDataIdentifierService;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Manages all of the thread context providers.
 */
@Component(name = "com.ibm.ws.context.manager",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           service = { WSContextService.class },
           property = { "service.pid=com.ibm.ws.context.manager", "default.for=contextService", "service.ranking:Integer=100" })
public class ThreadContextManager implements WSContextService {
    /**
     * The component.name service property.
     */
    private static final String COMPONENT_NAME = "component.name";

    /**
     * Names of thread context providers for which thread context is always captured,
     * and consequently, not configurable per contextService.
     */
    private final Set<String> alwaysEnabled = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * Jakarta EE versiom if Jakarta EE 9 or higher. If 0, assume a lesser EE spec version.
     */
    int eeVersion;

    /**
     * The metadata identifier service.
     */
    MetaDataIdentifierService metadataIdentifierService;

    /**
     * All registered thread context providers.
     */
    final ConcurrentServiceReferenceMap<String, ThreadContextProvider> threadContextProviders = new ConcurrentServiceReferenceMap<String, ThreadContextProvider>(THREAD_CONTEXT_PROVIDER);

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context for this component instance
     */
    @Activate
    protected void activate(ComponentContext context) {
        threadContextProviders.activate(context);
    }

    /**
     * @see com.ibm.wsspi.threadcontext.WSContextService#captureThreadContext(java.util.Map, java.util.Map[])
     */
    @Override
    public ThreadContextDescriptor captureThreadContext(Map<String, String> executionProperties, Map<String, ?>... additionalThreadContextConfig) {
        executionProperties = executionProperties == null ? new TreeMap<String, String>() : new TreeMap<String, String>(executionProperties);

        Map<String, Map<String, ?>> threadContextConfigurations;
        if (additionalThreadContextConfig.length > 0) {
            threadContextConfigurations = new HashMap<String, Map<String, ?>>();
            for (Map<String, ?> config : additionalThreadContextConfig) {
                String providerName = (String) config.get(THREAD_CONTEXT_PROVIDER);
                if (providerName == null)
                    throw new IllegalArgumentException("additionalThreadContextConfig: " + config);
                threadContextConfigurations.put(providerName, config);
            }
        } else
            threadContextConfigurations = Collections.emptyMap();

        return captureThreadContext(threadContextConfigurations, executionProperties);
    }

    /**
     * Capture thread context.
     *
     * @param threadContextConfigurations map of thread context provider name to configured thread context.
     * @param execProps                   execution properties.
     * @return thread context descriptor for the captured thread context.
     */
    public ThreadContextDescriptor captureThreadContext(final Map<String, Map<String, ?>> threadContextConfigurations, final Map<String, String> execProps) {
        int initialCapacity = threadContextConfigurations == null ? 5 : threadContextConfigurations.size() + 5;
        final ThreadContextDescriptorImpl capturedThreadContext = new ThreadContextDescriptorImpl(execProps, initialCapacity, this);

        if (!ALL_CONTEXT_TYPES.equals(execProps.get(DEFAULT_CONTEXT))) {
            final List<ThreadContextProvider> configuredProviders = new ArrayList<ThreadContextProvider>(threadContextConfigurations.size());
            final List<Map.Entry<String, Map<String, ?>>> configuredProviderProps = new ArrayList<Map.Entry<String, Map<String, ?>>>(threadContextConfigurations.size());

            final List<ThreadContextProvider> alwaysEnabledProviders = new ArrayList<ThreadContextProvider>(alwaysEnabled.size());
            final List<String> alwaysEnabledProviderNames = new ArrayList<String>(alwaysEnabled.size());

            // Lazily obtaining services is a privileged operation
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    // Identify thread context that is configurable per contextService
                    if (threadContextConfigurations != null)
                        for (Map.Entry<String, Map<String, ?>> threadContextConfig : threadContextConfigurations.entrySet()) {
                            String threadContextProviderName = threadContextConfig.getKey();
                            if (!capturedThreadContext.providerNamesToSkip.contains(threadContextProviderName)) {
                                ThreadContextProvider provider = threadContextProviders.getService(threadContextProviderName);
                                if (provider != null) {
                                    configuredProviders.add(provider);
                                    configuredProviderProps.add(threadContextConfig);
                                }
                            }
                        }

                    // Identify context that is always captured per the alwaysCaptureThreadContext service property
                    for (String threadContextProviderName : alwaysEnabled)
                        if (!capturedThreadContext.providerNamesToSkip.contains(threadContextProviderName)) {
                            ThreadContextProvider provider = threadContextProviders.getServiceWithException(threadContextProviderName);
                            alwaysEnabledProviders.add(provider);
                            alwaysEnabledProviderNames.add(threadContextProviderName);
                        }

                    return null;
                }
            });

            // capture thread context that is configurable per contextService
            for (int i = 0; i < configuredProviders.size(); i++) {
                ThreadContextProvider provider = configuredProviders.get(i);
                Map.Entry<String, Map<String, ?>> threadContextConfig = configuredProviderProps.get(i);
                ThreadContext context = provider.captureThreadContext(execProps, threadContextConfig.getValue());
                capturedThreadContext.add(threadContextConfig.getKey(), context);
            }

            // context that is always captured per the alwaysCaptureThreadContext service property
            for (int i = 0; i < alwaysEnabledProviders.size(); i++) {
                ThreadContextProvider provider = alwaysEnabledProviders.get(i);
                ThreadContext context = provider.captureThreadContext(execProps, null);
                capturedThreadContext.add(alwaysEnabledProviderNames.get(i), context);
            }
        }

        return capturedThreadContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T createContextualProxy(ThreadContextDescriptor threadContextDescriptor, T instance, final Class<T> intf) {
        if (intf == null || !intf.isInstance(instance))
            throw new IllegalArgumentException(instance + ", " + (intf == null ? null : intf.getName()));

        if (Callable.class.equals(intf)) {
            @SuppressWarnings("unchecked")
            Callable<Object> callable = (Callable<Object>) instance;
            instance = intf.cast(new ContextualCallable<Object>(threadContextDescriptor, callable, null));
        } else if (Runnable.class.equals(intf)) {
            instance = intf.cast(new ContextualRunnable(threadContextDescriptor, (Runnable) instance, null));
        } else {
            final InvocationHandler handler = new ContextualInvocationHandler(threadContextDescriptor, instance, null);
            instance = AccessController.doPrivileged(new PrivilegedAction<T>() {
                @Override
                public T run() {
                    return intf.cast(Proxy.newProxyInstance(intf.getClassLoader(), new Class<?>[] { intf }, handler));
                }
            });
        }
        return instance;
    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context for this component instance
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        threadContextProviders.deactivate(context);
    }

    /**
     * Called by Declarative Services to modify service config properties
     *
     * @param context DeclarativeService defined/populated component context
     */
    @Modified
    protected void modified(ComponentContext context) {
    }

    /**
     * Declarative Services method for setting the Jakarta/Java EE version
     *
     * @param ref reference to the service
     */
    @Reference(service = JavaEEVersion.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.STATIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setEEVersion(ServiceReference<JavaEEVersion> ref) {
        String version = (String) ref.getProperty("version");
        if (version == null) {
            eeVersion = 0;
        } else {
            int dot = version.indexOf('.');
            String major = dot > 0 ? version.substring(0, dot) : version;
            eeVersion = Integer.parseInt(major);
        }
    }

    /**
     * Declarative Services method for setting the metadata identifier service.
     *
     * @param svc the service
     */
    @Reference(service = MetaDataIdentifierService.class)
    protected void setMetadataIdentifierService(MetaDataIdentifierService svc) {
        metadataIdentifierService = svc;
    }

    /**
     * Declarative Services method for adding a thread context provider.
     *
     * @param ref reference to the service
     */
    @Reference(name = THREAD_CONTEXT_PROVIDER,
               service = ThreadContextProvider.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setThreadContextProvider(ServiceReference<ThreadContextProvider> ref) {
        String threadContextProviderName = (String) ref.getProperty(COMPONENT_NAME);
        threadContextProviders.putReference(threadContextProviderName, ref);
        if (Boolean.TRUE.equals(ref.getProperty(ThreadContextProvider.ALWAYS_CAPTURE_THREAD_CONTEXT)))
            alwaysEnabled.add(threadContextProviderName);
    }

    /**
     * Declarative Services method for unsetting the Jakarta/Java EE version
     *
     * @param ref reference to the service
     */
    protected void unsetEEVersion(ServiceReference<JavaEEVersion> ref) {
        eeVersion = 0;
    }

    /**
     * Declarative Services method for unsetting the metadata identifier service.
     *
     * @param ref reference to the service
     */
    protected void unsetMetadataIdentifierService(MetaDataIdentifierService svc) {
        metadataIdentifierService = null;
    }

    /**
     * Declarative Services method for removing a thread context provider.
     *
     * @param ref reference to the service
     */
    protected void unsetThreadContextProvider(ServiceReference<ThreadContextProvider> ref) {
        String threadContextProviderName = (String) ref.getProperty(COMPONENT_NAME);
        if (threadContextProviders.removeReference(threadContextProviderName, ref)
            && Boolean.TRUE.equals(ref.getProperty(ThreadContextProvider.ALWAYS_CAPTURE_THREAD_CONTEXT)))
            alwaysEnabled.remove(threadContextProviderName);
    }
}