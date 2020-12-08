/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.v1;

/**
 * Service to obtain instances of a user's Toolbox.
 */
public interface IToolboxService {

    /**
     * Returns the Toolbox instance for the specified user. References to the instance should not be cached.
     * 
     * @return IToolbox instance. Will not return {@code null}.
     */
    IToolbox getToolbox(String userId);

    /**
     * Reset all of the toolboxes to their default state.
     */
    void resetAllToolboxes();

    /**
     * Remove the tool entry specified by id from all of the toolboxes.
     * 
     * @param id The tool entry ID
     */
    void removeToolEntryFromAllToolboxes(String id);

}
