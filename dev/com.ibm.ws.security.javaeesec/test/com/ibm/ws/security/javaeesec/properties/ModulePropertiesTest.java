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
package com.ibm.ws.security.javaeesec.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

public class ModulePropertiesTest {

    /**
     *
     */
    @Test
    public void testGetAuthMechMapEmpty() {
        ModuleProperties mp = new ModuleProperties();
        assertTrue("AuthMechMap should be empty.", mp.getAuthMechMap().isEmpty());
    }

    /**
     *
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testGetAuthMechMapNotEmpty() {
        Map<Class, Properties> amm = new HashMap<Class, Properties>();
        Properties prop1 = new Properties();
        Properties prop2 = new Properties();
        amm.put(TestClass1.class, prop1);
        amm.put(TestClass2.class, prop2);
        ModuleProperties mp = new ModuleProperties(amm);
        assertEquals("AuthMechMap should be returned.", amm, mp.getAuthMechMap());
        assertEquals("AuthMechMap should contain two elements.", 2, mp.getAuthMechMap().size());
    }

    /**
     *
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testGetFromAuthMechMapNotEmpty() {
        Map<Class, Properties> amm = new HashMap<Class, Properties>();
        Properties prop1 = new Properties();
        Properties prop2 = new Properties();
        amm.put(TestClass1.class, prop1);
        amm.put(TestClass2.class, prop2);
        ModuleProperties mp = new ModuleProperties(amm);
        assertEquals("Properties1 should be returned.", prop1, mp.getFromAuthMechMap(TestClass1.class));
        assertEquals("Properties2 should be returned.", prop2, mp.getFromAuthMechMap(TestClass2.class));
        assertNull("null should be returned.", mp.getFromAuthMechMap(TestClass3.class));
    }

    /**
     *
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testPutToAuthMechMapNotEmpty() {
        Map<Class, Properties> amm = new HashMap<Class, Properties>();
        Properties prop1 = new Properties();
        ModuleProperties mp = new ModuleProperties(amm);
        assertNull("null should be returned.", mp.getFromAuthMechMap(TestClass1.class));
        mp.putToAuthMechMap(TestClass1.class, prop1);
        assertEquals("Properties should be returned.", prop1, mp.getFromAuthMechMap(TestClass1.class));
    }

    class TestClass1 {}

    class TestClass2 {}

    class TestClass3 {}
}
