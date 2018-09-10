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
package com.ibm.ws.ssl.internal;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.filemonitor.FileBasedActionable;
import com.ibm.ws.security.filemonitor.SecurityFileMonitor;
import com.ibm.ws.ssl.config.KeyStoreManager;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * This accepts configuration from the service registry, creates the KeystoreConfig
 * for that configuration (or retrieves one it already created), verifies that the
 * attributes make sense and are valid, and then registers the KeystoreConfig
 * into the service registry so that it will play well with the rest of the services.
 *
 * This is a managed service factory (rather than another DS service) to ensure that
 * we can yank/deregister/table/remove the service if the configuration is bad.
 * It is hard to do this kind of on-the-fly verification within a modified method
 * of a DS service: throwing the exception does not deactivate the component,
 * it just fails the update.
 */
@Component(service = ManagedServiceFactory.class,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM", "service.pid=com.ibm.ws.ssl.keystore" })
public class KeystoreConfigurationFactory implements ManagedServiceFactory, FileBasedActionable {
    /** Trace service */
    private static final TraceComponent tc = Tr.register(KeystoreConfigurationFactory.class);

    private final AtomicServiceReference<WsLocationAdmin> locSvc = new AtomicServiceReference<WsLocationAdmin>("LocMgr");
    private final ConcurrentHashMap<String, KeystoreConfig> keyConfigs = new ConcurrentHashMap<String, KeystoreConfig>();
    private ServiceRegistration<FileMonitor> keyStoreFileMonitorRegistration;
    private SecurityFileMonitor keyStoreFileMonitor;

    private BundleContext bContext = null;
    private volatile ComponentContext cc = null;

    @SuppressWarnings("unchecked")
    @Override
    @FFDCIgnore(IllegalArgumentException.class)
    public void updated(String pid, Dictionary properties) throws ConfigurationException {
        // If we are stopping ignore the update
        if (FrameworkState.isStopping()) {
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "updated keystore " + pid, properties);
        }

        String id = (String) properties.get(LibertyConstants.KEY_ID);
        KeystoreConfig svc = null;
        KeystoreConfig old = keyConfigs.get(pid);

        if (old == null) {
            svc = new KeystoreConfig(pid, id, locSvc);
            old = keyConfigs.putIfAbsent(pid, svc);
        }

        if (old != null) {
            svc = old;
        }

        // Try to update the keystore (which involves generating a new WSKeyStore.. )
        // If it succeeds, register the service in the bundle context
        // If it fails, unregister it.
        try {
            if (svc.updateKeystoreConfig(properties)) {
                svc.updateRegistration(bContext);

                //if needed set the file monitor
                String trigger = svc.getKeyStore().getTrigger();
                Boolean fileBased = svc.getKeyStore().getFileBased();
                if (!(trigger.equalsIgnoreCase("disabled")) && fileBased.booleanValue()) {
                    createFileMonitor(svc.getKeyStore().getLocation(), trigger, svc.getKeyStore().getPollingRate());
                }
            } else {
                svc.unregister();
            }

        } catch (IllegalStateException e) {
            // This must mean the bundle was stopped, which happens only if
            // we're trying to update and shut down simultaneously.
            // Harmless, so discard the exception rather than report FFDC.
        }
    }

    @Override
    public void deleted(String pid) {
        KeystoreConfig old = keyConfigs.get(pid);
        if (old != null) {
            KeyStoreManager.getInstance().clearKeyStoreFromMap(pid);
            KeyStoreManager.getInstance().clearKeyStoreFromMap(keyConfigs.get(pid).getId());
            old.unregister();

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "deleted keystore " + pid);
            }
        }
    }

    @Override
    public String getName() {
        return "Keystore configuration";
    }

    protected void activate(ComponentContext ctx) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "activate", ctx.getProperties());
        }
        cc = ctx;
        locSvc.activate(ctx);
        bContext = ctx.getBundleContext();
    }

    protected void deactivate(ComponentContext ctx, int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "deactivate, reason=" + reason);
        }
        locSvc.deactivate(ctx);
        unsetFileMonitorRegistration();
    }

    /**
     * Set the reference to the location manager.
     * Dynamic service: always use the most recent.
     *
     * @param locSvc Location service
     */
    @Reference(service = WsLocationAdmin.class, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setLocMgr(ServiceReference<WsLocationAdmin> locSvc) {
        this.locSvc.setReference(locSvc);
    }

    /**
     * Remove the reference to the location manager:
     * required service, do nothing.
     */
    protected void unsetLocMgr(ServiceReference<WsLocationAdmin> ref) {}

    /**
     * The specified files have been modified and we need to clear the SSLContext caches and
     * keystore caches. This will cause the new keystore file to get loaded on the next use of the
     * ssl context. If the keystore associated with the SSLContext that the process is using then
     * the process SSLContext needs to be reloaded.
     */
    @Override
    public void performFileBasedAction(Collection<File> modifiedFiles) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "performFileBasedAction", new Object[] { modifiedFiles });

        try {
            com.ibm.ws.ssl.provider.AbstractJSSEProvider.clearSSLContextCache();
            com.ibm.ws.ssl.config.KeyStoreManager.getInstance().clearJavaKeyStoresFromKeyStoreMap();
            com.ibm.ws.ssl.config.SSLConfigManager.getInstance().resetDefaultSSLContextIfNeeded(modifiedFiles);
            Tr.audit(tc, "ssl.keystore.modified.CWPKI0811I", modifiedFiles.toArray());
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception while trying to reload keystore file, exception is: " + e.getMessage());
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "performFileBasedAction");
    }

    /**
     * Retrieves the BundleContext, assuming we're still valid. If we've been
     * deactivated, then the registration no longer needs / can happen and in
     * that case return null.
     *
     * @return The BundleContext if available, {@code null} otherwise.
     */
    @Override
    public BundleContext getBundleContext() {
        if (cc != null) {
            return cc.getBundleContext();
        } else {
            return null;
        }
    }

    /**
     * Remove the reference to the file monitor.
     */
    protected void unsetFileMonitorRegistration() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "unsetFileMonitorRegistration");
        }
        if (keyStoreFileMonitorRegistration != null) {
            keyStoreFileMonitorRegistration.unregister();
            keyStoreFileMonitorRegistration = null;
        }
    }

    /**
     * Sets the keystore file monitor registration.
     *
     * @param keyStoreFileMonitorRegistration
     */
    protected void setFileMonitorRegistration(ServiceRegistration<FileMonitor> keyStoreFileMonitorRegistration) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "setFileMonitorRegistration");
        }
        this.keyStoreFileMonitorRegistration = keyStoreFileMonitorRegistration;
    }

    /**
     * Handles the creation of the keystore file monitor.
     */
    private void createFileMonitor(String keyStoreLocation, String trigger, long interval) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "createFileMonitor", new Object[] { keyStoreLocation, trigger, interval });
        try {
            keyStoreFileMonitor = new SecurityFileMonitor(this);
            setFileMonitorRegistration(keyStoreFileMonitor.monitorFiles(Arrays.asList(keyStoreLocation), interval, trigger));
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception creating the keystore file monitor.", e);
            }
            FFDCFilter.processException(e, getClass().getName(), "createFileMonitor", this, new Object[] { keyStoreLocation, interval });
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "createFileMonitor");
    }

}
