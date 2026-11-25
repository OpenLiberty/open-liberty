/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.wlm.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.ws.zos.wlm.WLMConfigManager;

/**
 * Test the UnauthorizedWLMNativeServices class
 */
@RunWith(JMock.class)
public class UnauthorizedWLMNativeServicesTest {

    /**
     * Mock environment for NativeMethodManager and native methods.
     */
    private static Mockery mockery = null;

    private static NativeMethodManager mockNmm = null;
    /**
     * Counter for generating unique mock object names.
     */
    private static int uniqueMockNameCount = 1;

    /**
     * Mock WLMConfigManager used by WLMNativeServices.
     */
    protected static WLMConfigManager wlmcmmock = null;

    /**
     * Mock WLMNativeServices
     */
    protected static UnauthorizedWLMNativeServices mock = null;
    protected static UnauthorizedWLMNativeServices unauthWLMNativeServices = null;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        unauthWLMNativeServices = createWLMNativeServices();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * Create the mock object(s). Each mock object created by this test
     * needs to have a unique name. Just use a simple counter to
     * ensure uniqueness.
     */
    public static void createMockEnv() {
        mockery = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        mock = mockery.mock(UnauthorizedWLMNativeServices.class, "WLMNativeServices" + uniqueMockNameCount);
        mockNmm = mockery.mock(NativeMethodManager.class, "NativeMethodManager" + uniqueMockNameCount);
        wlmcmmock = mockery.mock(WLMConfigManager.class, "WLMConfigManager" + uniqueMockNameCount);
    }

