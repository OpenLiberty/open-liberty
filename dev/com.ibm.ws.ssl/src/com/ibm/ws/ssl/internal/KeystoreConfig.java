/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.internal;

import java.util.Dictionary;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.ws.ssl.config.KeyStoreManager;
import com.ibm.ws.ssl.config.WSKeyStore;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Class to uniquely identify Keystore configuration objects. This is
 * not a DS managed service: see the KeystoreConfigurationFactory
 */
public class KeystoreConfig {
    /** Trace service */
    private static final TraceComponent tc = Tr.register(KeystoreConfig.class);

    private final String pid;
    private final String id;

    /** Atomic service reference held by the KeystoreConfigurationFactory */
    private final AtomicServiceReference<WsLocationAdmin> locSvc;

    /** The keystore represented by this configuration */
    private WSKeyStore wsKeyStore = null;
    private Dictionary<String, Object> properties = null;
    private ServiceRegistration<KeystoreConfig> registration = null;

    public KeystoreConfig(String pid, String id, AtomicServiceReference<WsLocationAdmin> locSvc) {
        this.pid = pid;
        this.id = id;
        this.locSvc = locSvc;
    }

    public String getPid() {
        return pid;
    }

    public String getId() {
        return id;
    }

    public WSKeyStore getKeyStore() {
        return wsKeyStore;
    }

    public String resolveString(String path) {
        return locSvc.getServiceWithException().resolveString(path);
    }

    /**
     * @return
     */
    public String getServerName() {
        return locSvc.getServiceWithException().getServerName();
    }

    /**
     * Create the new keystore based on the properties provided.
     * Package private.
     *
     * @param properties
     */
    synchronized boolean updateKeystoreConfig(Dictionary<String, Object> props) {
        properties = props;
        if (id != null) {
            properties.put(Constants.SSLPROP_ALIAS, id);
        }
        String location = (String) properties.get(LibertyConstants.KEY_KEYSTORE_LOCATION);
        if (location != null) {
            properties.put(Constants.SSLPROP_KEY_STORE, locSvc.getServiceWithException().resolveString(location));
        }

        try {
            // Try modifying the properties..
            // This may throw if arguments are bad
            wsKeyStore = new WSKeyStore(id, properties, this);
            KeyStoreManager.getInstance().addKeyStoreToMap(id, wsKeyStore);
            KeyStoreManager.getInstance().addKeyStoreToMap(pid, wsKeyStore);
            return true;
        } catch (Exception e) {
            wsKeyStore = null;
            return false;
        }
    }

    /**
     * Register this as a service in the service registry.
     * Package private.
     *
     * @param ctx Bundle context to register service with.
     */
    synchronized void updateRegistration(BundleContext ctx) {
        if (registration == null) {
            registration = ctx.registerService(KeystoreConfig.class, this, properties);
        } else {
            registration.setProperties(properties);
        }
    }

    /**
     * Remove the service registration
     * Package private.
     */
    synchronized void unregister() {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }
}
