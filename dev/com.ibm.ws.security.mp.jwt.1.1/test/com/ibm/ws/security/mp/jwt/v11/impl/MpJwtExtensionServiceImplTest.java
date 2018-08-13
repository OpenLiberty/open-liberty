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
package com.ibm.ws.security.mp.jwt.v11.impl;

import com.ibm.ws.security.mp.jwt.MpJwtExtensionService;
import com.ibm.ws.security.mp.jwt.v11.MpConfigProxyService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.microprofile.config.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

/**
 *
 */
public class MpJwtExtensionServiceImplTest {
    protected final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.mp.jwt.*=all");

    @SuppressWarnings("unchecked")
    private final ComponentContext cc = mockery.mock(ComponentContext.class);
    private final ServiceReference<MpConfigProxyService> mpConfigProxyServiceRef = mockery.mock(ServiceReference.class, "mpConfigProxyServiceRef");
    private final MpConfigProxyService mpConfigProxyService = mockery.mock(MpConfigProxyService.class, "mpConfigProxyService");
    private final AtomicServiceReference<MpJwtExtensionService> mpJwtExtensionServiceRef = mockery.mock(AtomicServiceReference.class, "mpJwtExtensionServiceRef");
    private final ClassLoader cl = mockery.mock(ClassLoader.class);

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
        MpJwtExtensionServiceImpl mpJwtExtensionServiceImpl = new MpJwtExtensionServiceImpl();
        mpJwtExtensionServiceImpl.setMpConfigProxyService(mpConfigProxyServiceRef);
        mpJwtExtensionServiceImpl.activate(cc);
        assertTrue("CWWKS5750I message was not logged", outputMgr.checkForMessages("CWWKS5750I:"));
        mpJwtExtensionServiceImpl.deactivate(cc);
        mpJwtExtensionServiceImpl.unsetMpConfigProxyService(mpConfigProxyServiceRef);
    }

    /**
     * Tests modified method logs a message.
     */
    @Test
    public void testModified() {
        MpJwtExtensionServiceImpl mpJwtExtensionServiceImpl = new MpJwtExtensionServiceImpl();
        mpJwtExtensionServiceImpl.setMpConfigProxyService(mpConfigProxyServiceRef);
        mpJwtExtensionServiceImpl.modified(cc);
        assertTrue("CWWKS5751I message was not logged", outputMgr.checkForMessages("CWWKS5751I:"));
        mpJwtExtensionServiceImpl.deactivate(cc);
        mpJwtExtensionServiceImpl.unsetMpConfigProxyService(mpConfigProxyServiceRef);
    }


    /**
     * Tests deactivate method logs a message.
     */
    @Test
    public void testDeactivate() {
        MpJwtExtensionServiceImpl mpJwtExtensionServiceImpl = new MpJwtExtensionServiceImpl();
        mpJwtExtensionServiceImpl.setMpConfigProxyService(mpConfigProxyServiceRef);
        mpJwtExtensionServiceImpl.deactivate(cc);
        assertTrue("CWWKS5752I message was not logged", outputMgr.checkForMessages("CWWKS5752I:"));
        mpJwtExtensionServiceImpl.unsetMpConfigProxyService(mpConfigProxyServiceRef);
    }

    /**
     * Tests getVersion method
     */
    @Test
    public void testGetVersion() {
        MpJwtExtensionServiceImpl mpJwtExtensionServiceImpl = new MpJwtExtensionServiceImpl();
        String output = mpJwtExtensionServiceImpl.getVersion();
        assertTrue("1.1 should be returned", "1.1".equals(output));
    }

    /**
     * Tests isMpConfigAvailable method
     */
    @Test
    public void testIsMpConfigAvailableTrue() {
        mockery.checking(new Expectations() {
            {
                one(cc).locateService("mpConfigProxyService", mpConfigProxyServiceRef);
                will(returnValue(mpConfigProxyService));
            }
        });
        MpJwtExtensionServiceImpl mpJwtExtensionServiceImpl = new MpJwtExtensionServiceImpl();
        mpJwtExtensionServiceImpl.setMpConfigProxyService(mpConfigProxyServiceRef);
        mpJwtExtensionServiceImpl.activate(cc);
        boolean output = mpJwtExtensionServiceImpl.isMpConfigAvailable();
        assertTrue("true should be returned", output);
        mpJwtExtensionServiceImpl.deactivate(cc);
        mpJwtExtensionServiceImpl.unsetMpConfigProxyService(mpConfigProxyServiceRef);
    }

    /**
     * Tests isMpConfigAvailable method
     */
    @Test
    public void testIsMpConfigAvailableFalse() {
        mockery.checking(new Expectations() {
            {
                one(cc).locateService("mpConfigProxyService", mpConfigProxyServiceRef);
                will(returnValue(null));
            }
        });
        MpJwtExtensionServiceImpl mpJwtExtensionServiceImpl = new MpJwtExtensionServiceImpl();
        mpJwtExtensionServiceImpl.setMpConfigProxyService(mpConfigProxyServiceRef);
        mpJwtExtensionServiceImpl.activate(cc);
        boolean output = mpJwtExtensionServiceImpl.isMpConfigAvailable();
        mpJwtExtensionServiceImpl.deactivate(cc);
        mpJwtExtensionServiceImpl.unsetMpConfigProxyService(mpConfigProxyServiceRef);
        assertFalse("false should be returned", output);
    }

    /**
     * Tests getConfigValue method
     */
    @Test
    public void testGetConfigValuePropertyExists() {
        String NAME = "name";
        String VALUE = "value";
        Class TYPE = String.class;
        mockery.checking(new Expectations() {
            {
                one(cc).locateService("mpConfigProxyService", mpConfigProxyServiceRef);
                will(returnValue(mpConfigProxyService));
                one(mpConfigProxyService).getConfigValue(cl, NAME, TYPE);
                will(returnValue(VALUE));
            }
        });
        MpJwtExtensionServiceImpl mpJwtExtensionServiceImpl = new MpJwtExtensionServiceImpl();
        mpJwtExtensionServiceImpl.setMpConfigProxyService(mpConfigProxyServiceRef);
        mpJwtExtensionServiceImpl.activate(cc);
        String output = (String)mpJwtExtensionServiceImpl.getConfigValue(cl, NAME, TYPE);
        mpJwtExtensionServiceImpl.deactivate(cc);
        mpJwtExtensionServiceImpl.unsetMpConfigProxyService(mpConfigProxyServiceRef);
        assertEquals("The expected value should be returned", VALUE, output);
    }

    /**
     * Tests getConfigValue method
     */
    @Test
    public void testGetConfigValueNoProperty() {
        String NAME = "name";
        String VALUE = "value";
        Class TYPE = String.class;
        mockery.checking(new Expectations() {
            {
                one(cc).locateService("mpConfigProxyService", mpConfigProxyServiceRef);
                will(returnValue(mpConfigProxyService));
                one(mpConfigProxyService).getConfigValue(cl, NAME, TYPE);
                will(throwException(new NoSuchElementException()));
            }
        });
        MpJwtExtensionServiceImpl mpJwtExtensionServiceImpl = new MpJwtExtensionServiceImpl();
        mpJwtExtensionServiceImpl.setMpConfigProxyService(mpConfigProxyServiceRef);
        mpJwtExtensionServiceImpl.activate(cc);
        try {
            mpJwtExtensionServiceImpl.getConfigValue(cl, NAME, TYPE);
            fail("NoSuchElementException should be caught.");
        } catch (NoSuchElementException e) {
        }
        mpJwtExtensionServiceImpl.deactivate(cc);
        mpJwtExtensionServiceImpl.unsetMpConfigProxyService(mpConfigProxyServiceRef);
    }

    /**
     * Tests getConfigValue method
     */
    @Test
    public void testGetConfigValueNoConfigProxy() {
        String NAME = "name";
        String VALUE = "value";
        Class TYPE = String.class;
        mockery.checking(new Expectations() {
            {
                allowing(cc).locateService("mpConfigProxyService", mpConfigProxyServiceRef);
                will(returnValue(null));
                never(mpConfigProxyService).getConfigValue(cl, NAME, TYPE);
            }
        });
        MpJwtExtensionServiceImpl mpJwtExtensionServiceImpl = new MpJwtExtensionServiceImpl();
        mpJwtExtensionServiceImpl.setMpConfigProxyService(mpConfigProxyServiceRef);
        mpJwtExtensionServiceImpl.activate(cc);
        try {
            mpJwtExtensionServiceImpl.getConfigValue(cl, NAME, TYPE);
            fail("IllegalStateException should be caught.");
        } catch (IllegalStateException e) {
        }
        mpJwtExtensionServiceImpl.deactivate(cc);
        mpJwtExtensionServiceImpl.unsetMpConfigProxyService(mpConfigProxyServiceRef);
    }

}
