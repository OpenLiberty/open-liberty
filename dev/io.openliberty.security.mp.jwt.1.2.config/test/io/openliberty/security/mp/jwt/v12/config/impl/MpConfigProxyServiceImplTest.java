/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.mp.jwt.v12.config.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

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

import com.ibm.ws.security.mp.jwt.config.MpConstants;

import test.common.SharedOutputManager;

@SuppressWarnings("restriction")
public class MpConfigProxyServiceImplTest {
    protected final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("io.openliberty.security.mp.jwt*=all:com.ibm.ws.security.mp.jwt.*=all");

    private final ClassLoader cl = mockery.mock(ClassLoader.class);
    private final Config configNoClassLoader = mockery.mock(Config.class, "configNoClassLoader");
    private final Config configClassLoader = mockery.mock(Config.class, "configClassLoader");

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Before
    public void beforeTest() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @Test
    public void testActivate() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImpl();
        mpConfigProxyServiceImpl.activate(null, null);
        assertTrue("CWWKS5780I message was not logged.", outputMgr.checkForMessages("CWWKS5780I:"));
    }

    @Test
    public void testModified() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImpl();
        mpConfigProxyServiceImpl.modified(null, null);
        assertTrue("CWWKS5781I message was not logged.", outputMgr.checkForMessages("CWWKS5781I:"));
    }

    @Test
    public void testDeactivate() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImpl();
        mpConfigProxyServiceImpl.deactivate(null);
        assertTrue("CWWKS5782I message was not logged.", outputMgr.checkForMessages("CWWKS5782I:"));
    }

    @Test
    public void testGetVersion() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImpl();
        String output = mpConfigProxyServiceImpl.getVersion();
        assertEquals("MP version did not match the expected value.", "1.2", output);
    }

    @Test
    public void testIsMpConfigAvailable() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImpl();
        boolean output = mpConfigProxyServiceImpl.isMpConfigAvailable();
        assertTrue("MP config should have been considered available.", output);
    }

    @Test
    public void testGetConfigValueNoClassLoader() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImplDouble();
        String NAME = "name";
        Class CLAZZ = Object.class;

        String output = (String) mpConfigProxyServiceImpl.getConfigValue(null, NAME, CLAZZ);
        assertNull("Expected the result to be null but was [" + output + "].", output);
    }

    @Test
    public void testGetConfigValueNoClassLoader_supportedMpJwtConfigProperty() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImplDouble();
        String NAME = MpConstants.PUBLIC_KEY;
        Class CLAZZ = Object.class;
        String VALUE = "value";

        mockery.checking(new Expectations() {
            {
                never(configClassLoader).getValue(NAME, CLAZZ);
                one(configNoClassLoader).getOptionalValue(NAME, CLAZZ);
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
    public void testGetConfigValueClassLoader_unknownMpJwtConfigProperty() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImplDouble();
        String NAME = "name";
        Class CLAZZ = Object.class;

        String output = (String) mpConfigProxyServiceImpl.getConfigValue(cl, NAME, CLAZZ);
        assertNull("Expected the result to be null but was [" + output + "].", output);
    }

    @Test
    public void testGetConfigValueClassLoader_supportedMpJwtConfigProperty() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImplDouble();
        String NAME = MpConstants.ISSUER;
        Class CLAZZ = Object.class;
        String VALUE = "value";

        mockery.checking(new Expectations() {
            {
                never(configNoClassLoader).getValue(NAME, CLAZZ);
                one(configClassLoader).getOptionalValue(NAME, CLAZZ);
                will(returnValue(Optional.of(VALUE)));
            }
        });

        String output = (String) mpConfigProxyServiceImpl.getConfigValue(cl, NAME, CLAZZ);
        assertEquals("the expected value should be returned", VALUE, output);
    }

    class MpConfigProxyServiceImplDouble extends MpConfigProxyServiceImpl {
        @Override
        protected Config getConfig(ClassLoader cl) {
            if (cl != null) {
                return configClassLoader;
            } else {
                return configNoClassLoader;
            }
        }
    }

}
