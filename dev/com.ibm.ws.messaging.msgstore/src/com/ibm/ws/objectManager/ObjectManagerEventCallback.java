package com.ibm.ws.objectManager;

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

/**
 * This interface defines methods that can be called on a
 * registered call back when defined events occur within the
 * ObjectManager.
 */
public interface ObjectManagerEventCallback {
    /**
     * Called when the ObjectManager has stopped either due to a specific
     * shutdown request or an unrecoverable error.
     * <ul>
     * <li> args
     * <ul>
     * <li>none.
     * </ul>
     * </ul>
     */
    public static final int objectManagerStopped = 0;

    /**
     * Called just before an Object store is opened.
     * <ul>
     * <li> args
     * <ul>
     * <li>ObjectStore being opened.
     * </ul>
     * </ul>
     */
    public static final int objectStoreOpened = 1;
    static final String[] eventNames = { "objectManagerStopped",
                                        "objectStoreOpened"
    };

    /**
     * Called when an event occurs.
     * 
     * @param event the type of event.
     * @param args defined for each event type.
     */
    public void notification(int event, Object[] args);
} // interface ObjectManagerEventCallback.