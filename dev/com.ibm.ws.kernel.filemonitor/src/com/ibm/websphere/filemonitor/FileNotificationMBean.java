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
package com.ibm.websphere.filemonitor;

import java.util.Collection;

/**
 * Management interface for the MBean "WebSphere:service=com.ibm.ws.kernel.filemonitor.FileNotificationMBean"
 * The Liberty profile makes this MBean available in its platform MBean server so that users may notify the system of
 * file changes when file monitoring is disabled. This interface can be used to request a proxy object via the {@link javax.management.JMX#newMBeanProxy} method.
 * 
 * @ibm-api
 */
public interface FileNotificationMBean {

    /**
     * Invokes the notifyFileChanges operation, which can be used to notify the Liberty profile of created, modified, or deleted files. This is most useful
     * when file monitoring is disabled. It may be preferable to disable file monitoring and use this operation instead if files are being modified in-place
     * (as part of a build process, for example) and the changes should be made visible to the Liberty profile as a group. If the
     * user does not notify of a particular file change on a given call to notifyFileChanges, that change will remain eligible
     * for subsequent calls.
     * 
     * @param createdFiles the absolute paths of any created files
     * @param modifiedFiles the absolute paths of any modified files
     * @param deletedFiles the absolute paths of any deleted files
     */
    void notifyFileChanges(Collection<String> createdFiles, Collection<String> modifiedFiles, Collection<String> deletedFiles);
}
