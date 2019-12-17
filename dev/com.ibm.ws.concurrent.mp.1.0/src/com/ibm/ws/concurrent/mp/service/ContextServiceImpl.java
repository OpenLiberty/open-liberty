/*******************************************************************************
 * Copyright (c) 2018,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp.service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.enterprise.concurrent.ContextService;

import org.eclipse.microprofile.context.ThreadContext;
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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.ContextualAction;
import com.ibm.ws.concurrent.mp.spi.ContextFactory;
import com.ibm.ws.concurrent.mp.spi.ThreadContextConfig;
import com.ibm.ws.concurrent.service.AbstractContextService;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Subclass of ContextServiceImpl to be used with Java 8 and above.
 * This class provides implementation of the MicroProfile Context Propagation methods.
 * These methods can be collapsed into ContextServiceImpl once there is
 * no longer a need for OpenLiberty to support Java 7.
 */
@Component(name = "com.ibm.ws.context.service",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = { ResourceFactory.class, ContextService.class, ThreadContext.class, WSContextService.class, ApplicationRecycleComponent.class },
           property = { "creates.objectClass=javax.enterprise.concurrent.ContextService",
                        "creates.objectClass=org.eclipse.microprofile.context.ThreadContext" })
public class ContextServiceImpl extends AbstractContextService implements ThreadContext, ThreadContextConfig {
    private static final TraceComponent tc = Tr.register(ContextServiceImpl.class);

    @Activate
    @Override
    @Trivial
    protected void activate(ComponentContext context) {
        super.activate(context);
    }

    @Override
    public final <R> Callable<R> contextualCallable(Callable<R> callable) {
        return ContextFactory.contextualCallable(callable, this);
    }

    @Override
    public final <T, U> BiConsumer<T, U> contextualConsumer(BiConsumer<T, U> consumer) {
        return ContextFactory.contextualConsumer(consumer, this);
    }

    @Override
    public final <T> Consumer<T> contextualConsumer(Consumer<T> consumer) {
        return ContextFactory.contextualConsumer(consumer, this);
    }

    @Override
    public final <T, U, R> BiFunction<T, U, R> contextualFunction(BiFunction<T, U, R> function) {
        return ContextFactory.contextualFunction(function, this);
    }

    @Override
    public final <T, R> Function<T, R> contextualFunction(Function<T, R> function) {
        return ContextFactory.contextualFunction(function, this);
    }

    @Override
    public final Runnable contextualRunnable(Runnable runnable) {
        return ContextFactory.contextualRunnable(runnable, this);
    }

    @Override
    public final <R> Supplier<R> contextualSupplier(Supplier<R> supplier) {
        return ContextFactory.contextualSupplier(supplier, this);
    }

    @Override
    public final ThreadContextDescriptor captureThreadContext() {
        return captureThreadContext(Collections.emptyMap());
    }

    @Override
    @Trivial
    public Object createContextualProxy(Object instance, Class<?>... interfaces) {
        if (instance instanceof ContextualAction)
            throw new IllegalArgumentException(instance.getClass().getSimpleName());

        return super.createContextualProxy(instance, interfaces);
    }

    @Override
    @Trivial
    public Object createContextualProxy(Object instance, Map<String, String> executionProperties, Class<?>... interfaces) {
        if (instance instanceof ContextualAction)
            throw new IllegalArgumentException(instance.getClass().getSimpleName());

        return super.createContextualProxy(instance, executionProperties, interfaces);
    }

    @Override
    @Trivial
    public <T> T createContextualProxy(T instance, Class<T> intf) {
        if (instance instanceof ContextualAction)
            throw new IllegalArgumentException(instance.getClass().getSimpleName());

        return super.createContextualProxy(instance, intf);
    }

    @Override
    @Trivial
    public <T> T createContextualProxy(T instance, Map<String, String> executionProperties, final Class<T> intf) {
        if (instance instanceof ContextualAction)
            throw new IllegalArgumentException(instance.getClass().getSimpleName());

        return super.createContextualProxy(instance, executionProperties, intf);
    }

    @Override
    public Executor currentContextExecutor() {
        return ContextFactory.currentContextExecutor(this);
    }

    @Deactivate
    @Override
    @Trivial
    protected void deactivate(ComponentContext context) {
        super.deactivate(context);
    }

    @Modified
    @Override
    @Trivial
    protected void modified(ComponentContext context) {
        super.modified(context);
    }

    @Override
    @Reference(name = "baseInstance",
               service = ContextService.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               target = "(id=unbound)")
    @Trivial
    protected void setBaseInstance(ServiceReference<ContextService> ref) {
        super.setBaseInstance(ref);
    }

    @Override
    @Reference(name = "threadContextManager",
               service = WSContextService.class,
               cardinality = ReferenceCardinality.MANDATORY,
               policy = ReferencePolicy.STATIC,
               target = "(component.name=com.ibm.ws.context.manager)")
    @Trivial
    protected void setThreadContextManager(WSContextService svc) {
        super.setThreadContextManager(svc);
    }

    @Override
    @Trivial
    protected void unsetBaseInstance(ServiceReference<ContextService> ref) {
        super.unsetBaseInstance(ref);
    }

    @Override
    @Trivial
    protected void unsetThreadContextManager(WSContextService svc) {
        super.unsetThreadContextManager(svc);
    }

    @Override
    public <T> CompletableFuture<T> withContextCapture(CompletableFuture<T> stage) {
        return ContextFactory.withContextCapture(stage, this, this, tc);
    }

    @Override
    public <T> CompletionStage<T> withContextCapture(CompletionStage<T> stage) {
        return ContextFactory.withContextCapture(stage, this, this, tc);
    }
}
