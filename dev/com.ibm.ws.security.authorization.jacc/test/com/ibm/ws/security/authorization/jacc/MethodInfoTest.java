/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

/**
 *
 */
public class MethodInfoTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule outputRule = outputMgr;

    /**
     * Tests getMethodName
     * Expected result: get the expected string.
     */
    @Test
    public void getMethodNameNormal() {
        String mn = "methodName";
        String min = "methodInterfaceName";
        List<String> pl = null;
        MethodInfo mi = new MethodInfo(mn, min, pl);
        assertEquals(mn, mi.getMethodName());
    }

    /**
     * Tests getMethodInterfaceName
     * Expected result: get the expected string.
     */
    @Test
    public void getMethodInterfaceNameNormal() {
        String mn = "methodName";
        String min = "methodInterfaceName";
        List<String> pl = null;
        MethodInfo mi = new MethodInfo(mn, min, pl);
        assertEquals(min, mi.getMethodInterfaceName());
    }

    /**
     * Tests getParamList
     * Expected result: get the null object.
     */
    @Test
    public void getParamListNull() {
        String mn = "methodName";
        String min = "methodInterfaceName";
        List<String> pl = null;
        MethodInfo mi = new MethodInfo(mn, min, pl);
        assertNull(mi.getParamList());
    }

    /**
     * Tests getParamList
     * Expected result: get the expected object.
     */
    @Test
    public void getParamListNormal() {
        String mn = "methodName";
        String min = "methodInterfaceName";
        List<String> pl = new ArrayList<String>();
        pl.add("com.ibm.class1");
        pl.add("com.ibm.class2");
        MethodInfo mi = new MethodInfo(mn, min, pl);
        assertEquals(pl, mi.getParamList());
    }

    /**
     * Tests toString
     * Expected result: get the expected result
     */
    @Test
    public void toStringNormalParamList() {
        String mn = "methodName";
        String min = "methodInterfaceName";
        List<String> pl = new ArrayList<String>();
        String i1 = "class1";
        String i2 = "class2";
        pl.add(i1);
        pl.add(i2);
        String output = "method : " + mn + " interface : " + min + " parameters : " + i1 + ", " + i2 + ", ";

        MethodInfo mi = new MethodInfo(mn, min, pl);
        assertEquals(output, mi.toString());
    }

    /**
     * Tests toString
     * Expected result: get the expected result
     */
    @Test
    public void toStringNullParamList() {
        String mn = "methodName";
        String min = "methodInterfaceName";
        List<String> pl = null;
        String output = "method : " + mn + " interface : " + min + " parameters : null";
        MethodInfo mi = new MethodInfo(mn, min, pl);
        assertEquals(output, mi.toString());
    }

}
