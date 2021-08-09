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

import org.junit.Test;

import com.ibm.websphere.security.wim.scim20.model.groups.GroupMember;
import com.ibm.ws.security.wim.scim20.SCIMUtil;

public class MemberImplTest {

    @Test
    public void serialize() throws Exception {

        GroupMemberImpl member = getTestInstance();

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"value\":\"value\",");
        expected.append("\"$ref\":\"$ref\"");
        expected.append("}");

        /*
         * Serialize.
         */
        String serialized = SCIMUtil.serialize(member);
        assertEquals(expected.toString(), serialized);

        /*
         * Deserialize.
         */
        GroupMember deserialized = SCIMUtil.deserialize(serialized, GroupMember.class);
        assertEquals(member, deserialized);
    }

    public static GroupMemberImpl getTestInstance() {
        GroupMemberImpl member = new GroupMemberImpl();
        member.setValue("value");
        member.setRef("$ref");
        return member;
    }
}
