/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.jaspi;

import java.io.File;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SecurityPermission;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.auth.message.config.RegistrationListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.security.jaspi.ProviderService;

/**
 * This class implements the SPI contract for AuthConfigFactory defined in JSR-196.
 */
public class ProviderRegistry extends AuthConfigFactory {

    private static final TraceComponent tc = Tr.register(ProviderRegistry.class, "Security", null);

    /*
     * The Cache key is a unique registrationID of the form "layer[appContext]".
     * The layer & appContext values are as specified in JSR-196 spec, e.g. "HttpServlet[default_host /snoop]"
     */
    private final Map<RegistrationID, CacheEntry<AuthConfigProvider, RegistrationContext, Collection<RegistrationListener>>> cache =
                    new HashMap<RegistrationID, CacheEntry<AuthConfigProvider, RegistrationContext, Collection<RegistrationListener>>>();
    private final Lock lock = new ReentrantLock();
    private PersistenceManager persistenceMgr = null;
    private static String registerDefaultProviderForAllContexts = "com.ibm.websphere.jaspi.registerDefaultProviderForAllContexts";

    private static class CacheEntry<P, C, L> {
        P provider; // AuthConfigProvider
        C context; // RegistrationContext
        L listeners; // RegistrationListeners

        CacheEntry(P provider, C context, L listeners) {
            this.provider = provider;
            this.context = context;
            this.listeners = listeners;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("CacheEntry[");
            builder.append(context + ",provider=" + provider + ",listeners=" + listeners);
            return builder.append("]").toString();
        }
    }

    RegistrationID defaultRegistrationID = new RegistrationID(null, null);

    private class Context implements RegistrationContext {

        public String layer, appContext, description;
        public boolean isPersistent;

        public Context(boolean isPersistent, String layer, String appContext, String description) {
            this.isPersistent = isPersistent;
            this.layer = layer;
            this.appContext = appContext;
            this.description = description;
        }

        @Override
        public String getAppContext() {
            return appContext;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getMessageLayer() {
            return layer;
        }

        @Override
        public boolean isPersistent() {
            return isPersistent;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("RegistrationContext[");
            builder.append("layer=" + layer + ",appContext=" + appContext + ",isPersistent=" + isPersistent + ",description=" + description);
            return builder.append("]").toString();
        }
    }

