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

import java.util.List;

/**
 * Contains common attributes for all resources.
 */
public interface Resource {

    /**
     * Get an identifier for the resource as defined by the provisioning client.
     *
     * @return An identifier for the resource as defined by the provisioning
     *         client.
     */
    public String getExternalId();

    /**
     * Get a unique identifier for a SCIM resource as defined by the service
     * provider.
     *
     * @return A unique identifier for a SCIM resource as defined by the service
     *         provider.
     */
    public String getId();

    /**
     * Get the resource's meta data.
     *
     * @return The resource's meta data.
     */
    public Meta getMeta();

    /**
     * Get the list of one or more URIs that indicate included SCIM schemas that
     * are used to indicate the attributes contained within a resource.
     *
     * @return List of one or more URIs that indicate included SCIM schemas.
     */
    public List<String> getSchemas();
}
