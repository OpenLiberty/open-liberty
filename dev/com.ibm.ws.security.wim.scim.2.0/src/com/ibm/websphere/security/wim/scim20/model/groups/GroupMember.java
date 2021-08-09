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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ibm.ws.security.wim.scim20.model.groups.GroupMemberImpl;

@JsonDeserialize(as = GroupMemberImpl.class)
public interface GroupMember {

    /*
     * TODO Not sure if "type" should be in here.
     *
     * See it in some of the JSON samples, but isn't mentioned in the
     * description. I also see "display" in some of the samples as well.
     */

    public String getRef();

    public String getValue();

    public void setRef(String ref);

    public void setValue(String value);
}
