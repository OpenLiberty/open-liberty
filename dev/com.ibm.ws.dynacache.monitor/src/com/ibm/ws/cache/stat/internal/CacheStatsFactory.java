/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.stat.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.stat.CachePerf;
import com.ibm.ws.cache.stat.CachePerfFactory;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;

@Component(service = CachePerfFactory.class, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = "service.vendor=IBM")
public class CacheStatsFactory implements CachePerfFactory {
    private static TraceComponent tc = Tr
            .register(CacheStatsFactory.class, "WebSphere Dynamic Cache Monitor", "com.ibm.ws.cache.resources.dynacache");

    @Override
    public CachePerf create(DCache cache) {
        final String methodName = "create()";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, cache.getCacheName());
        }

        CachePerf cachePerf = null;
        if (StatsFactory.isPMIEnabled()) {
            try {
                cachePerf = new CacheStatsModule(cache.getCacheName(), cache.getSwapToDisk());
            } catch (StatsFactoryException e) {
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, cachePerf);
        }

        return cachePerf;
    }

}
