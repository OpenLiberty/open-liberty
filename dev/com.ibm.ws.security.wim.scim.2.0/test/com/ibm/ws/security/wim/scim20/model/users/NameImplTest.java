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

import com.ibm.websphere.security.wim.scim20.model.users.Name;
import com.ibm.ws.security.wim.scim20.SCIMUtil;

public class NameImplTest {

    @Test
    public void serialize() throws Exception {
        NameImpl name = getTestInstance();

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"formatted\":\"formatted\",");
        expected.append("\"familyName\":\"familyName\",");
        expected.append("\"givenName\":\"givenName\",");
        expected.append("\"middleName\":\"middleName\",");
        expected.append("\"honorificPrefix\":\"honorificPrefix\",");
        expected.append("\"honorificSuffix\":\"honorificSuffix\"");
        expected.append("}");

        /*
         * Serialize.
         */
        String serialized = SCIMUtil.serialize(name);
        assertEquals(expected.toString(), serialized);

        /*
         * Deserialize.
         */
        Name deserialized = SCIMUtil.deserialize(serialized, Name.class);
        assertEquals(name, deserialized);
    }

    public static NameImpl getTestInstance() {
        NameImpl name = new NameImpl();
        name.setFamilyName("familyName");
        name.setFormatted("formatted");
        name.setGivenName("givenName");
        name.setHonorificPrefix("honorificPrefix");
        name.setHonorificSuffix("honorificSuffix");
        name.setMiddleName("middleName");
        return name;
    }
}
