/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.concurrent.context;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializationInfo;

import io.openliberty.concurrent.processor.ContextServiceResourceFactoryBuilder;
import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.spi.ThreadContextProvider;

/**
 * Java EE application component context service provider.
 */
@Component(name = "io.openliberty.thirdparty.context.provider",
           configurationPolicy = ConfigurationPolicy.IGNORE)
public class ThirdPartyContextCoordinator implements com.ibm.wsspi.threadcontext.ThreadContextProvider {
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

    // TODO clear entry when application is uninstalled to avoid leaking memory
    /**
     * Mapping to a list of available thread context providers that are available for a thread context class loader.
     * The list determines the order in which thread context should be captured and applied to threads (after the container-implemented providers).
     * It is the reverse of the order in which thread context is restored on threads (before the container-implemented providers).
     */
    private final ConcurrentHashMap<Object, ArrayList<ThreadContextProvider>> providersPerClassLoader = //
                    new ConcurrentHashMap<Object, ArrayList<ThreadContextProvider>>();

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

        context.coordinator = this;
        // TODO recreate cleared context
        return context;
    }

    @Override
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
        ArrayList<ThreadContextProvider> providers = providersPerClassLoader.get(key);
        if (providers == null) {
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
                    throw new IllegalStateException("duplicate provider: " + type); // TODO Tr.formatMessage(tc, "CWWKC1150.duplicate.context", type, provider, findConflictingProvider(provider, classloader)));
            }
            providers = providersPerClassLoader.putIfAbsent(key, providersNew);
            if (providers == null)
                providers = providersNew;
        }
        return providers;
    }
}
