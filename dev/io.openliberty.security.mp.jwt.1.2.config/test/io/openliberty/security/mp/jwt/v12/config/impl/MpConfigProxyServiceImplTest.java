/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
package io.openliberty.security.mp.jwt.v12.config.impl;

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

@SuppressWarnings("restriction")
public class MpConfigProxyServiceImplTest {
    protected final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("io.openliberty.security.mp.jwt*=all:com.ibm.ws.security.mp.jwt.*=all");

    private final ClassLoader cl = mockery.mock(ClassLoader.class);
    private final MpConfigProxy configNoClassLoader = mockery.mock(MpConfigProxy.class, "configNoClassLoader");
    private final MpConfigProxy configClassLoader = mockery.mock(MpConfigProxy.class, "configClassLoader");

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
    public void testGetConfigValuesNoClassLoader() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImplDouble();
        Class CLAZZ = String.class;

        mockery.checking(new Expectations() {
            {
                one(configNoClassLoader).getOptionalValue(MpConfigProperties.ISSUER, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.ISSUER)));
                one(configNoClassLoader).getOptionalValue(MpConfigProperties.PUBLIC_KEY, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.PUBLIC_KEY)));
                one(configNoClassLoader).getOptionalValue(MpConfigProperties.KEY_LOCATION, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.KEY_LOCATION)));
                one(configNoClassLoader).getOptionalValue(MpConfigProperties.PUBLIC_KEY_ALG, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.PUBLIC_KEY_ALG)));
                one(configNoClassLoader).getOptionalValue(MpConfigProperties.DECRYPT_KEY_LOCATION, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.DECRYPT_KEY_LOCATION)));
                one(configNoClassLoader).getOptionalValue(MpConfigProperties.VERIFY_AUDIENCES, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.VERIFY_AUDIENCES)));
                one(configNoClassLoader).getOptionalValue(MpConfigProperties.TOKEN_HEADER, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.TOKEN_HEADER)));
                one(configNoClassLoader).getOptionalValue(MpConfigProperties.TOKEN_COOKIE, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.TOKEN_COOKIE)));
            }
        });

        MpConfigProperties configProperties = mpConfigProxyServiceImpl.getConfigProperties(null);
        assertEquals("the list should be 8 items.", 8, configProperties.size());
    }

    @Test
    public void testGetConfigValuesClassLoader() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImplDouble();
        Class CLAZZ = String.class;

        mockery.checking(new Expectations() {
            {
                one(configClassLoader).getOptionalValue(MpConfigProperties.ISSUER, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.ISSUER)));
                one(configClassLoader).getOptionalValue(MpConfigProperties.PUBLIC_KEY, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.PUBLIC_KEY)));
                one(configClassLoader).getOptionalValue(MpConfigProperties.KEY_LOCATION, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.KEY_LOCATION)));
                one(configClassLoader).getOptionalValue(MpConfigProperties.PUBLIC_KEY_ALG, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.PUBLIC_KEY_ALG)));
                one(configClassLoader).getOptionalValue(MpConfigProperties.DECRYPT_KEY_LOCATION, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.DECRYPT_KEY_LOCATION)));
                one(configClassLoader).getOptionalValue(MpConfigProperties.VERIFY_AUDIENCES, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.VERIFY_AUDIENCES)));
                one(configClassLoader).getOptionalValue(MpConfigProperties.TOKEN_HEADER, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.TOKEN_HEADER)));
                one(configClassLoader).getOptionalValue(MpConfigProperties.TOKEN_COOKIE, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.TOKEN_COOKIE)));
            }
        });

        MpConfigProperties configProperties = mpConfigProxyServiceImpl.getConfigProperties(cl);
        assertEquals("the list should be 8 items.", 8, configProperties.size());
    }

    @Test
    public void testGetConfigValuesClassLoader_noProperties() {
        MpConfigProxyServiceImpl mpConfigProxyServiceImpl = new MpConfigProxyServiceImplDouble();
        Class CLAZZ = String.class;

        mockery.checking(new Expectations() {
            {
                one(configClassLoader).getOptionalValue(MpConfigProperties.ISSUER, CLAZZ);
                will(returnValue(null));
                one(configClassLoader).getOptionalValue(MpConfigProperties.PUBLIC_KEY, CLAZZ);
                will(returnValue(null));
                one(configClassLoader).getOptionalValue(MpConfigProperties.KEY_LOCATION, CLAZZ);
                will(returnValue(null));
                one(configClassLoader).getOptionalValue(MpConfigProperties.PUBLIC_KEY_ALG, CLAZZ);
                will(returnValue(null));
                one(configClassLoader).getOptionalValue(MpConfigProperties.DECRYPT_KEY_LOCATION, CLAZZ);
                will(returnValue(null));
                one(configClassLoader).getOptionalValue(MpConfigProperties.VERIFY_AUDIENCES, CLAZZ);
                will(returnValue(null));
                one(configClassLoader).getOptionalValue(MpConfigProperties.TOKEN_HEADER, CLAZZ);
                will(returnValue(null));
                one(configClassLoader).getOptionalValue(MpConfigProperties.TOKEN_COOKIE, CLAZZ);
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
                one(configClassLoader).getOptionalValue(MpConfigProperties.ISSUER, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.ISSUER + "         ")));
                one(configClassLoader).getOptionalValue(MpConfigProperties.PUBLIC_KEY, CLAZZ);
                will(returnValue(Optional.of("value_" + MpConfigProperties.PUBLIC_KEY + "\t\t\t\n")));
                one(configClassLoader).getOptionalValue(MpConfigProperties.KEY_LOCATION, CLAZZ);
                will(returnValue(Optional.of("     ")));
                one(configClassLoader).getOptionalValue(MpConfigProperties.PUBLIC_KEY_ALG, CLAZZ);
                will(returnValue(null));
                one(configClassLoader).getOptionalValue(MpConfigProperties.DECRYPT_KEY_LOCATION, CLAZZ);
                will(returnValue(null));
                one(configClassLoader).getOptionalValue(MpConfigProperties.VERIFY_AUDIENCES, CLAZZ);
                will(returnValue(null));
                one(configClassLoader).getOptionalValue(MpConfigProperties.TOKEN_HEADER, CLAZZ);
                will(returnValue(null));
                one(configClassLoader).getOptionalValue(MpConfigProperties.TOKEN_COOKIE, CLAZZ);
                will(returnValue(null));
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
                return configClassLoader;
            } else {
                return configNoClassLoader;
            }
        }
    }

}
