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

package com.ibm.ws.security.wim.scim20.model;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.websphere.security.wim.scim20.model.ListResponse;
import com.ibm.websphere.security.wim.scim20.model.Resource;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
@JsonPropertyOrder(value = { "schemas", "totalResults", "startIndex", "itemsPerPage", "Resources" })
public class ListResponseImpl<T extends Resource> implements ListResponse<T> {

    public static final String SCHEMA_URI = "urn:ietf:params:scim:api:messages:2.0:ListResponse";

    @JsonProperty("itemsPerPage")
    private Integer itemsPerPage;

    @JsonProperty("Resources")
    private List<T> resources;

    @JsonProperty("schemas")
    private final List<String> schemas;

    @JsonProperty("startIndex")
    private Integer startIndex;

    @JsonProperty("totalResults")
    private Integer totalResults;

    /**
     * Construct a new {@link ListResponseImpl} instance.
     */
    public ListResponseImpl() {
        schemas = Arrays.asList(new String[] { SCHEMA_URI });
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ListResponseImpl<?> other = (ListResponseImpl<?>) obj;
        if (itemsPerPage == null) {
            if (other.itemsPerPage != null) {
                return false;
            }
        } else if (!itemsPerPage.equals(other.itemsPerPage)) {
            return false;
        }
        if (resources == null) {
            if (other.resources != null) {
                return false;
            }
        } else if (!resources.equals(other.resources)) {
            return false;
        }
        if (schemas == null) {
            if (other.schemas != null) {
                return false;
            }
        } else if (!schemas.equals(other.schemas)) {
            return false;
        }
        if (startIndex == null) {
            if (other.startIndex != null) {
                return false;
            }
        } else if (!startIndex.equals(other.startIndex)) {
            return false;
        }
        if (totalResults == null) {
            if (other.totalResults != null) {
                return false;
            }
        } else if (!totalResults.equals(other.totalResults)) {
            return false;
        }
        return true;
    }

    @Override
    public Integer getItemsPerPage() {
        return itemsPerPage;
    }

    @Override
    public List<T> getResources() {
        return resources;
    }

    @Override
    public List<String> getSchemas() {
        return schemas;
    }

    @Override
    public Integer getStartIndex() {
        return startIndex;
    }

    @Override
    public Integer getTotalResults() {
        return totalResults;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((itemsPerPage == null) ? 0 : itemsPerPage.hashCode());
        result = prime * result + ((resources == null) ? 0 : resources.hashCode());
        result = prime * result + ((schemas == null) ? 0 : schemas.hashCode());
        result = prime * result + ((startIndex == null) ? 0 : startIndex.hashCode());
        result = prime * result + ((totalResults == null) ? 0 : totalResults.hashCode());
        return result;
    }

    /**
     * Set the number of resources returned in a list response page when partial
     * results are returned due to pagination.
     *
     * @param itemsPerPage
     *            Get the number of resources returned.
     */
    public void setItemsPerPage(Integer itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    /**
     * Set a list containing the requested resources. This may be a subset of
     * the full set of resources if pagination is requested.
     *
     * @param resources
     *            A list containing the requested resources.
     */
    public void setResources(List<T> resources) {
        this.resources = resources;
    }

    /**
     * Set the 1-based index of the first result in the current set of list
     * results when partial results are returned due to pagination.
     *
     * @param startIndex
     *            The 1-based index of the first result in the current set of
     *            list results.
     */
    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    /**
     * Set the total number of results returned by the list or query operation.
     * The value may be larger than the number of resources returned, such as
     * when returning a single page of results where multiple pages are
     * available.
     *
     * @param totalResults
     *            The total number of results returned by the list or query
     *            operation.
     */
    public void setTotalResults(Integer totalResults) {
        this.totalResults = totalResults;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ListResponseImpl [");
        if (itemsPerPage != null) {
            builder.append("itemsPerPage=");
            builder.append(itemsPerPage);
            builder.append(", ");
        }
        if (resources != null) {
            builder.append("resources=");
            builder.append(resources);
            builder.append(", ");
        }
        if (schemas != null) {
            builder.append("schemas=");
            builder.append(schemas);
            builder.append(", ");
        }
        if (startIndex != null) {
            builder.append("startIndex=");
            builder.append(startIndex);
            builder.append(", ");
        }
        if (totalResults != null) {
            builder.append("totalResults=");
            builder.append(totalResults);
        }
        builder.append("]");
        return builder.toString();
    }
}
