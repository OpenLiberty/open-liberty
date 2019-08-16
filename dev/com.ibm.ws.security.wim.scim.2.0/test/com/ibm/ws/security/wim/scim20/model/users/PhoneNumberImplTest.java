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

import com.ibm.websphere.security.wim.scim20.model.users.PhoneNumber;
import com.ibm.ws.security.wim.scim20.SCIMUtil;

public class PhoneNumberImplTest {

    @Test
    public void serialize() throws Exception {

        PhoneNumberImpl phone = getTestInstance();

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
        String serialized = SCIMUtil.serialize(phone);
        assertEquals(expected.toString(), serialized);

        /*
         * Deserialize.
         */
        PhoneNumber deserialized = SCIMUtil.deserialize(serialized, PhoneNumber.class);
        assertEquals(phone, deserialized);
    }

    public static PhoneNumberImpl getTestInstance() {

        PhoneNumberImpl phone = new PhoneNumberImpl();
        phone.setDisplay("display");
        phone.setPrimary(false);
        phone.setType("type");
        phone.setValue("value");
        return phone;
    }
}
