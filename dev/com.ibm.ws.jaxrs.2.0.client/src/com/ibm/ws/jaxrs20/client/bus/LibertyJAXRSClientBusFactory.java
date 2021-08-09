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
package com.ibm.ws.jaxrs20.client.bus;

import java.security.AccessController;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.Extension;
import org.apache.cxf.bus.extension.ExtensionManager;
import org.apache.cxf.bus.extension.ExtensionManagerImpl;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.api.ExtensionProvider;
import com.ibm.ws.jaxrs20.bus.LibertyApplicationBus;
import com.ibm.ws.jaxrs20.bus.LibertyApplicationBusFactory;
import com.ibm.ws.jaxrs20.bus.LibertyApplicationBusListener;
import com.ibm.ws.util.ThreadContextAccessor;

public class LibertyJAXRSClientBusFactory extends LibertyApplicationBusFactory {

    private static final TraceComponent tc = Tr.register(LibertyJAXRSClientBusFactory.class);

    private static final LibertyJAXRSClientBusFactory factory = new LibertyJAXRSClientBusFactory();

    //202957
    private static final ThreadContextAccessor THREAD_CONTEXT_ACCESSOR =
                    AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction());

    public static LibertyJAXRSClientBusFactory getInstance() {
        return factory;
    }

    private LibertyJAXRSClientBusFactory() {
        super();
    }

    public LibertyApplicationBus createClientScopedBus(String baseURI, ClassLoader appContextClassLoader) {

        if (baseURI == null || "".equalsIgnoreCase(baseURI))
            return null;

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("org.apache.cxf.bus.id", "RS-Client-Bus-" + baseURI);
        Map<Class<?>, Object> extensions = new HashMap<Class<?>, Object>();
        extensions.put(LibertyApplicationBus.Type.class, LibertyApplicationBus.Type.CLIENT);

        LibertyApplicationBus bus = createBus(extensions, properties, appContextClassLoader);

        return bus;
    }

    public LibertyApplicationBus getClientScopeBus(String id) {
        LibertyApplicationBus bus = null;

        if (id == null || "".equalsIgnoreCase(id))
            return null;

        //as there is a warining: Invalid character ':' or '=' in value part of property
        //replace : to -, = to @@ as the key
//        String id = JaxRSClientUtil.convertURItoBusId(baseURI);
//
//        LibertyJaxRsWorkQueueThreadContext wqtc = LibertyJaxRsWorkQueueThreadContextHelper.getThreadContext();
//        JaxRsClientMetaData clientMetaData = null;
//
//        /**
//         * for async thread of jaxrs engine, we should get clientMetaData from threadcontext
//         */
//        if (wqtc != null) {
//            clientMetaData = (JaxRsClientMetaData) wqtc.get(JaxRsClientMetaData.class);
//        }
//        /**
//         * in most cases, the thread from webcontainer can get the JaxRsModuleMetaData
//         */
//        if (clientMetaData == null)
//        {
//            clientMetaData = JaxRsMetaDataManager.getJaxRsModuleMetaData().getClientMetaData();
//        }
//
//        if (clientMetaData == null) {
//            throw new RuntimeException("JaxRsClientMetaData is not found");
//        }

//        Map<String, LibertyApplicationBus> busCache = clientMetaData.getBusCache();
//        synchronized (busCache) {
//            if (!busCache.containsKey(id)) {
        bus = createClientScopedBus(id, THREAD_CONTEXT_ACCESSOR.getContextClassLoader(Thread.currentThread()));
//                //bind provider cache to new created bus
//                clientMetaData.bindProviderCacheToBus(bus);
//                busCache.put(id, bus);
//            }
//            else {
//                bus = busCache.get(id);
//                AtomicInteger ai = bus.getBusCounter();
//                ai.incrementAndGet();
//            }
//        }

        return bus;
    }

    @Override
    protected void initializeBus(Bus bus) {

        /**
         * run initializers before bus.initialize()
         */
        if (listeners.isEmpty())
        {
            List<LibertyApplicationBusListener> listenersCopy = LibertyApplicationBusFactory.getInstance().getBusListenersCopy();

            if (listenersCopy != null) {
                for (LibertyApplicationBusListener listener : listenersCopy) {
                    listeners.add(listener);
                }
            }
        }
        if (!listeners.isEmpty())
        {
            for (LibertyApplicationBusListener listener : listeners) {
                listener.preInit(bus);
            }
        }

        /**
         * Add Override Extension provided by ExtensionProvider
         */

        ExtensionManager extensionManager = bus.getExtension(ExtensionManager.class);
        if (extensionManager != null && (extensionManager instanceof ExtensionManagerImpl)) {
            ExtensionManagerImpl managerImpl = (ExtensionManagerImpl) extensionManager;
            if (extensionProviders.isEmpty())
            {
                List<ExtensionProvider> extensionProvidersCopy = LibertyApplicationBusFactory.getInstance().getExtensionProviderCopy();
                if (extensionProvidersCopy != null) {
                    for (ExtensionProvider extensionProvider : extensionProvidersCopy) {
                        extensionProviders.add(extensionProvider);
                    }
                }

            }
            if (extensionProviders.isEmpty()) {
                for (ExtensionProvider extensionProvider : extensionProviders) {
                    Extension extension = extensionProvider.getExtension(bus);
                    if (extension != null) {
                        managerImpl.add(extension);
                    }
                }
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, (extensionManager == null ? "Unable to locate extension manager " : "The extension manager is not of type ExtensionManagerImpl")
                             + ", all the extensions from ExtensionProvider are ignored");
            }
        }
    }
}
