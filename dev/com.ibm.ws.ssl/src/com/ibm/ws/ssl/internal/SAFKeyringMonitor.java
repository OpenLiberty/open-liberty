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

import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.filemonitor.FileBasedActionable;
import com.ibm.ws.ssl.KeyringMonitor;

/**
 * The security keyringMonitor gets notified through the mbean call and it will tell the
 * actionable to perform its action.
 */
public class SAFKeyringMonitor implements KeyringMonitor {

    /** Trace service */
    private static final TraceComponent tc = Tr.register(SAFKeyringMonitor.class);

    private final FileBasedActionable actionable;

    public SAFKeyringMonitor(FileBasedActionable fileBasedActionable) {
        this.actionable = fileBasedActionable;
    }

    /**
     * Registers this KeyringMonitor to start monitoring the specified SAF keyrings
     * by mbean notification.
     *
     * @param name of keyrings to monitor.
     * @param trigger what trigger the keyring update notification mbean
     * @return The <code>KeyringMonitor</code> service registration.
     */
    public ServiceRegistration<KeyringMonitor> monitorKeyRings(String name, String trigger) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "monitorKeyRing registration for", name);
        }
        BundleContext bundleContext = actionable.getBundleContext();
        final Hashtable<String, Object> keyRingMonitorProps = new Hashtable<String, Object>();
        keyRingMonitorProps.put(KeyringMonitor.KEYRING_NAME, name);
        if (!(trigger.equalsIgnoreCase("disabled")) && trigger.equals("polled")) {
            Tr.warning(tc, "Cannot have polled trigger for keyRing name", name);
        }
        return bundleContext.registerService(KeyringMonitor.class, this, keyRingMonitorProps);
    }

    /** {@inheritDoc} */
    @Override
    public void refreshRequested(String name) {
        actionable.performFileBasedAction(null);
    }

}
