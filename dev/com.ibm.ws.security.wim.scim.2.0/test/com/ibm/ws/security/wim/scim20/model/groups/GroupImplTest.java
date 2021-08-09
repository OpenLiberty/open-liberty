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

package com.ibm.ws.security.wim.scim20.model.groups;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import com.ibm.websphere.security.wim.scim20.model.groups.Group;
import com.ibm.websphere.security.wim.scim20.model.groups.GroupMember;
import com.ibm.ws.security.wim.scim20.SCIMUtil;
import com.ibm.ws.security.wim.scim20.model.MetaImplTest;
import com.ibm.ws.security.wim.scim20.model.extensions.WIMGroupImpl;
import com.ibm.ws.security.wim.scim20.model.extensions.WIMGroupImplTest;

public class GroupImplTest {

    @Test
    public void serialize() throws Exception {

        GroupImpl group = new GroupImpl();
        group.setId("id");
        group.setExternalId("externalId");
        group.setMeta(MetaImplTest.getTestInstance());
        group.setDisplayName("displayName");
        group.setMembers(Arrays.asList(new GroupMember[] { MemberImplTest.getTestInstance() }));
        group.setWIMGroup(WIMGroupImplTest.getTestInstance());

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"schemas\":[\"" + GroupImpl.SCHEMA_URN + "\",\"" + WIMGroupImpl.SCHEMA_URN + "\"],");
        expected.append("\"id\":\"id\",");
        expected.append("\"externalId\":\"externalId\",");
        expected.append("\"meta\":{");
        expected.append("\"resourceType\":\"resourceType\",");
        expected.append("\"created\":0,");
        expected.append("\"lastModified\":0,");
        expected.append("\"location\":\"location\",");
        expected.append("\"version\":\"version\"");
        expected.append("},");
        expected.append("\"displayName\":\"displayName\",");
        expected.append("\"members\":[{");
        expected.append("\"value\":\"value\",");
        expected.append("\"$ref\":\"$ref\"");
        expected.append("}],");
        expected.append("\"" + WIMGroupImpl.SCHEMA_URN + "\":{");
        expected.append("\"identifier\":{");
        expected.append("\"uniqueId\":\"uniqueId\",");
        expected.append("\"uniqueName\":\"uniqueName\",");
        expected.append("\"externalId\":\"externalId\",");
        expected.append("\"externalName\":\"externalName\",");
        expected.append("\"repositoryId\":\"repositoryId\"");
        expected.append("},");
        expected.append("\"cn\":\"cn\",");
        expected.append("\"myProperty1\":\"myValue1\",");
        expected.append("\"myProperty2\":\"myValue2\"");
        expected.append("}");
        expected.append("}");

        /*
         * Serialize.
         */
        String serialized = SCIMUtil.serialize(group);
        assertEquals(expected.toString(), serialized);

        /*
         * Deserialize.
         */
        Group deserialized = SCIMUtil.deserialize(serialized, Group.class);
        assertEquals(group, deserialized);
    }
}
