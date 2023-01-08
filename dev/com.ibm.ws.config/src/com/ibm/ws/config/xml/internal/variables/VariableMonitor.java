/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal.variables;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.xml.internal.ConfigComparator.DeltaType;
import com.ibm.ws.config.xml.internal.ConfigRefresher;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

public class VariableMonitor implements com.ibm.ws.kernel.filemonitor.FileMonitor {

    static final TraceComponent tc = Tr.register(VariableMonitor.class, "config", "com.ibm.ws.config.internal.resources.ConfigMessages");

    /**  */
    private ServiceRegistration<FileMonitor> serviceRegistration;
    private Long monitorInterval;
    private final Collection<String> monitoredDirectories;
    private String monitorType;
    private final BundleContext bundleContext;
    private final ConfigRefresher configRefresher;

    private final ConfigVariableRegistry variableRegistry;

    public VariableMonitor(BundleContext bc, Long monitorInterval,
                           String fileMonitorType,
                           ConfigRefresher refresher, ConfigVariableRegistry variableRegistry) {
        this.bundleContext = bc;
        ServiceReference<WsLocationAdmin> sr = bc.getServiceReference(WsLocationAdmin.class);
        this.monitoredDirectories = new ArrayList<String>();
        String fileSystemVariableRoot = bc.getService(sr).resolveString(WsLocationConstants.SYMBOL_VARIABLE_SOURCE_DIRS);
        StringTokenizer st = new StringTokenizer(fileSystemVariableRoot, File.pathSeparator);
        while (st.hasMoreTokens()) {
            String directory = st.nextToken();
            monitoredDirectories.add(directory);
        }

        this.monitorInterval = monitorInterval;
        this.monitorType = fileMonitorType;
        this.configRefresher = refresher;
        this.variableRegistry = variableRegistry;
    }

    public void register() {
        Hashtable<String, Object> properties = getFileMonitorProperties();
        serviceRegistration = bundleContext.registerService(FileMonitor.class, this, properties);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Configuration monitoring is enabled. Monitoring interval is " + monitorInterval);
        }
    }

    public void unregister() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Monitoring of VARIABLE_SOURCE_DIRS is disabled.");
        }
    }

    private Hashtable<String, Object> getFileMonitorProperties() {
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        properties.put(FileMonitor.MONITOR_FILES, Collections.EMPTY_LIST);
        properties.put(FileMonitor.MONITOR_DIRECTORIES, monitoredDirectories);
        properties.put(FileMonitor.MONITOR_INTERVAL, monitorInterval);
        properties.put(FileMonitor.MONITOR_TYPE, monitorType);
        properties.put(FileMonitor.MONITOR_RECURSE, true);
        properties.put(FileMonitor.MONITOR_FILTER, ".*");

        return properties;
    }

    /**
     * Update FileMonitor service with a new monitoring interval and monitor type. Both parameters must not be <code>null</code>.
     */
    public void update(Long monitorInterval, String fileMonitorType) {
        if (this.monitorInterval.equals(monitorInterval) && this.monitorType.equals(fileMonitorType)) {
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Updating VariableMonitor with a new monitoring interval: " + monitorInterval + " and type: " + fileMonitorType);
        }

        this.monitorInterval = monitorInterval;
        this.monitorType = fileMonitorType;

        Hashtable<String, Object> properties = getFileMonitorProperties();
        serviceRegistration.setProperties(properties);
    }

    @Override
    public void onBaseline(Collection<File> baseline) {

    }

    @Override
    public void onChange(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles) {
        Map<String, DeltaType> deltaMap = new HashMap<String, DeltaType>();

        variableRegistry.removeFileSystemVariableDeletes(deletedFiles, deltaMap);

        variableRegistry.addFileSystemVariableCreates(createdFiles, deltaMap);

        variableRegistry.modifyFileSystemVariables(modifiedFiles, deltaMap);

        configRefresher.variableRefresh(deltaMap);
    }

    @Override
    public void onChange(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles, String filter) {
        // Filter is not needed here
        onChange(createdFiles, modifiedFiles, deletedFiles);
    }
}