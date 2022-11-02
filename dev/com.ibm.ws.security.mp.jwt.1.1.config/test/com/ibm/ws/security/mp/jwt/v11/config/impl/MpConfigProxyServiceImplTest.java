/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.v11.config.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.microprofile.config.Config;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.security.jwt.config.MpConfigProperties;

import test.common.SharedOutputManager;

/**
 *
 */
public class MpConfigProxyServiceImplTest {
    protected final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.mp.jwt.*=all");

    @SuppressWarnings("unchecked")
    private final ClassLoader cl = mockery.mock(ClassLoader.class);
    private final Config configNoCL = mockery.mock(Config.class, "configNoCL");
    private final Config configCL = mockery.mock(Config.class, "configCL");

    @Rule
    public final TestName testName = new TestName();

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void beforeTest() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    /**
     * Tests activate method logs a message.
     */
    @Test
    public void testActivate() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImpl();
        mpConfigProxyServiceImpl.activate(null, null);
        assertTrue("CWWKS5775I message was not logged", outputMgr.checkForMessages("CWWKS5775I:"));
    }

    /**
     * Tests modified method logs a message.
     */
    @Test
    public void testModified() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImpl();
        mpConfigProxyServiceImpl.modified(null, null);
        assertTrue("CWWKS5776I message was not logged", outputMgr.checkForMessages("CWWKS5776I:"));
    }

    /**
     * Tests deactivate method logs a message.
     */
    @Test
    public void testDeactivate() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImpl();
        mpConfigProxyServiceImpl.deactivate(null);
        assertTrue("CWWKS5777I message was not logged", outputMgr.checkForMessages("CWWKS5777I:"));
    }

    /**
     * Tests getVersion method
     */
    @Test
    public void testGetVersion() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImpl();
        String output = mpConfigProxyServiceImpl.getVersion();
        assertTrue("1.1 should be returned", "1.1".equals(output));
    }

    /**
     * Tests isMpConfigAvailable method
     */
    @Test
    public void testIsMpConfigAvailable() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImpl();
        boolean output = mpConfigProxyServiceImpl.isMpConfigAvailable();
        assertTrue("true should be returned", output);
    }

    /**
     * Tests getConfigValue method
     */
    @Test
    public void testGetConfigValueNoCL() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImplDouble();
        String NAME = "name";
        Class CLAZZ = Object.class;

        String output = (String) mpConfigProxyServiceImpl.getConfigValue(null, NAME, CLAZZ);
        assertNull("Expected the result to be null but was [" + output + "].", output);
    }

    @Test
    public void testGetConfigValueNoCL_supportedMpJwtConfigProperty() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImplDouble();
        String NAME = MpConfigProperties.PUBLIC_KEY;
        Class CLAZZ = Object.class;
        String VALUE = "value";

        mockery.checking(new Expectations() {
            {
                never(configCL).getValue(NAME, CLAZZ);
                one(configNoCL).getOptionalValue(NAME, CLAZZ);
                will(returnValue(Optional.of(VALUE)));
            }
        });

        String output = (String) mpConfigProxyServiceImpl.getConfigValue(null, NAME, CLAZZ);
        assertEquals("the expected value should be returned", VALUE, output);
    }

    /**
     * Tests getConfigValue method
     */
    @Test
    public void testGetConfigValueCL_unknownMpJwtConfigProperty() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImplDouble();
        String NAME = "name";
        Class CLAZZ = Object.class;

        String output = (String) mpConfigProxyServiceImpl.getConfigValue(cl, NAME, CLAZZ);
        assertNull("Expected the result to be null but was [" + output + "].", output);
    }

    @Test
    public void testGetConfigValueCL_supportedMpJwtConfigProperty() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImplDouble();
        String NAME = MpConfigProperties.ISSUER;
        Class CLAZZ = Object.class;
        String VALUE = "value";

        mockery.checking(new Expectations() {
            {
                never(configNoCL).getValue(NAME, CLAZZ);
                one(configCL).getOptionalValue(NAME, CLAZZ);
                will(returnValue(Optional.of(VALUE)));
            }
        });

        String output = (String) mpConfigProxyServiceImpl.getConfigValue(cl, NAME, CLAZZ);
        assertEquals("the expected value should be returned", VALUE, output);
    }

    @Test
    public void testGetConfigValuesNoClassLoader() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImplDouble();
        Set<String> properties = new TreeSet<>();
        properties.add(MpConfigProperties.PUBLIC_KEY);
        properties.add(MpConfigProperties.ISSUER);
        Class CLAZZ = String.class;
        String VALUE = "value";

        mockery.checking(new Expectations() {
            {
                one(configNoCL).getOptionalValue(MpConfigProperties.PUBLIC_KEY, CLAZZ);
                will(returnValue(Optional.of(VALUE)));
                one(configNoCL).getOptionalValue(MpConfigProperties.ISSUER, CLAZZ);
                will(returnValue(Optional.of(VALUE)));
            }
        });

        List<String> output = mpConfigProxyServiceImpl.getConfigValues(null, properties, CLAZZ);
        assertEquals("the list should be 2 items.", 2, output.size());
    }

    @Test
    public void testGetConfigValuesClassLoader() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImplDouble();
        Set<String> properties = new TreeSet<>();
        properties.add(MpConfigProperties.PUBLIC_KEY);
        properties.add(MpConfigProperties.ISSUER);
        Class CLAZZ = String.class;
        String VALUE = "value";

        mockery.checking(new Expectations() {
            {
                one(configCL).getOptionalValue(MpConfigProperties.PUBLIC_KEY, CLAZZ);
                will(returnValue(Optional.of(VALUE)));
                one(configCL).getOptionalValue(MpConfigProperties.ISSUER, CLAZZ);
                will(returnValue(Optional.of(VALUE)));
            }
        });

        List<String> output = mpConfigProxyServiceImpl.getConfigValues(cl, properties, CLAZZ);
        assertEquals("the list should be 2 items.", 2, output.size());
    }

    @Test
    public void testGetConfigValuesClassLoader_unknownMpJwtConfigProperty() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImplDouble();
        Set<String> properties = new TreeSet<>();
        properties.add(MpConfigProperties.PUBLIC_KEY);
        properties.add(MpConfigProperties.ISSUER);
        properties.add("Unknown");

        Class CLAZZ = String.class;
        String VALUE = "value";

        mockery.checking(new Expectations() {
            {
                one(configCL).getOptionalValue(MpConfigProperties.PUBLIC_KEY, CLAZZ);
                will(returnValue(Optional.of(VALUE)));
                one(configCL).getOptionalValue(MpConfigProperties.ISSUER, CLAZZ);
                will(returnValue(Optional.of(VALUE)));
                never(configCL).getOptionalValue("Unknown", CLAZZ);
            }
        });

        List<String> output = mpConfigProxyServiceImpl.getConfigValues(cl, properties, CLAZZ);
        assertEquals("the list should be 3 items.", 3, output.size());
    }

    class MpConfigProxyServiceImplDouble extends MpConfigProxyServiceImpl {
        @Override
        protected Config getConfig(ClassLoader cl) {
            if (cl != null) {
                return configCL;
            } else {
                return configNoCL;
            }
        }
    }

}
