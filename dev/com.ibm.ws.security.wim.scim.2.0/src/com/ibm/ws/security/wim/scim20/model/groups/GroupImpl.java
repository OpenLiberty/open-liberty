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

package com.ibm.ws.security.wim.scim20.model.groups;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ibm.websphere.security.wim.scim20.model.Meta;
import com.ibm.websphere.security.wim.scim20.model.extensions.WIMGroup;
import com.ibm.websphere.security.wim.scim20.model.groups.Group;
import com.ibm.websphere.security.wim.scim20.model.groups.GroupMember;
import com.ibm.ws.security.wim.scim20.model.extensions.WIMGroupImpl;

@JsonInclude(Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
@JsonPropertyOrder(value = { "schemas", "id", "externalId", "meta", "displayName", "members" })
public class GroupImpl implements Group {

    /**
     * The URN for the SCIM 2.0 core group schema.
     */
    public static final String SCHEMA_URN = "urn:ietf:params:scim:schemas:core:2.0:Group";

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("externalId")
    private String externalId;

    @JsonProperty("id")
    private String id;

    @JsonProperty("members")
    private List<GroupMember> members;

    @JsonProperty("meta")
    private Meta meta;

    @JsonProperty("schemas")
    private final List<String> schemas;

    @JsonProperty(WIMGroupImpl.SCHEMA_URN)
    private WIMGroup wimGroup;

    public GroupImpl() {
        schemas = new ArrayList<String>();
        schemas.add(SCHEMA_URN);
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
        GroupImpl other = (GroupImpl) obj;
        if (displayName == null) {
            if (other.displayName != null) {
                return false;
            }
        } else if (!displayName.equals(other.displayName)) {
            return false;
        }
        if (externalId == null) {
            if (other.externalId != null) {
                return false;
            }
        } else if (!externalId.equals(other.externalId)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (members == null) {
            if (other.members != null) {
                return false;
            }
        } else if (!members.equals(other.members)) {
            return false;
        }
        if (meta == null) {
            if (other.meta != null) {
                return false;
            }
        } else if (!meta.equals(other.meta)) {
            return false;
        }
        if (schemas == null) {
            if (other.schemas != null) {
                return false;
            }
        } else if (!schemas.equals(other.schemas)) {
            return false;
        }
        if (wimGroup == null) {
            if (other.wimGroup != null) {
                return false;
            }
        } else if (!wimGroup.equals(other.wimGroup)) {
            return false;
        }
        return true;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getExternalId() {
        return externalId;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<GroupMember> getMembers() {
        return members;
    }

    @Override
    public Meta getMeta() {
        return meta;
    }

    @Override
    public List<String> getSchemas() {
        return schemas;
    }

    @Override
    public WIMGroup getWIMGroup() {
        return wimGroup;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
        result = prime * result + ((externalId == null) ? 0 : externalId.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((members == null) ? 0 : members.hashCode());
        result = prime * result + ((meta == null) ? 0 : meta.hashCode());
        result = prime * result + ((schemas == null) ? 0 : schemas.hashCode());
        result = prime * result + ((wimGroup == null) ? 0 : wimGroup.hashCode());
        return result;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void setMembers(List<GroupMember> members) {
        this.members = members;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    @Override
    public void setWIMGroup(WIMGroup wimGroup) {
        this.wimGroup = wimGroup;

        /*
         * Update the schema list.
         */
        synchronized (schemas) {
            if (wimGroup == null) {
                schemas.remove(WIMGroupImpl.SCHEMA_URN);
            } else {
                if (!schemas.contains(WIMGroupImpl.SCHEMA_URN)) {
                    schemas.add(WIMGroupImpl.SCHEMA_URN);
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GroupImpl [");
        if (displayName != null) {
            builder.append("displayName=");
            builder.append(displayName);
            builder.append(", ");
        }
        if (externalId != null) {
            builder.append("externalId=");
            builder.append(externalId);
            builder.append(", ");
        }
        if (id != null) {
            builder.append("id=");
            builder.append(id);
            builder.append(", ");
        }
        if (members != null) {
            builder.append("members=");
            builder.append(members);
            builder.append(", ");
        }
        if (meta != null) {
            builder.append("meta=");
            builder.append(meta);
            builder.append(", ");
        }
        if (schemas != null) {
            builder.append("schemas=");
            builder.append(schemas);
            builder.append(", ");
        }
        if (wimGroup != null) {
            builder.append("wimGroup=");
            builder.append(wimGroup);
        }
        builder.append("]");
        return builder.toString();
    }
}
