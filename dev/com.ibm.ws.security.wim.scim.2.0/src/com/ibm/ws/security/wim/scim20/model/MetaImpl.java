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

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.websphere.security.wim.scim20.model.Meta;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
@JsonPropertyOrder(value = { "resourceType", "created", "lastModified", "location", "version" })
public class MetaImpl implements Meta {

    @JsonProperty("created")
    private Date created;

    @JsonProperty("lastModified")
    private Date lastModified;

    @JsonProperty("location")
    private String location;

    @JsonProperty("resourceType")
    private String resourceType;

    @JsonProperty("version")
    private String version;

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
        MetaImpl other = (MetaImpl) obj;
        if (created == null) {
            if (other.created != null) {
                return false;
            }
        } else if (!created.equals(other.created)) {
            return false;
        }
        if (lastModified == null) {
            if (other.lastModified != null) {
                return false;
            }
        } else if (!lastModified.equals(other.lastModified)) {
            return false;
        }
        if (location == null) {
            if (other.location != null) {
                return false;
            }
        } else if (!location.equals(other.location)) {
            return false;
        }
        if (resourceType == null) {
            if (other.resourceType != null) {
                return false;
            }
        } else if (!resourceType.equals(other.resourceType)) {
            return false;
        }
        if (version == null) {
            if (other.version != null) {
                return false;
            }
        } else if (!version.equals(other.version)) {
            return false;
        }
        return true;
    }

    @Override
    public Date getCreated() {
        return (Date) created.clone();
    }

    @Override
    public Date getLastModified() {
        return (Date) lastModified.clone();
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public String getResourceType() {
        return resourceType;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((created == null) ? 0 : created.hashCode());
        result = prime * result + ((lastModified == null) ? 0 : lastModified.hashCode());
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + ((resourceType == null) ? 0 : resourceType.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    /**
     * Set the "DateTime" that the resource was added to the service provider.
     *
     * @param created
     *            The "DateTime" that the resource was added to the service
     *            provider.
     */
    public void setCreated(Date created) {
        this.created = created == null ? null : (Date) created.clone();
    }

    /**
     * Set the most recent DateTime that the details of this resource were
     * updated at the service provider.
     *
     * @param lastModified
     *            The most recent DateTime that the details of this resource
     *            were updated at the service provider.
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified == null ? null : (Date) lastModified.clone();
    }

    /**
     * Set the URI of the resource being returned.
     *
     * @param location
     *            The URI of the resource being returned.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Set the name of the resource type of the resource.
     *
     * @param resourceType
     *            The name of the resource type of the resource.
     */
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * Set the version of the resource being returned.
     *
     * @param version
     *            The version of the resource being returned.
     */
    // TODO Not sure we will support this since we won't support etags.
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MetaImpl [");
        if (created != null) {
            builder.append("created=");
            builder.append(created);
            builder.append(", ");
        }
        if (lastModified != null) {
            builder.append("lastModified=");
            builder.append(lastModified);
            builder.append(", ");
        }
        if (location != null) {
            builder.append("location=");
            builder.append(location);
            builder.append(", ");
        }
        if (resourceType != null) {
            builder.append("resourceType=");
            builder.append(resourceType);
            builder.append(", ");
        }
        if (version != null) {
            builder.append("version=");
            builder.append(version);
        }
        builder.append("]");
        return builder.toString();
    }
}
