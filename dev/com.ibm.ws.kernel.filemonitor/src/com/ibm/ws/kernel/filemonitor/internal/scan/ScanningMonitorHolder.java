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
package com.ibm.ws.kernel.filemonitor.internal.scan;

import java.io.File;

import org.osgi.framework.ServiceReference;

import com.ibm.ws.kernel.filemonitor.internal.CoreService;
import com.ibm.ws.kernel.filemonitor.internal.MonitorHolder;
import com.ibm.ws.kernel.filemonitor.internal.UpdateMonitor;
import com.ibm.ws.kernel.filemonitor.internal.UpdateMonitor.MonitorType;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;

/**
 *
 */
public class ScanningMonitorHolder extends MonitorHolder {
    /**
     * @param coreService
     * @param monitorRef
     */
    public ScanningMonitorHolder(CoreService coreService, ServiceReference<FileMonitor> monitorRef) {
        super(coreService, monitorRef);
    }

    @Override
    protected UpdateMonitor createUpdateMonitor(File file, MonitorType type, String monitorFilter) {
        // NOTE: File caching is disabled. if/when we want it back, we can start
        // checking against a minimum interval before setting the value.
        UpdateMonitor um = UpdateMonitor.getMonitor(file, type, monitorFilter);
        return um;
    }

}
