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
package com.ibm.ws.security.registry.saf.internal;

import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.saf.SAFServiceResult;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.wsspi.security.registry.saf.SAFPasswordChangeException;

/**
 * Unit test for the SAF passwordUtility service. The SAF passwordUtility service requires
 * a z/OS native environment; however, this unit test is designed to run on
 * distributed platforms. Hence, the native environment is mocked using JMock.
 *
 * The mockery environment is configured by each test to receive and return
 * expected values for every native call expected during the test.
 *
 * Thus, this unit test is only focused on testing the Java code.
 */
@RunWith(JMock.class)
@SuppressWarnings("unchecked")
public class SAFPasswordUtilityImplTest {

    /**
     * Mock environment for NativeMethodManager and native methods.
     */
    private static Mockery mockery = null;

    /**
     * Counter for generating unique mock object names.
     */
    private static int uniqueMockNameCount = 1;

    /**
     * The mocked SAFPasswordUtilityService. This mock object handles all of
     * SAFPasswordUtilityService's native methods.
     */
    protected SAFPasswordUtilityImpl mockSAFPassUtil = null;

    /**
     * Mock ComponentContext used by SAFPasswordUtilityService.
     */
    protected ComponentContext mockCC = null;

    /**
     * Mock NativeMethodManager used by SAFPasswordUtilityService.
     */
    protected NativeMethodManager mockNmm = null;

    /**
     * Mock UserRegistryr used by SAFPasswordUtilityService.
     */
    protected SAFDelegatingUserRegistry mockUR = null;

    /**
     * Mock SAFServiceResult that allows the test code to insert various SAF results.
     */
    protected SAFServiceResult mockSSR = null;

