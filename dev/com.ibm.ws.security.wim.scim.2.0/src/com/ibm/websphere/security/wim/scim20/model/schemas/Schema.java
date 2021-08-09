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

package com.ibm.websphere.security.wim.scim20.model.schemas;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ibm.websphere.security.wim.scim20.model.Meta;
import com.ibm.websphere.security.wim.scim20.model.Resource;
import com.ibm.ws.security.wim.scim20.model.schemas.SchemaImpl;

/**
 * A schema in use by resources available and accepted by the SCIM service
 * provider.
 */
@JsonDeserialize(as = SchemaImpl.class)
public interface Schema extends Resource {
    // TODO Do we really want to extend Resource?

    /**
     * Get the list of service provider attributes.
     *
     * @return List of service provider attributes.
     */
    public List<SchemaAttribute> getAttributes();

    /**
     * Get the schema's human-readable description.
     *
     * @return The schema's human-readable description.
     */
    public String getDescription();

    /**
     * The {@link Schema} resource does not support the externalId attribute.
     *
     * @return Null.
     */
    @Override
    public String getExternalId();

    /**
     * Get the unique URI of the schema. Unlike most other schemas, which use
     * some sort of Globally Unique Identifier (GUID) for the "id", the schema
     * "id" is a URI so that it can be registered and is portable between
     * different service providers and clients.
     *
     * @return The unique URI of the schema.
     */
    @Override
    public String getId();

    /**
     * The {@link Schema} resource does not support the meta attribute.
     *
     * @return Null.
     */
    @Override
    public Meta getMeta();

    /**
     * Get the schema's human-readable name.
     *
     * @return The schema's human-readable name.
     */
    public String getName();

    /**
     * The {@link Schema} resource does not support the schemas attribute.
     *
     * @return Null.
     */
    @Override
    public List<String> getSchemas();
}
