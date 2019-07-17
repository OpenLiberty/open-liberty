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

package com.ibm.websphere.security.wim.scim20.model.groups;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ibm.websphere.security.wim.scim20.model.Resource;
import com.ibm.websphere.security.wim.scim20.model.extensions.WIMGroup;
import com.ibm.ws.security.wim.scim20.model.groups.GroupImpl;

@JsonDeserialize(as = GroupImpl.class)
public interface Group extends Resource {

    public String getDisplayName();

    public List<GroupMember> getMembers();

    public WIMGroup getWIMGroup();

    public void setDisplayName(String displayName);

    public void setExternalId(String externalId);

    public void setId(String id);

    public void setMembers(List<GroupMember> members);

    public void setWIMGroup(WIMGroup wimGroup);
}
