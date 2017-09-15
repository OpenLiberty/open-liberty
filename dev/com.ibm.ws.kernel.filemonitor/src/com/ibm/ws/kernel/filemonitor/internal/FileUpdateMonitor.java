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

/**
 * The <code>FileUpdateMonitor</code> monitors simple/single files for changes.
 * This monitor is only used for files that exist: if the file does not exist
 * when monitoring is started, it will be watched by a {@link ResourceUpdateMonitor} until it does.
 * <p>
 * NOT THREAD SAFE: Calling/using class must ensure that only one operation (scan/init) is
 * active on the monitored file at a time.
 */
public class FileUpdateMonitor extends UpdateMonitor {

    private boolean exists = false;
    private long monitoredTime = 0;
    private long monitoredSize = 0;

    protected FileUpdateMonitor(File monitoredFile) {
        super(monitoredFile, MonitorType.FILE);
    }

    /** {@inheritDoc} */
    @Override
    public void init(Collection<File> baseline) {
        // Baseline: we haven't found this file before.
        exists = monitoredFile.isFile();
        performScan(monitoredFile);
        // Always return the file being monitored as part of the
        // baseline so the caller can verify against information it may
        // have cached.
        if (exists) {
            addToList(baseline, monitoredFile);
        }
    }

    @Override
    protected void destroy() {}

    /**
     * {@inheritDoc}
     * 
     * <p>
     * FileUpdateMonitor:
     * If the file being monitored is deleted, the File will be added to the
     * deleted list, and a {@link ResourceUpdateMonitor} will be returned.
     * If the file being monitored is changed, the File will be added to the modified
     * list.
     */
    @Override
    public void scanForUpdates(Collection<File> created, Collection<File> modified, Collection<File> deleted) {
        if (monitoredFile.isFile()) {
            if (performScan(monitoredFile)) {
                if (exists) { // If file previously existed, it was modified
                    addToList(modified, monitoredFile);
                } else {
                    exists = true;
                    addToList(created, monitoredFile);
                }
            }
        } else {
            if (exists) {
                addToList(deleted, monitoredFile);
                exists = false;
                monitoredTime = 0;
                monitoredSize = 0;
            }
        }
    }

    /**
     * Attempts to detect if the file has changed. The checked attributes are
     * file size and modification time.
     * <p>
     * Be aware, there is a system/JDK limitation for Unix and Java 1.6 for
     * detecting timestamp changes. See technote:
     * https://www-01.ibm.com/support/docview.wss?uid=swg21446506
     * <p>
     * Added File parameter for better logging... otherwise it is difficult
     * to know what is actually getting scanned.
     * 
     * @param monitoredFile the file to scan
     * @return {@code true} if the file has changed
     */
    private boolean performScan(File monitoredFile) {
        // Otherwise, test to see if the modifiedTime or size have changed
        long newTime = monitoredFile.lastModified();
        long newSize = monitoredFile.length();

        if (newTime != monitoredTime || newSize != monitoredSize) {
            monitoredTime = newTime;
            monitoredSize = newSize;
            return true;
        }
        return false;
    }
}
