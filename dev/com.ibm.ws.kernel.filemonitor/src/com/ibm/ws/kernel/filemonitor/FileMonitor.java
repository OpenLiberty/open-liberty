/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.filemonitor;

import java.io.File;
import java.util.Collection;

/**
 * This is an internal interface that extends com.ibm.wsspi.kernel.filemonitor.FileMonitor
 * including all the property plus the MONITOR_IDENTIFICATION_NAME.
 *
 * This class adds the internal parameter MONITOR_IDENTIFICATION_NAME to identify monitors
 * that are used for the fileRefresh updates.
 */
public interface FileMonitor extends com.ibm.wsspi.kernel.filemonitor.FileMonitor {

    /**
     * <h4>Service property</h4>
     *
     * The value should be a String, indicating the type of monitor
     * for external updates.
     */
    String MONITOR_IDENTIFICATION_NAME = "monitor.identification";

    /**
     * Called by the monitor service when a scheduled scan completes with
     * changes (indicated by the presence of files in the corresponding collection).
     *
     * @param createdFiles A collection of files that were created since the last scan.
     * @param modifiedFiles A collection of files that were modified since the last scan.
     * @param deletedFiles A collection of files that were deleted since the last scan.
     * @param filter A string used to filter listener notifications. Only the listeners matching the filter are called.
     */
    void onChange(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles, String filter);
}
