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
package com.ibm.ws.kernel.filemonitor;

import com.ibm.wsspi.kernel.filemonitor.FileMonitor;

/**
 * This MBean will be used to notify the runtime about changes to files.
 * Changes may be monitored by the server, but this MBean is to be used
 * when internal monitoring is disabled.
 * <p>
 * This may be used by the tooling to tell the server when an
 * application has been updated. This mechanism is preferable in such a
 * case because the tooling will know when all changes are completed
 * (e.g. at the end of a build) whereas the server might notice them
 * in-process.
 * <p>
 * Because this interface resides in a package with non-public items (such as FileMonitor), {@link com.ibm.websphere.filemonitor.FileNotificationMBean} has been added as the
 * public management interface for the MBean which can be used to create proxy objects.
 *
 * @see FileMonitor
 */
public interface FileNotificationMBean extends FileNotification {
    /**
     * This is the name to be used to register and to look up the MBean.
     * It should match the <code>jmx.objectname</code> property in the
     * bnd.bnd file for the component that provides this interface.
     */
    String INSTANCE_NAME = "WebSphere:service=com.ibm.ws.kernel.filemonitor.FileNotificationMBean";

    /**
     * Processed pending server configuration changes {see#link: FileNotificationImpl}
     */
    void processConfigurationChanges();

    /**
     * Processed pending application updates {see#link: FileNotificationImpl}
     */
    void processApplicationChanges();
}
