/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.bus;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.bus.extension.Extension;
import org.apache.cxf.bus.extension.ExtensionManager;
import org.apache.cxf.bus.extension.ExtensionManagerImpl;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.jaxws.metadata.JaxWsModuleMetaData;
import com.ibm.ws.jaxws.support.LibertyLoggingInInterceptor;
import com.ibm.ws.jaxws.support.LibertyLoggingOutInterceptor;
import com.ibm.ws.util.ThreadContextAccessor;

/**
 * LibertyApplicationBusFactory is used to create the bus instance for both server and client side in the server.
 */
public class LibertyApplicationBusFactory extends CXFBusFactory {

    private static final TraceComponent tc = Tr.register(LibertyApplicationBusFactory.class);

    private static final ThreadContextAccessor THREAD_CONTEXT_ACCESSOR = AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction());

    private static final LibertyApplicationBusFactory INSTANCE = new LibertyApplicationBusFactory();

    private final List<LibertyApplicationBusListener> listeners = new CopyOnWriteArrayList<LibertyApplicationBusListener>();

    private final List<ExtensionProvider> extensionProviders = new CopyOnWriteArrayList<ExtensionProvider>();

    private final List<Bus> serverScopedBuses = Collections.synchronizedList(new ArrayList<Bus>());

    private final List<Bus> clientScopedBuses = Collections.synchronizedList(new ArrayList<Bus>());

    public static LibertyApplicationBusFactory getInstance() {
        return INSTANCE;
    }

    public LibertyApplicationBus createServerScopedBus(JaxWsModuleMetaData moduleMetaData) {
        ModuleInfo moduleInfo = moduleMetaData.getModuleInfo();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("org.apache.cxf.bus.id", moduleInfo.getName() + "-Server-Bus");
        Map<Class<?>, Object> extensions = new HashMap<Class<?>, Object>();
        extensions.put(ClassLoader.class, moduleMetaData.getAppContextClassLoader());
        extensions.put(JaxWsModuleMetaData.class, moduleMetaData);
        extensions.put(LibertyApplicationBus.Type.class, LibertyApplicationBus.Type.SERVER);

        final ClassLoader moduleClassLoader = moduleInfo.getClassLoader();
        return createBus(extensions, properties, moduleClassLoader);
    }

    public LibertyApplicationBus createClientScopedBus(JaxWsModuleMetaData moduleMetaData) {
        ModuleInfo moduleInfo = moduleMetaData.getModuleInfo();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("org.apache.cxf.bus.id", moduleInfo.getName() + "-Client-Bus");
        Map<Class<?>, Object> extensions = new HashMap<Class<?>, Object>();
        extensions.put(ClassLoader.class, moduleMetaData.getAppContextClassLoader());
        extensions.put(JaxWsModuleMetaData.class, moduleMetaData);
        extensions.put(LibertyApplicationBus.Type.class, LibertyApplicationBus.Type.CLIENT);

        final ClassLoader moduleClassLoader = moduleInfo.getClassLoader();
        return createBus(extensions, properties, moduleClassLoader);
    }

    @Override
    public Bus createBus(Map<Class<?>, Object> e, Map<String, Object> properties) {
        return createBus(e, properties, THREAD_CONTEXT_ACCESSOR.getContextClassLoader(Thread.currentThread()));
    }

    public LibertyApplicationBus createBus(Map<Class<?>, Object> e, Map<String, Object> properties, ClassLoader classLoader) {

        Bus originalBus = getThreadDefaultBus(false);

        final Map<Class<?>, Object> e1 = e;
        final Map<String, Object> properties1 = properties;
        final ClassLoader classLoader1 = classLoader;
        try {
            LibertyApplicationBus bus = AccessController.doPrivileged(new PrivilegedAction<LibertyApplicationBus>() {
                @Override
                public LibertyApplicationBus run() {
                    return new LibertyApplicationBus(e1, properties1, classLoader1);
                }
            });
            //Considering that we have set the default bus in JaxWsService, no need to set default bus
            //Also, it avoids polluting the thread bus.
            //possiblySetDefaultBus(bus);

            /* initialize the bus */
            initializeBus(bus);
            BusLifeCycleManager lifeCycleManager = bus.getExtension(BusLifeCycleManager.class);
            if (lifeCycleManager != null) {
                lifeCycleManager.registerLifeCycleListener(new BusLifeCycleListenerAdapter(bus));
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to locate LifeCycleManager for the bus " + bus.getId()
                                 + ", postShutdown and preShutDown methods configured in LibertyApplicationBusListener will not be invoked");
                }
            }

            bus.initialize();

            // Always register LibertyLoggingIn(Out)Interceptor Pretty print the SOAP Messages
            final LibertyLoggingInInterceptor in = new LibertyLoggingInInterceptor();
            in.setPrettyLogging(true);
            bus.getInInterceptors().add(in);

            final LibertyLoggingOutInterceptor out = new LibertyLoggingOutInterceptor();
            out.setPrettyLogging(true);
            bus.getOutInterceptors().add(out);

            return bus;

        } finally {
            setThreadDefaultBus(originalBus);
        }
    }

    @Override
    protected void initializeBus(Bus bus) {
        super.initializeBus(bus);

        /**
         * run initializers before bus.initialize()
         */
        for (LibertyApplicationBusListener listener : listeners) {
            listener.preInit(bus);
        }

        /**
         * Add Override Extension provided by ExtensionProvider
         */
        ExtensionManager extensionManager = bus.getExtension(ExtensionManager.class);
        if (extensionManager != null && (extensionManager instanceof ExtensionManagerImpl)) {
            ExtensionManagerImpl managerImpl = (ExtensionManagerImpl) extensionManager;
            for (ExtensionProvider extensionProvider : extensionProviders) {
                Extension extension = extensionProvider.getExtension(bus);
                if (extension != null) {
                    managerImpl.add(extension);
                }
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, (extensionManager == null ? "Unable to locate extension manager " : "The extension manager is not of type ExtensionManagerImpl")
                             + ", all the extensions from ExtensionProvider are ignored");
            }
        }
    }

    /**
     * register LibertyApplicationBusListener to bus factory, those methods will be invoked with the bus lifecycle
     *
     * @param initializer
     */
    public void registerApplicationBusListener(LibertyApplicationBusListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * unregister LibertyApplicationBusListener from the bus factory
     */
    public void unregisterApplicationBusListener(LibertyApplicationBusListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public List<Bus> getServerScopedBuses() {
        return new ArrayList<Bus>(serverScopedBuses);
    }

    public List<Bus> getClientScopedBuses() {
        return new ArrayList<Bus>(clientScopedBuses);
    }

    /**
     * Provide a new implemented static method for setting the default Bus.
     * The static method from parent class BusFactory also tries to change the thread bus, which is not required.
     * Use BusFactory.class as the synchronized lock due to the signature of the BusFactory.setDefaultBus is
     * public static void synchronized setDefaultBus(Bus bus)
     *
     * @param bus
     */
    public static void setDefaultBus(Bus bus) {
        synchronized (BusFactory.class) {
            if (bus == null) {
                defaultBus = null;
            } else {
                defaultBus = bus;
            }
        }
    }

    public void registerExtensionProvider(ExtensionProvider extensionProvider) {
        if (extensionProvider != null) {
            extensionProviders.add(extensionProvider);
        }
    }

    public void unregisterExtensionProvider(ExtensionProvider extensionProvider) {
        if (extensionProvider != null) {
            extensionProviders.remove(extensionProvider);
        }
    }

    private class BusLifeCycleListenerAdapter implements BusLifeCycleListener {

        private final Bus bus;

        public BusLifeCycleListenerAdapter(Bus bus) {
            this.bus = bus;
        }

        @Override
        public void initComplete() {
            for (LibertyApplicationBusListener listener : listeners) {
                listener.initComplete(bus);
            }
        }

        @Override
        public void postShutdown() {
            try {
                for (LibertyApplicationBusListener listener : listeners) {
                    listener.postShutdown(bus);
                }
            } finally {
                if (!serverScopedBuses.remove(bus)) {
                    clientScopedBuses.remove(bus);
                }
            }
        }

        @Override
        public void preShutdown() {
            for (LibertyApplicationBusListener listener : listeners) {
                listener.preShutdown(bus);
            }
        }
    }
}
