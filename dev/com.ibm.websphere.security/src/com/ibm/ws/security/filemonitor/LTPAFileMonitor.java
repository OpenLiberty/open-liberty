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

import org.osgi.framework.ServiceRegistration;

import com.ibm.wsspi.kernel.filemonitor.FileMonitor;

/**
 * The LTPA file monitor gets notified through the scanComplete method
 * of the creation, modification, or deletion of the file(s) being monitored.
 * It will tell the actionable to perform its action if an action is needed.
 */
public class LTPAFileMonitor extends SecurityFileMonitor {

//    protected final String LTPA_FILEMONITOR_ID = com.ibm.ws.kernel.filemonitor.FileMonitor.SECURITY_LTPA_MONITOR_IDENTIFICATION_VALUE;

    /**
     * @param fileBasedActionable
     */
    public LTPAFileMonitor(FileBasedActionable fileBasedActionable) {
        super(fileBasedActionable);
    }

    @Override
    public ServiceRegistration<FileMonitor> monitorFiles(String id, Collection<String> dirs, Collection<String> paths, long pollingRate, String trigger) {
        return super.monitorFiles(id, dirs, paths, pollingRate, trigger);
    }

    @Override
    public ServiceRegistration<FileMonitor> monitorFiles(String id, Collection<String> paths, long pollingRate, String trigger) {
        return super.monitorFiles(id, null, paths, pollingRate, trigger);
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
