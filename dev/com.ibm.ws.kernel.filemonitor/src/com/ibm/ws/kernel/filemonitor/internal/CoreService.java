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

import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.ServiceReference;

import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

/**
 *
 */
public interface CoreService {

    /**
     * @return the active/bound instance of the ScheduledExecutorService; will not return null
     * @throws IllegalStateException if service can not be found
     */
    ScheduledExecutorService getScheduler();

    /**
     * @return the active/bound instance of the WsLocationAdmin service; will not return null
     * @throws IllegalStateException if service can not be found
     */
    WsLocationAdmin getLocationService();

    /**
     * @param monitorRef ServiceReference to a bound FileMonitor
     * @return the active/bound instance of the FileMonitor associated with the provided ServiceReference; will not return null
     * @throws IllegalStateException if service can not be found
     */
    FileMonitor getReferencedMonitor(ServiceReference<FileMonitor> monitorRef);

    /**
     * @return true if detailed scan trace is enabled (trace for scan start/stop, etc. regardless of
     *         whether or not changes are discovered: very noisy!)
     */
    boolean isDetailedScanTraceEnabled();
}
