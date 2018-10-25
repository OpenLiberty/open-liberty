/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import javax.enterprise.concurrent.ManagedExecutorService;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
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

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrency.policy.ConcurrencyPolicy;
import com.ibm.ws.concurrent.service.AbstractManagedExecutorService;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Super class of ManagedExecutorServiceImpl to be used with Java 8 and above.
 * This class provides implementation of the MicroProfile Concurrency methods.
 * These methods can be collapsed into ManagedExecutorServiceImpl once there is
 * no longer a need for OpenLiberty to support Java 7.
 */
@Component(configurationPid = "com.ibm.ws.concurrent.managedExecutorService", configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = { ExecutorService.class, ManagedExecutor.class, ManagedExecutorService.class, ResourceFactory.class, ApplicationRecycleComponent.class },
           reference = @Reference(name = "ApplicationRecycleCoordinator", service = ApplicationRecycleCoordinator.class),
           property = { "creates.objectClass=java.util.concurrent.ExecutorService",
                        "creates.objectClass=javax.enterprise.concurrent.ManagedExecutorService",
                        "creates.objectClass=org.eclipse.microprofile.concurrent.ManagedExecutor" })
public class ManagedExecutorImpl extends AbstractManagedExecutorService implements ManagedExecutor {
    @Activate
    @Trivial
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        super.activate(context, properties);
    }

    @Override
    public <U> CompletableFuture<U> completedFuture(U value) {
        return ManagedCompletableFuture.completedFuture(value, this);
    }

    @Override
    public <U> CompletionStage<U> completedStage(U value) {
        return ManagedCompletableFuture.completedStage(value, this);
    }

    @Deactivate
    @Trivial
    protected void deactivate(ComponentContext context) {
        super.deactivate(context);
    }

    @Override
    public <U> CompletableFuture<U> failedFuture(Throwable ex) {
        return ManagedCompletableFuture.failedFuture(ex, this);
    }

    @Override
    public <U> CompletionStage<U> failedStage(Throwable ex) {
        return ManagedCompletableFuture.failedStage(ex, this);
    }

    @Modified
    @Trivial
    protected void modified(final ComponentContext context, Map<String, Object> properties) {
        super.modified(context, properties);
    }

    @Override
    public <U> CompletableFuture<U> newIncompleteFuture() {
        return ManagedCompletableFuture.newIncompleteFuture(this);
    }

    @Override
    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return ManagedCompletableFuture.runAsync(runnable, this);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, target = "(id=unbound)")
    @Trivial
    protected void setConcurrencyPolicy(ConcurrencyPolicy svc) {
        super.setConcurrencyPolicy(svc);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, target = "(id=unbound)")
    @Trivial
    protected void setContextService(ServiceReference<WSContextService> ref) {
        super.setContextService(ref);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, target = "(id=unbound)")
    @Trivial
    protected void setLongRunningPolicy(ConcurrencyPolicy svc) {
        super.setLongRunningPolicy(svc);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, target = "(component.name=com.ibm.ws.transaction.context.provider)")
    @Trivial
    protected void setTransactionContextProvider(ServiceReference<ThreadContextProvider> ref) {
        super.setTransactionContextProvider(ref);
    }

    @Override
    public <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return ManagedCompletableFuture.supplyAsync(supplier, this);
    }

    @Trivial
    protected void unsetConcurrencyPolicy(ConcurrencyPolicy svc) {
        super.unsetConcurrencyPolicy(svc);
    }

    @Trivial
    protected void unsetContextService(ServiceReference<WSContextService> ref) {
        super.unsetContextService(ref);
    }

    @Trivial
    protected void unsetLongRunningPolicy(ConcurrencyPolicy svc) {
        super.unsetLongRunningPolicy(svc);
    }

    @Trivial
    protected void unsetTransactionContextProvider(ServiceReference<ThreadContextProvider> ref) {
        super.unsetTransactionContextProvider(ref);
    }
}
