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
package com.ibm.ws.jaxrs20.metadata;

import org.apache.cxf.Bus;

import com.ibm.ws.jaxrs20.cache.LibertyJaxRsProviderCache;

/**
 * The class holds client runtime meta data for target application, those data will be recreated once the application is restarted.
 */
public class JaxRsClientMetaData {

    //private final Map<String, LibertyApplicationBus> busCache = new ConcurrentHashMap<String, LibertyApplicationBus>();

    private final LibertyJaxRsProviderCache providerCache = new LibertyJaxRsProviderCache();

    /**
     * using the counter to record all usage of the bus with the same url among webclients
     */
    //private final Map<String, AtomicInteger> busCounter = new ConcurrentHashMap<String, AtomicInteger>();

    private final JaxRsModuleMetaData moduleMetaData;

    public JaxRsClientMetaData(JaxRsModuleMetaData moduleMetaData) {
        this.moduleMetaData = moduleMetaData;
    }

    public void destroy() {
        //destroy provider cache
        providerCache.destroy();
    }

    public JaxRsModuleMetaData getModuleMetaData() {
        return moduleMetaData;
    }

//    /**
//     * @return busCache
//     */
//    public Map<String, LibertyApplicationBus> getBusCache() {
//        return busCache;
//    }

//    public Map<String, AtomicInteger> getBusCounter() {
//        return busCounter;
//    }

    public void bindProviderCacheToBus(Bus bus) {
        bus.setExtension(providerCache, LibertyJaxRsProviderCache.class);
    }

}
