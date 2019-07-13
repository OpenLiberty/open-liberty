/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.security.wim.scim20;

import java.util.Set;

import com.ibm.websphere.security.wim.scim20.exceptions.InvalidFilterException;
import com.ibm.websphere.security.wim.scim20.exceptions.InvalidValueException;
import com.ibm.websphere.security.wim.scim20.exceptions.MutabilityException;
import com.ibm.websphere.security.wim.scim20.exceptions.TooManyResultsException;
import com.ibm.websphere.security.wim.scim20.exceptions.UniquenessException;
import com.ibm.websphere.security.wim.scim20.model.ListResponse;
import com.ibm.websphere.security.wim.scim20.model.Resource;
import com.ibm.websphere.security.wim.scim20.model.SearchRequest;
import com.ibm.websphere.security.wim.scim20.model.groups.Group;
import com.ibm.websphere.security.wim.scim20.model.resourcetype.ResourceType;
import com.ibm.websphere.security.wim.scim20.model.schemas.Schema;
import com.ibm.websphere.security.wim.scim20.model.serviceprovider.ServiceProviderConfig;
import com.ibm.websphere.security.wim.scim20.model.users.User;

// TODO Bulk
// TODO Will need exceptions for Authorization.
public interface SCIMService {

    /**
     * Create a group.
     *
     * @param group
     *            The group to create.
     * @return The view of the group after creation.
     * @throws UniquenessException
     *             One or more of the attribute values are already in use or are
     *             reserved.
     * @throws InvalidValueException
     *             A required value was missing or the value specified was not
     *             compatible with the operation or attribute type or resource
     *             schema.
     */
    public Group createGroup(Group group) throws UniquenessException, InvalidValueException;

    /**
     * Create a user.
     *
     * @param user
     *            The user to create.
     * @return The view of the user after creation.
     * @throws UniquenessException
     *             One or more of the attribute values are already in use or are
     *             reserved.
     * @throws InvalidValueException
     *             A required value was missing or the value specified was not
     *             compatible with the operation or attribute type or resource
     *             schema.
     */
    public User createUser(User user) throws UniquenessException, InvalidValueException;

    /**
     * Remove a known group using the group's identifier.
     *
     * @param id
     *            The group's identifier.
     */
    public void deleteGroup(String id);

    /**
     * Remove a known user using the user's identifier.
     *
     * @param id
     *            The user's identifier.
     */
    public void deleteUser(String id);

    /**
     * Retrieve a known group using the group's identifier.
     *
     * @param id
     *            The group's identifier.
     * @param attributes
     *            A list of attributes to return, overriding the default set of
     *            attributes to return.
     * @param excludedAttributes
     *            A list of attributes to be removed from the default set of
     *            attributes to return. This has no effect on attributes whose
     *            schema "returned" settings is "always". Ignored if
     *            'attributes' parameter is non-null.
     * @return The group.
     * @throws InvalidValueException
     *             A required value was missing or the value specified was not
     *             compatible with the operation or attribute type or resource
     *             schema.
     */
    public Group getGroup(String id, Set<String> attributes) throws InvalidValueException;

    /**
     * Retrieve all groups that match the input filter expression.
     *
     * @param filter
     *            Filter expression used to filter groups.
     * @param attributes
     *            A list of attributes to return, overriding the default set of
     *            attributes to return.
     * @param excludedAttributes
     *            A list of attributes to be removed from the default set of
     *            attributes to return. This has no effect on attributes whose
     *            schema "returned" settings is "always". Ignored if the
     *            <code>attributes</code> parameter is non-null.
     * @return All groups that match the input filter expression.
     * @throws InvalidFilterException
     *             The specified filter syntax is invalid or the specified
     *             attribute and filter comparison combination is not valid.
     * @throws TooManyResultsException
     *             The specified filter yields more results than the server
     *             allows.
     * @throws InvalidValueException
     *             A required value was missing or the value specified was not
     *             compatible with the operation or attribute type or resource
     *             schema.
     */
    public ListResponse<Group> getGroups(String filter, Set<String> attributes, Set<String> excludedAttributes) throws InvalidFilterException, TooManyResultsException;

    /**
     * Retrieve all resources (groups and / or users) that match the input
     * filter expression.
     *
     * @param endpoint One of "Users", "Groups" or null for root.
     * @param searchRequest The parameters for the search request.
     * @return All resources that match the input filter expression.
     * @throws InvalidFilterException
     *             The specified filter syntax is invalid or the specified
     *             attribute and filter comparison combination is not valid.
     * @throws TooManyResultsException
     *             The specified filter yields more results than the server
     *             allows.
     * @throws InvalidValueException
     *             A required value was missing or the value specified was not
     *             compatible with the operation or attribute type or resource
     *             schema.
     */
    // TODO Not sure if we should put endpoint in searchrequest... would NOT want to serialize it
    public ListResponse<Resource> getResources(String endpoint, SearchRequest searchRequest) throws InvalidFilterException, TooManyResultsException, InvalidValueException;

