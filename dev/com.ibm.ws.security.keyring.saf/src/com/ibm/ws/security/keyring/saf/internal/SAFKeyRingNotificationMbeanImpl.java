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
package com.ibm.ws.security.keyring.saf.internal;

import java.util.concurrent.ConcurrentHashMap;

import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.keyring.saf.KeyringNotificationMBean;
import com.ibm.ws.ssl.KeyringMonitor;

/**
 *
 */
@Component(configurationPolicy = ConfigurationPolicy.OPTIONAL,
           service = { KeyringNotificationMBean.class, DynamicMBean.class },
           immediate = true,
           property = { "service.vendor=IBM", "jmx.objectname=" + KeyringNotificationMBean.INSTANCE_NAME })
public class SAFKeyRingNotificationMbeanImpl extends StandardMBean implements KeyringNotificationMBean {

    static final TraceComponent tc = Tr.register(SAFKeyRingNotificationMbeanImpl.class);

    /** Concurrent map for (optional,multiple,dynamic) FileMonitor reference */
    private final ConcurrentHashMap<ServiceReference<KeyringMonitor>, String> keyRingMonitors = new ConcurrentHashMap<ServiceReference<KeyringMonitor>, String>();

    BundleContext bContext;

    /**
     * @param mbeanInterface
     * @throws NotCompliantMBeanException
     */
    public SAFKeyRingNotificationMbeanImpl() throws NotCompliantMBeanException {
        super(KeyringNotificationMBean.class, false);
    }

    @Reference(service = KeyringMonitor.class, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
    protected void setNotificationDelegate(ServiceReference<KeyringMonitor> monitorRef) {
        keyRingMonitors.put(monitorRef, (String) monitorRef.getProperty(KeyringMonitor.MONITOR_KEYSTORE_CONFIG_ID));
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting Keyring monitor reference", monitorRef, keyRingMonitors);
        }
    }

    /** service uninjection method */
    protected void unsetNotificationDelegate(ServiceReference<KeyringMonitor> monitorRef) {
        keyRingMonitors.remove(monitorRef);
    }

    /** {@inheritDoc} */
    @Override
    public boolean refreshRequested(String id) {
        boolean monitorFound = false;
        for (ServiceReference<KeyringMonitor> km : keyRingMonitors.keySet()) {
            String monitorConfigId = keyRingMonitors.get(km);
            String keyStoreLocation = (String) km.getProperty(KeyringMonitor.KEYSTORE_LOCATION);

            //Getting the service associated with the reference
            KeyringMonitor SAFMonitor = bContext.getService(km);
            if (SAFMonitor == null) {
                //bContext.getService can return null if the service backing the reference has been unregistered.
                //So removing from the list -- Defect 263386
                unsetNotificationDelegate(km);
            } else {
                try {
                    //Checking if the ID of the keystore to be refreshed is null
                    if (id != null) {
                        if (monitorConfigId != null && monitorConfigId.equalsIgnoreCase(id)) {
                            SAFMonitor.refreshRequested(keyStoreLocation);
                            monitorFound = true;
                        }
                    } else {
                        SAFMonitor.refreshRequested(keyStoreLocation);
                        monitorFound = true;
                    }
                } finally {
                    bContext.ungetService(km);
                }
            }
        }
        return monitorFound;
    }

    /**
     * DS-driven component activation
     */
    @Activate
    protected void activate(BundleContext bContext) throws Exception {
        this.bContext = bContext;
    }

    /**
     * DS-driven de-activation
     */
    @Deactivate
    protected void deactivate(int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Keyring monitor service deactivated", "reason=" + reason, keyRingMonitors);
        }

        keyRingMonitors.clear();
        this.bContext = null;
    }

}
