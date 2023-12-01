/*******************************************************************************
 * Copyright (c) 2017,2023 IBM Corporation and others.
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
package com.ibm.ws.threading;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

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
    private ExecutorService libertyThreadPool;

    /**
     * Programmatically created instances (via PolicyExecutorProvider) which have not yet been shut down.
     * Instances of PolicyExecutorImpl manage their own membership in this list, adding themselves
     * upon construction, and removing themselves upon completion of shutdown.
     */
    private final ConcurrentHashMap<String, PolicyExecutorImpl> policyExecutors = new ConcurrentHashMap<String, PolicyExecutorImpl>();

    /**
     * Virtual thread operations that are only available when a Java 21+ feature includes the io.openliberty.threading.internal.java21 bundle.
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected volatile VirtualThreadOps virtualThreadOps;

    /**
     * Creates a new policy executor instance and initializes it per the specified OSGi service component properties.
     * The config.displayId of the OSGi service component properties is used as the unique identifier.
     *
     * @param props properties for a configuration-based OSGi service component instance. For example, an instance of concurrencyPolicy.
     * @return a new policy executor instance.
     * @throws IllegalStateException if an instance with the specified unique identifier already exists and has not been shut down.
     * @throws NullPointerException  if the specified identifier is null
     */
    public PolicyExecutor create(Map<String, Object> props) {
        PolicyExecutor executor = new PolicyExecutorImpl((ExecutorServiceImpl) libertyThreadPool, (String) props.get("config.displayId"), null, policyExecutors, virtualThreadOps);
        executor.updateConfig(props);
        return executor;
    }

    /**
     * Creates a new policy executor instance.
     *
     * @param identifier unique identifier for the new instance, to be used for monitoring and problem determination.
     *                       Note: The prefix, PolicyExecutorProvider-, is prepended to the identifier.
     * @return a new policy executor instance.
     * @throws IllegalStateException if an instance with the specified unique identifier already exists and has not been shut down.
     * @throws NullPointerException  if the specified identifier is null
     */
    public PolicyExecutor create(String identifier) {
        return new PolicyExecutorImpl((ExecutorServiceImpl) libertyThreadPool, "PolicyExecutorProvider-" + identifier, null, policyExecutors, virtualThreadOps);
    }

    /**
     * Creates a new policy executor instance for use by a single application.
     * Policy executors owned by this application can be shut down via the shutdownNow method of this class.
     *
     * @param fullIdentifier unique identifier for the new instance, to be used for monitoring and problem determination.
     * @param owner          name of application that the policy executor is created for.
     * @return a new policy executor instance.
     * @throws IllegalStateException if an instance with the specified unique identifier already exists and has not been shut down.
     * @throws NullPointerException  if the specified identifier is null
     */
    public PolicyExecutor create(String fullIdentifier, String owner) {
        return new PolicyExecutorImpl((ExecutorServiceImpl) libertyThreadPool, fullIdentifier, owner, policyExecutors, virtualThreadOps);
    }

    public void introspectPolicyExecutors(PrintWriter out) {
        for (PolicyExecutorImpl executor : policyExecutors.values()) {
            executor.introspect(out);
        }
    }

    /**
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

    /**
     * Shuts down (via shutdownNow) all policy executors with the specified owner.
     *
     * @param owner name of the application for which a policy executor was created.
     */
    public void shutdownNow(String owner) {
        for (PolicyExecutorImpl executor : policyExecutors.values())
            if (owner.equals(executor.owner))
                executor.shutdownNow();
    }

    /**
     * Shuts down (via shutdownNow) all policy executors with an identifier that starts with the specified prefix.
     *
     * @param prefix policy executor identifier prefix that must match if the executor is to be shut down.
     */
    public void shutdownNowByIdentifierPrefix(String prefix) {
        for (PolicyExecutorImpl executor : policyExecutors.values())
            if (executor.getIdentifier().startsWith(prefix))
                executor.shutdownNow();
    }
}
