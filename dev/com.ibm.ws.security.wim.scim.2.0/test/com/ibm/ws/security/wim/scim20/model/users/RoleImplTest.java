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

import com.ibm.websphere.security.wim.scim20.model.users.Role;
import com.ibm.ws.security.wim.scim20.SCIMUtil;

public class RoleImplTest {

    @Test
    public void serialize() throws Exception {

        RoleImpl address = getTestInstance();

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"value\":\"value\",");
        expected.append("\"display\":\"display\",");
        expected.append("\"type\":\"type\",");
        expected.append("\"primary\":false");
        expected.append("}");

        /*
         * Serialize.
         */
        String serialized = SCIMUtil.serialize(address);
        assertEquals(expected.toString(), serialized);

        /*
         * Deserialize.
         */
        Role deserialized = SCIMUtil.deserialize(serialized, Role.class);
        assertEquals(address, deserialized);
    }

    public static RoleImpl getTestInstance() {
        RoleImpl role = new RoleImpl();
        role.setDisplay("display");
        role.setPrimary(false);
        role.setType("type");
        role.setValue("value");
        return role;
    }
}