    /*
     * An instance of this class is created by AuthConfigFactory.getFactory().
     * It must register providers defined in the configuration as stated in spec:
     * "This constructor must support the construction and registration (including
     * self-registration) of AuthConfigProviders from a persistent declarative
     * representation."
     */
    public ProviderRegistry() {
        super();
        String persistConfigFileName = System.getProperty(PersistenceManager.JASPI_CONFIG);
        if (persistConfigFileName != null && !persistConfigFileName.isEmpty()) {
            String path = JaspiServiceImpl.getServerResourceAbsolutePath(persistConfigFileName);
            // if we can't get an absolute path use the property value
            path = path != null ? path : persistConfigFileName;
            File persistConfigFile = new File(path);
            persistenceMgr = new XMLJaspiConfiguration();
            persistenceMgr.setAuthConfigFactory(this);
            persistenceMgr.setFile(persistConfigFile);
            persistenceMgr.load();
        }
        else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "System property " + PersistenceManager.JASPI_CONFIG + " not set, persistent config will not be used");
        }
    }

    public PersistenceManager getPersistenceManager() {
        return persistenceMgr;
    }

    @Override
    public String[] detachListener(RegistrationListener listener, String layer, String appContext) {
        checkPermission(AuthConfigFactory.PROVIDER_REGISTRATION_PERMISSION_NAME);
        Collection<String> registrationIDs = new HashSet<String>();
        for (RegistrationID id : cache.keySet()) {
            CacheEntry<AuthConfigProvider, RegistrationContext, Collection<RegistrationListener>> entry = cache.get(id);
            if (entry != null) {
                if (matchesRegistrationContext(layer, appContext, entry.context)) {
                    registrationIDs.add(id.toString());
                    if (!entry.listeners.isEmpty()) {
                        lock.lock();
                        try {
                            entry.listeners.remove(listener);
                        } finally {
                            lock.unlock();
                        }
                    }
                }
            }
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "detachListener registrationIDs", registrationIDs);
        return registrationIDs.toArray(new String[0]);
    }

    /*
     * Implements lookup algorithm following the precedence rules in JSR-196:
     * 
     * All factories shall employ the following precedence rules to select the registered AuthConfigProvider that
     * matches the layer and appContext arguments:
     * 
     * (1) The provider that is specifically registered for both the corresponding message layer and appContext shall be selected.
     * (2) If no provider is selected according to the preceding rule, the provider specifically registered for the corresponding
     * appContext and for all message layers shall be selected.
     * (3) If no provider is selected according to the preceding rules, the provider specifically registered for the corresponding
     * message layer and for all appContexts shall be selected.
     * (4) If no provider is selected according to the preceding rules, the provider registered for all message layers and for all
     * appContexts shall be selected.
     * (5) If no provider is selected according to the preceding rules, the factory shall terminate its search for a registered provider.
     * 
     * The above precedence rules apply equivalently to registrations created with a null or non-null className argument.
     */
    @Override
    public AuthConfigProvider getConfigProvider(String layer, String appContext, RegistrationListener listener) {
        CacheEntry<AuthConfigProvider, RegistrationContext, Collection<RegistrationListener>> entry = null;
        lock.lock();
        try {
            if (layer != null && appContext != null)
                entry = cache.get(new RegistrationID(layer, appContext)); // rule (1)
            if (entry == null) {
                if (appContext != null)
                    entry = cache.get(new RegistrationID(null, appContext)); // rule (2)
                if (entry == null) {
                    if (layer != null)
                        entry = cache.get(new RegistrationID(layer, null)); // rule (3)
                    if (entry == null) {
                        entry = cache.get(new RegistrationID(null, null)); // rule (4)
                    }
                }
            }

            if (listener != null && entry != null) {
                entry.listeners.add(listener);
            }
        } finally {
            lock.unlock();
        }
        AuthConfigProvider provider = entry == null ? null : entry.provider;
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getConfigProvider entry", entry);
        return provider;
    }

    @Override
    public RegistrationContext getRegistrationContext(String registrationID) {
        RegistrationContext context = null;
        CacheEntry<AuthConfigProvider, RegistrationContext, Collection<RegistrationListener>> entry = null;
        if ((entry = cache.get(new RegistrationID(registrationID))) != null) {
            context = entry.context;
        }
        return context;
    }

    @Override
    public String[] getRegistrationIDs(AuthConfigProvider provider) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getRegistrationIDs");
        Set<String> registrationIDs = new HashSet<String>();
        if (provider == null) {
            for (RegistrationID key : cache.keySet()) {
                registrationIDs.add(key.toString());
            }
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getRegistrationIDs", registrationIDs);
            return registrationIDs.toArray(new String[0]);
        }
        Set<Entry<RegistrationID, CacheEntry<AuthConfigProvider, RegistrationContext, Collection<RegistrationListener>>>> entries = cache.entrySet();
        for (Entry<RegistrationID, CacheEntry<AuthConfigProvider, RegistrationContext, Collection<RegistrationListener>>> entry : entries) {
            if (provider.equals(entry.getValue().provider)) {
                registrationIDs.add(entry.getKey().toString());
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "getRegistrationIDs", registrationIDs);
        return registrationIDs.toArray(new String[0]);
    }

    @Override
    public void refresh() {
        // TODO depends on whether or not we support dynamic changes to security.xml/domain-security.xml or to app-bindings 
        checkPermission(AuthConfigFactory.PROVIDER_REGISTRATION_PERMISSION_NAME);
    }

    @Override
    public String registerConfigProvider(AuthConfigProvider provider, String layer, String appContext, String description) {
        checkPermission(AuthConfigFactory.PROVIDER_REGISTRATION_PERMISSION_NAME);
        String registrationID = registerProvider(false, provider, layer, appContext, description);
        return registrationID;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String registerConfigProvider(String className, Map properties, String layer, String appContext, String description) {
        checkPermission(AuthConfigFactory.PROVIDER_REGISTRATION_PERMISSION_NAME);
        AuthConfigProvider provider = newInstance((AuthConfigFactory) null, className, true, doPrivGetContextClassLoader(), properties);
        String registrationID = registerProvider(true, provider, layer, appContext, description);
        if (persistenceMgr != null) {
            persistenceMgr.registerProvider(className, properties, layer, appContext, description);
        }
        return registrationID;
    }

    protected String registerProvider(boolean isPersistent, AuthConfigProvider provider, String layer, String appContext, String description) {
        RegistrationID registrationID = new RegistrationID(layer, appContext);
        RegistrationContext context = new Context(isPersistent, layer, appContext, description);
        CacheEntry<AuthConfigProvider, RegistrationContext, Collection<RegistrationListener>> newEntry;
        newEntry = new CacheEntry<AuthConfigProvider, RegistrationContext, Collection<RegistrationListener>>(provider, context, null);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "adding new entry to provider cache", newEntry);
        lock.lock();
        try {
            CacheEntry<AuthConfigProvider, RegistrationContext, Collection<RegistrationListener>> oldEntry = cache.put(registrationID, newEntry);
            if (oldEntry == null) {
                newEntry.listeners = new HashSet<RegistrationListener>();
            } else {
                newEntry.listeners = oldEntry.listeners;
            }
        } finally {
            lock.unlock();
        }
        notifyListener(newEntry.listeners, layer, appContext);
        return registrationID.toString();
    }

    protected void notifyListener(Collection<RegistrationListener> listeners, String layer, String appContext) {
        boolean hasListeners = listeners != null && !listeners.isEmpty();
        if (hasListeners) {
            for (RegistrationListener listener : listeners) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "notifyListener", listener);
                listener.notify(layer, appContext);
            }
        }
    }

    @Override
    public boolean removeRegistration(String registrationID) {
        checkPermission(AuthConfigFactory.PROVIDER_REGISTRATION_PERMISSION_NAME);
        CacheEntry<AuthConfigProvider, RegistrationContext, Collection<RegistrationListener>> entry = null;
        boolean removed = false;
        if (registrationID != null) {
            RegistrationID id = new RegistrationID(registrationID);
            lock.lock();
            try {
                entry = cache.remove(id);
                removed = entry != null ? true : false;
            } finally {
                lock.unlock();
            }
            if (entry != null) {
                String layer = entry.context.getMessageLayer();
                String appContext = entry.context.getAppContext();
                notifyListener(entry.listeners, layer, appContext);
                if (persistenceMgr != null) {
                    persistenceMgr.removeProvider(layer, appContext);
                }
            }
        }
        return removed;
    }

    protected void checkPermission(String permName) {
        SecurityManager secMgr = System.getSecurityManager();
        if (secMgr != null) {
            secMgr.checkPermission(new SecurityPermission(permName));
        }
    }

    protected AuthConfigProvider newInstance(AuthConfigFactory factory, String className, boolean init, ClassLoader loader, Map<?, ?> properties) {
        AuthConfigProvider provider = null;
        if (className != null)
            try {
                if (properties != null) {
                    for (Map.Entry<?, ?> entry : properties.entrySet()) {
                        boolean isValid = (entry.getKey() instanceof String) && (entry.getValue() instanceof String);
                        if (!isValid) {
                            throw new IllegalArgumentException("All keys and values in properties parameter must be of type String.");
                        }
                    }
                }
                Class<?> clz = Class.forName(className, init, loader == null ? doPrivGetContextClassLoader() : loader);
                Constructor<?> ctor = clz.getConstructor(Map.class, AuthConfigFactory.class);
                Object obj = ctor.newInstance(properties, factory);
                if (obj instanceof AuthConfigProvider)
                    provider = (AuthConfigProvider) obj;
            } catch (Throwable t) {
                throw new SecurityException("Unable to create a provider, class name: " + className, t);
            }
        return provider;
    }

    protected boolean matchesRegistrationContext(String layer, String appContext, RegistrationContext context) {
        boolean match = false;
        if (context != null) {
            String ctxLayer = context.getMessageLayer();
            String ctxId = context.getAppContext();
            if (ctxLayer != null && ctxId != null) {
                match = ctxLayer.equals(layer) && ctxId.equals(appContext); // layer & appContext must match 
            } else if (ctxLayer == null && ctxId == null) { // anything is a match
                match = true;
            } else if (ctxLayer == null && ctxId != null) { // appContext must match
                match = ctxId.equals(appContext);
            } else if (ctxLayer != null && ctxId == null) { // layer must match
                match = ctxLayer.equals(layer);
            }
        }
        return match;
    }

    protected ClassLoader doPrivGetContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    /**
     * Called when a Liberty user defined feature provider is set or unset
     * 
     * @param providerService the provider if set, null if unset
     */
    public AuthConfigProvider setProvider(ProviderService providerService) {
        AuthConfigProvider authConfigProvider = null;
        if (providerService != null) {
            authConfigProvider = providerService.getAuthConfigProvider(this);
            registerConfigProvider(authConfigProvider, null, null, null);
        } else {
            removeRegistration(defaultRegistrationID.toString());
        }
        return authConfigProvider;
    }

    /**
     * Returns true if any providers are registered
     */
    boolean isAnyProviderRegistered() {
        return !cache.isEmpty();
    }
}
