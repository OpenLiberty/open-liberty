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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.HashMap;
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
import com.ibm.ws.zos.wlm.AlreadyClassifiedException;
import com.ibm.ws.zos.wlm.Enclave;

/**
 *
 */
@RunWith(JMock.class)
public class EnclaveManagerImplTest {

    /**
     * Mock environment for NativeMethodManager and native methods.
     */
    private static Mockery mockery = null;

    /**
     * Counter for generating unique mock object names.
     */
    private static int uniqueMockNameCount = 1;

    /**
     * Mock WLMNativeServices used by EnclaveManagerImpl.
     */
    protected static WLMNativeServices wlmnsmock = null;

    /**
     * EnclaveManager for the unit test.
     */
    private static EnclaveManagerImpl enclaveManager = null;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        enclaveManager = new EnclaveManagerImpl();
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
        wlmnsmock = mockery.mock(WLMNativeServices.class, "WLMNativeServices" + uniqueMockNameCount);
        uniqueMockNameCount++;
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.EnclaveManagerImpl#activate(java.util.Map)}.
     */
    @Test
    public final void testActivate() throws Exception {
        createMockEnv();
        final NativeMethodManager mockNmm = mockery.mock(NativeMethodManager.class);

        //config supplied to activate.
        final Map<String, Object> wlmConfig = new HashMap<String, Object>();

        enclaveManager.setNativeMethodManager(mockNmm);
        enclaveManager.activate(wlmConfig);
        enclaveManager.unsetNativeMethodManager(mockNmm);
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.EnclaveManagerImpl#setWlmNativeService(com.ibm.ws.zos.wlm.internal.WLMNativeServices)}.
     */
    //@Test
    public final void testSetWlmNativeService() {
        createMockEnv();
        enclaveManager.setWlmNativeService(wlmnsmock);
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.EnclaveManagerImpl#unsetWlmNativeService(com.ibm.ws.zos.wlm.internal.WLMNativeServices)}.
     */
    //@Test
    public final void testUnsetWlmNativeService() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.EnclaveManagerImpl#joinEnclave(com.ibm.ws.zos.wlm.Enclave)}.
     */
    @Test
    public final void testJoinEnclave() throws Exception {
        createMockEnv();
        enclaveManager.setWlmNativeService(wlmnsmock);
        EnclaveImpl enclave = new EnclaveImpl(classificationData_ebc);

        // Set up Expectations of method calls for the mock NativeMethodManager.
        mockery.checking(new Expectations() {
            {
                oneOf(wlmnsmock).joinWorkUnit(with(equal(classificationData_ebc)));
                will(returnValue(true));
            }
        });
        enclaveManager.joinEnclave(enclave);
    }

    /**
     * Test method for (@link com.ibm.ws.zos.wlm.internal.EnclaveManagerImpl#preJoinEnclave(com.ibm.ws.zos.wlm.Enclave)).
     */
    @Test
    public final void testPreJoinEnclave() throws Exception {
        createMockEnv();
        enclaveManager.setWlmNativeService(wlmnsmock);
        EnclaveImpl enclave = new EnclaveImpl(classificationData_ebc);
        enclaveManager.preJoinEnclave(enclave);
        assertEquals(true, enclave.isInUse());
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.EnclaveManagerImpl#joinEnclave(com.ibm.ws.zos.wlm.Enclave)}.
     */
    @Test(expected = AlreadyClassifiedException.class)
    public final void testJoinEnclaveException() throws Exception {
        createMockEnv();
        enclaveManager.setWlmNativeService(wlmnsmock);
        EnclaveImpl enclave = new EnclaveImpl(classificationData_ebc);

        // Set up Expectations of method calls for the mock NativeMethodManager.
        mockery.checking(new Expectations() {
            {
                oneOf(wlmnsmock).joinWorkUnit(with(equal(classificationData_ebc)));
                will(returnValue(false));
            }
        });

        enclaveManager.joinEnclave(enclave);
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.EnclaveManagerImpl#joinEnclave(com.ibm.ws.zos.wlm.Enclave)}.
     */
    @Test(expected = NullPointerException.class)
    public final void testNullJoinEnclave() throws Exception {
        createMockEnv();
        enclaveManager.setWlmNativeService(wlmnsmock);
        enclaveManager.joinEnclave(null);
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.EnclaveManagerImpl#leaveEnclave(com.ibm.ws.zos.wlm.Enclave, boolean)}.
     */
    @Test
    public final void testLeaveEnclaveEnclaveBoolean() {
        createMockEnv();
        enclaveManager.setWlmNativeService(wlmnsmock);
        EnclaveImpl enclave = new EnclaveImpl(classificationData_ebc);

        // Set up Expectations of method calls for the mock NativeMethodManager.
        mockery.checking(new Expectations() {
            {
                oneOf(wlmnsmock).leaveWorkUnit(with(equal(classificationData_ebc)));
                will(returnValue(true));
            }
        });
        enclaveManager.leaveEnclave(enclave, false);
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.EnclaveManagerImpl#leaveEnclave(com.ibm.ws.zos.wlm.Enclave, boolean)}.
     */
    @Test
    public final void testLeaveEnclaveEnclaveBoolean2() {
        createMockEnv();
        enclaveManager.setWlmNativeService(wlmnsmock);
        EnclaveImpl enclave = new EnclaveImpl(classificationData_ebc);
        enclave.setCreatedByEnclaveManager(true);

        // Set up Expectations of method calls for the mock NativeMethodManager.
        mockery.checking(new Expectations() {
            {
                oneOf(wlmnsmock).deleteWorkUnit(with(equal(classificationData_ebc)));

            }
        });
        enclaveManager.leaveEnclave(enclave, true);
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.EnclaveManagerImpl#leaveEnclave(com.ibm.ws.zos.wlm.Enclave, boolean)}.
     */
    @Test
    public final void testLeaveEnclaveEnclave() {
        createMockEnv();
        enclaveManager.setWlmNativeService(wlmnsmock);
        EnclaveImpl enclave = new EnclaveImpl(classificationData_ebc);
        enclave.setCreatedByEnclaveManager(true);

        // Set up Expectations of method calls for the mock NativeMethodManager.
        mockery.checking(new Expectations() {
            {
                oneOf(wlmnsmock).leaveDeleteWorkUnit(with(equal(classificationData_ebc)));

            }
        });
        enclaveManager.leaveEnclave(enclave);
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.EnclaveManagerImpl#create(com.ibm.ws.zos.wlm.ClassificationInfo)}.
     */
    @Test
    public final void testCreate() {
        createMockEnv();
        enclaveManager.setWlmNativeService(wlmnsmock);

        final Calendar calendar = Calendar.getInstance();
        final long arrivalTime = calendar.getTime().getTime();
        final ClassificationInfoImpl classificationInfo = new ClassificationInfoImpl(txClass, txName);
        // Set up Expectations of method calls for the mock NativeMethodManager.
        mockery.checking(new Expectations() {
            {
                oneOf(wlmnsmock).createWorkUnit(with(equal(classificationData_ebc)), with(equal(arrivalTime)));
                will(returnValue(classificationData_ebc));
            }
        });

        assertNotNull(enclaveManager.create(classificationInfo, arrivalTime));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.EnclaveManagerImpl#delete(com.ibm.ws.zos.wlm.Enclave)}.
     */
    @Test
    public final void testDelete() {
        createMockEnv();
        enclaveManager.setWlmNativeService(wlmnsmock);

        final Calendar calendar = Calendar.getInstance();
        final long arrivalTime = calendar.getTime().getTime();
        final ClassificationInfoImpl classificationInfo = new ClassificationInfoImpl(txClass, txName);

        // Set up Expectations of method calls for the mock NativeMethodManager.
        mockery.checking(new Expectations() {
            {
                oneOf(wlmnsmock).createWorkUnit(with(equal(classificationData_ebc)), with(equal(arrivalTime)));
                will(returnValue(classificationData_ebc));
            }
        });
        EnclaveImpl e = (EnclaveImpl) (enclaveManager.create(classificationInfo, arrivalTime));
        final byte[] token = e.getToken();
        // Just to drive the code somewhere...
        String s = enclaveManager.getStringToken(e);
        EnclaveImpl e1 = (EnclaveImpl) enclaveManager.getEnclaveFromToken(s);
        assertEquals(token, e1.getToken());

        // Set up Expectations of method calls for the mock NativeMethodManager.
        mockery.checking(new Expectations() {
            {
                oneOf(wlmnsmock).deleteWorkUnit(with(equal(token)));
            }
        });

        e.joining();
        e.leaving();
        enclaveManager.deleteEnclave(e, false);
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.EnclaveManagerImpl#removeEnclaveFromThread()}
     */
    @Test
    public final void testRemoveRestore() {
        createMockEnv();
        enclaveManager.setWlmNativeService(wlmnsmock);

        final Calendar calendar = Calendar.getInstance();
        final long arrivalTime = calendar.getTime().getTime();

        final byte[] tclassNative;

        try {
            tclassNative = txClass.getBytes("Cp1047");
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("code page conversion error", uee);
        }

        // Set up Expectations of method calls for the mock NativeMethodManager.
        mockery.checking(new Expectations() {
            {
                oneOf(wlmnsmock).createJoinWorkUnit(with(equal(tclassNative)), with(equal(arrivalTime)));
                will(returnValue(classificationData_ebc));
            }
        });

        Enclave enclave = enclaveManager.joinNewEnclave(txClass, arrivalTime);

        // Set up Expectations of method calls for the mock NativeMethodManager.
        mockery.checking(new Expectations() {
            {
                oneOf(wlmnsmock).leaveWorkUnit(with(equal(classificationData_ebc)));
            }
        });

        Enclave enclave2 = enclaveManager.removeCurrentEnclaveFromThread();

        // Set up Expectations of method calls for the mock NativeMethodManager.
        mockery.checking(new Expectations() {
            {
                oneOf(wlmnsmock).joinWorkUnit(with(equal(classificationData_ebc)));
                will(returnValue(true));
            }
        });

        try {
            enclaveManager.restoreEnclaveToThread(enclave2);
        } catch (AlreadyClassifiedException e) {
            fail("unexpected AlreadyClassifiedException" + e);
        }

        // Set up Expectations of method calls for the mock NativeMethodManager.
        mockery.checking(new Expectations() {
            {
                oneOf(wlmnsmock).leaveDeleteWorkUnit(with(equal(classificationData_ebc)));
            }
        });
        enclaveManager.leaveEnclave(enclave, false);

    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.EnclaveManagerImpl#joinNewEnclave(java.lang.String,java.lang.Long)}.
     */
    @Test
    public final void testJoinNew() {
        createMockEnv();
        enclaveManager.setWlmNativeService(wlmnsmock);

        final Calendar calendar = Calendar.getInstance();
        final long arrivalTime = calendar.getTime().getTime();

        final byte[] tclassNative;

        try {
            tclassNative = txClass.getBytes("Cp1047");
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("code page conversion error", uee);
        }

        // Set up Expectations of method calls for the mock NativeMethodManager.
        mockery.checking(new Expectations() {
            {
                oneOf(wlmnsmock).createJoinWorkUnit(with(equal(tclassNative)), with(equal(arrivalTime)));
                will(returnValue(classificationData_ebc));
            }
        });

        assertNotNull(enclaveManager.joinNewEnclave(txClass, arrivalTime));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.wlm.internal.EnclaveManagerImpl#create(com.ibm.ws.zos.wlm.ClassificationInfo)}.
     */
    @Test
    public final void testCreateNullEnclave() {
        createMockEnv();
        enclaveManager.setWlmNativeService(wlmnsmock);

        // classificationData should be in EBCDIC
        // transaction class (txClass) is string in ASCII

        final Calendar calendar = Calendar.getInstance();
        final long arrivalTime = calendar.getTime().getTime();
        final ClassificationInfoImpl classificationInfo = new ClassificationInfoImpl(txClass, txName);
        // Set up Expectations of method calls for the mock NativeMethodManager.
        mockery.checking(new Expectations() {
            {
                oneOf(wlmnsmock).createWorkUnit(with(equal(classificationData_ebc)), with(equal(arrivalTime)));
                will(returnValue(null));
            }
        });

        assertNull(enclaveManager.create(classificationInfo, arrivalTime));
    }

    // classificationData should be in EBCDIC 36 bytes , 1st byte as 1
    // transaction class (txClass) is string in ASCII
    private final String txClass = new String("blahblah");
    private final String txName = new String("wlmtrans");

    private final byte[] classificationData_ebc = new byte[] { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -126, -109, -127, -120, -126, -109, -127, -120,
                                                               0, 0, 0, 0, 0, 0, 0, 0,
                                                               -90, -109, -108, -93, -103, -127, -107, -94 };

    //    private final byte[] classificationData_ebc = new byte[] { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -62, -45, -63, -56, -62, -45, -63, -56,
    //                                                               0, 0, 0, 0, 0, 0, 0, 0,
    //                                                               -90, -109, -108, -93, -103, -127, -107, -94 };
}
