/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.security.filemonitor;

import java.io.File;
import java.util.Collection;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * The LTPA file monitor gets notified through the scanComplete method
 * of the creation, modification, or deletion of the file(s) being monitored.
 * It will tell the actionable to perform its action if an action is needed.
 */
public class LTPAFileMonitor extends SecurityFileMonitor {

    /**
     * @param fileBasedActionable
     */
    public LTPAFileMonitor(FileBasedActionable fileBasedActionable) {
        super(fileBasedActionable);
    }

    /**
     * Registers this file monitor to start monitoring the specified files at the specified interval.
     *
     * @param paths           the paths of the files to monitor.
     * @param monitorInterval the rate to monitor the files.
     *
     * @return the <code>FileMonitor</code> service registration.
     */
    public ServiceRegistration<FileMonitor> monitorFiles(Collection<String> paths, long monitorInterval, String updateTrigger) {
        Collection<String> dirs = null;
        return monitorFiles(dirs, paths, monitorInterval, updateTrigger);
    }

    /**
     * Registers this file monitor to start monitoring the specified directory and/or files at the specified interval.
     *
     * @param dirs            the dirs to monitor.
     * @param paths           the paths of the files to monitor.
     * @param monitorInterval the rate to monitor the directory and/or files.
     * @param updateTrigger   the updateTrigger to update the keys.
     *
     * @return the <code>FileMonitor</code> service registration.
     */
    public ServiceRegistration<FileMonitor> monitorFiles(Collection<String> dirs, Collection<String> paths, long monitorInterval, String updateTrigger) {
        BundleContext bundleContext = actionable.getBundleContext();
        final Hashtable<String, Object> fileMonitorProps = new Hashtable<String, Object>();
        fileMonitorProps.put(FileMonitor.MONITOR_FILES, paths);
        if (dirs != null && !dirs.isEmpty()) {
            // Currently MONITOR_DIRECTORIES is only used for the LTPAFileMonitor
            // this is not used for other securityFileMonitors
            fileMonitorProps.put(FileMonitor.MONITOR_DIRECTORIES, dirs);
            fileMonitorProps.put(FileMonitor.MONITOR_FILTER, ".*\\.keys");
        }

        if (!(updateTrigger.equalsIgnoreCase("disabled"))) {
            if (updateTrigger.equals("mbean")) {
                fileMonitorProps.put(FileMonitor.MONITOR_TYPE, FileMonitor.MONITOR_TYPE_EXTERNAL);
            } else {
                fileMonitorProps.put(FileMonitor.MONITOR_TYPE, FileMonitor.MONITOR_TYPE_TIMED);
                fileMonitorProps.put(FileMonitor.MONITOR_INTERVAL, monitorInterval);
            }
        } else {
            fileMonitorProps.put(FileMonitor.MONITOR_TYPE, FileMonitor.MONITOR_TYPE_DISABLED);
        }

        // Don't attempt to register the file monitor if the server is stopping
        if (FrameworkState.isStopping())
            return null;

        return bundleContext.registerService(FileMonitor.class, this, fileMonitorProps);
    }

    /** {@inheritDoc} */
    @Override
    public void onBaseline(Collection<File> baseline) {
        actionable.performFileBasedAction(baseline);
    }

    /** {@inheritDoc} */
    @Override
    public void onChange(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles) {
        actionable.performFileBasedAction(createdFiles, modifiedFiles, deletedFiles);
    }
}
