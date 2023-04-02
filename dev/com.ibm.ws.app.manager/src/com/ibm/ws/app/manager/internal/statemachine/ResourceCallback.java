/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
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
package com.ibm.ws.app.manager.internal.statemachine;

import java.io.File;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.kernel.service.location.WsResource;

interface ResourceCallback {
    /**
     * An indication that an attempt was made to access the resource, and it
     * is not currently available, but it will be monitored for availability.
     * This method may be called multiple times.
     */
    void pending();

    void successfulCompletion(Container c, WsResource r);

    void failedCompletion(Throwable t);

    /**
     * Setup a container for a application file.
     *
     * The application file is expected to have been resolved in the local file
     * system, possibly after having been downloaded.
     * 
     * This method is now obsolete: Without the configuration ID, the cache location
     * of the application may not be consistently set between server starts.
     * Use instead {@link #setupContainer(String, String, File)}.
     * 
     * @param _servicePid The application PID.
     * @param file The application file.
     *  
     * @return The new container.
     */
    @Deprecated
    Container setupContainer(String _servicePid, File file);
    
    /**
     * Setup a container for a application file.
     *
     * The application file is expected to have been resolved in the local file
     * system, possibly after having been downloaded.
     * 
     * This method replaces {@link #setupContainer(String, File)}, and uses the
     * application ID in preference to the application PID for the application
     * cache location.  This is more likely to be consistent between server
     * starts.
     *
     * @param _servicePid The application PID.
     * @param _configId The application "id" property.
     * @param file The application file.
     *  
     * @return The new container.
     */    
    Container setupContainer(String _servicePid, String _configId, File file);
}
