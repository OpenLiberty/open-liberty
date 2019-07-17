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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ibm.ws.security.wim.scim20.model.ListResponseImpl;

/**
 * Contains a response for a resource query.
 *
 * @param <T>
 *            The resource type that will be contained in the response.
 */
@JsonDeserialize(as = ListResponseImpl.class)
public interface ListResponse<T extends Resource> {

    /**
     * Get the number of resources returned in a list response page when partial
     * results are returned due to pagination.
     *
     * @return Get the number of resources returned.
     */
    public Integer getItemsPerPage();

    /**
     * Get a list containing the requested resources. This may be a subset of
     * the full set of resources if pagination is requested.
     *
     * @return A list containing the requested resources.
     */
    public List<T> getResources();

    /**
     * Get the list of one or more URIs that indicate included SCIM schemas that
     * are used to indicate the attributes contained within a resource.
     *
     * @return
     */
    public List<String> getSchemas();

    /**
     * Get the 1-based index of the first result in the current set of list
     * results when partial results are returned due to pagination.
     *
     * @return The 1-based index of the first result in the current set of list
     *         results.
     */
    public Integer getStartIndex();

    /**
     * Get the total number of results returned by the list or query operation.
     * The value may be larger than the number of resources returned, such as
     * when returning a single page of results where multiple pages are
     * available.
     *
     * @return The total number of results returned by the list or query
     *         operation.
     */
    public Integer getTotalResults();
}
