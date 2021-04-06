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
 * Service to obtain per user's tool data.
 */
public interface IToolDataService {

    /**
     * Returns the tool data for the specified user and tool. References to the instance
     * should not be cached.
     * 
     * @param userId user id
     * @param toolName the name of the tool
     * @return tool data string. If the tool data doesn't exist, null will be returned.
     *         If the persistence layer throws the IOException, the IOException will be
     *         logged in FFDC and null will be returned.
     */
    public String getToolData(String userId, String toolName);

    /**
     * Deletes the tool data for the specified user and tool.
     * Will try to delete the given tool data for the given user from
     * all available persistence layers.
     * If some persistence layers are not available at the time of the call, the data from those
     * persistence layers are not deleted, however other available persistence layers' tool data
     * are deleted.
     * 
     * @param userId user id
     * @param toolName the name of the tool
     * @return <code>true</code> if the tool data for the given user is deleted from all
     *         available persistence layers. Otherwise return <code>false/code>.
     */
    public boolean deleteToolData(String userId, String toolName);

    /**
     * Adds the tool data for the specified user and tool
     * 
     * @param userId user id
     * @param toolName the name of the tool
     * @return the added tool data if the data is successfully added to the persistence
     *         layer. If errors occurred during the operation, <code>null</code> will be
     *         returned and error will be logged.
     */
    public String addToolData(String userId, String toolName, String toolData);

    /**
     * Checks if the tool data for the specified user and tool exists.
     * 
     * @param userId user Id
     * @param toolName the name of the tool.
     * @return <code>true</code> if the tool exists. Otherwise returns <code>false</code>.
     */
    public boolean exists(String userId, String toolName);

    /**
     * Promotes the tool data from File persistence layer to collective if data
     * doesn't exist in collective.
     * 
     * @param userId The user id
     * @param toolName Name of the tool.
     */
    public void promoteIfPossible(String userId, String toolName);
}
