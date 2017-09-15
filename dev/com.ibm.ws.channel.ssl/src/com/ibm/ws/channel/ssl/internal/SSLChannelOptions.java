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
package com.ibm.ws.channel.ssl.internal;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.channelfw.ChannelConfiguration;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.ssl.SSLConfiguration;

/**
 *
 */
public class SSLChannelOptions extends ChannelConfiguration {

    private static final TraceComponent tc =
                    Tr.register(SSLChannelOptions.class,
                                SSLChannelConstants.SSL_TRACE_NAME,
                                SSLChannelConstants.SSL_BUNDLE);

    private String sslRefId = null;
    private boolean useDefaultId = false;

    private Dictionary<String, Object> properties = null;
    private ServiceRegistration<ChannelConfiguration> registration = null;

    /**
     * Create the new keystore based on the properties provided.
     * Package private.
     * 
     * @param properties
     */
    void updateConfguration(Dictionary<String, Object> props, String defaultId) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "updateConfguration", props, defaultId);
        }

        String id = (String) props.get(SSLChannelProvider.SSL_CFG_REF);

        synchronized (this) {
            properties = props;

            if (id == null || id.isEmpty()) {
                useDefaultId = true;
                id = sslRefId = defaultId;
                properties.put(SSLChannelProvider.SSL_CFG_REF, defaultId);
            } else {
                sslRefId = id;
                useDefaultId = false;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "updateConfguration", id);
        }
    }

    /**
     * @param pid
     */
    synchronized void updateRefId(String newDefaultId) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "updateRefId", newDefaultId);
        }
        if (useDefaultId) {
            sslRefId = newDefaultId;
            properties.put(SSLChannelProvider.SSL_CFG_REF, sslRefId);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "updateRefId");
        }
    }

    @SuppressWarnings("unchecked")
    private void updateSuper() {
        if (properties instanceof Map<?, ?>) {
            super.modified((Map<String, Object>) properties);
        } else {
            Map<String, Object> map = new HashMap<String, Object>();
            Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                map.put(key, properties.get(key));
            }
            super.modified(map);
        }
    }

    /**
     * Register this as a service in the service registry. Sanity is preserved
     * in this one single method. SSL configurations (repertoires) are required
     * for the ssl connection to have a chance of working. This method needs to
     * ensure that the registration is only created (or updated) if that
     * referenced repertoire exists.
     * 
     * 
     * Package private.
     * 
     * @param ctx Bundle context to register service with.
     */
    synchronized void updateRegistration(BundleContext ctx, ConcurrentServiceReferenceMap<String, SSLConfiguration> sslConfigs) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "updateRegistration", ctx, sslConfigs);
        }

        if (ctx == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "updateRegistration");
            }
            return;
        }

        // Only activate this registration *IF* there is a valid SSL repertoire configuration available for it
        ServiceReference<SSLConfiguration> requiredRef = sslConfigs.getReference(sslRefId);
        if (requiredRef != null && sslConfigs.getService(sslRefId) != null) {
            properties.put(SSLChannelData.ALIAS_KEY, sslRefId);
            // Set the service reference id as the alias 
            updateSuper();
            if (registration == null) {
                registration = ctx.registerService(ChannelConfiguration.class, this, properties);
            } else {
                // Only update the service properties if we have outstanding changes
                registration.setProperties(properties);
            }
        } else {
            // No shoes, no shirt, no service.
            unregister();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "updateRegistration");
        }
    }

    /**
     * Remove the service registration
     * Package private.
     */
    synchronized void unregister() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "unregister", this);
        }
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }
}
