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
package com.ibm.ws.security.mp.jwt.v11.config.impl;

import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.microprofile.config.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import org.osgi.framework.ServiceReference;

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
        String VALUE = "value";

        mockery.checking(new Expectations() {
            {
                one(configNoCL).getValue(NAME, CLAZZ);
                will(returnValue(VALUE));
                never(configCL).getValue(NAME, CLAZZ);
            }
        });

        String output = (String)mpConfigProxyServiceImpl.getConfigValue(null, NAME, CLAZZ);
        assertEquals("the expected value should be returned", VALUE, output);
    }

    /**
     * Tests getConfigValue method
     */
    @Test
    public void testGetConfigValueCL() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImplDouble();
        String NAME = "name";
        Class CLAZZ = Object.class;
        String VALUE = "value";

        mockery.checking(new Expectations() {
            {
                never(configNoCL).getValue(NAME, CLAZZ);
                one(configCL).getValue(NAME, CLAZZ);
                will(returnValue(VALUE));
            }
        });

        String output = (String)mpConfigProxyServiceImpl.getConfigValue(cl, NAME, CLAZZ);
        assertEquals("the expected value should be returned", VALUE, output);
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