    // TODO DOC
    public ResourceType getResourceType(String schemaUri);

    // TODO DOC
    public ListResponse<ResourceType> getResourceTypes();

    /**
     * Get resource schemas supported by the SCIM service provider.
     *
     * @return The supported resource schemas.
     */
    public ListResponse<Schema> getSchemas();

    /**
     * Get the resource schema specified by the schema URI provided.
     *
     * @param schemaUri The schema URI to retrieve the schema for.
     * @return The resource schema.
     */
    public Schema getSchemas(String schemaUri);

    /**
     * Get the service provider configuration that describes the SCIM specification features available on
     * a service provider.
     *
     * @return The service provider configuration.
     */
    public ServiceProviderConfig getServiceProviderConfig();

    /**
     * Retrieve the currently authenticated user.
     *
     * @param attributes
     *            A list of attributes to return, overriding the default set of
     *            attributes to return.
     * @param excludedAttributes
     *            A list of attributes to be removed from the default set of
     *            attributes to return. This has no effect on attributes whose
     *            schema "returned" settings is "always". Ignored if
     *            'attributes' parameter is non-null.
     * @return The currently authenticated user.
     * @throws InvalidValueException
     *             A required value was missing or the value specified was not
     *             compatible with the operation or attribute type or resource
     *             schema.
     */
    public User getMe(Set<String> attributes, Set<String> excludedAttributes) throws InvalidValueException;

    /**
     * Retrieve a known user using the user's identifier.
     *
     * @param id
     *            The user's identifier.
     * @param attributes
     *            A list of attributes to return, overriding the default set of
     *            attributes to return.
     * @param excludedAttributes
     *            A list of attributes to be removed from the default set of
     *            attributes to return. This has no effect on attributes whose
     *            schema "returned" settings is "always". Ignored if
     *            'attributes' parameter is non-null.
     * @return The user.
     * @throws InvalidValueException
     *             A required value was missing or the value specified was not
     *             compatible with the operation or attribute type or resource
     *             schema.
     */
    public User getUser(String id, Set<String> attributes, Set<String> excludedAttributes) throws InvalidValueException;

    /**
     * Retrieve all users that match the input filter expression.
     *
     * @param filter
     *            Filter expression used to filter users.
     * @param attributes
     *            A list of attributes to return, overriding the default set of
     *            attributes to return.
     * @param excludedAttributes
     *            A list of attributes to be removed from the default set of
     *            attributes to return. This has no effect on attributes whose
     *            schema "returned" settings is "always". Ignored if the
     *            <code>attributes</code> parameter is non-null.
     * @return All users that match the input filter expression.
     * @throws InvalidFilterException
     *             The specified filter syntax is invalid or the specified
     *             attribute and filter comparison combination is not valid.
     * @throws TooManyResultsException
     *             The specified filter yields more results than the server
     *             allows.
     * @throws InvalidValueException
     *             A required value was missing or the value specified was not
     *             compatible with the operation or attribute type or resource
     *             schema.
     */
    public ListResponse<User> getUsers(String filter, Set<String> attributes,
                                       Set<String> excludedAttributes) throws InvalidFilterException, TooManyResultsException, InvalidValueException;

    /**
     * Update an existing group.
     *
     * @param id
     *            The ID of the group to update.
     * @param group
     *            The group containing the values to update.
     * @return The view of the group after making the update.
     * @throws MutabilityException
     *             The attempted modification is not compatible with the target
     *             attribute's mutability or current state.
     * @throws InvalidValueException
     *             A required value was missing or the value specified was not
     *             compatible with the operation or attribute type or resource
     *             schema.
     */
    public Group updateGroup(String id, Group group) throws MutabilityException, InvalidValueException;

    /**
     * Update an existing user.
     *
     * @param id
     *            The ID of the user to update.
     * @param user
     *            The user containing the values to update.
     * @return The view of the user after making the update.
     * @throws MutabilityException
     *             The attempted modification is not compatible with the target
     *             attribute's mutability or current state.
     * @throws InvalidValueException
     *             A required value was missing or the value specified was not
     *             compatible with the operation or attribute type or resource
     *             schema.
     */
    public User updateUser(String id, User user) throws MutabilityException, InvalidValueException;
}
