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

package com.ibm.ws.sib.ra;

import com.ibm.ws.sib.admin.JsMessagingEngine;

/**
 * Interface used to notified interested parties about the starting and stopping
 * of messaging engines.
 */
public interface SibRaMessagingEngineListener {

    /**
     * Notifies the listener that the given messaging engine is initializing.
     * 
     * @param messagingEngine
     *            the messaging engine
     */
    void messagingEngineInitializing(JsMessagingEngine messagingEngine);

    /**
     * Notifies the listener that the given messaging engine is starting.
     * 
     * @param messagingEngine
     *            the messaging engine
     */
    void messagingEngineStarting(JsMessagingEngine messagingEngine);

    /**
     * Notifies the listener that the given messaging engine is stopping.
     * 
     * @param messagingEngine
     *            the messaging engine
     * @param mode
     *            the mode with which the engine is stopping
     */
    void messagingEngineStopping(JsMessagingEngine messagingEngine, int mode);

    /**
     * Notifies the listener that the given messaging engine is being destroyed.
     * 
     * @param messagingEngine
     *            the messaging engine
     */
    void messagingEngineDestroyed(JsMessagingEngine messagingEngine);

    /**
     * Notifies the listener that the given messaging engine has been reloaded
     * following a configuration change to the bus on which the engine resides.
     * 
     * @param engine
     *            the messaging engine that has been reloaded
     */
    void messagingEngineReloaded(JsMessagingEngine engine);

}
