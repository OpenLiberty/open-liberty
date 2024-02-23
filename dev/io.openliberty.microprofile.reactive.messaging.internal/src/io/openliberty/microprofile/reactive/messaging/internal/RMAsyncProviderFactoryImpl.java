/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

import io.openliberty.microprofile.reactive.messaging.internal.interfaces.RMAsyncProvider;
import io.openliberty.microprofile.reactive.messaging.internal.interfaces.RMAsyncProviderFactory;
import io.openliberty.microprofile.reactive.messaging.internal.interfaces.RMContext;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class RMAsyncProviderFactoryImpl implements RMAsyncProviderFactory {

    private static final TraceComponent tc = Tr.register(RMAsyncProviderFactoryImpl.class);

    @SuppressWarnings("unchecked")
    private static final Map<String, ?>[] DEFAULT_CONTEXT_PROVIDERS = new Map[] {
                                                                                  Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER,
                                                                                                           "com.ibm.ws.classloader.context.provider"),
                                                                                  Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER,
                                                                                                           "com.ibm.ws.javaee.metadata.context.provider"),
                                                                                  Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER,
                                                                                                           "com.ibm.ws.security.context.provider"),
    };

    /**
     * The built-in context service
     */
    @Reference(target = "(service.pid=com.ibm.ws.context.manager)", policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile WSContextService builtInContextService;

    /**
     * The default context service provided by the concurrent feature
     */
    @Reference(target = "(id=DefaultContextService)", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile WSContextService defaultContextService;

    /**
     * A map of named context services configured in the server.xml
     * <p>
     * Maps from the context service ID to the context service.
     * <p>
     * Updated dynamically if context services are added or removed while the server is running.
     */
    private final Map<String, WSContextService> namedContextServices = new ConcurrentHashMap<>();

    /**
     * The liberty global thread pool executor
     */
    @Reference(target = "(component.name=com.ibm.ws.threading)", policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile ExecutorService globalExecutor;

    /**
     * A scheduled executor which delegates to the global thread pool
     */
    @Reference(target = "(deferrable=false)", policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile ScheduledExecutorService scheduledExecutor;

    /**
     * Updates {@link #namedContextServices} when a new service is added
     */
    @Reference(service = WSContextService.class, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addContextService(WSContextService contextService, Map<String, Object> properties) {
        String name = getContextServiceName(properties);
        if ((name != null) && !isApplicationDefinedContextService(properties)) {
            namedContextServices.put(name, contextService);
        }
    }

    /**
     * Updates {@link #namedContextServices} when the properties of a context service are updated
     * <p>
     * Not sure if this actually happens, but since the properties includes the name, that could in theory change.
     */
    protected void updatedContextService(WSContextService contextService, Map<String, Object> properties) {
        String name = getContextServiceName(properties);
        if ((name == null) || isApplicationDefinedContextService(properties)) {
            namedContextServices.values().remove(contextService);
        } else if (!contextService.equals(namedContextServices.get(name))) {
            // If name has changed, remove and re-add
            namedContextServices.values().remove(contextService);
            namedContextServices.put(name, contextService);
        }
    }

    /**
     * Updates {@link #namedContextServices} when a context service is removed
     */
    protected void removeContextService(WSContextService contextService) {
        namedContextServices.values().remove(contextService);
    }

    /**
     * Extract the name of a context service from its OSGi service properties
     *
     * @param properties the OSGi service properties
     * @return the name, or {@code null} if the context service doesn't have a name (e.g. because it's an internal one)
     */
    private String getContextServiceName(Map<String, Object> properties) {
        Object name = properties.get("id");
        if (name instanceof String) {
            return (String) name;
        } else {
            return null;
        }
    }

    /**
     * Returns whether a context service is one defined within an application using {@code @ContextServiceDefinition}.
     *
     * @param properties the service properties of the {@code WSContextService} service to check
     * @return {@code true} if the service properties are for an application-defined context service, otherwise {@code false}
     */
    private boolean isApplicationDefinedContextService(Map<String, Object> properties) {
        return !"file".equals(properties.get("config.source"));
    }

    @Override
    public RMAsyncProvider getAsyncProvider(String contextServiceRef, String channelName) {
        if (contextServiceRef != null) {
            // Validate the supplied context service name
            getNamedContextService(contextServiceRef, channelName);
        }
        return new NamedAsyncProvider(contextServiceRef, channelName);
    }

    /**
     * Look up the context service with the given name and use it to capture the thread context
     *
     * @param contextServiceName the context service to use, or {@code null} to use the default context service
     * @return the captured thread context
     */
    @SuppressWarnings("unchecked")
    private RMContext captureContext(String contextServiceName, String channel) {
        WSContextService namedContextService = null;
        if ((contextServiceName != null) && !contextServiceName.isEmpty()) {
            namedContextService = getNamedContextService(contextServiceName, channel);
        }

        if (namedContextService != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Capturing context with named context service", contextServiceName, namedContextService);
            }
            return new RMContextImpl(namedContextService.captureThreadContext(null));
        } else if (defaultContextService != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Capturing context with default context service");
            }
            return new RMContextImpl(defaultContextService.captureThreadContext(null));
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Capturing context with build-in context service");
            }
            return new RMContextImpl(builtInContextService.captureThreadContext(null, DEFAULT_CONTEXT_PROVIDERS));
        }
    }

    /**
     * Retrieve the context service with the given name.
     * <p>
     * Reports an error if the context service cannot be found.
     *
     * @param contextServiceName the name, must not be {@code null}
     * @param channelName the name of the channel, for use in the error message if required
     * @return the context service
     * @throws IllegalArgumentException if the named context service cannot be found
     */
    private WSContextService getNamedContextService(String contextServiceName, String channelName) {
        WSContextService result = namedContextServices.get(contextServiceName);
        if (result == null) {
            Tr.error(tc, "missing.context.service.CWMRX1200E", channelName, contextServiceName);
            throw new IllegalArgumentException(Tr.formatMessage(tc, "missing.context.service.CWMRX1200E", channelName, contextServiceName));
        }
        return result;
    }

    /**
     * An RMAsyncProvider which uses a named context service to capture thread context.
     * <p>
     * {@code null} can be provided as the name to use the default context service.
     */
    private class NamedAsyncProvider implements RMAsyncProvider {

        private final String name;
        private final String channelName;

        /**
         * Create a new AsyncProvider
         *
         * @param name the name of the context service to use for capturing thread context, or {@code null} to use the default
         */
        public NamedAsyncProvider(String name, String channelName) {
            this.name = name;
            this.channelName = channelName;
        }

        @Override
        public RMContext captureContext() {
            return RMAsyncProviderFactoryImpl.this.captureContext(name, channelName);
        }

        @Override
        public ExecutorService getExecutorService() {
            return globalExecutor;
        }

        @Override
        public ScheduledExecutorService getScheduledExecutorService() {
            return scheduledExecutor;
        }
    }

    private static class RMContextImpl implements RMContext {

        private final ThreadContextDescriptor context;

        /**
         * @param context the thread context captured with {@link WSContextService#captureThreadContext(Map, Map...)}
         */
        public RMContextImpl(ThreadContextDescriptor context) {
            this.context = context;
        }

        @Override
        public void execute(Runnable runnable) {
            ArrayList<ThreadContext> contextApplied = context.taskStarting();
            try {
                runnable.run();
            } finally {
                context.taskStopping(contextApplied);
            }
        }
    }

}
