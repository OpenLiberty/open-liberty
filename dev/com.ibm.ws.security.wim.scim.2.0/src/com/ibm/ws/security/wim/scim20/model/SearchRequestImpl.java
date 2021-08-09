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
package com.ibm.ws.security.wim.scim20.model;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.websphere.security.wim.scim20.model.SearchRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
@JsonPropertyOrder(value = { "attributes", "excludedAttributes", "filter", "sortBy", "sortOrder", "startIndex", "count" })
public class SearchRequestImpl implements SearchRequest {

    public static final String SCHEMA_URI = "urn:ietf:params:scim:api:messages:2.0:SearchRequest";

    @JsonProperty("attributes")
    private Set<String> attributes;

    @JsonProperty("count")
    private Integer count;

    @JsonProperty("excludedAttributes")
    private Set<String> excludedAttributes;

    @JsonProperty("filter")
    private String filter;

    @JsonProperty("schemas")
    private final List<String> schemas;

    @JsonProperty("sortBy")
    private String sortBy;

    @JsonProperty("sortOrder")
    private String sortOrder;

    @JsonProperty("startIndex")
    private Integer startIndex;

    public SearchRequestImpl() {
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
        SearchRequestImpl other = (SearchRequestImpl) obj;
        if (attributes == null) {
            if (other.attributes != null) {
                return false;
            }
        } else if (!attributes.equals(other.attributes)) {
            return false;
        }
        if (count == null) {
            if (other.count != null) {
                return false;
            }
        } else if (!count.equals(other.count)) {
            return false;
        }
        if (excludedAttributes == null) {
            if (other.excludedAttributes != null) {
                return false;
            }
        } else if (!excludedAttributes.equals(other.excludedAttributes)) {
            return false;
        }
        if (filter == null) {
            if (other.filter != null) {
                return false;
            }
        } else if (!filter.equals(other.filter)) {
            return false;
        }
        if (schemas == null) {
            if (other.schemas != null) {
                return false;
            }
        } else if (!schemas.equals(other.schemas)) {
            return false;
        }
        if (sortBy == null) {
            if (other.sortBy != null) {
                return false;
            }
        } else if (!sortBy.equals(other.sortBy)) {
            return false;
        }
        if (sortOrder == null) {
            if (other.sortOrder != null) {
                return false;
            }
        } else if (!sortOrder.equals(other.sortOrder)) {
            return false;
        }
        if (startIndex == null) {
            if (other.startIndex != null) {
                return false;
            }
        } else if (!startIndex.equals(other.startIndex)) {
            return false;
        }
        return true;
    }

    @Override
    public Set<String> getAttributes() {
        return attributes;
    }

    @Override
    public Integer getCount() {
        return count;
    }

    @Override
    public Set<String> getExcludedAttributes() {
        return excludedAttributes;
    }

    @Override
    public String getFilter() {
        return filter;
    }

    public List<String> getSchemas() {
        return schemas;
    }

    @Override
    public String getSortBy() {
        return sortBy;
    }

    @Override
    public String getSortOrder() {
        return sortOrder;
    }

    @Override
    public Integer getStartIndex() {
        return startIndex;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((count == null) ? 0 : count.hashCode());
        result = prime * result + ((excludedAttributes == null) ? 0 : excludedAttributes.hashCode());
        result = prime * result + ((filter == null) ? 0 : filter.hashCode());
        result = prime * result + ((schemas == null) ? 0 : schemas.hashCode());
        result = prime * result + ((sortBy == null) ? 0 : sortBy.hashCode());
        result = prime * result + ((sortOrder == null) ? 0 : sortOrder.hashCode());
        result = prime * result + ((startIndex == null) ? 0 : startIndex.hashCode());
        return result;
    }

    public void setAttributes(Set<String> attributes) {
        this.attributes = attributes;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public void setExcludedAttributes(Set<String> excludedAttributes) {
        this.excludedAttributes = excludedAttributes;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SearchRequestImpl [");
        if (attributes != null) {
            builder.append("attributes=");
            builder.append(attributes);
            builder.append(", ");
        }
        if (count != null) {
            builder.append("count=");
            builder.append(count);
            builder.append(", ");
        }
        if (excludedAttributes != null) {
            builder.append("excludedAttributes=");
            builder.append(excludedAttributes);
            builder.append(", ");
        }
        if (filter != null) {
            builder.append("filter=");
            builder.append(filter);
            builder.append(", ");
        }
        if (schemas != null) {
            builder.append("schemas=");
            builder.append(schemas);
            builder.append(", ");
        }
        if (sortBy != null) {
            builder.append("sortBy=");
            builder.append(sortBy);
            builder.append(", ");
        }
        if (sortOrder != null) {
            builder.append("sortOrder=");
            builder.append(sortOrder);
            builder.append(", ");
        }
        if (startIndex != null) {
            builder.append("startIndex=");
            builder.append(startIndex);
        }
        builder.append("]");
        return builder.toString();
    }

}
