/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.threading.internal.ExecutorServiceImpl;
import com.ibm.ws.threading.internal.PolicyExecutorImpl;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;

/**
 * <p>Provider class which can programmatically create policy executors.
 * The ability to create programmatically is provided for server components
 * which do not have any way of using a policyExecutor from server configuration.
 * Components with server configuration should instead rely on policyExecutor
 * instances from server config, which is the preferred approach, rather than
 * using PolicyExecutorProvider.</p>
 *
 * <p>Policy executors are backed by the Liberty global thread pool,
 * but allow concurrency constraints and various queue attributes
 * to be controlled independently of the global thread pool.</p>
 *
 * <p>For example, to create a policy executor that allows at most 3 tasks to
 * be active at any given point and can queue up to 20 tasks,</p>
 *
 * <code>
 * executor = PolicyExecutorProvider.create("AtMost3ConcurrentPolicy").maxConcurrency(3).maxQueueSize(20);
 * </code>
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, service = { PolicyExecutorProvider.class, ServerQuiesceListener.class })
public class PolicyExecutorProvider implements ServerQuiesceListener {
    @Reference(target = "(component.name=com.ibm.ws.threading)")
    private ExecutorService globalExecutor;

    /**
     * Programmatically created instances (via PolicyExecutorProvider) which have not yet been shut down.
     * Instances of PolicyExecutorImpl manage their own membership in this list, adding themselves
     * upon construction, and removing themselves upon completion of shutdown.
     */
    private final ConcurrentHashMap<String, PolicyExecutorImpl> policyExecutors = new ConcurrentHashMap<String, PolicyExecutorImpl>();

    /**
     * Creates a new policy executor instance and initializes it per the specified OSGi service component properties.
     * The config.displayId of the OSGi service component properties is used as the unique identifier.
     *
     * @param props properties for a configuration-based OSGi service component instance. For example, an instance of concurrencyPolicy.
     * @return a new policy executor instance.
     * @throws IllegalStateException if an instance with the specified unique identifier already exists and has not been shut down.
     * @throws NullPointerException if the specified identifier is null
     */
    public PolicyExecutor create(Map<String, Object> props) {
        PolicyExecutor executor = new PolicyExecutorImpl((ExecutorServiceImpl) globalExecutor, (String) props.get("config.displayId"), policyExecutors);
        executor.updateConfig(props);
        return executor;
    }

    /**
     * Creates a new policy executor instance.
     *
     * @param identifier unique identifier for the new instance, to be used for monitoring and problem determination.
     *            Note: The prefix, PolicyExecutorProvider-, is prepended to the identifier.
     * @return a new policy executor instance.
     * @throws IllegalStateException if an instance with the specified unique identifier already exists and has not been shut down.
     * @throws NullPointerException if the specified identifier is null
     */
    public PolicyExecutor create(String identifier) {
        return new PolicyExecutorImpl((ExecutorServiceImpl) globalExecutor, "PolicyExecutorProvider-" + identifier, policyExecutors);
    }

    public void introspectPolicyExecutors(PrintWriter out) {
        for (PolicyExecutorImpl executor : policyExecutors.values()) {
            executor.introspect(out);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener#serverStopping()
     */
    @Override
    public void serverStopping() {
        ConcurrentHashMap<String, PolicyExecutor> existingExecutors = new ConcurrentHashMap<String, PolicyExecutor>();
        synchronized (policyExecutors) {
            existingExecutors.putAll(policyExecutors);
        }
        for (PolicyExecutor pe : existingExecutors.values()) {
            pe.shutdown();
        }

    }
}
