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
package com.ibm.ws.security.wim.scim20;

import java.util.Set;

import com.ibm.websphere.security.wim.scim20.SCIMService;
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
import com.ibm.ws.security.wim.scim20.model.ListResponseImpl;
import com.ibm.ws.security.wim.scim20.model.groups.GroupImpl;
import com.ibm.ws.security.wim.scim20.model.resourcetype.ResourceTypeImpl;
import com.ibm.ws.security.wim.scim20.model.schemas.SchemaImpl;
import com.ibm.ws.security.wim.scim20.model.serviceprovider.ServiceProviderConfigImpl;
import com.ibm.ws.security.wim.scim20.model.users.UserImpl;

public class SCIMServiceV20 implements SCIMService {

    @Override
    public Group createGroup(Group group) throws UniquenessException, InvalidValueException {
        // TODO Auto-generated method stub
        return new GroupImpl();
    }

    @Override
    public User createUser(User user) throws UniquenessException, InvalidValueException {
        // TODO Auto-generated method stub
        return new UserImpl();
    }

    @Override
    public void deleteGroup(String id) {
        // TODO Auto-generated method stub
    }

    @Override
    public void deleteUser(String id) {
        // TODO Auto-generated method stub
    }

    @Override
    public Group getGroup(String id, Set<String> attributes) throws InvalidValueException {
        // TODO Auto-generated method stub
        return new GroupImpl();
    }

    @Override
    public ListResponse<Group> getGroups(String filter, Set<String> attributes, Set<String> excludedAttributes) throws InvalidFilterException, TooManyResultsException {
        // TODO Auto-generated method stub
        return new ListResponseImpl<Group>();
    }

    @Override
    public ListResponse<Resource> getResources(String endpoint, SearchRequest searchRequest) throws InvalidFilterException, TooManyResultsException, InvalidValueException {
        // TODO Auto-generated method stub
        return new ListResponseImpl<Resource>();
    }

    @Override
    public ResourceType getResourceType(String schemaUri) {
        // TODO Auto-generated method stub
        return new ResourceTypeImpl();
    }

    @Override
    public ListResponse<ResourceType> getResourceTypes() {
        // TODO Auto-generated method stub
        return new ListResponseImpl<ResourceType>();
    }

    @Override
    public ListResponse<Schema> getSchemas() {
        // TODO Auto-generated method stub
        return new ListResponseImpl<Schema>();
    }

    @Override
    public Schema getSchemas(String schemaUri) {
        // TODO Auto-generated method stub
        return new SchemaImpl();
    }

    @Override
    public ServiceProviderConfig getServiceProviderConfig() {
        // TODO Auto-generated method stub
        return new ServiceProviderConfigImpl();
    }

    @Override
    public User getMe(Set<String> attributes, Set<String> excludedAttributes) throws InvalidValueException {
        // TODO Auto-generated method stub
        return new UserImpl();
    }

    @Override
    public User getUser(String id, Set<String> attributes, Set<String> excludedAttributes) throws InvalidValueException {
        // TODO Auto-generated method stub
        return new UserImpl();
    }

    @Override
    public ListResponse<User> getUsers(String filter, Set<String> attributes,
                                       Set<String> excludedAttributes) throws InvalidFilterException, TooManyResultsException, InvalidValueException {
        // TODO Auto-generated method stub
        return new ListResponseImpl<User>();
    }

    @Override
    public Group updateGroup(String id, Group group) throws MutabilityException, InvalidValueException {
        // TODO Auto-generated method stub
        return new GroupImpl();
    }

    @Override
    public User updateUser(String id, User user) throws MutabilityException, InvalidValueException {
        // TODO Auto-generated method stub
        return new UserImpl();
    }

}
