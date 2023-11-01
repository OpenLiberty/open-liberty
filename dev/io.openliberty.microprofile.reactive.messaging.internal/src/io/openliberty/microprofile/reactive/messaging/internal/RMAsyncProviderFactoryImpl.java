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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.service.component.annotations.Component;
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

@Component
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

    private final Map<String, WSContextService> namedContextServices = Collections.synchronizedMap(new HashMap<>());

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

    @Reference(service = WSContextService.class, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addContextService(WSContextService contextService, Map<String, Object> properties) {
        String name = getContextServiceName(properties);
        if (name != null) {
            namedContextServices.put(name, contextService);
        }
    }

    protected void updatedContextService(WSContextService contextService, Map<String, Object> properties) {
        String name = getContextServiceName(properties);
        if (name == null) {
            namedContextServices.entrySet().removeIf(e -> e.getValue() == contextService);
        } else if (namedContextServices.get(name) != contextService) {
            // If name has changed, remove and re-add
            namedContextServices.entrySet().removeIf(e -> e.getValue() == contextService);
            namedContextServices.put(name, contextService);
        }
    }

    protected void removeContextService(WSContextService contextService) {
        namedContextServices.entrySet().removeIf(e -> e.getValue() == contextService);
    }

    private String getContextServiceName(Map<String, Object> properties) {
        Object name = properties.get("id");
        if (name instanceof String) {
            return (String) name;
        } else {
            return null;
        }
    }

    @Override
    public RMAsyncProvider getAsyncProvider(String contextServiceRef) {
        return new NamedAsyncProvider(contextServiceRef);
    }

    @SuppressWarnings("unchecked")
    private RMContext captureContext(String contextServiceName) {
        WSContextService namedContextService = null;
        if (contextServiceName != null) {
            namedContextService = namedContextServices.get(contextServiceName);
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

    private class NamedAsyncProvider implements RMAsyncProvider {

        private final String name;

        public NamedAsyncProvider(String name) {
            this.name = name;
        }

        @Override
        public RMContext captureContext() {
            return RMAsyncProviderFactoryImpl.this.captureContext(name);
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
