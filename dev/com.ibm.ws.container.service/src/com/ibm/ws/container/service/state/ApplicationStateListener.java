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
package com.ibm.ws.container.service.state;

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;

public interface ApplicationStateListener {

    /**
     * Notification that an application is starting.
     * 
     * @param appInfo The ApplicationInfo of the app
     */
    void applicationStarting(ApplicationInfo appInfo) throws StateChangeException;

    /**
     * Notification that an application has started.
     * 
     * @param appInfo The ApplicationInfo of the app
     */
    void applicationStarted(ApplicationInfo appInfo) throws StateChangeException;

    /**
     * Notification that an application is stopping.
     * 
     * @param appInfo The ApplicationInfo of the app
     */
    void applicationStopping(ApplicationInfo appInfo);

    /**
     * Notification that an application has stopped.
     * 
     * @param appInfo The ApplicationInfo of the app
     */
    void applicationStopped(ApplicationInfo appInfo);
}
