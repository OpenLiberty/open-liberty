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
 * This service interface manages a set of notifications which provide for
 * handshakes between service components during server runtime updates.
 *
 * As notifications are created they are passed to update listeners and are
 * also added to the current set of notifications. More notifications can
 * be created by different components as they participate in the runtime
 * update. When all of the notifications for a given update have completed
 * they are all removed from the set and the coordination between the
 * participants ends. While the update is in progress participants can
 * lookup notifications which may have been previously sent to listeners
 * before they joined.
 *
 * Unlike the event admin service, notifications are created and made
 * available to interested parties when the service that will later
 * complete the notification joins the update. The manager also holds
 * onto the set of notifications for the current update as long as there
 * are notifications which have not been completed. This allows for
 * those notifications to be discovered by participants that were not
 * involved in the update when those notifications were created earlier
 * in the same update.
 */
public interface RuntimeUpdateManager {
    /**
     * Create a notification
     *
     * @param name The name of the notification
     * @return the notification that was created
     */
    public RuntimeUpdateNotification createNotification(String name);

    /**
     * Create a notification
     *
     * @param name The name of the notification
     * @param ignoreOnQuiesce During quiesce, this notification will be ignored, and won't wait for completion (Default is false)
     * @return the notification that was created
     */
    public RuntimeUpdateNotification createNotification(String name, boolean ignoreOnQuiesce);

    /**
     * Lookup an existing notification that is part of the current update.
     * 
     * @param name The name of the notification
     * @return the notification if it was found, otherwise null.
     */
    public RuntimeUpdateNotification getNotification(String name);
}
