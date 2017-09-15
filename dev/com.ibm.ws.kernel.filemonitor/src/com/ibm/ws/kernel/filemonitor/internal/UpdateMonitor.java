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
package com.ibm.ws.kernel.filemonitor.internal;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Abstract class that manages creation/deletion of appropriate monitor
 * types based on the monitored resource/file.
 * <p>
 * The expected usage pattern is for a {@link MonitorHolder} to obtain
 * an {@link UpdateMonitor} via {@link #getMonitor(File, File, String, boolean)}.
 * The caller should then schedule a task that will invoke the {@link #scanForUpdates(List, List, List)},
 * taking care to ensure that the returned {@link UpdateMonitor} is used for subsequent
 * scan invocations.
 */
public abstract class UpdateMonitor {
    @Trivial
    public enum MonitorType {
        FILE,
        DIRECTORY,
        DIRECTORY_RECURSE,
        DIRECTORY_SELF,
        DIRECTORY_RECURSE_SELF
    }

    /**
     * Obtain an instance of an update monitor that uses the specified cache location, and
     * monitors the specified resource with the specified properties. Caller must call {@link #init()} on
     * this monitor before the first scheduled scan.
     * 
     * @param monitoredFile
     *            The name of the resource to monitor (file or directory, may or may not exist)
     * @param filter A regex filter limiting the types of resources that will be monitored.
     *            Only applicable if the primary monitored file is a directory
     * @param recurse
     *            If true, all resources in all subdirectories will be monitored
     * 
     * @return an instance of the UpdateMonitor appropriate for the monitored resource (e.g. File, Directory, or non-existant)
     */
    public static UpdateMonitor getMonitor(File monitoredFile, MonitorType type, String filter) {

        if (monitoredFile == null)
            throw new NullPointerException("MonitoredFile must be non-null");
        if (type == null)
            throw new NullPointerException("MonitorType must be non-null");

        switch (type) {
            case DIRECTORY:
            case DIRECTORY_RECURSE:
            case DIRECTORY_SELF:
            case DIRECTORY_RECURSE_SELF:
                return new DirectoryUpdateMonitor(monitoredFile, type, filter);
            case FILE:
                return new FileUpdateMonitor(monitoredFile);
            default:
                throw new IllegalArgumentException("Unknown monitor type: " + type);
        }
    }

    protected final File monitoredFile;
    protected final MonitorType type;
    protected final int hashCode;

    /**
     * Constructor for the core of the update monitor
     * 
     * @param cacheLocation A file describing where the cache of previously seen files should be stored.
     *            This may be null, in which case the cache will be maintained in memory.
     * @param monitoredFile The resource (file or directory) being monitored. This may or may not exist.
     * @param type one of: {@link MonitorType#FILE}, {@link MonitorType#DIRECTORY}, {@link MonitorType#DIRECTORY_RECURSE}, {@link MonitorType#DIRECTORY_RECURSE_SELF},
     *            {@link MonitorType#DIRECTORY_SELF}
     */
    protected UpdateMonitor(File monitoredFile, MonitorType type) {
        this.monitoredFile = monitoredFile;
        this.type = type;

        final int prime = 31;
        int result = 1;
        result = prime * result + monitoredFile.hashCode();
        this.hashCode = prime * result + type.hashCode();
    }

    /**
     * Called when an update monitor should perform an initial baseline
     * scan.
     */
    public abstract void init(Collection<File> baseline);

    /**
     * Called to destroy an update monitor: this method should be
     * called when a resource should no longer be monitored, or when the
     * parameters (recurse/filter) have changed.
     */
    protected abstract void destroy();

    /**
     * Scan for created, updated, or deleted files in the path watched
     * by this monitor. The list parameters are updated with the modified
     * files.
     * <p>
     * This method returns the update monitor that should be used for the next
     * scan: if an existing resource (represented by a {@link DirectoryUpdateMonitor} or
     * a {@link FileUpdateMonitor}) is deleted, a {@link ResourceUpdateMonitor} will
     * be returned, which waits for the resource to be recreated. When a resource
     * is created, the {@link ResourceUpdateMonitor} will return a new monitor of
     * the appropriate type. In most cases, "this" will be returned.
     * 
     * @param created a modifiable list to be filled with new/created files
     * @param modified a modifiable list to be filled with modified files
     * @param deleted a modifiable list to be filled with deleted files
     * @return the update monitor that should be used for the next scan
     */
    public abstract void scanForUpdates(Collection<File> created, Collection<File> modified, Collection<File> deleted);

    protected void addToList(Collection<File> created, File file) {
        if (created != null)
            created.add(file);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
               + "[type=" + type
               + ",file=" + monitoredFile
               + "]";
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UpdateMonitor other = (UpdateMonitor) obj;
        if (monitoredFile == null) {
            if (other.monitoredFile != null)
                return false;
        } else if (!monitoredFile.equals(other.monitoredFile))
            return false;
        if (type != other.type)
            return false;
        return true;
    }
}