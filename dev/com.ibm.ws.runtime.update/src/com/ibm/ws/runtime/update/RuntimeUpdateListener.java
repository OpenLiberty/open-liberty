/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.runtime.update;

/**
 * Service interface for components interested in being informed of runtime update
 * notifications as they are created.
 */
public interface RuntimeUpdateListener {
    /**
     * Called as notifications are created on the set of all listeners known to
     * the runtime update manager at the time the notification is created.
     * 
     * @param updateManager the runtime update manager
     * @param notification the newly created notification
     */
    public void notificationCreated(RuntimeUpdateManager updateManager, RuntimeUpdateNotification notification);
}
