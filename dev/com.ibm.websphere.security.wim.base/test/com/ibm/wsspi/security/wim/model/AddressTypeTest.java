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
package com.ibm.wsspi.security.wim.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Set;

import org.junit.Test;

public class AddressTypeTest {

    public static final AddressType TEST_ADDRESS_TYPE = new AddressType();

    static {
        /*
         * Set all fields on the AddressType.
         */
        TEST_ADDRESS_TYPE.setNickName("nickName");
        TEST_ADDRESS_TYPE.set("street", "street1");
        TEST_ADDRESS_TYPE.set("street", "street2");
        TEST_ADDRESS_TYPE.setCity("city");
        TEST_ADDRESS_TYPE.setStateOrProvinceName("stateOrProvinceName");
        TEST_ADDRESS_TYPE.setPostalCode("postalCode");
        TEST_ADDRESS_TYPE.setCountryName("countryName");
    }

    @Test
    public void testToString() {
        /*
         * Test an empty instance.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:AddressType " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new AddressType().toString());

        /*
         * Test a fully configured instance.
         */
        sb = new StringBuffer();
        sb.append("<wim:AddressType " + RootTest.WIM_XMLNS + ">\n");
        sb.append("    <wim:nickName>nickName</wim:nickName>\n");
        sb.append("    <wim:street>street1</wim:street>\n");
        sb.append("    <wim:street>street2</wim:street>\n");
        sb.append("    <wim:city>city</wim:city>\n");
        sb.append("    <wim:stateOrProvinceName>stateOrProvinceName</wim:stateOrProvinceName>\n");
        sb.append("    <wim:postalCode>postalCode</wim:postalCode>\n");
        sb.append("    <wim:countryName>countryName</wim:countryName>\n");
        sb.append("</wim:AddressType>");
        assertEquals(sb.toString(), TEST_ADDRESS_TYPE.toString());
    }

    @Test
    public void getSubTypes() {
        Set<String> types = AddressType.getSubTypes();
        assertNotNull(types);
        assertEquals(0, types.size());
    }

    @Test
    public void getSuperTypes() {
        List<String> types = new AddressType().getSuperTypes();
        assertNotNull(types);
        assertEquals(0, types.size());
    }
}