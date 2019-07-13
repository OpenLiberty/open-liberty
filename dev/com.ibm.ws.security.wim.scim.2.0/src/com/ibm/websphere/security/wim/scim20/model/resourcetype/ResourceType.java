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

package com.ibm.websphere.security.wim.scim20.model.resourcetype;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ibm.websphere.security.wim.scim20.model.Meta;
import com.ibm.websphere.security.wim.scim20.model.Resource;
import com.ibm.ws.security.wim.scim20.model.resourcetype.ResourceTypeImpl;

/**
 * Metadata for a resource type.
 */
@JsonDeserialize(as = ResourceTypeImpl.class)
public interface ResourceType extends Resource {
    // TODO Do we really want to extend Resource?

    /**
     * Get the resource type's human-readable description.
     *
     * @return The resource type's human-readable description.
     */
    public String getDescription();

    /**
     * Get the resource type's HTTP-addressable endpoint relative to the Base
     * URL of the service provider, e.g., "Users".
     *
     * @return The resource type's HTTP-addressable endpoint relative to the
     *         Base URL of the service provider.
     */
    public String getEndpoint();

    /**
     * The {@link ResourceType} resource does not support the externalId
     * attribute.
     *
     * @return Null.
     */
    @Override
    public String getExternalId();

    /**
     * The {@link ResourceType} resource does not support the meta attribute.
     *
     * @return Null.
     */
    @Override
    public Meta getMeta();

    /**
     * Get the resource type name. This name is referenced by the
     * "meta.resourceType" attribute in all resources.
     *
     * @return The resource type name
     */
    public String getName();

    /**
     * Get the resource type's primary/base schema URI, e.g.,
     * "urn:ietf:params:scim:schemas:core:2.0:User".
     *
     * @return The resource type's primary/base schema URI
     */
    public String getSchema();

    /**
     * Get a list of URIs of the resource type's schema extensions.
     *
     * @return A list of URIs of the resource type's schema extensions.
     */
    public List<SchemaExtension> getSchemaExtensions();
}
