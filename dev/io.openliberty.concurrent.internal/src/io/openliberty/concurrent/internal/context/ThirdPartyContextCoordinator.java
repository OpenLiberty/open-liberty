/*******************************************************************************
 * Copyright (c) 2021,2022 IBM Corporation and others.
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
package io.openliberty.concurrent.internal.context;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.AccessController;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializationInfo;

import io.openliberty.concurrent.internal.processor.ContextServiceResourceFactoryBuilder;
import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.spi.ThreadContextProvider;

/**
 * Java EE application component context service provider.
 */
@Component(name = "io.openliberty.thirdparty.context.provider",
           configurationPolicy = ConfigurationPolicy.IGNORE)
@SuppressWarnings("deprecation")
public class ThirdPartyContextCoordinator implements ApplicationStateListener, //
                com.ibm.wsspi.threadcontext.ThreadContextProvider {
    private static final TraceComponent tc = Tr.register(ThirdPartyContextCoordinator.class);

    /**
     * Context types that are provided by built-in components.
     */
    private static final HashSet<String> BUILT_IN_CONTEXT = new HashSet<String>();
    static {
        BUILT_IN_CONTEXT.addAll(ContextServiceResourceFactoryBuilder.BUILT_IN_CONTEXT_PIDS.keySet());
        BUILT_IN_CONTEXT.add(ContextServiceDefinition.ALL_REMAINING);
        BUILT_IN_CONTEXT.add("EmptyHandleList");
        BUILT_IN_CONTEXT.add(ContextServiceDefinition.TRANSACTION);
    }

    /**
     * Key for providersPerClassLoader indicating that there is no context class loader on the thread.
     * This is needed because ConcurrentHashMap does not allow null keys.
     */
    private static final String NO_CONTEXT_CLASSLOADER = "NO_CONTEXT_CLASSLOADER";

    static final SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    /**
     * Mapping to an application name and list of available thread context providers
     * that are available for a thread context class loader.
     * The list determines the order in which thread context should be captured and
     * applied to threads (after the container-implemented providers).
     * It is the reverse of the order in which thread context is restored on threads
     * (before the container-implemented providers).
     */
    private final ConcurrentHashMap<Object, Entry<String, ArrayList<ThreadContextProvider>>> providersPerClassLoader = //
                    new ConcurrentHashMap<Object, Entry<String, ArrayList<ThreadContextProvider>>>();

    @Override
    @Trivial
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
    }

    @Override
    @Trivial
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
    }

    /**
     * Remove third-party thread context provider information when the application stops
     * to avoid leaking the class loader.
     */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        String appName = appInfo.getName();
        for (Iterator<Entry<Object, Entry<String, ArrayList<ThreadContextProvider>>>> it = //
                        providersPerClassLoader.entrySet().iterator(); //
                        it.hasNext();) {
            Entry<Object, Entry<String, ArrayList<ThreadContextProvider>>> providerInfo = it.next();
            if (!NO_CONTEXT_CLASSLOADER.equals(providerInfo.getKey())) {
                Entry<String, ArrayList<ThreadContextProvider>> entry = providerInfo.getValue();
                if (appName.equals(entry.getKey())) {
                    it.remove();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "removed providers", entry.getValue());
                }
            }
        }
    }

    @Override
    @Trivial
    public void applicationStopping(ApplicationInfo appInfo) {
    }

    @Override
    public ThreadContext captureThreadContext(Map<String, String> execProps, Map<String, ?> threadContextConfig) {
        return new ThirdPartyContext(this, execProps, threadContextConfig);
    }

    @Override
    public ThreadContext createDefaultThreadContext(Map<String, String> execProps) {
        return new ThirdPartyContext(this, execProps);
    }

    @Override
    public ThreadContext deserializeThreadContext(ThreadContextDeserializationInfo info, byte[] bytes) throws ClassNotFoundException, IOException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        ThirdPartyContext context;
        try {
            context = (ThirdPartyContext) in.readObject();
        } finally {
            in.close();
        }

        context.initPostDeserialize(this, ((ThreadContextDescriptor) info).getExecutionProperties());

        return context;
    }

    /**
     * Finds and returns the first thread context provider that provides the same
     * thread context type as the specified provider.
     *
     * @param provider    provider that is found to provide a conflicting thread context type with another provider.
     * @param classloader class loader from which to load thread context providers.
     * @return thread context provider with which the specified provider conflicts.
     */
    private Object findConflictingProvider(ThreadContextProvider provider, ClassLoader classloader) {
        String conflictingType = provider.getThreadContextType();

        if (BUILT_IN_CONTEXT.contains(conflictingType))
            return "built-in";

        for (ThreadContextProvider p : ServiceLoader.load(ThreadContextProvider.class, classloader)) {
            if (conflictingType.equals(p.getThreadContextType()) && !provider.equals(p))
                return p;
        }

        // should be unreachable
        throw new IllegalStateException(conflictingType);
    }

    @Override
    @Trivial
    public List<com.ibm.wsspi.threadcontext.ThreadContextProvider> getPrerequisites() {
        return null;
    }

    /**
     * Obtain the list of third-party context providers for the current thread context class loader.
     *
     * @return the list of third-party context providers for the current thread context class loader.
     */
    ArrayList<ThreadContextProvider> getProviders() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        ClassLoader classLoader = priv.getContextClassLoader();
        Object key = classLoader == null ? NO_CONTEXT_CLASSLOADER : classLoader;
        Entry<String, ArrayList<ThreadContextProvider>> providerInfo = providersPerClassLoader.get(key);
        if (providerInfo == null) {
            // Thread context types for which providers are available
            HashSet<String> available = new HashSet<String>(BUILT_IN_CONTEXT);

            // Thread context providers for the supplied class loader
            ArrayList<ThreadContextProvider> providersNew = new ArrayList<ThreadContextProvider>();
            for (ThreadContextProvider provider : ServiceLoader.load(ThreadContextProvider.class, classLoader)) {
                String type = provider.getThreadContextType();

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "context type " + type + " provided by " + provider);

                if (available.add(type))
                    providersNew.add(provider);
                else
                    throw new IllegalStateException(Tr.formatMessage(tc, "CWWKC1203.duplicate.context",
                                                                     type,
                                                                     provider,
                                                                     findConflictingProvider(provider, classLoader)));
            }

            ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            String appName = cData == null ? "" : cData.getJ2EEName().getApplication();

            Entry<String, ArrayList<ThreadContextProvider>> providerInfoNew = //
                            new SimpleEntry<String, ArrayList<ThreadContextProvider>>(appName, providersNew);
            providerInfo = providersPerClassLoader.putIfAbsent(key, providerInfoNew);
            if (providerInfo == null)
                providerInfo = providerInfoNew;

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "providers for", appName, classLoader, providerInfo);
        }
        return providerInfo.getValue();
    }
}
