/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.v11.config.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

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
import com.ibm.ws.security.mp.jwt.MpConfigProxyService.MpConfigProxy;

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
    private final MpConfigProxy configNoCL = mockery.mock(MpConfigProxy.class, "configNoCL");
    private final MpConfigProxy configCL = mockery.mock(MpConfigProxy.class, "configCL");

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

    @Test
    public void testGetConfigValuesNoClassLoader() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImplDouble();
        Class CLAZZ = String.class;

        mockery.checking(new Expectations() {
            {
                one(configNoCL).getOptionalValue(MpConfigProperties.ISSUER, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.ISSUER)));
                one(configNoCL).getOptionalValue(MpConfigProperties.PUBLIC_KEY, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.PUBLIC_KEY)));
                one(configNoCL).getOptionalValue(MpConfigProperties.KEY_LOCATION, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.KEY_LOCATION)));
            }
        });

        MpConfigProperties configProperties = mpConfigProxyServiceImpl.getConfigProperties(null);
        assertEquals("the list should be 3 items.", 3, configProperties.size());
    }

    @Test
    public void testGetConfigValuesClassLoader() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImplDouble();
        Class CLAZZ = String.class;

        mockery.checking(new Expectations() {
            {
                one(configCL).getOptionalValue(MpConfigProperties.ISSUER, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.ISSUER)));
                one(configCL).getOptionalValue(MpConfigProperties.PUBLIC_KEY, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.PUBLIC_KEY)));
                one(configCL).getOptionalValue(MpConfigProperties.KEY_LOCATION, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.KEY_LOCATION)));
            }
        });

        MpConfigProperties configProperties = mpConfigProxyServiceImpl.getConfigProperties(cl);
        assertEquals("the list should be 3 items.", 3, configProperties.size());
    }

    @Test
    public void testGetConfigValuesClassLoader_noProperties() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImplDouble();
        Class CLAZZ = String.class;

        mockery.checking(new Expectations() {
            {
                one(configCL).getOptionalValue(MpConfigProperties.ISSUER, CLAZZ);
                will(returnValue(null));
                one(configCL).getOptionalValue(MpConfigProperties.PUBLIC_KEY, CLAZZ);
                will(returnValue(null));
                one(configCL).getOptionalValue(MpConfigProperties.KEY_LOCATION, CLAZZ);
                will(returnValue(null));
            }
        });

        MpConfigProperties configProperties = mpConfigProxyServiceImpl.getConfigProperties(cl);
        assertEquals("the list should be 0 items.", 0, configProperties.size());
    }

    @Test
    public void testGetConfigValuesClassLoader_trim() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImplDouble();
        Class CLAZZ = String.class;

        mockery.checking(new Expectations() {
            {
                one(configCL).getOptionalValue(MpConfigProperties.ISSUER, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.ISSUER + "         ")));
                one(configCL).getOptionalValue(MpConfigProperties.PUBLIC_KEY, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.PUBLIC_KEY + "\t\t\t\n")));
                one(configCL).getOptionalValue(MpConfigProperties.KEY_LOCATION, CLAZZ);
                will(returnValue(Optional.of("     ")));
            }
        });

        MpConfigProperties configProperties = mpConfigProxyServiceImpl.getConfigProperties(cl);
        assertEquals("the list should be 2 items.", 2, configProperties.size());
        assertTrue("the map should contain value_" + MpConfigProperties.ISSUER, configProperties.get(MpConfigProperties.ISSUER).equals("value_" + MpConfigProperties.ISSUER));
        assertTrue("the map should contain value_" + MpConfigProperties.PUBLIC_KEY, configProperties.get(MpConfigProperties.PUBLIC_KEY).equals("value_" + MpConfigProperties.PUBLIC_KEY));

    }

    class MpConfigProxyServiceImplDouble extends MpConfigProxyServiceImpl {
        @Override
        public MpConfigProxy getConfigProxy(ClassLoader cl) {
            if (cl != null) {
                return configCL;
            } else {
                return configNoCL;
            }
        }
    }

}
