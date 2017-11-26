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
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ModulePropertiesProviderImplTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    @SuppressWarnings("rawtypes")
    private ModulePropertiesUtils mpu;
    private final Map<String, ModuleProperties> mm = new HashMap<String, ModuleProperties>();
    private ModulePropertiesProviderImpl mppi;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        mpu = mockery.mock(ModulePropertiesUtils.class);

        mppi = new ModulePropertiesProviderImpl(mm) {
            @SuppressWarnings("rawtypes")
            @Override
            protected ModulePropertiesUtils getModulePropertiesUtils() {
                return mpu;
            }
        };

    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    /**
     *
     */
    @Test
    public void testGetModulePropertiesValid() {
        final String MODULENAME = "ModuleName";
        withModuleName(MODULENAME);
        mm.clear();
        ModuleProperties mp = new ModuleProperties();
        mm.put(MODULENAME, mp);
        assertEquals("A module properties should be returned.", mp, mppi.getModuleProperties());
    }

    /**
     *
     */
    @Test
    public void testGetModulePropertiesNull() {
        final String MODULENAME = "ModuleName";
        final String DIFFERENTMODULENAME = "DifferentModuleName";
        withModuleName(MODULENAME);
        mm.clear();
        ModuleProperties mp = new ModuleProperties();
        mm.put(DIFFERENTMODULENAME, mp);
        assertNull("A null should be returned.", mppi.getModuleProperties());
    }

    /**
     *
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testGetAuthMechClassListValid() {
        final String MODULENAME = "ModuleName";
        withModuleName(MODULENAME);
        mm.clear();
        Map<Class, Properties> amm = new HashMap<Class, Properties>();
        Properties prop1 = new Properties();
        Properties prop2 = new Properties();
        amm.put(TestClass1.class, prop1);
        amm.put(TestClass2.class, prop2);
        ModuleProperties mp = new ModuleProperties(amm);
        mm.put(MODULENAME, mp);
        List<Class> output = mppi.getAuthMechClassList();
        assertEquals("Size should be 2", 2, output.size());
        assertTrue("TestClass1 should be contained", output.contains(TestClass1.class));
        assertTrue("TestClass2 should be contained", output.contains(TestClass2.class));
    }

    /**
     *
     */
    @Test
    public void testGetAuthMechClassListEmpty() {
        final String MODULENAME = "ModuleName";
        final String ANOTHERMODULENAME = "AnotherModuleName";
        withModuleName(MODULENAME);
        mm.clear();
        ModuleProperties mp = new ModuleProperties();
        mm.put(ANOTHERMODULENAME, mp);
        assertNull("null should be returend", mppi.getAuthMechClassList());
    }

    /**
     *
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testGetAuthMechProperties() {
        final String MODULENAME = "ModuleName";
        withModuleName(MODULENAME).withModuleName(MODULENAME);
        mm.clear();
        Map<Class, Properties> amm = new HashMap<Class, Properties>();
        Properties prop1 = new Properties();
        Properties prop2 = new Properties();
        amm.put(TestClass1.class, prop1);
        amm.put(TestClass2.class, prop2);
        ModuleProperties mp = new ModuleProperties(amm);
        mm.put(MODULENAME, mp);
        assertEquals("prop1 should be returned", prop1, mppi.getAuthMechProperties(TestClass1.class));
        assertNull("null should be returned", mppi.getAuthMechProperties(TestClass3.class));
    }

    /*************** support methods **************/
    private ModulePropertiesProviderImplTest withModuleName(final String name) {
        mockery.checking(new Expectations() {
            {
                one(mpu).getJ2EEModuleName();
                will(returnValue(name));
            }
        });
        return this;
    }

    class TestClass1 {}

    class TestClass2 {}

    class TestClass3 {}
}
