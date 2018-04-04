/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.wim;

import java.rmi.RemoteException;

import com.ibm.wsspi.security.wim.SchemaConstants;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Root;

/**
 * The profile service interface.
 */
public interface ProfileServiceLite extends SchemaConstants {
    public static int ASSIGN_MODE = 1;
    public static int REPLACE_ASSIGN_MODE = 2;
    public static int UNASSIGN_MODE = 3;
    public static int ALL_LEVELS = 0;

    /**
     * Returns information of the specified entity or entities.
     * The entity or entities to be retrieved are added under the root data object.
     * Controls can be added under the root data object to specify what kind of information is returned.
     * The entity or entities to be retrieved need to be added under the root data object with the identifiers specified.
     * By specifying different controls, different information can be returned.
     * For example, PropertyControl is used for returning the properties of the
     * entity/entities. GroupMembershipControl is used for returning groups the
     * entity/entities belongs to.
     *
     * @param root The root data object containing the request information.
     * @return The root data object containing the requested information.
     * @throws WIMException If there was an error retrieving the specified entities.
     * @throws RemoteException If there was an error making a remote call.
     */
    Root get(Root root) throws WIMException, RemoteException;

    /**
     * Searches the profile repositories for entities matching the given search
     * expression and returns them with the requested properties.
     * The search method is used to search entities. Only the entities which match
     * the search expression will be returned.
     * The following four controls are
     * related to search: SearchControl, PageControl, SortControl,
     * PageResponseControl, and SortResponseControl. The SearchControl contains
     * the property name list which you want to return from the search operation.
     * For example, you want to get uid, cn for all the people whose sn equals to "Doe".
     * The search expression is also included in the SearchControl. If you want to
     * use the paged search function, the PageControl is needed.
     *
     * @param root the root data object containing the control(s) related to search.
     * @return the root data object containing the entities matching the search expression.
     * @throws WIMException If there was an error searching for the specified entities.
     * @throws RemoteException If there was an error making a remote call.
     */
    Root search(Root root) throws WIMException, RemoteException;

    /**
     * Authenticates the LoginAccount data object in the specified root data object.
     * User can be authenticated either using loginId/password or using X509Certificate.
     * The successfully authenticated LoginAccount data object will be returned with requested properties
     * specified in the LoginControl. <br>
     *
     * @param root the root data object containing the LoginAccount to authenticate.
     * @return the root data object containing the LoginAccount which is successfully authenticated.
     * @throws WIMException If there was an error authenticating the user.
     * @throws RemoteException If there was an error making a remote call.
     */
    Root login(Root root) throws WIMException, RemoteException;

    /**
     * Deletes the entity specified in the root data object. Only one entity can be delete at one time.
     *
     * @param root The root data object which contains the entity to delete.
     *            The identifier of the entity should be specified.
     *
     * @return The root data object containing the deleted entity and its descendants
     *         (if there are any), with their identifiers.
     *
     * @throws WIMException If there was an error deleting the specified entity.
     * @throws RemoteException If there was an error making a remote call.
     */
    Root delete(Root root) throws WIMException, RemoteException;

    /**
     * Creates the entity under the given root data object.
     * This method is used for creating an entity. The entity needed to be created
     * can be added under the root data object along with the properties.<br>
     * The output root data object of the create method contains the created entity data object
     * which contains its identifier.
     *
     * @param root The root data object which contains the entity to be created.
     * @return The root data object which contains the created entity and its identifier.
     * @throws WIMException If there was an error creating the specified entity.
     * @throws RemoteException If there was an error making a remote call.
     */
    Root create(Root root) throws WIMException, RemoteException;

    /**
     * Updates entity specified in the root data object.
     * The caller can create a empty root data object and specify
     * the changes needed. All the changes will replace the existing values.
     *
     * @param root The root data object containing entity with changes need to update.
     * @return The root data object containing the updated entity with its identifier.
     * @throws WIMException If there was an error updating the specified entity.
     * @throws RemoteException If there was an error making a remote call.
     */
    Root update(Root root) throws WIMException, RemoteException;
}