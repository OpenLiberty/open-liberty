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
package com.ibm.ws.security.mp.jwt.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Version;

import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.mp.jwt.osgi.MpJwtRuntimeVersion;
import test.common.SharedOutputManager;

public class MicroProfileJwtConfigImplTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("io.openliberty.security.mp.jwt*=all:com.ibm.ws.security.mp.jwt*=all");

    private final MpJwtRuntimeVersion runtimeVersion = mockery.mock(MpJwtRuntimeVersion.class);

    MicroProfileJwtConfigImpl config;

    class MockMicroProfileJwtConfigImpl extends MicroProfileJwtConfigImpl {
        @Override
        MpJwtRuntimeVersion getMpJwtRuntimeVersion() {
            return runtimeVersion;
        }
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        config = new MockMicroProfileJwtConfigImpl();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_isRuntimeVersionAtLeast_minimumEqualsRuntime() {
        final Version minimumVersionRequired = MpJwtRuntimeVersion.VERSION_1_1;
        final Version thisRuntimeVersion = MpJwtRuntimeVersion.VERSION_1_1;
        mockery.checking(new Expectations() {
            {
                one(runtimeVersion).getVersion();
                will(returnValue(thisRuntimeVersion));
            }
        });
        boolean result = config.isRuntimeVersionAtLeast(minimumVersionRequired);
        assertTrue("Runtime version [" + thisRuntimeVersion + "] should have been considered at or above [" + minimumVersionRequired + "].", result);
    }

    @Test
    public void test_isRuntimeVersionAtLeast_minimumLessThanRuntime() {
        final Version minimumVersionRequired = MpJwtRuntimeVersion.VERSION_1_0;
        final Version thisRuntimeVersion = MpJwtRuntimeVersion.VERSION_1_2;
        mockery.checking(new Expectations() {
            {
                one(runtimeVersion).getVersion();
                will(returnValue(thisRuntimeVersion));
            }
        });
        boolean result = config.isRuntimeVersionAtLeast(minimumVersionRequired);
        assertTrue("Runtime version [" + thisRuntimeVersion + "] should have been considered at or above [" + minimumVersionRequired + "].", result);
    }

    @Test
    public void test_isRuntimeVersionAtLeast_minimumGreaterThanRuntime() {
        final Version minimumVersionRequired = MpJwtRuntimeVersion.VERSION_1_2;
        final Version thisRuntimeVersion = MpJwtRuntimeVersion.VERSION_1_1;
        mockery.checking(new Expectations() {
            {
                one(runtimeVersion).getVersion();
                will(returnValue(thisRuntimeVersion));
            }
        });
        boolean result = config.isRuntimeVersionAtLeast(minimumVersionRequired);
        assertFalse("Runtime version [" + thisRuntimeVersion + "] should NOT have been considered at or above [" + minimumVersionRequired + "].", result);
    }

}
