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

package com.ibm.ws.security.wim.scim20.model.users;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.websphere.security.wim.scim20.model.users.UserGroup;
import com.ibm.ws.security.wim.scim20.SCIMUtil;

public class UserGroupImplTest {

    @Test
    public void serialize() throws Exception {

        UserGroupImpl user = getTestInstance();

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"value\":\"value\",");
        expected.append("\"$ref\":\"$ref\",");
        expected.append("\"display\":\"display\",");
        expected.append("\"type\":\"type\"");
        expected.append("}");

        /*
         * Serialize.
         */
        String serialized = SCIMUtil.serialize(user);
        assertEquals(expected.toString(), serialized);

        /*
         * Deserialize.
         */
        UserGroup deserialized = SCIMUtil.deserialize(serialized, UserGroup.class);
        assertEquals(user, deserialized);
    }

    public static UserGroupImpl getTestInstance() {

        UserGroupImpl userGroup = new UserGroupImpl();
        userGroup.setDisplay("display");
        userGroup.setRef("$ref");
        userGroup.setType("type");
        userGroup.setValue("value");
        return userGroup;
    }
}
