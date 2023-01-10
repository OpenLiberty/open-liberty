/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.kernel.filemonitor.internal.scan;

import org.osgi.framework.ServiceReference;

import com.ibm.ws.kernel.filemonitor.internal.CoreService;
import com.ibm.ws.kernel.filemonitor.internal.MonitorHolderTestParent;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;

public class ScanningMonitorHolderTest extends MonitorHolderTestParent {

    /**
     * @param mockCoreService
     * @param mockServiceReference
     * @return
     */
    @Override
    protected ScanningMonitorHolder instantiateMonitor(CoreService mockCoreService, ServiceReference<FileMonitor> mockServiceReference) {
        return new ScanningMonitorHolder(mockCoreService, mockServiceReference);
    }

}