    /**
     * Before each test.
     */
    @Before
    public void beforeTest() throws Exception {
        System.setSecurityManager(null);
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    /**
     * Create the Mockery environemnt and all the mock objects. Call this method at the
     * beginning of each test, to create a fresh isolated Mockery environment for the test.
     * This makes debugging easier when a test fails, because all the Expectations from
     * previous tests don't get dumped to the console.
     */
    protected void createMockEnv() {
        mockery = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        ++uniqueMockNameCount;
        mockSAFPassUtil = mockery.mock(SAFPasswordUtilityImpl.class, "SAFPasswordUtility" + uniqueMockNameCount);
        mockCC = mockery.mock(ComponentContext.class, "ComponentContext" + uniqueMockNameCount);
        mockNmm = mockery.mock(NativeMethodManager.class, "NativeMethodManager" + uniqueMockNameCount);
        mockUR = mockery.mock(SAFDelegatingUserRegistry.class, "SAFDelegatingUserRegistry" + uniqueMockNameCount);
        mockSSR = mockery.mock(SAFServiceResult.class, "SAFServiceResult" + uniqueMockNameCount);
    }

    /**
     * Create a SAFPasswordUtilityService for the unit test. The SAFPasswordUtilityService
     * impl returned by this method forwards all native method invocations (ntv_*) to the
     * SAFPasswordUtilityService mock object in this class (mockSAFPassUtil).
     */
    protected SAFPasswordUtilityImpl getSAFAuthorizationServiceMockNative() throws Exception {
        return new SAFPasswordUtilityImpl() {
            @Override
            protected int ntv_changePassword(byte[] safServiceResult,
                                             byte[] applId,
                                             byte[] username,
                                             byte[] oldPassword,
                                             byte[] newPassword) {
                // Copy the SAFServiceResult bytes from the test into safServiceResultBytes
                // (which the SAFAuthorizationService will use to detect SAF failures).
                System.arraycopy(mockSSR.getBytes(), 0, safServiceResult, 0, safServiceResult.length);

                return mockSAFPassUtil.ntv_changePassword(safServiceResult,
                                                          applId,
                                                          username,
                                                          oldPassword,
                                                          newPassword);
            }

            @Override
            protected boolean ntv_isMixedCasePasswordEnabled() {
                return mockSAFPassUtil.ntv_isMixedCasePasswordEnabled();
            }
        };
    }

    /**
     * New up a SAFPasswordUtilityService and set all its required service references
     * (all of which are mocked).
     */
    protected SAFPasswordUtilityImpl createSAFPasswordUtilityService() throws Exception {
        createMockEnv();

        // Expectations on the mock objects...
        mockery.checking(new Expectations() {
            {
                // NativeMethodManager.registerNatives is called by SAFAuthorizationService.setNativeMethodManager.
                oneOf(mockNmm).registerNatives(with(equal(SAFPasswordUtilityImpl.class)));
                oneOf(mockUR).getReportPasswordChangeDetailsConfig();
                will(returnValue(true));
                oneOf(mockSAFPassUtil).ntv_isMixedCasePasswordEnabled();
            }
        });

        final Map<String, Object> safPassConfig = new HashMap<String, Object>();
        safPassConfig.put("reportPasswordChangeDetails", true);

        // Create the SAFAuthorizationService and inject the dependencies.
        SAFPasswordUtilityImpl safPassUtility = getSAFAuthorizationServiceMockNative();
        safPassUtility.setNativeMethodManager(mockNmm);
        safPassUtility.setSAFDelegatingUserRegistry(mockUR);
        safPassUtility.activate(mockCC, safPassConfig);

        return safPassUtility;
    }

    /**
     * Unset the (mocked) services from SAFPasswordUtilityService and deactivate it.
     */
    protected void deactivateSAFPasswordUtilityService(SAFPasswordUtilityImpl safPassUtil) throws Exception {
        safPassUtil.deactivate(mockCC);
    }

    /**
     * Test for basic lifecycle operations.
     */
    @Test
    public void basicLifecycle() throws Exception {
        SAFPasswordUtilityImpl safPassutil = createSAFPasswordUtilityService();
        deactivateSAFPasswordUtilityService(safPassutil);
    }

    /**
     * Test method for SAFPasswordUtilityService.passwordChange.
     * Passing a empty userName shall result in a IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void passwordChange_emptyUserName() throws Exception {
        SAFPasswordUtilityImpl safPassUtil = createSAFPasswordUtilityService();
        safPassUtil.passwordChange("", TD.p1_str, TD.p2_str);
    }

    /**
     * Test method for SAFPasswordUtilityService.passwordChange.
     * Passing a null userName shall result in a IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void passwordChange_nullUserName() throws Exception {
        SAFPasswordUtilityImpl safPassUtil = createSAFPasswordUtilityService();
        safPassUtil.passwordChange(null, TD.p1_str, TD.p2_str);
    }

    /**
     * Test method for SAFPasswordUtilityService.passwordChange.
     * Passing a long userName shall result in a IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void passwordChange_longUserName() throws Exception {
        SAFPasswordUtilityImpl safPassUtil = createSAFPasswordUtilityService();
        safPassUtil.passwordChange("BOBOBOBOBOB", TD.p1_str, TD.p2_str);
    }

    /**
     * Test method for SAFPasswordUtilityService.passwordChange.
     * Passing a empty oldPassword shall result in a IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void passwordChange_emptyOldPassword() throws Exception {
        SAFPasswordUtilityImpl safPassUtil = createSAFPasswordUtilityService();
        safPassUtil.passwordChange(TD.u1_str, "", TD.p2_str);
    }

    /**
     * Test method for SAFPasswordUtilityService.passwordChange.
     * Passing a null oldPassword shall result in a IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void passwordChange_nullOldPassword() throws Exception {
        SAFPasswordUtilityImpl safPassUtil = createSAFPasswordUtilityService();
        safPassUtil.passwordChange(TD.u1_str, null, TD.p2_str);
    }

    /**
     * Test method for SAFPasswordUtilityService.passwordChange.
     * Passing a empty newPassword shall result in a IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void passwordChange_emptyNewPassword() throws Exception {
        SAFPasswordUtilityImpl safPassUtil = createSAFPasswordUtilityService();
        safPassUtil.passwordChange(TD.u1_str, TD.p1_str, "");
    }

    /**
     * Test method for SAFPasswordUtilityService.passwordChange.
     * Passing a null newPassword shall result in a IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void passwordChange_nullNewPassword() throws Exception {
        SAFPasswordUtilityImpl safPassUtil = createSAFPasswordUtilityService();
        safPassUtil.passwordChange(TD.u1_str, TD.p1_str, null);
    }

    /**
     * Test method for SAFPasswordUtilityService.passwordChange.
     * If native change method return non-zero code, it should throw a safException
     */
    @Test(expected = SAFPasswordChangeException.class)
    public void passwordChange_exception() throws Exception {
        SAFPasswordUtilityImpl safPassUtil = createSAFPasswordUtilityService();
        // Set up Expectations of native method calls for the mock SAFPasswordUtilityImpl
        final byte[] safResultBytes = new SAFServiceResult().getBytes();
        // Profile prefixes.
        final byte[] bbz_ebc = new byte[] { -62, -62, -57, -23, -60, -58, -45, -29, 0 }; // "BBGZDFLT" in EBCDIC (hex.c2.c2.c7.e9.c4.c6.d3.e3.00)
        mockery.checking(new Expectations() {
            {

                oneOf(mockSSR).getBytes();
                will(returnValue(safResultBytes));

                oneOf(mockUR).getProfilePrefix();
                will(returnValue("BBGZDFLT"));

                oneOf(mockSAFPassUtil).ntv_changePassword(with(equal(safResultBytes)),
                                                          with(equal(bbz_ebc)),
                                                          with(equal(TD.u1_ebc)),
                                                          with(equal(TD.p1_ebc)),
                                                          with(equal(TD.p3_ebc)));
                will(returnValue(4));
            }
        });

        safPassUtil.passwordChange(TD.u1_str, TD.p1_str, TD.p3_str);

    }

}
