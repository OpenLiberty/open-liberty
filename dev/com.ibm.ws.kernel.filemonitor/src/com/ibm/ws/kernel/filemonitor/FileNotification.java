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

import java.util.Collection;

import com.ibm.wsspi.kernel.filemonitor.FileMonitor;

public interface FileNotification {
    /**
     * This is the notification method. Only {@link FileMonitor}s that
     * have external monitoring enabled will receive these notifications
     * and then only for the files they are monitoring.
     *
     * @param createdFiles the absolute paths of any created files
     * @param modifiedFiles the absolute paths of any modified files
     * @param deletedFiles the absolute paths of any deleted files
     */
    void notifyFileChanges(Collection<String> createdFiles, Collection<String> modifiedFiles, Collection<String> deletedFiles);

    /**
     * Processed pending server configuration changes {see#link: FileNotificationImpl}
     */
    void processConfigurationChanges();

    /**
     * Processed pending application updates {see#link: FileNotificationImpl}
     */
    void processApplicationChanges();
}
