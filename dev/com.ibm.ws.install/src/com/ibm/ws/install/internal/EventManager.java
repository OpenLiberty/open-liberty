/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallEventListener;
import com.ibm.ws.install.InstallProgressEvent;

/**
 * This class provides APIs for progress event listeners.
 */
public class EventManager {

    private Map<String, Collection<InstallEventListener>> listenersMap;

    /**
     * Adds an install event listener to the listenersMap with a specified notification type
     *
     * @param listener InstallEventListener to add
     * @param notificationType Notification type of listener
     */
    public void addListener(InstallEventListener listener, String notificationType) {
        if (listener == null || notificationType == null)
            return;
        if (notificationType.isEmpty())
            return;
        if (listenersMap == null) {
            listenersMap = new HashMap<String, Collection<InstallEventListener>>();
        }
        Collection<InstallEventListener> listeners = listenersMap.get(notificationType);
        if (listeners == null) {
            listeners = new ArrayList<InstallEventListener>(1);
            listenersMap.put(notificationType, listeners);
        }
        listeners.add(listener);
    }

    /**
     * Removes a listener from listenersMap
     *
     * @param listener Listener to remove
     */
    public void removeListener(InstallEventListener listener) {
        if (listenersMap != null) {
            for (Collection<InstallEventListener> listeners : listenersMap.values()) {
                listeners.remove(listener);
            }
        }
    }

    /**
     * Fires progress event messages
     *
     * @param state The state integer
     * @param progress The progress integer
     * @param message The message to be displayed
     * @throws Exception
     */
    public void fireProgressEvent(int state, int progress, String message) throws Exception {
        if (listenersMap != null) {
            Collection<InstallEventListener> listeners = listenersMap.get(InstallConstants.EVENT_TYPE_PROGRESS);
            if (listeners != null) {
                for (InstallEventListener listener : listeners) {
                    listener.handleInstallEvent(new InstallProgressEvent(state, progress, message));
                }
            }
        }
    }

}
