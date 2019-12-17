/*******************************************************************************
 * Copyright (c) 2018,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.contextpropagation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManager;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.context.ThreadIdentityContextProvider;
import com.ibm.ws.microprofile.context.WLMContextProvider;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * Context manager, which includes the collection of ThreadContextProviders
 * for a particular class loader.
 */
public class ContextManagerImpl implements ContextManager {
    private static final TraceComponent tc = Tr.register(ContextManagerImpl.class);

    // Counter of managed executor & thread context instances created
    static final AtomicInteger instanceCount = new AtomicInteger();

    /**
     * Application for which this context manager was created, if any can be determined.
     */
    final String appName;

    final ContextManagerProviderImpl cmProvider;

    /**
     * List of available thread context providers.
     * Container-implemented context providers are ordered according to their prerequisites.
     * This is the order in which thread context should be captured and applied to threads.
     * It is the reverse of the order in which thread context is restored on threads.
     */
    final ArrayList<ThreadContextProvider> contextProviders = new ArrayList<ThreadContextProvider>();

    /**
     * Lazily initialized reference to MicroProfile Config, if available.
     * When uninitialized, the value is FALSE. Once initialized, the value is either a valid MP Config instance or null (no MP Config).
     */
    private final AtomicReference<Object> mpConfigRef = new AtomicReference<Object>(Boolean.FALSE);

    /**
     * Merge built-in thread context providers from the container with those found
     * on the class loader, detecting any duplicate provider types.
     *
     * @param cmProvider the registered context manager provider
     * @param classloader the class loader from which to discover thread context providers
     */
    ContextManagerImpl(ContextManagerProviderImpl cmProvider, ClassLoader classloader) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        this.cmProvider = cmProvider;

        ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        appName = cData == null ? null : cData.getJ2EEName().getApplication();

        // Thread context types for which providers are available
        HashSet<String> available = new HashSet<String>();

        // Built-in thread context providers (always available)
        contextProviders.add(cmProvider.applicationContextProvider);
        available.add(ThreadContext.APPLICATION);
        contextProviders.add(cmProvider.cdiContextProvider);
        available.add(ThreadContext.CDI);
        contextProviders.add(cmProvider.securityContextProvider);
        available.add(ThreadContext.SECURITY);
        // SYNC_TO_OS_THREAD must come after Security and Application as it depends on both.
        contextProviders.add(cmProvider.threadIdendityContextProvider);
        available.add(ThreadIdentityContextProvider.SYNC_TO_OS_THREAD);
        contextProviders.add(cmProvider.transactionContextProvider);
        available.add(ThreadContext.TRANSACTION);
        contextProviders.add(cmProvider.wlmContextProvider);
        available.add(WLMContextProvider.CLASSIFICATION);

        // Thread context providers for the supplied class loader
        for (ThreadContextProvider provider : ServiceLoader.load(ThreadContextProvider.class, classloader)) {
            String type = provider.getThreadContextType();

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "context type " + type + " provided by " + provider);

            if (available.add(type))
                contextProviders.add(provider);
            else
                throw new IllegalStateException(Tr.formatMessage(tc, "CWWKC1150.duplicate.context",
                                                                 type, provider, findConflictingProvider(provider, classloader)));
        }
    }

    /**
     * Finds and returns the first thread context provider that provides the same
     * thread context type as the specified provider.
     *
     * @param provider provider that is found to provide a conflicting thread context type with another provider.
     * @param classloader class loader from which to load thread context providers.
     * @return thread context provider with which the specified provider conflicts.
     */
    private ThreadContextProvider findConflictingProvider(ThreadContextProvider provider, ClassLoader classloader) {
        String conflictingType = provider.getThreadContextType();
        for (ThreadContextProvider p : ServiceLoader.load(ThreadContextProvider.class, classloader)) {
            if (conflictingType.equals(p.getThreadContextType()) && !provider.equals(p))
                return p;
        }

        // Not found: probably a conflict with a built-in type
        if (ThreadContext.APPLICATION.equals(conflictingType))
            return cmProvider.applicationContextProvider;

        if (ThreadContext.CDI.equals(conflictingType))
            return cmProvider.cdiContextProvider;

        if (ThreadContext.SECURITY.equals(conflictingType))
            return cmProvider.securityContextProvider;

        if (ThreadContext.TRANSACTION.equals(conflictingType))
            return cmProvider.transactionContextProvider;

        if (WLMContextProvider.CLASSIFICATION.equals(conflictingType))
            return cmProvider.wlmContextProvider;

        // should be unreachable
        throw new IllegalStateException(conflictingType);
    }

    /**
     * Obtain a default value from MicroProfile Config if available.
     *
     * @param mpConfigPropName name of the MicroProfile Config property.
     * @param defaultValue value to use if not found in MicroProfile Config.
     * @return default value.
     */
    <T> T getDefault(String mpConfigPropName, T defaultValue) {
        MPConfigAccessor accessor = cmProvider.mpConfigAccessor;
        if (accessor != null) {
            Object mpConfig = mpConfigRef.get();
            if (mpConfig == Boolean.FALSE) // not initialized yet
                if (!mpConfigRef.compareAndSet(Boolean.FALSE, mpConfig = accessor.getConfig()))
                    mpConfig = mpConfigRef.get();

            if (mpConfig != null) {
                defaultValue = accessor.get(mpConfig, mpConfigPropName, defaultValue);
            }
        }
        return defaultValue;
    }

    @Override
    public ManagedExecutor.Builder newManagedExecutorBuilder() {
        return new ManagedExecutorBuilderImpl(this, contextProviders);
    }

    @Override
    public ThreadContext.Builder newThreadContextBuilder() {
        return new ThreadContextBuilderImpl(this, contextProviders);
    }
}
