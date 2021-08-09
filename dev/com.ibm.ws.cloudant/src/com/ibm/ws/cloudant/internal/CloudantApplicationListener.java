/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cloudant.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;

/**
 * Clears out Cloudant Client caches when applications are stopped in order to avoid memory leaks.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class CloudantApplicationListener implements ApplicationStateListener {
    private static final TraceComponent tc = Tr.register(CloudantApplicationListener.class);

    private final ConcurrentMap<CloudantService, ConcurrentMap<ClientKey, Object>> registrations = new ConcurrentHashMap<CloudantService, ConcurrentMap<ClientKey, Object>>();

    @Override
    public void applicationStarting(ApplicationInfo appInfo) {
    }

    @Override
    public void applicationStarted(ApplicationInfo appInfo) {
    }

    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
    }

    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        if (!registrations.isEmpty()) {
            String appName = appInfo.getName();
            for (ConcurrentMap<ClientKey, Object> cache : registrations.values())
                for (Iterator<Entry<ClientKey, Object>> it = cache.entrySet().iterator(); it.hasNext(); ) {
                    Entry<ClientKey, Object> cacheEntry = it.next();
                    String identifier = cacheEntry.getKey().getApplicationClassLoaderIdentifier();
                    if (identifier != null) {
                        // Example classloader identifier:  WebModule:cloudantfat#cloudantfat.war
                        int start = identifier.indexOf(':') + 1;
                        int len = appName.length();
                        if (start > 0
                                && identifier.length() - start > len
                                && identifier.charAt(start + len) == '#'
                                && appName.equals(identifier.substring(start, start + len))) {
                            final Object client = cacheEntry.getValue();

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(this, tc, "remove and shut down cached client", client);

                            // remove from cache to avoid classloader leak
                            it.remove();

                            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                                @Override
                                public Void run() {
                                    try {
                                        client.getClass().getMethod("shutdown").invoke(client);
                                    } catch (Throwable x) {
                                        // FFDC will be logged, otherwise ignore
                                    }
                                    return null;
                                }
                            });
                        }
                    }
                }
        }
    }

    /**
     * Lazily registers a CloudantService to have its client cache purged of entries related to a stopped application.
     *  
     * @param svc CloudantService instance. This is used as a HashMap key because the client cache itself is not suitable as a key.
     * @param clients client cache for the CloudantService.
     */
    void register(CloudantService svc, ConcurrentMap<ClientKey, Object> clients) {
        registrations.put(svc, clients);
    }

    /**
     * Unregister the specified CloudantService. This should be invoked upon CloudantService.deactivate.
     * 
     * @param svc CloudantService to unregister.
     */
    void unregister(CloudantService svc) {
        registrations.remove(svc);
    }
}