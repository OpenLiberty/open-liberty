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
package com.ibm.websphere.security.wim.scim20.model;

import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ibm.ws.security.wim.scim20.model.SearchRequestImpl;

@JsonDeserialize(as = SearchRequestImpl.class)
public interface SearchRequest {

    /**
     * A multi-valued list of strings indicating the names of
     * resource attributes to return in the response, overriding the set
     * of attributes that would be returned by default. Attribute names
     * MUST be in standard attribute notation (Section 3.10) form. See
     * Section 3.9 for additional retrieval query parameters. OPTIONAL.
     *
     * @return The list of attributes to return in results.
     */
    public Set<String> getAttributes();

    /**
     * A multi-valued list of strings indicating the
     * names of resource attributes to be removed from the default set of
     * attributes to return. This parameter SHALL have no effect on
     * attributes whose schema "returned" setting is "always" (see
     * Sections 2.2 and 7 of [RFC7643]). Attribute names MUST be in
     * standard attribute notation (Section 3.10) form. See Section 3.9
     * for additional retrieval query parameters. OPTIONAL.
     *
     * @return The list of attributes to exclude from results.
     */
    public Set<String> getExcludedAttributes();

    /**
     * The filter string used to request a subset of resources. The
     * filter string MUST be a valid filter (Section 3.4.2.2) expression.
     * OPTIONAL.
     *
     * @return The filter string.
     */
    public String getFilter();

    /**
     * A string indicating the attribute whose value SHALL be used
     * to order the returned responses. The "sortBy" attribute MUST be
     * in standard attribute notation (Section 3.10) form. See
     * Section 3.4.2.3. OPTIONAL.
     *
     * @return The attribute used to sort the results by.
     */
    public String getSortBy();

    /**
     * A string indicating the order in which the "sortBy"
     * parameter is applied. Allowed values are "ascending" and
     * "descending". See Section 3.4.2.3. OPTIONAL.
     *
     * @return The sort order.
     */
    public String getSortOrder();

    /**
     * An integer indicating the 1-based index of the first
     * query result. See Section 3.4.2.4. OPTIONAL.
     *
     * @return The starting index.
     */
    public Integer getStartIndex();

    /**
     * An integer indicating the desired maximum number of query
     * results per page. See Section 3.4.2.4. OPTIONAL.
     *
     * @return The maximum number of results.
     */
    public Integer getCount();
}
