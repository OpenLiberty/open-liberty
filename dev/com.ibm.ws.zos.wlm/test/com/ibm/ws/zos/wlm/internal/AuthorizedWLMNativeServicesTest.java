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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

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
 * Test AuthorizedWLMNativeServices
 */
@RunWith(JMock.class)
public class AuthorizedWLMNativeServicesTest {

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
    protected static AuthorizedWLMNativeServices mock = null;
    protected static AuthorizedWLMNativeServices authWLMNativeServices = null;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        authWLMNativeServices = createWLMNativeServices();
        WLMServiceResults wlmServiceResults = new WLMServiceResults();
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
        mock = mockery.mock(AuthorizedWLMNativeServices.class, "WLMNativeServices" + uniqueMockNameCount);
        mockNmm = mockery.mock(NativeMethodManager.class, "NativeMethodManager" + uniqueMockNameCount);
        wlmcmmock = mockery.mock(WLMConfigManager.class, "WLMConfigManager" + uniqueMockNameCount);
    }

    /**
     * Create the AuthorizedWLMNativeServices object for the unit test.
     */
    protected static AuthorizedWLMNativeServices createWLMNativeServices() {
        createMockEnv();
        return new AuthorizedWLMNativeServicesMockNative();
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.AuthorizedWLMNativeServices#activate(java.util.Map)}.
     */
    @Test(expected = java.lang.RuntimeException.class)
    public final void testActivateException() throws Exception {
        authWLMNativeServices.setNativeMethodManager(mockNmm);
        final Dictionary<String, Object> config = new Hashtable<String, Object>();
        final Object[] obj = new Object[] { WLMServiceResults.class, "setResults" };
        final String name = "test";

        // Add NativeMethodManager.registerNatives
        mockery.checking(new Expectations() {
            {
                oneOf(mockNmm).registerNatives(with(equal(AuthorizedWLMNativeServices.class)), with(equal(obj)));
                oneOf(wlmcmmock).getSubSystem();
                will(returnValue(name));
                oneOf(wlmcmmock).getSubSystemName();
                will(returnValue(name));
                oneOf(wlmcmmock).getCreateFunctionName();
                will(returnValue(name));
                oneOf(wlmcmmock).getClassifyCollectionName();
                will(returnValue(name));
                oneOf(mock).ntv_connectAsWorkMgr(with(equal(name)),
                                                 with(equal(name)),
                                                 with(equal(name)),
                                                 with(equal(name)));
                will(returnValue(-1));
            }
        });

        authWLMNativeServices.setWlmConfig(wlmcmmock);
        authWLMNativeServices.activate((Map<String, Object>) config);
    }

    /**
     * Setup authWLMNativeServices
     */
    public final void Setup_authWLMNativeServices() throws Exception {
        authWLMNativeServices.setNativeMethodManager(mockNmm);
        final Dictionary<String, Object> config = new Hashtable<String, Object>();
        final Object[] obj = new Object[] { WLMServiceResults.class, "setResults" };
        final String name = "test";

        // Add NativeMethodManager.registerNatives
        mockery.checking(new Expectations() {
            {
                oneOf(mockNmm).registerNatives(with(equal(AuthorizedWLMNativeServices.class)), with(equal(obj)));
                oneOf(wlmcmmock).getSubSystem();
                will(returnValue(name));
                oneOf(wlmcmmock).getSubSystemName();
                will(returnValue(name));
                oneOf(wlmcmmock).getCreateFunctionName();
                will(returnValue(name));
                oneOf(wlmcmmock).getClassifyCollectionName();
                will(returnValue(name));
                oneOf(mock).ntv_connectAsWorkMgr(with(equal(name)),
                                                 with(equal(name)),
                                                 with(equal(name)),
                                                 with(equal(name)));
                will(returnValue(0));
            }
        });

        authWLMNativeServices.setWlmConfig(wlmcmmock);
        authWLMNativeServices.activate((Map<String, Object>) config);
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.AuthorizedWLMNativeServices#createWorkUnit(byte[], long)}.
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
        final int serviceClassToken = 0;
        final byte[] serviceClassTokenBytes = new byte[4];

        Setup_authWLMNativeServices();

        // Add mock for WLMConfigManager
        mockery.checking(new Expectations() {
            {
                oneOf(wlmcmmock).getCreateFunctionName();
                will(returnValue(name));
                oneOf(wlmcmmock).getClassifyCollectionName();
                will(returnValue(name));
                oneOf(wlmcmmock).getServiceClassToken(with(equal(name + "blahblahwlmtrans")));
                will(returnValue(0));
                oneOf(mock).ntv_createWorkUnit(with(equal(connectToken)),
                                               with(equal(enclaveToken_ebc)),
                                               with(equal(name)),
                                               with(equal(name)),
                                               with(equal(arrivalTime)),
                                               with(equal(serviceClassToken)),
                                               with(serviceClassTokenBytes));
                will(returnValue(enclaveToken_ebc));
            }
        });
        assertNotNull(authWLMNativeServices.createWorkUnit(enclaveToken_ebc, arrivalTime));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.AuthorizedWLMNativeServices#joinWorkUnit(byte[])}.
     */
    @Test
    public final void testJoinWorkUnit() throws Exception {

        final byte[] token_ebc = new byte[] { -126, -109, -127, -120, -126, -109, -127, -120 };
        // Mock native methods
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_joinWorkUnit(with(equal(token_ebc)));
                will(returnValue(true));
                oneOf(mock).ntv_joinWorkUnit(with(equal(token_ebc)));
                will(returnValue(false));
            }
        });
        assertTrue(authWLMNativeServices.joinWorkUnit(token_ebc));
        assertFalse(authWLMNativeServices.joinWorkUnit(token_ebc));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.AuthorizedWLMNativeServices#leaveWorkUnit()}.
     */
    @Test
    public final void testLeaveWorkUnit() throws Exception {
        final byte[] token_ebc = new byte[] { -126, -109, -127, -120, -126, -109, -127, -120 };
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_leaveWorkUnit(with(equal(token_ebc)));
                will(returnValue(true));
                oneOf(mock).ntv_leaveWorkUnit(with(equal(token_ebc)));
                will(returnValue(false));
            }
        });
        assertTrue(authWLMNativeServices.leaveWorkUnit(token_ebc));
        assertFalse(authWLMNativeServices.leaveWorkUnit(token_ebc));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.AuthorizedWLMNativeServices#deleteWorkUnit(byte[])}.
     */
    @Test
    public final void testDeleteWorkUnit() throws Exception {
        final byte[] token_ebc = new byte[] { -126, -109, -127, -120, -126, -109, -127, -120 };
        final byte[] deleteDataBytes = new byte[WLMNativeServices.WLM_DELETE_DATA_LENGTH];
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_deleteWorkUnit(with(equal(token_ebc)), with(equal(deleteDataBytes)));
                will(returnValue(0));
            }
        });
        authWLMNativeServices.deleteWorkUnit(token_ebc);
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.AuthorizedWLMNativeServices#deleteWorkUnit(byte[])}.
     */
    @Test
    public final void testErrorDeleteWorkUnit() throws Exception {
        final byte[] token_ebc = new byte[] { -126, -109, -127, -120, -126, -109, -127, -120 };
        final byte[] deleteDataBytes = new byte[WLMNativeServices.WLM_DELETE_DATA_LENGTH];
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_deleteWorkUnit(with(equal(token_ebc)), with(equal(deleteDataBytes)));
                will(returnValue(1));
            }
        });
        authWLMNativeServices.deleteWorkUnit(token_ebc);
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.AuthorizedWLMNativeServices#deactivate(int)}.
     */
    @Test
    public final void testDeactivate() throws Exception {

        final int d_reason = 777;
        final int l_connectToken = 1234678;

        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_disconnectAsWorkMgr(with(equal(l_connectToken)));
                will(returnValue(0));
            }
        });

        authWLMNativeServices.setConnectToken(l_connectToken);
        authWLMNativeServices.deactivate(d_reason);

        // getConnectToken should be 0 after deactivate.
        assertTrue((authWLMNativeServices.getConnectToken() == 0));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.AuthorizedWLMNativeServices#deactivate(int)}.
     */
    @Test
    public final void testDeactivateFail() throws Exception {

        final int d_reason = 777;
        final int l_connectToken = 1234678;

        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_disconnectAsWorkMgr(with(equal(l_connectToken)));
                will(returnValue(-1));
            }
        });

        authWLMNativeServices.setConnectToken(l_connectToken);
        authWLMNativeServices.deactivate(d_reason);

    }
}
