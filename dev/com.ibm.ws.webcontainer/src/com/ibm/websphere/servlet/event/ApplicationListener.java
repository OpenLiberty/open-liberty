/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.servlet.event;


/**
 * Event listener interface used for notifications about the application.
 * @ibm-api
 */
public interface ApplicationListener extends java.util.EventListener{

    /**
     * Triggered when the application is started.
     * This event is triggered before any object initializations occur within the application
     * (including auto-start servlet initialization).  This method is the perfect place for
     * applications to register for other events and to setup the application before any other
     * objects are created by the application.
     * 
     */
    public void onApplicationStart(ApplicationEvent evt);

    /**
     * Final application event that occurs before the application is terminated by the server process.
     */
    public void onApplicationEnd(ApplicationEvent evt);

    /**
     * Triggered when the application is activated to receive external requests.
     */
    public void onApplicationAvailableForService(ApplicationEvent evt);

    /**
     * Triggered when the application is taken offline.
     * When an application is taken offline, all requests to the application
     * will be denied.
     */
    public void onApplicationUnavailableForService(ApplicationEvent evt);
}

