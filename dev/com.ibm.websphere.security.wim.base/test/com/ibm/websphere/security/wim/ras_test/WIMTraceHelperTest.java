/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.wim.ras_test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

public class WIMTraceHelperTest {

    @Test
    public void testAllBeans() {
        Customer cs = new Customer();
        cs.setFirtName("John");
        cs.setLastName("Doe");
        cs.setAddress("123 Broadway");
        cs.setPinCode(101010);

        PhoneInfo pi = new PhoneInfo();
        pi.setLandLineNo("1234567890");
        pi.setMobileNo("0987654321");
        pi.setStdCode("777");

        Geography geo = new Geography();
        geo.setCity("Bangalore");
        geo.setCountry("India");
        geo.setState("Karnataka");

        cs.setPi(pi);
        cs.setGeo(geo);

        try {
            String trace = WIMTraceHelper.trace(cs);
            assertTrue("address=123 Broadway - not Found", trace.contains("address=123 Broadway"));
            assertTrue("firtName=John - not Found", trace.contains("firtName=John"));
            assertTrue("city=Bangalore -  not Found", trace.contains("city=Bangalore"));
            assertTrue("country=India - not Found", trace.contains("country=India"));
            assertTrue("state=Karnataka - not Found", trace.contains("state=Karnataka"));
            assertTrue("lastName=Doe - not Found", trace.contains("lastName=Doe"));
            assertTrue("landLineNo=1234567890 - not Found", trace.contains("landLineNo=1234567890"));
            assertTrue("mobileNo=0987654321 - not Found", trace.contains("mobileNo=0987654321"));
            assertTrue("stdCode=777 - not Found", trace.contains("stdCode=777"));
            assertTrue("mobileNo=0987654321 - not Found", trace.contains("mobileNo=0987654321"));
            assertTrue("pinCode=101010 - not Found", trace.contains("pinCode=101010"));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            assertEquals("Call completed successfully", true, false + " with " + errorMessage);
        }
    }

    @Test
    public void testPrintPrimitiveObjects() {
        assertEquals("[-2, 0, 2]", WIMTraceHelper.printPrimitiveArray(new short[] { -2, 0, 2 }));
        assertEquals("[false, true, false]", WIMTraceHelper.printPrimitiveArray(new boolean[] { false, true, false }));
        assertEquals("[-3.14, 0.12345, 1337.0]", WIMTraceHelper.printPrimitiveArray(new double[] { -3.14, 0.12345, 1337.0 }));
        assertEquals("[-2, 0, 2]", WIMTraceHelper.printPrimitiveArray(new byte[] { -2, 0, 2 }));
        assertEquals("[Z, &, 6]", WIMTraceHelper.printPrimitiveArray(new char[] { 'Z', '&', '6' }));
        assertEquals("[-2, 0, 2]", WIMTraceHelper.printPrimitiveArray(new int[] { -2, 0, 2 }));
        assertEquals("[-2, 0, 2]", WIMTraceHelper.printPrimitiveArray(new long[] { -2L, 0, 2L }));
        assertEquals("[-2.0E10, 0.0, 2.0E10]", WIMTraceHelper.printPrimitiveArray(new float[] { -2e10f, 0, 2e10f }));
    }
}
