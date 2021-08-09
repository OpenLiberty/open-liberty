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

import com.ibm.websphere.security.wim.scim20.model.users.Entitlement;
import com.ibm.ws.security.wim.scim20.SCIMUtil;

public class EntitlementImplTest {

    @Test
    public void serialize() throws Exception {

        EntitlementImpl address = getTestInstance();

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
        Entitlement deserialized = SCIMUtil.deserialize(serialized, Entitlement.class);
        assertEquals(address, deserialized);
    }

    public static EntitlementImpl getTestInstance() {
        EntitlementImpl entitlement = new EntitlementImpl();
        entitlement.setDisplay("display");
        entitlement.setPrimary(false);
        entitlement.setType("type");
        entitlement.setValue("value");
        return entitlement;
    }
}
