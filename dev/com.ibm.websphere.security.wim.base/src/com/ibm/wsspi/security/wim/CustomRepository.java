/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.wim;

import java.util.Map;

import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Root;

/**
 * <i>CustomRepository</i> interface (<code>com.ibm.wsspi.security.wim.CustomRepository</code>)
 * is used to implement the custom repository as a user feature.
 * <ul>
 * <li>The CustomRepository interface provides similar read capabilities that the UserRegistry interface provides {@link com.ibm.websphere.security.UserRegistry} , but
 * additionally, it supports write capabilities such as create, delete, and update.
 * </li>
 * <li>The CustomRepository interface also provides the ability to add customized attributes to both users and groups. Customized attributes are any attributes which the current
 * schema does not already have pre-defined.
 * </li>
 * </ul>
 *
 * @see com.ibm.websphere.security.UserRegistry
 */
public interface CustomRepository {
    //From former RepositoryConfiguration

    Map<String, String> getRepositoryBaseEntries();

    String[] getRepositoriesForGroups();

    //from internal Repository
    /**
     * Returns information of the specified entities.
     * The entities to be retrieved are added under the root data object.
     * Controls can be added under the root data object to specify what kind of information is returned.
     * This method is used for retrieving information of an entity or entities.
     * The entities to retrieve need to add under the root data object with the identifiers specified.
     * By specifying different controls, different information can be returned.
     * For example, PropertyControl is used for returning the properties of the
     * entity/entities. GroupMembershipControl is used for returning groups the
     * entity/entities belongs to.
     *
     * @param root The root data object containing the request information.
     * @return The root data object containing the requested information.
     * @throws WIMException If there was an error retrieving the specified entities.
     */
    Root get(Root root) throws WIMException;

    /**
     * Searches the profile repositories for entities matching the given search
     * expression and returns them with the requested properties.
     * The search method is used to search entities. Only the entities which match
     * the search expression will be returned.
     * The following four controls are
     * related to search. There are SearchControl, PageControl, SortControl,
     * PageResponseControl, and SortResponseControl. The SearchControl contains
     * the property name list which you want to return from the search operation.
     * For example, you want get uid, cn for all the people whose sn equals to "Doe".
     * The search expression is also included in the SearchControl. If you want to
     * use the paged search function, the PageControl is needed.
     *
     * @param root the root data object containing the control(s) related to search.
     * @return the root data object containing the entities matching the search expression.
     * @throws WIMException If there was an error searching for the specified entities.
     */
    Root search(Root root) throws WIMException;

    /**
     * Authenticates the account data object in the specified root data object.
     * User can be authenticated either using loginId/password or using X509Certificate.
     * The successfully authenticated account data object will be returned with requested properties. <br>
     *
     * @param root the root data object containing the account to authenticate.
     * @return the root data object containing the account which is successfully authenticated.
     * @throws WIMException If there was an error authenticating the user.
     */
    Root login(Root root) throws WIMException;

    /**
     * Returns the realm name
     *
     * @return The realm name.
     */
    String getRealm();

    /**
     * Delete the entity specified in the root data object.
     *
     * @param root The root data object which contains the entity to delete.
     *            The identifier of the entity should be specified.
     * @return The root data object containing the deleted entity and its descendants
     *         (if there are any), with their identifiers.
     * @throws WIMException If there was an error deleting the specified entity.
     */
    Root delete(Root root) throws WIMException;

    /**
     * Creates the entity under the given root data object.
     * This method is used for creating an entity. Empty root data object
     * can be got from getRootDataObject API. The entity needed to create
     * can be added under the root data object along with the properties.<br>
     * The output root data object of the create method contains the created entity data object
     * which contains its identifier.
     *
     * @param root The root data object which contains the entity to be created.
     * @return The root data object which contains the created entity and its identifier.
     * @throws WIMException If there was an error creating the specified entity.
     */
    Root create(Root root) throws WIMException;

    /**
     * Updates entity specified in the root data object.
     * The caller can create an empty root data object and specify
     * the changes needed. All the changes will replace the existing values.
     *
     * @param root The root data object containing entity with changes need to update.
     * @return The root data object containing the updated entity with its identifier.
     * @throws WIMException If there was an error updating the specified entity.
     */
    Root update(Root root) throws WIMException;

}
