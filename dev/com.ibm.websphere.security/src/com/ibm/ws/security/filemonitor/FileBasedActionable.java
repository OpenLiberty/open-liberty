/*******************************************************************************
 * Copyright (c) 2012, 2023 IBM Corporation and others.
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

import org.osgi.framework.BundleContext;

/**
 * Component that need to be notified by the SecurityFileMonitor when the files
 * they are interested in are modified or recreated need to implement this interface
 * and pass themselves to a new instance of SecurityFileMonitor.
 */
public interface FileBasedActionable {

    /**
     * Callback method to be invoked by the file monitor
     * to instruct the implementation to perform its action.
     *
     * @param modifiedFiles
     */
    void performFileBasedAction(Collection<File> modifiedFiles);

    /**
     * Callback method to be invoked by the file monitor
     * to instruct the implementation to perform its action.
     *
     * @param createdFiles
     * @param modifiedFiles
     * @param deletedFiles
     */
    void performFileBasedAction(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles);

    /**
     * Returns the implementation's BundleContext.
     */
    BundleContext getBundleContext();

}
