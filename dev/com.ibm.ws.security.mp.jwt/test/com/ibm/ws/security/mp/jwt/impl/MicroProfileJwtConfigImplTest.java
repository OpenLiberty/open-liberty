/*******************************************************************************
 * Copyright (c) 2020, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.mp.jwt.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;

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

    private Map<String, Object> buildMinimalProps() {
        Map<String, Object> props = new HashMap<String, Object>();
        // Provide the mpConfigProxy cardinality minimum that initProps expects
        props.put("MpConfigProxy.cardinality.minimum", "0");
        return props;
    }

    @Test
    public void test_realmIdentifier_defaultValue() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(runtimeVersion).getVersion();
                will(returnValue(io.openliberty.security.mp.jwt.osgi.MpJwtRuntimeVersion.VERSION_1_1));
            }
        });
        Map<String, Object> props = buildMinimalProps();
        // Simulate OSGi metatype default injection
        props.put(MicroProfileJwtConfigImpl.KEY_realmIdentifier, "realm");
        config.initProps(null, props);
        assertEquals("Expected default realmIdentifier to be 'realm'",
                     "realm", config.getRealmIdentifier());
    }

    @Test
    public void test_realmIdentifier_customValue() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(runtimeVersion).getVersion();
                will(returnValue(io.openliberty.security.mp.jwt.osgi.MpJwtRuntimeVersion.VERSION_1_1));
            }
        });
        Map<String, Object> props = buildMinimalProps();
        props.put(MicroProfileJwtConfigImpl.KEY_realmIdentifier, "tenant");
        config.initProps(null, props);
        assertEquals("Expected realmIdentifier to be 'tenant'",
                     "tenant", config.getRealmIdentifier());
    }

    @Test
    public void test_realmName_notSet() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(runtimeVersion).getVersion();
                will(returnValue(io.openliberty.security.mp.jwt.osgi.MpJwtRuntimeVersion.VERSION_1_1));
            }
        });
        Map<String, Object> props = buildMinimalProps();
        // realmName intentionally absent — no default in metatype
        config.initProps(null, props);
        assertNull("Expected realmName to be null when not configured",
                   config.getRealmName());
    }

    @Test
    public void test_realmName_setValue() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(runtimeVersion).getVersion();
                will(returnValue(io.openliberty.security.mp.jwt.osgi.MpJwtRuntimeVersion.VERSION_1_1));
            }
        });
        Map<String, Object> props = buildMinimalProps();
        props.put(MicroProfileJwtConfigImpl.KEY_realmName, "CorporateRealm");
        config.initProps(null, props);
        assertEquals("Expected realmName to be 'CorporateRealm'",
                     "CorporateRealm", config.getRealmName());
    }

}