    /**
     * Create the UnauthorizedWLMNativeServicesMockNative object for the unit test.
     */
    protected static UnauthorizedWLMNativeServices createWLMNativeServices() {
        createMockEnv();
        return new UnauthorizedWLMNativeServicesMockNative();
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.UnauthorizedWLMNativeServices#activate(java.util.Map)}.
     */
    @Test
    public final void testActivate() throws Exception {
        unauthWLMNativeServices.setNativeMethodManager(mockNmm);
        final Object[] obj = new Object[] { WLMServiceResults.class, "setResults" };
        // Add NativeMethodManager.registerNatives
        final String name = "test";
        mockery.checking(new Expectations() {
            {
                oneOf(mockNmm).registerNatives(with(equal(UnauthorizedWLMNativeServices.class)), with(equal(obj)));
                oneOf(wlmcmmock).getSubSystem();
                will(returnValue(name));
                oneOf(wlmcmmock).getSubSystemName();
                will(returnValue(name));
                oneOf(wlmcmmock).getCreateFunctionName();
                will(returnValue(name));
                oneOf(wlmcmmock).getClassifyCollectionName();
                will(returnValue(name));
                oneOf(mock).ntv_le_connectAsWorkMgr(with(equal(name)),
                                                    with(equal(name)),
                                                    with(equal(name)),
                                                    with(equal(name)));
                will(returnValue(0));
            }
        });

        unauthWLMNativeServices.setWlmConfig(wlmcmmock);
        unauthWLMNativeServices.activate();
    }

    /**
     * Setup unauthWLMNativeServices
     */
    public final void Setup_unauthWLMNativeServices() throws Exception {
        unauthWLMNativeServices.setNativeMethodManager(mockNmm);
        final Object[] obj = new Object[] { WLMServiceResults.class, "setResults" };
        final String name = "test";

        // Add NativeMethodManager.registerNatives
        mockery.checking(new Expectations() {
            {
                oneOf(mockNmm).registerNatives(with(equal(UnauthorizedWLMNativeServices.class)), with(equal(obj)));
                oneOf(wlmcmmock).getSubSystem();
                will(returnValue(name));
                oneOf(wlmcmmock).getSubSystemName();
                will(returnValue(name));
                oneOf(wlmcmmock).getCreateFunctionName();
                will(returnValue(name));
                oneOf(wlmcmmock).getClassifyCollectionName();
                will(returnValue(name));
                oneOf(mock).ntv_le_connectAsWorkMgr(with(equal(name)),
                                                    with(equal(name)),
                                                    with(equal(name)),
                                                    with(equal(name)));
                will(returnValue(0));
            }
        });

        unauthWLMNativeServices.setWlmConfig(wlmcmmock);
        unauthWLMNativeServices.activate();
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.UnauthorizedWLMNativeServices#createWorkUnit(byte[])}.
     */
    @Test
    public final void testCreateWorkUnit() throws Exception {
        //in ebcdic 44 bytes "blahblah........wlmtrans"
        final byte[] enclaveToken_ebc = new byte[] { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -126, -109, -127, -120, -126, -109, -127, -120,
                                                     0, 0, 0, 0, 0, 0, 0, 0,
                                                     -90, -109, -108, -93, -103, -127, -107, -94 };
        final int connectToken = 0;
        final long arrivalTime = Calendar.getInstance().getTime().getTime();
        final String name = "test";

        Setup_unauthWLMNativeServices();

        // Add NativeMethodManager.registerNatives
        mockery.checking(new Expectations() {
            {
                oneOf(wlmcmmock).getCreateFunctionName();
                will(returnValue(name));
                oneOf(wlmcmmock).getClassifyCollectionName();
                will(returnValue(name));
                oneOf(mock).ntv_le_createWorkUnit(with(equal(connectToken)),
                                                  with(equal(enclaveToken_ebc)),
                                                  with(equal(name)),
                                                  with(equal(name)),
                                                  with(equal(arrivalTime)));
                will(returnValue(enclaveToken_ebc));
            }
        });
        assertNotNull(unauthWLMNativeServices.createWorkUnit(enclaveToken_ebc, arrivalTime));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.UnauthorizedWLMNativeServices#joinWorkUnit(byte[])}.
     */
    @Test
    public final void testJoinWorkUnit() throws Exception {

        final byte[] token_ebc = new byte[] { -126, -109, -127, -120, -126, -109, -127, -120 };

        // Add NativeMethodManager.registerNatives
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_le_joinWorkUnit(with(equal(token_ebc)));
                will(returnValue(true));
                oneOf(mock).ntv_le_joinWorkUnit(with(equal(token_ebc)));
                will(returnValue(false));
            }
        });
        assertTrue(unauthWLMNativeServices.joinWorkUnit(token_ebc));
        assertFalse(unauthWLMNativeServices.joinWorkUnit(token_ebc));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.UnauthorizedWLMNativeServices#leaveWorkUnit(byte[])}.
     */
    @Test
    public final void testLeaveWorkUnit() throws Exception {
        final byte[] token_ebc = new byte[] { -126, -109, -127, -120, -126, -109, -127, -120 };
        // Add NativeMethodManager.registerNatives
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_le_leaveWorkUnit(with(equal(token_ebc)));
                will(returnValue(true));
                oneOf(mock).ntv_le_leaveWorkUnit(with(equal(token_ebc)));
                will(returnValue(false));
            }
        });
        assertTrue(unauthWLMNativeServices.leaveWorkUnit(token_ebc));
        assertFalse(unauthWLMNativeServices.leaveWorkUnit(token_ebc));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.UnauthorizedWLMNativeServices#deleteWorkUnit(byte[])}.
     */
    @Test
    public final void testDeleteWorkUnit() throws Exception {
        final byte[] token_ebc = new byte[] { -126, -109, -127, -120, -126, -109, -127, -120 };
        // Add NativeMethodManager.registerNatives
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_le_deleteWorkUnit(with(equal(token_ebc)));
                will(returnValue(0));
            }
        });
        unauthWLMNativeServices.deleteWorkUnit(token_ebc);
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.UnauthorizedWLMNativeServices#deleteWorkUnit(byte[])}.
     */
    @Test
    public final void testErrorDeleteWorkUnit() throws Exception {
        final byte[] token_ebc = new byte[] { -126, -109, -127, -120, -126, -109, -127, -120 };
        // Add NativeMethodManager.registerNatives
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_le_deleteWorkUnit(with(equal(token_ebc)));
                will(returnValue(1));
            }
        });
        unauthWLMNativeServices.deleteWorkUnit(token_ebc);
    }

}
