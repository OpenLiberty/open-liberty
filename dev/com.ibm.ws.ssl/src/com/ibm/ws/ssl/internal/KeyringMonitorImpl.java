/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.internal;

import java.util.Arrays;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ssl.KeyringMonitor;

/**
 * The security keyringMonitor gets notified through the mbean call and it will tell the
 * actionable to perform its action.
 */
public class KeyringMonitorImpl implements KeyringMonitor {

    /** Trace service */
    private static final TraceComponent tc = Tr.register(KeyringMonitorImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private final KeyringBasedActionable actionable;

    public KeyringMonitorImpl(KeyringBasedActionable fileBasedActionable) {
        this.actionable = fileBasedActionable;
    }

    /**
     * Registers this KeyringMonitor to start monitoring the specified keyrings
     * by mbean notification.
     *
     * @param id      of keyrings to monitor.
     * @param trigger what trigger the keyring update notification mbean
     * @return The <code>KeyringMonitor</code> service registration.
     */
    public ServiceRegistration<KeyringMonitor> monitorKeyRings(String ID, String trigger, String keyStoreLocation) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "monitorKeyRing registration for", ID);
        }
        BundleContext bundleContext = actionable.getBundleContext();
        final Hashtable<String, Object> keyRingMonitorProps = new Hashtable<String, Object>();
        keyRingMonitorProps.put(KeyringMonitor.MONITOR_KEYSTORE_CONFIG_ID, ID);
        keyRingMonitorProps.put(KeyringMonitor.KEYSTORE_LOCATION, keyStoreLocation);
        if (!(trigger.equalsIgnoreCase("disabled")) && trigger.equals("polled")) {
            Tr.warning(tc, "Cannot have polled trigger for keyRing ID: ", ID);
        }
        return bundleContext.registerService(KeyringMonitor.class, this, keyRingMonitorProps);
    }

    /** {@inheritDoc} */
    @Override
    public void refreshRequested(String keyStoreLocation) {
        actionable.performKeyStoreAction(Arrays.asList(keyStoreLocation));
    }

}
