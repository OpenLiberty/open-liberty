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

import java.util.HashMap;

/**
 * Defines a pauseable component that can be driven for pause and resume actions.
 */
public interface PauseableComponent {

    /**
     * Returns the name of the pauseable component.
     *
     * @return the name of the pauseable component. It is the same string that is used when performing targeted
     *         pause or resume operations.
     */
    public String getName();

    /**
     * Pauses the work managed by the pauseable component.
     *
     * @throws PauseableComponentException if the pauseable component experiences problems pausing
     *             the activity it manages.
     */
    public void pause() throws PauseableComponentException;

    /**
     * Resumes the work managed by the pauseable component.
     *
     * @throws PauseableComponentException if the pauseable component experiences problems resuming
     *             the activity it manages.
     */
    public void resume() throws PauseableComponentException;

    /**
     * Indicates whether or not the pauseable component is paused.
     *
     * @return True if the pauseable component is Paused. False Otherwise.
     */
    public boolean isPaused();

    /**
     * Returns pauseable component extended information.
     *
     * @return A map of name and value pairs that describe the pauseable component.
     */
    public HashMap<String, String> getExtendedInfo();
}