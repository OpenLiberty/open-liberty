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
package com.ibm.ws.security.filemonitor;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.wsspi.kernel.filemonitor.FileMonitor;

/**
 * The security file monitor gets notified through the scanComplete method
 * of the creation, modification, or deletion of the file(s) being monitored.
 * It will tell the actionable to perform its action if an action is needed.
 */
public class SecurityFileMonitor implements FileMonitor {

    private final FileBasedActionable actionable;
    private final Collection<File> currentlyDeletedFiles;

    public SecurityFileMonitor(FileBasedActionable fileBasedActionable) {
        this.actionable = fileBasedActionable;
        currentlyDeletedFiles = new HashSet<File>();
    }

    /**
     * Registers this file monitor to start monitoring the specified files at the specified interval.
     * 
     * @param paths the paths of the files to monitor.
     * @param monitorInterval the rate to monitor the files.
     * 
     * @return the <code>FileMonitor</code> service registration.
     */
    public ServiceRegistration<FileMonitor> monitorFiles(Collection<String> paths, long monitorInterval) {
        BundleContext bundleContext = actionable.getBundleContext();
        final Hashtable<String, Object> fileMonitorProps = new Hashtable<String, Object>();
        fileMonitorProps.put(FileMonitor.MONITOR_FILES, paths);
        fileMonitorProps.put(FileMonitor.MONITOR_INTERVAL, monitorInterval);
        return bundleContext.registerService(FileMonitor.class, this, fileMonitorProps);
    }

    /**
     * Registers this file monitor to start monitoring the specified files either by mbean
     * notification or polling rate.
     * 
     * @param paths the paths of the files to monitor.
     * @param pollingRate the rate to pole he file for a change.
     * @param trigger what trigger the file update notification mbean or poll
     * @return The <code>FileMonitor</code> service registration.
     */
    public ServiceRegistration<FileMonitor> monitorFiles(Collection<String> paths, long pollingRate, String trigger) {
        BundleContext bundleContext = actionable.getBundleContext();
        final Hashtable<String, Object> fileMonitorProps = new Hashtable<String, Object>();
        fileMonitorProps.put(FileMonitor.MONITOR_FILES, paths);
        if (!(trigger.equalsIgnoreCase("disabled"))) {
            if (trigger.equals("mbean")) {
                fileMonitorProps.put(FileMonitor.MONITOR_TYPE, FileMonitor.MONITOR_TYPE_EXTERNAL);
            }
            else
            {
                fileMonitorProps.put(FileMonitor.MONITOR_TYPE, FileMonitor.MONITOR_TYPE_TIMED);
                fileMonitorProps.put(FileMonitor.MONITOR_INTERVAL, pollingRate);
            }
        }
        return bundleContext.registerService(FileMonitor.class, this, fileMonitorProps);
    }

    /** {@inheritDoc} */
    @Override
    public void onBaseline(Collection<File> baseline) {}

    /** {@inheritDoc} */
    @Override
    public void onChange(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles) {
        Collection<File> allFiles = new HashSet<File>();

        if (deletedFiles.isEmpty() == false) {
            currentlyDeletedFiles.addAll(deletedFiles);
            allFiles.addAll(deletedFiles);
        }

        if (isActionNeeded(createdFiles, modifiedFiles)) {
            if (createdFiles.isEmpty() == false) {
                allFiles.addAll(createdFiles);
            }

            if (modifiedFiles.isEmpty() == false) {
                allFiles.addAll(modifiedFiles);
            }

            actionable.performFileBasedAction(allFiles);
        }
    }

    /**
     * Action is needed if a file is modified or if it is recreated after it was deleted.
     * 
     * @param modifiedFiles
     */
    private Boolean isActionNeeded(Collection<File> createdFiles, Collection<File> modifiedFiles) {
        boolean actionNeeded = false;

        for (File createdFile : createdFiles) {
            if (currentlyDeletedFiles.contains(createdFile)) {
                currentlyDeletedFiles.remove(createdFile);
                actionNeeded = true;
            }
        }

        if (modifiedFiles.isEmpty() == false) {
            actionNeeded = true;
        }
        return actionNeeded;
    }

}
