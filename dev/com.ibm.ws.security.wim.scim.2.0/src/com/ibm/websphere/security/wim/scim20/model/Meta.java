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

package com.ibm.websphere.security.wim.scim20.model;

import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ibm.ws.security.wim.scim20.model.MetaImpl;

/**
 * Resource metadata.
 */
@JsonDeserialize(as = MetaImpl.class)
public interface Meta {

    /**
     * Get the "DateTime" that the resource was added to the service provider.
     *
     * @return The "DateTime" that the resource was added to the service
     *         provider.
     */
    public Date getCreated();

    /**
     * Get the most recent DateTime that the details of this resource were
     * updated at the service provider.
     *
     * @return The most recent DateTime that the details of this resource were
     *         updated at the service provider.
     */
    public Date getLastModified();

    /**
     * Get the URI of the resource being returned.
     *
     * @return The URI of the resource being returned.
     */
    public String getLocation();

    /**
     * Get the name of the resource type of the resource.
     *
     * @return The name of the resource type of the resource.
     */
    public String getResourceType();

    /**
     * Get the version of the resource being returned.
     *
     * @return The version of the resource being returned.
     */
    // TODO Not sure we will support this since we won't support etags.
    public String getVersion();
}
