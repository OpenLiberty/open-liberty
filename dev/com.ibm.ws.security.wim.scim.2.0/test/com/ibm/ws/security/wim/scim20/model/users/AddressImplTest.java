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

import com.ibm.websphere.security.wim.scim20.model.users.Address;
import com.ibm.ws.security.wim.scim20.SCIMUtil;

public class AddressImplTest {

    @Test
    public void serialize() throws Exception {

        AddressImpl address = getTestInstance();

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"formatted\":\"formatted\",");
        expected.append("\"streetAddress\":\"streetAddress\",");
        expected.append("\"locality\":\"locality\",");
        expected.append("\"region\":\"region\",");
        expected.append("\"postalCode\":\"postalCode\",");
        expected.append("\"country\":\"country\",");
        expected.append("\"type\":\"type\"");
        expected.append("}");

        /*
         * Serialize.
         */
        String serialized = SCIMUtil.serialize(address);
        assertEquals(expected.toString(), serialized);

        /*
         * Deserialize.
         */
        Address deserialized = SCIMUtil.deserialize(serialized, Address.class);
        assertEquals(address, deserialized);
    }

    public static AddressImpl getTestInstance() {
        AddressImpl address = new AddressImpl();
        address.setCountry("country");
        address.setFormatted("formatted");
        address.setLocality("locality");
        address.setPostalCode("postalCode");
        address.setRegion("region");
        address.setStreetAddress("streetAddress");
        address.setType("type");
        return address;
    }
}
