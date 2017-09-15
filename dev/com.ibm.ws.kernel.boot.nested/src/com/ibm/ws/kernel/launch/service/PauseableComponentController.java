/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.launch.service;

import java.util.Collection;

/**
 * This service provides a means of delivering actions to Listeners.
 *
 * Pause and resume are supported through this service. It maintains a
 * List of Listeners that it will drive specific actions to.
 * <p>
 * Currently, this Service is used by the server script and by the z/OS CommandHandler
 * functions interact with this server's Listeners that participate by providing
 * an implementation for the PauseResumeListener interface.
 */
public interface PauseableComponentController {

    /**
     * Deliver pause request to all the currently registered pauseable components.
     */
    void pause() throws PauseableComponentControllerRequestFailedException;

    /**
     * Deliver pause request to specific registered pauseable components.
     */
    void pause(String targets) throws PauseableComponentControllerRequestFailedException;

    /**
     * Deliver resume request to all the currently registered pauseable components.
     */
    void resume() throws PauseableComponentControllerRequestFailedException;

    /**
     * Deliver resume request to specific registered pauseable components.
     */
    void resume(String targets) throws PauseableComponentControllerRequestFailedException;

    /**
     * Check if the server is paused.
     */
    boolean isPaused();

    /**
     * Check if specific pauseable components are paused
     */
    boolean isPaused(String targets) throws PauseableComponentControllerRequestFailedException;

    /**
     * Return all pauseable components. The collection returned can be changed dynamically
     * as pauseable components are registered and deregistered. While iterating through this list
     * an exception may be thrown if the collection changes.
     */
    Collection<PauseableComponent> getPauseableComponents();

}
