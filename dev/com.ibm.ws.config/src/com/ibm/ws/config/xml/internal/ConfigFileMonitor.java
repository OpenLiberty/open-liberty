/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import java.io.File;
import java.util.Collection;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;

class ConfigFileMonitor implements com.ibm.ws.kernel.filemonitor.FileMonitor {

    static final TraceComponent tc = Tr.register(ConfigFileMonitor.class, XMLConfigConstants.TR_GROUP, XMLConfigConstants.NLS_PROPS);

    /**  */
    private ServiceRegistration<FileMonitor> serviceRegistration;
    private Long monitorInterval;
    private Collection<String> monitoredFiles;
    private Collection<String> monitoredDirectories;
    private final boolean modified;
    private String monitorType;
    private final BundleContext bundleContext;
    private final ConfigRefresher configRefresher;

    public ConfigFileMonitor(BundleContext bc, Collection<String> monitoredFiles, Collection<String> directoriesToMonitor, Long monitorInterval, boolean modified,
                             String fileMonitorType,
                             ConfigRefresher refresher) {
        this.bundleContext = bc;
        this.monitoredFiles = monitoredFiles;
        this.monitoredDirectories = directoriesToMonitor;
        this.monitorInterval = monitorInterval;
        this.modified = modified;
        this.monitorType = fileMonitorType;
        this.configRefresher = refresher;
    }

    void register() {
        Hashtable<String, Object> properties = getFileMonitorProperties();
        serviceRegistration = bundleContext.registerService(FileMonitor.class, this, properties);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Configuration monitoring is enabled. Monitoring interval is " + monitorInterval);
        }
    }

    void unregister() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Configuration monitoring is disabled.");
        }
    }

    private Hashtable<String, Object> getFileMonitorProperties() {
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        properties.put(FileMonitor.MONITOR_FILES, monitoredFiles);
        properties.put(FileMonitor.MONITOR_DIRECTORIES, monitoredDirectories);
        properties.put(FileMonitor.MONITOR_INTERVAL, monitorInterval);
        properties.put(FileMonitor.MONITOR_TYPE, monitorType);
        properties.put(FileMonitor.MONITOR_RECURSE, false);
        properties.put(FileMonitor.MONITOR_FILTER, ".*");
        //Adding INTERNAL parameter MONITOR_IDENTIFICATION_NAME to identify this monitor.
        properties.put(com.ibm.ws.kernel.filemonitor.FileMonitor.MONITOR_IDENTIFICATION_NAME, "com.ibm.ws.kernel.monitor.config");
        return properties;
    }

    /**
     * Update FileMonitor service with a new monitoring interval and monitor type. Both parameters must not be <code>null</code>.
     */
    void updateFileMonitor(Long monitorInterval, String fileMonitorType) {
        if (this.monitorInterval.equals(monitorInterval) && this.monitorType.equals(fileMonitorType)) {
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Updating FileMonitor with a new monitoring interval: " + monitorInterval + " and type: " + fileMonitorType);
        }

        this.monitorInterval = monitorInterval;
        this.monitorType = fileMonitorType;

        Hashtable<String, Object> properties = getFileMonitorProperties();
        serviceRegistration.setProperties(properties);
    }

    /*
     * Update FileMonitor service with a new set of files to monitor.
     */
    void updateFileMonitor(Collection<String> filesToMonitor) {
        if (monitoredFiles.equals(filesToMonitor)) {
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Updating FileMonitor with a new set of files to monitor: " + filesToMonitor);
        }

        monitoredFiles = filesToMonitor;

        Hashtable<String, Object> properties = getFileMonitorProperties();
        serviceRegistration.setProperties(properties);
    }

    void updateDirectoryMonitor(Collection<String> directories) {
        if (monitoredDirectories.equals(directories)) {
            return;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Updating FileMonitor with a new set of directories to monitor: " + directories);
        }

        monitoredDirectories = directories;

        Hashtable<String, Object> properties = getFileMonitorProperties();
        serviceRegistration.setProperties(properties);

    }

    @Override
    public void onBaseline(Collection<File> baseline) {
        if (modified) {
            configRefresher.refreshConfiguration();
        }
    }

    @Override
    public void onChange(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles) {
        configRefresher.refreshConfiguration();
    }

    @Override
    public void onChange(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles, String filter) {
        configRefresher.refreshConfiguration();
    }
}