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
package com.ibm.ws.security.credentials.saf.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Subject;

import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.authentication.cache.CacheObject;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.saf.SAFException;
import com.ibm.ws.security.saf.SAFServiceResult;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.wsspi.security.credentials.saf.SAFCredential;

/**
 * Unit test for SAFCredentialsService. The SAFCredentialsService requires
 * a z/OS native environment; however, this unit test is designed to run on
 * distributed platforms. Hence, the native environment is mocked using JMock.
 *
 * Since the native environment is mocked, we're not actually running these tests
 * against an actual SAF database. Instead, the mockery environment is configured by
 * each test to receive and return expected values for every native call expected
 * during the test.
 *
 * Thus, this unit test is only focused on testing the Java code.
 */
@RunWith(JMock.class)
public class SAFCredentialsServiceTest {

    /**
     * Mock environment for NativeMethodManager and native methods.
     */
    private static Mockery mockery = null;

    /**
     * Counter for generating unique mock object names.
     */
    private static int uniqueMockNameCount = 1;

    /**
     * The mocked SAFCredentialsServiceImpl. This mock object handles all of SAFCredentials's
     * native methods.
     */
    protected static SAFCredentialsServiceImpl scsmock = null;
    protected X509Certificate x509mock = null;

    private final String unauthenticatedUser = "WSGUEST";
    private String profilePrefix = pp0_str;

    private final SAFCredentialsConfig config = new SAFCredentialsConfig() {

        @Override
        public Class<? extends Annotation> annotationType() {
            return SAFCredentialsConfig.class;
        }

        @Override
        public String unauthenticatedUser() {
            return unauthenticatedUser;
        }

        @Override
        public String profilePrefix() {
            return profilePrefix;
        }

        @Override
        public boolean mapDistributedIdentities() {
            return false;
        }

        @Override
        public boolean suppressAuthFailureMessages() {
            return false;
        }
    };

    /**
     * Create the mock object(s). Each mock object created by this test
     * needs to have a unique name. Just use a simple counter to
     * ensure uniqueness.
     */
    public void createMockEnv() {
        mockery = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        scsmock = mockery.mock(SAFCredentialsServiceImpl.class, "safCredential" + uniqueMockNameCount++);
        x509mock = mockery.mock(X509Certificate.class, "X509Certificate" + uniqueMockNameCount++);
    }

    /**
     * Create a SAFCredentialsServiceImpl for the unit test using the given config.
     * The SACredentialsServiceImpl forwards all native method invocations (ntv_*) to the
     * SAFCredentialsService mock object in this class.
     */
    public SAFCredentialsServiceImpl createSAFCredentialsService() throws Exception {
        createMockEnv();
        final SAFCredentialsServiceImpl safCredentials = new SAFCredentialsServiceImplMockNative();
        safCredentials.cs = mockery.mock(CredentialsService.class);

        // Set up Expectations of method calls for the mock NativeMethodManager.
        mockery.checking(new Expectations() {
            {
                oneOf(safCredentials.cs).setUnauthenticatedUserid(unauthenticatedUser);
                oneOf(scsmock).ntv_setPenaltyBoxDefaults(false);
            }
        });

        safCredentials.activate(config);
        return safCredentials;
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createPasswordCredential(String, String,String)}.
     * Test that only userid is converted to uppercase properly. Password not converted because it is >8chars.
     */
    @Test
    public void createPasswordCredential_mixedCasePwdDisabled() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        // build resultTokenBytes with some random byte values
        final byte[] resultTokenBytes = new byte[] { -43, -29, -27, -39, -59, -63, -45, -44, 0 };

        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id1_up_ebc)),
                                                            with(equal(p0_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));

                will(returnValue(resultTokenBytes));
            }
        });

        assertNotNull(safCredentials.createPasswordCredential(id1_str, p0_str, a1_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createPasswordCredential(String, String,String)}.
     * Test that both userid and password are converted to uppercase properly. Password is converted because it is <= 8chars and MixedCasePassword is disabled.
     */
    @Test
    public void createPasswordCredential_8charPassword_mixedCasePWDisabled() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        // build resultTokenBytes with some random byte values
        final byte[] resultTokenBytes = new byte[] { -43, -29, -27, -39, -59, -63, -45, -44, 0 };

        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id1_up_ebc)),
                                                            with(equal(p9_up_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
            }
        });

        assertNotNull(safCredentials.createPasswordCredential(id1_str, p9_str, a1_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createPasswordCredential(String, String,String)}.
     * Test that only userid is converted to uppercase properly. Password is not converted because it is <= 8chars and MixedCasePassword is enabled.
     */
    @Test
    public void createPasswordCredential_8charPassword_mixedCasePWEnabled() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        setMixedCaseEnabled(safCredentials, true);
        // build resultTokenBytes with some random byte values
        final byte[] resultTokenBytes = new byte[] { -43, -29, -27, -39, -59, -63, -45, -44, 0 };

        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id1_up_ebc)),
                                                            with(equal(p9_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
            }
        });

        assertNotNull(safCredentials.createPasswordCredential(id1_str, p9_str, a1_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createPasswordCredential(String, String,String)}.
     * Test that both userid and password are converted to uppercase properly. Password is converted because it is <= 8chars and MixedCasePassword is disabled.
     */
    @Test
    public void createPasswordCredential_7charPassword_mixedCasePWDisabled() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        // build resultTokenBytes with some random byte values
        final byte[] resultTokenBytes = new byte[] { -43, -29, -27, -39, -59, -63, -45, -44, 0 };

        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id1_up_ebc)),
                                                            with(equal(p10_up_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
            }
        });

        assertNotNull(safCredentials.createPasswordCredential(id1_str, p10_str, a1_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createPasswordCredential(String, String,String)}.
     * Test that only userid is converted to uppercase properly. Password is not converted because it is <= 8chars and MixedCasePassword is enabled.
     */
    @Test
    public void createPasswordCredential_7charPassword_mixedCasePWEnabled() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        setMixedCaseEnabled(safCredentials, true);
        // build resultTokenBytes with some random byte values
        final byte[] resultTokenBytes = new byte[] { -43, -29, -27, -39, -59, -63, -45, -44, 0 };

        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id1_up_ebc)),
                                                            with(equal(p10_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
            }
        });

        assertNotNull(safCredentials.createPasswordCredential(id1_str, p10_str, a1_str));
    }

    /**
     * Test method for: {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#setNativeMethodManager(NativeMethodManager)}.
     * {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#unsetNativeMethodManager(NativeMethodManager)}.
     *
     * Basic lifecycle operations.
     */
    @Test
    public void lifecycle_test() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        final NativeMethodManager mockNmm = mockery.mock(NativeMethodManager.class);

        // Set up Expectations of method calls for the mock NativeMethodManager.
        mockery.checking(new Expectations() {
            {
                oneOf(mockNmm).registerNatives(with(equal(SAFCredentialsServiceImpl.class)));
            }
        });

        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_isMixedCasePWEnabled();
                will(returnValue(true));
            }
        });

        safCredentials.setNativeMethodManager(mockNmm);

        safCredentials.unsetNativeMethodManager(mockNmm);
    }

    /**
     * Set the mixedCaseEnabled flag for the given SAFCredentialsService.
     */
    protected void setMixedCaseEnabled(SAFCredentialsServiceImpl safCredentials, final boolean isMixedCaseEnabled) {
        final NativeMethodManager mockNmm = mockery.mock(NativeMethodManager.class);

        // NativeMethodManager.registerNatives is called from SAFCredentialsServiceImpl.setNativeMethodManager.
        mockery.checking(new Expectations() {
            {
                oneOf(mockNmm).registerNatives(with(equal(SAFCredentialsServiceImpl.class)));
            }
        });

        // ntv_isMixedCasePWEnabled is called from SAFCredentialsServiceImpl.setNativeMethodManager.
        mockery.checking(new Expectations() {
            {
                oneOf(scsmock).ntv_isMixedCasePWEnabled();
                will(returnValue(isMixedCaseEnabled));
            }
        });

        safCredentials.setNativeMethodManager(mockNmm);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createPasswordCredential(String, String,String)}.
     * Test that various userids and only userid is converted to uppercase. Password not converted because it is >8chars.
     *
     */
    @Test
    public void createPasswordCredential_variousUserIds() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();

        // build resultTokenBytes with some random byte values
        final byte[] resultTokenBytes = new byte[] { -43, -29, -27, -39, -59, -63, -45, -44, 0 };

        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id1_up_ebc)),
                                                            with(equal(p1_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id2_ebc)),
                                                            with(equal(p1_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id3_up_ebc)),
                                                            with(equal(p1_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id4_ebc)),
                                                            with(equal(p1_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id5_ebc)),
                                                            with(equal(p1_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id6_ebc)),
                                                            with(equal(p1_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id7_ebc)),
                                                            with(equal(p1_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
            }
        });

        assertNotNull(safCredentials.createPasswordCredential(id1_str, p1_str, "AUDIT"));
        assertNotNull(safCredentials.createPasswordCredential(id2_str, p1_str, a1_str));
        assertNotNull(safCredentials.createPasswordCredential(id3_str, p1_str, a1_str));
        assertNotNull(safCredentials.createPasswordCredential(id4_str, p1_str, a1_str));
        assertNotNull(safCredentials.createPasswordCredential(id5_str, p1_str, a1_str));
        assertNotNull(safCredentials.createPasswordCredential(id6_str, p1_str, a1_str));
        assertNotNull(safCredentials.createPasswordCredential(id7_str, p1_str, a1_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createPasswordCredential(String, String,String)}.
     * Test that various password strings are handled properly.
     * Since the mixed case password is enabled, the password shouldnot be converted to uppercase
     */
    @Test
    public void createPasswordCredential_variousPasswords() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        setMixedCaseEnabled(safCredentials, true);

        // build resultTokenBytes with some random byte values
        final byte[] resultTokenBytes = new byte[] { -43, -29, -27, -39, -59, -63, -45, -44, 0 };

        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id2_ebc)),
                                                            with(equal(p1_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id2_ebc)),
                                                            with(equal(p1_up_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id2_ebc)),
                                                            with(equal(p2_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id2_ebc)),
                                                            with(equal(p3_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id2_ebc)),
                                                            with(equal(p4_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id2_ebc)),
                                                            with(equal(p5_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id2_ebc)),
                                                            with(equal(p6_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id2_ebc)),
                                                            with(equal(p7_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id2_ebc)),
                                                            with(equal(p8_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
            }
        });

        assertNotNull(safCredentials.createPasswordCredential(id2_str, p1_str, a1_str));
        assertNotNull(safCredentials.createPasswordCredential(id2_str, p1_up_str, a1_str));
        assertNotNull(safCredentials.createPasswordCredential(id2_str, p2_str, a1_str));
        assertNotNull(safCredentials.createPasswordCredential(id2_str, p3_str, a1_str));
        assertNotNull(safCredentials.createPasswordCredential(id2_str, p4_str, a1_str));
        assertNotNull(safCredentials.createPasswordCredential(id2_str, p5_str, a1_str));
        assertNotNull(safCredentials.createPasswordCredential(id2_str, p6_str, a1_str));
        assertNotNull(safCredentials.createPasswordCredential(id2_str, p7_str, a1_str));
        assertNotNull(safCredentials.createPasswordCredential(id2_str, p8_str, a1_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createPasswordCredential(String, String,String)}.
     * Test that null audit string is replaced with the default audit string "WebSphere Userid/Password Login"
     */
    @Test
    public void createPasswordCredential_nullAuditStrings() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();

        // build resultTokenBytes with some random byte values
        final byte[] resultTokenBytes = new byte[] { -43, -29, -27, -39, -59, -63, -45, -44, 0 };

        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id2_ebc)),
                                                            with(equal(p1_ebc)),
                                                            with(equal(a2_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
            }
        });

        assertNotNull(safCredentials.createPasswordCredential(id2_str, p1_str, null));
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createPasswordCredential(String, String,String)}.
     * Test that null userid throws a NullPointerException.
     */
    @Test(expected = NullPointerException.class)
    public void createPasswordCredential_nullUser() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        safCredentials.createPasswordCredential(null, p1_str, a1_str);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createPasswordCredential(String, String,String)}.
     * Test that null password throws a NullPointerException.
     */
    @Test(expected = NullPointerException.class)
    public void createPasswordCredential_nullPassword() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        safCredentials.createPasswordCredential(id1_str, null, a1_str);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createPasswordCredential(String, String,String)}.
     * Test that non-existent userid or password returning null from ntv_createPasswordCredential throws a SAFexception.
     */
    @Test(expected = SAFException.class)
    public void createPasswordCredential_null() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();

        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id2_ebc)),
                                                            with(equal(p1_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(null));
            }
        });

        safCredentials.createPasswordCredential(id2_str, p1_str, a1_str);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createPasswordCredential(String, String,String)}.
     * Test that long userid( >8 chars ) throws a SAFException.
     */
    @Test(expected = SAFException.class)
    public void createPasswordCredential_longUserid() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id8_ebc)),
                                                            with(equal(p1_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(null));
            }
        });

        safCredentials.createPasswordCredential(id8_str, p1_str, a1_str);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createPasswordCredential(String, String,String)}.
     * Test long auditString( > 255 chars) should throw a SAFException
     */
    @Test(expected = SAFException.class)
    public void createPasswordCredential_longAuditString() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id2_ebc)),
                                                            with(equal(p1_ebc)),
                                                            with(equal(a4_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(null));
            }
        });

        safCredentials.createPasswordCredential(id2_str, p1_str, a4_str);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createPasswordCredential(String, String,String)}.
     * Test zerolength userid should throw a SAFException
     */
    @Test(expected = SAFException.class)
    public void createPasswordCredential_zeroLengthUserid() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id6_ebc)),
                                                            with(equal(p1_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(null));
            }
        });

        safCredentials.createPasswordCredential(id6_str, p1_str, a1_str);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createPasswordCredential(String, String,String)}.
     * Test zerolength password should throw a SAFException
     */
    @Test(expected = SAFException.class)
    public void createPasswordCredential_zeroLengthPwd() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {
                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id2_ebc)),
                                                            with(equal(p5_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(null));
            }
        });

        safCredentials.createPasswordCredential(id2_str, p5_str, a1_str);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createPasswordCredential(String, String,String)}.
     * Test weird character password should throw a SAFException
     */
    @Test(expected = SAFException.class)
    public void createPasswordCredential_badPassword() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id2_ebc)),
                                                            with(equal(p6_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(null));
            }
        });

        safCredentials.createPasswordCredential(id2_str, p6_str, a1_str);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createPasswordCredential(String, String,String)}.
     * Test weird character userid (user doesnot exist) should throw a SAFException
     */
    @Test(expected = SAFException.class)
    public void createPasswordCredential_badUserid() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {
                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id7_ebc)),
                                                            with(equal(p1_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(null));
            }
        });

        safCredentials.createPasswordCredential(id7_str, p1_str, a1_str);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createPasswordCredential(String, String,String)}.
     * Test zerolength userid should throw a SAFException
     */
    @Test(expected = SAFException.class)
    public void createAssertedCredential_zeroLengthUserid() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createAssertedCredential(with(equal(id6_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(0)),
                                                            with(equal(safResultBytes)));
                will(returnValue(null));
            }
        });

        safCredentials.createAssertedCredential(id6_str, a1_str, 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createAssertedCredential(String, String)}.
     * Test long userid ( >8 chars) should throw a SAFException
     */
    @Test(expected = SAFException.class)
    public void createAssertedCredential_longUserid() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createAssertedCredential(with(equal(id8_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(0)),
                                                            with(equal(safResultBytes)));
                will(returnValue(null));
            }
        });

        safCredentials.createAssertedCredential(id8_str, a1_str, 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createAssertedCredential(String, String)}.
     * Test weird userid ( user doesnot exist) should throw a SAFException
     */
    @Test(expected = SAFException.class)
    public void createAssertedCredential_badUserid() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createAssertedCredential(with(equal(id7_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(0)),
                                                            with(equal(safResultBytes)));
                will(returnValue(null));
            }
        });

        safCredentials.createAssertedCredential(id7_str, a1_str, 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createAssertedCredential(String, String)}.
     * Test long audit string ( > 255 chars) should throw a SAFException
     */
    @Test(expected = SAFException.class)
    public void createAssertedCredential_longAuditString() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createAssertedCredential(with(equal(id2_ebc)),
                                                            with(equal(a4_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(0)),
                                                            with(equal(safResultBytes)));
                will(returnValue(null));
            }
        });

        safCredentials.createAssertedCredential(id2_str, a4_str, 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createAssertedCredential(String, String)}.
     * Test that various userids are handled properly and verify that the userids are converted to uppercase
     *
     */
    @Test
    public void createAssertedCredential_variousUserIds() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        // build resultTokenBytes with some random byte values
        final byte[] resultTokenBytes = new byte[] { -43, -29, -27, -39, -59, -63, -45, -44, 0 };

        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createAssertedCredential(with(equal(id1_up_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(0)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_createAssertedCredential(with(equal(id2_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(0)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_createAssertedCredential(with(equal(id3_up_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(0)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_createAssertedCredential(with(equal(id4_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(0)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_createAssertedCredential(with(equal(id5_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(0)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_createAssertedCredential(with(equal(id6_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(0)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_createAssertedCredential(with(equal(id7_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(0)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));

            }
        });

        assertNotNull(safCredentials.createAssertedCredential(id1_str, a1_str, 0));
        assertNotNull(safCredentials.createAssertedCredential(id2_str, a1_str, 0));
        assertNotNull(safCredentials.createAssertedCredential(id3_str, a1_str, 0));
        assertNotNull(safCredentials.createAssertedCredential(id4_str, a1_str, 0));
        assertNotNull(safCredentials.createAssertedCredential(id5_str, a1_str, 0));
        assertNotNull(safCredentials.createAssertedCredential(id6_str, a1_str, 0));
        assertNotNull(safCredentials.createAssertedCredential(id7_str, a1_str, 0));
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createAssertedCredential(String, String)}.
     * The new credential must be returned since the registry will delete the credential itself.
     *
     */
    @Test
    public void createAssertedCredential_duplicateUserIds() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();

        // build resultTokenBytes with some random byte values
        final byte[] resultTokenBytes = new byte[] { -43, -29, -27, -39, -59, -63, -45, -44, 0 };
        final byte[] secondResultTokenBytes = new byte[] { -42, -28, -26, -38, -58, -62, -44, -43, 0 };

        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {
                exactly(2).of(scsmock).ntv_createAssertedCredential(with(equal(id1_up_ebc)),
                                                                    with(equal(a1_ebc)),
                                                                    with(equal(pp0_ebc)),
                                                                    with(equal(0)),
                                                                    with(equal(safResultBytes)));
                will(returnDifferentTokenInEachInvocation(resultTokenBytes, secondResultTokenBytes));
            }
        });

        SAFCredential firstCred = safCredentials.createAssertedCredential(id1_str, a1_str, 0);
        String firstTokenKey = safCredentials.getSAFCredentialTokenKey(firstCred);
        SAFCredential secondCred = safCredentials.createAssertedCredential(id1_str, a1_str, 0);
        String secondTokenKey = safCredentials.getSAFCredentialTokenKey(secondCred);
        assertFalse("The token keys must not be equal.", firstTokenKey.equals(secondTokenKey));
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createAssertedCredential(String, String)}.
     * Test that null audit string is substituted with default authorized audit string "WebSphere Authorized Login"
     */
    @Test
    public void createAssertedCredential_nullAuditStrings() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();

        // build resultTokenBytes with some random byte values
        final byte[] resultTokenBytes = new byte[] { -43, -29, -27, -39, -59, -63, -45, -44, 0 };

        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createAssertedCredential(with(equal(id2_ebc)),
                                                            with(equal(a3_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(0)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
            }
        });

        assertNotNull(safCredentials.createAssertedCredential(id2_str, null, 0));
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createAssertedCredential(String, String)}.
     * Test that null userids, returning a null SAFCredential throws a NullPointerException.
     */
    @Test(expected = NullPointerException.class)
    public void createAssertedCredential_nullUser() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        safCredentials.createAssertedCredential(null, a1_str, 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createAssertedCredential(String, String)}.
     * Test that non-existent userid returning a null SAFCredential throws a SAFException.
     */
    @Test(expected = SAFException.class)
    public void createAssertedCredential_null() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();

        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createAssertedCredential(with(equal(id2_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(0)),
                                                            with(equal(safResultBytes)));
                will(returnValue(null));
            }
        });

        safCredentials.createAssertedCredential(id2_str, a1_str, 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#deleteCredential(SAFCredential)}.
     * Test that SAFCredential which was created using createdPasswordCredential is deleted properly
     * and also verifying that the native token associated with the SAFCredential is the same token
     * that was returned from a previous createPassowordCredential
     */
    @Test
    public void deleteCredential() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();

        // build resultTokenBytes with some random byte values
        final byte[] resultTokenBytes = new byte[] { -43, -29, -27, -39, -59, -63, -45, -44, 0 };
        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id2_ebc)),
                                                            with(equal(p1_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_deleteCredential(with(equal(resultTokenBytes)));
                will(returnValue(0));

            }
        });

        SAFCredential safcred = safCredentials.createPasswordCredential(id2_str, p1_str, a1_str);
        safCredentials.deleteCredential(safcred);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#deleteCredential(SAFCredential)}.
     * Test that deleting a non-existent SAFCredential ( i,e the SAFCredentialToken doesnt match the one that was created earlier) throws
     * a SAFException
     */
    @Test(expected = SAFException.class)
    public void deleteCredential_error() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();

        // build resultTokenBytes with some random byte values
        final byte[] resultTokenBytes = new byte[] { -43, -29, -27, -39, -59, -63, -45, -44, 0 };
        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id2_ebc)),
                                                            with(equal(p1_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_deleteCredential(with(equal(resultTokenBytes)));
                will(returnValue(1));

            }
        });

        SAFCredential safcred = safCredentials.createPasswordCredential(id2_str, p1_str, a1_str);
        safCredentials.deleteCredential(safcred);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#deleteCredential(SAFCredential)}.
     * Test that if a null SAFCredential is passed to deleteCredential, it doesnt invoke the ntv_deleteCredential method and
     * so doesnot give error
     */
    @Test
    public void deleteCredential_nullCredential() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        // should not call the ntv_deleteCredential
        safCredentials.deleteCredential(null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#deleteCredential(SAFCredential)}.
     * Test that deleting a SAFCredential with empty SAFCredentialToken, gives a SAFException
     */
    @Test(expected = SAFException.class)
    public void deleteCredential_emptySAFCredToken() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();

        // build resultTokenBytes with some random byte values
        final byte[] resultTokenBytes = new byte[] { 0 };
        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id2_ebc)),
                                                            with(equal(p1_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(scsmock).ntv_deleteCredential(with(equal(resultTokenBytes)));
                will(returnValue(1));

            }
        });

        SAFCredential safcred = safCredentials.createPasswordCredential(id2_str, p1_str, a1_str);
        safCredentials.deleteCredential(safcred);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#deleteCredential(SAFCredential)}.
     * Test deleting a SAFCredential without a SAFCredentialToken doesnot give an error
     * This is equivalent to passing a null SAFCredential, so ntv_deleteCredential doesnt get invoked
     */
    @Test
    public void deleteCredential_noSAFCredToken() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        // delete a SAFCredential with no SAFCredentialToken
        SAFCredential safcred = new SAFCredentialImpl(null, null, SAFCredential.Type.BASIC);
        safCredentials.deleteCredential(safcred);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#evicted(List<Object>)}.
     * Test that SAFCredential which was created using createdPasswordCredential is deleted properly
     * when the subject is evicted.
     */
    @Test
    public void deleteCredential_fromSubject() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        final CacheObject comock = mockery.mock(CacheObject.class, "CacheObject" + uniqueMockNameCount++);
        final Subject subject = new Subject();

        // build resultTokenBytes with some random byte values
        final byte[] resultTokenBytes = new byte[] { -43, -29, -27, -39, -59, -63, -45, -44, 0 };
        // Set up Expectations of native method calls for the mock SAFCredentialsServiceImpl
        mockery.checking(new Expectations() {
            {

                oneOf(scsmock).ntv_createPasswordCredential(with(equal(id2_ebc)),
                                                            with(equal(p1_ebc)),
                                                            with(equal(a1_ebc)),
                                                            with(equal(pp0_ebc)),
                                                            with(equal(safResultBytes)));
                will(returnValue(resultTokenBytes));
                oneOf(comock).getSubject();
                will(returnValue(subject));
                oneOf(scsmock).ntv_deleteCredential(with(equal(resultTokenBytes)));
                will(returnValue(0));

            }
        });

        SAFCredential safcred = safCredentials.createPasswordCredential(id2_str, p1_str, a1_str);
        subject.getPrivateCredentials().add(safcred);

        List<Object> victims = new ArrayList<Object>();
        victims.add(comock);
        safCredentials.evicted(victims);

        mockery.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#evicted(List<Object>)}.
     * Test that subject with no saf credential (null safcredential) doesnot call deleteCredential
     * when the subject is evicted.
     */
    @Test
    public void deleteCredential_SubjectWithNoCred() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();

        final CacheObject comock = mockery.mock(CacheObject.class, "CacheObject" + uniqueMockNameCount++);
        final Subject subject = new Subject();

        // Set up Expectations of CacheObject calls for the mock CacheObject
        mockery.checking(new Expectations() {
            {
                oneOf(comock).getSubject();
                will(returnValue(subject));
            }
        });

        List<Object> victims = new ArrayList<Object>();
        victims.add(comock);
        safCredentials.evicted(victims);

        mockery.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createCertificateCredential(X509Certificate, String)}.
     * Test business as usual path
     */
    @Test
    public void createCertificateCredential_test() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();

        final byte[] ecert = new byte[] { 'b', 'l', 'a', 'h' };
        final byte[] outputUsername = new byte[8];
        final SAFServiceResult safResult = new SAFServiceResult();

        mockery.checking(new Expectations() {
            {
                exactly(1).of(x509mock).getEncoded();
                will(returnValue(new byte[] { 'b', 'l', 'a', 'h' }));

                oneOf(scsmock).ntv_createCertificateCredential(ecert,
                                                               ecert.length,
                                                               a3_ebc,
                                                               pp0_ebc,
                                                               outputUsername,
                                                               safResult.getBytes());
                will(returnValue("testToeknBytes".getBytes()));
            }
        });

        safCredentials.createCertificateCredential(x509mock, a3_str);
    }

    private Action returnDifferentTokenInEachInvocation(final byte[] resultTokenBytes, final byte[] secondResultTokenBytes) {
        return new Action() {

            private boolean alreadyInvoked = false;

            @Override
            public Object invoke(Invocation invocation) throws Throwable {
                if (alreadyInvoked == false) {
                    alreadyInvoked = true;
                    return resultTokenBytes;
                }
                return secondResultTokenBytes;
            }

            @Override
            public void describeTo(Description desc) {
            }
        };
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createCertificateCredential(X509Certificate, String)}.
     * Test that null audit string is ok
     */
    @Test
    public void createCertificateCredential_nullAuditString() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        final byte[] ecert = new byte[] { 'b', 'l', 'a', 'h' };
        final byte[] outputUsername = new byte[8];;
        final SAFServiceResult safResult = new SAFServiceResult();
        final byte[] audit_ebc = (DEFAULT_CERTIFICATE_AUDIT_STRING + "\0").getBytes("Cp1047");

        mockery.checking(new Expectations() {
            {
                exactly(1).of(x509mock).getEncoded();
                will(returnValue(new byte[] { 'b', 'l', 'a', 'h' }));

                oneOf(scsmock).ntv_createCertificateCredential(ecert,
                                                               ecert.length,
                                                               audit_ebc,
                                                               pp0_ebc,
                                                               outputUsername,
                                                               safResult.getBytes());
                will(returnValue("testTokenBytes".getBytes()));
            }
        });

        safCredentials.createCertificateCredential(x509mock, null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createCertificateCredential(X509Certificate, String)}.
     * Test that null response from createCertificateCredential throws a SAFException
     */
    @Test(expected = SAFException.class)
    public void createCertificateCredential_nullFromNative() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        final byte[] ecert = new byte[] { 'b', 'l', 'a', 'h' };
        final byte[] outputUsername = new byte[8];
        final SAFServiceResult safResult = new SAFServiceResult();

        mockery.checking(new Expectations() {
            {
                exactly(1).of(x509mock).getEncoded();
                will(returnValue(new byte[] { 'b', 'l', 'a', 'h' }));

                oneOf(scsmock).ntv_createCertificateCredential(ecert,
                                                               ecert.length,
                                                               a1_ebc,
                                                               pp0_ebc,
                                                               outputUsername,
                                                               safResult.getBytes());
                will(returnValue(null));
            }
        });

        safCredentials.createCertificateCredential(x509mock, a1_str);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createCertificateCredential(X509Certificate, String)}.
     * Test that CertificateEncodingException from X509Certificate.getEncoded throws a SAFException
     */
    @Test(expected = SAFException.class)
    public void createCertificateCredential_certEncodedException() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();

        mockery.checking(new Expectations() {
            {
                oneOf(x509mock).getEncoded();
                will(throwException(new CertificateEncodingException()));
            }
        });

        safCredentials.createCertificateCredential(x509mock, a1_str);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createCertificateCredential(X509Certificate, String)}.
     * Test that null certificate throws exception
     */
    @Test(expected = NullPointerException.class)
    public void createCertificateCredential_NullCert() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        final String auditString = "Test WebSphere Authorized Login";
        safCredentials.createCertificateCredential(null, auditString);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createCertificateCredential(X509Certificate, String)}.
     * Test that null certificate throws exception
     */
    @Test(expected = NullPointerException.class)
    public void createCertificateCredential_DoubleNull() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        safCredentials.createCertificateCredential(null, null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createCertificateCredential(X509Certificate, String)}.
     * Test that an invalid certificate throws a SAFException
     */
    @Test(expected = SAFException.class)
    public void createCertificateCredential_InvalidCert() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        final byte[] ecert = new byte[] { 'b', 'l', 'a', 'h' };
        final byte[] outputUsername = new byte[8];;
        final SAFServiceResult safResult = new SAFServiceResult();
        final byte[] audit_ebc = (DEFAULT_CERTIFICATE_AUDIT_STRING + "\0").getBytes("Cp1047");

        mockery.checking(new Expectations() {
            {
                exactly(1).of(x509mock).getEncoded();
                will(returnValue(new byte[] { 'b', 'l', 'a', 'h' }));

                oneOf(scsmock).ntv_createCertificateCredential(ecert,
                                                               ecert.length,
                                                               audit_ebc,
                                                               pp0_ebc,
                                                               outputUsername,
                                                               safResult.getBytes());
                will(returnValue(null));
            }
        });

        safCredentials.createCertificateCredential(x509mock, null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createCertificateCredential(X509Certificate, String)}.
     * Test that a long audit string throws a SAFException
     */
    @Test(expected = SAFException.class)
    public void createCertificateCredential_LongAudit() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        final byte[] ecert = new byte[] { 'b', 'l', 'a', 'h' };
        final byte[] outputUsername = new byte[8];;
        final SAFServiceResult safResult = new SAFServiceResult();

        mockery.checking(new Expectations() {
            {
                exactly(1).of(x509mock).getEncoded();
                will(returnValue(new byte[] { 'b', 'l', 'a', 'h' }));

                oneOf(scsmock).ntv_createCertificateCredential(ecert,
                                                               ecert.length,
                                                               a4_ebc,
                                                               pp0_ebc,
                                                               outputUsername,
                                                               safResult.getBytes());
                will(returnValue(null));
            }
        });

        safCredentials.createCertificateCredential(x509mock, a4_str);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#createCertificateCredential(X509Certificate, String)}.
     * Test that a zero length certificate throws a SAFException
     */
    @Test(expected = SAFException.class)
    public void createCertificateCredential_ZeroLengthCert() throws Exception {
        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();
        final byte[] ecert = new byte[0];
        final byte[] outputUsername = new byte[8];;
        final SAFServiceResult safResult = new SAFServiceResult();
        final byte[] audit_ebc = (DEFAULT_CERTIFICATE_AUDIT_STRING + "\0").getBytes("Cp1047");

        mockery.checking(new Expectations() {
            {
                exactly(1).of(x509mock).getEncoded();
                will(returnValue(new byte[] {}));

                oneOf(scsmock).ntv_createCertificateCredential(ecert,
                                                               ecert.length,
                                                               audit_ebc,
                                                               pp0_ebc,
                                                               outputUsername,
                                                               safResult.getBytes());
                will(returnValue(null));
            }
        });

        safCredentials.createCertificateCredential(x509mock, null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl#modify().
     */
    @Test
    public void modify_flushPenaltyBoxCache() throws Exception {

        profilePrefix = "TEST";

        SAFCredentialsServiceImpl safCredentials = createSAFCredentialsService();

        // Change the profilePrefix.
        profilePrefix = "PROD";

        // Since the profilePrefix changed, modify should drive ntv_flushPenaltyBoxCache.
        mockery.checking(new Expectations() {
            {
                oneOf(scsmock).ntv_flushPenaltyBoxCache();
                oneOf(scsmock).ntv_setPenaltyBoxDefaults(false);
            }
        });

        safCredentials.modify(config);
    }

    @Test
    public void isSubjectValid() {
        createMockEnv();
        SAFCredentialsServiceImpl provider = new SAFCredentialsServiceImpl();
        assertTrue("This method is hardcoded to return true",
                   provider.isSubjectValid(new Subject()));
    }

    /**
     *
     */
    @Test
    public void testFirstNotNull() {
        createMockEnv();
        SAFCredentialsServiceImpl saf = new SAFCredentialsServiceImpl();

        assertEquals(null, saf.firstNotNull(null, null, null));
        assertEquals("str1", saf.firstNotNull("str1", "str2"));
        assertEquals("str2", saf.firstNotNull(null, "str2"));
    }

    // Various userIds in EBCDIC.
    private final byte[] id0_ebc = null;
    private final String id1_str = "user1";
    private final byte[] id1_up_ebc = new byte[] { -28, -30, -59, -39, -15, 0 }; // "USER1" in EBCDIC (hex.e4.e2.c5.d9.f1.00)
    private final String id2_str = "RSTID1";
    private final byte[] id2_ebc = new byte[] { -39, -30, -29, -55, -60, -15, 0 }; // "RSTID1" in EBCDIC (hex.d9.e2.e3.c9.c4.f1.00)
    private final String id3_str = "rst id3";
    private final byte[] id3_ebc = new byte[] { -103, -94, -93, 64, -119, -124, -13, 0 }; // "rst id3" in EBCDIC (hex.99.a2.a3.40.89.84.f3.00)
    private final byte[] id3_up_ebc = new byte[] { -39, -30, -29, 64, -55, -60, -13, 0 }; // "RST ID3" in EBCDIC (hex.d9.e2.e3.40.c9.c4.f3.00)
    private final String id4_str = "RST\0ID4";
    private final byte[] id4_ebc = new byte[] { -39, -30, -29, 0, -55, -60, -12, 0 }; // "RST\0ID4" in EBCDIC (hex.d9.e2.e3.00.c9.c4.f4.00)
    private final String id5_str = "        ";
    private final byte[] id5_ebc = new byte[] { 64, 64, 64, 64, 64, 64, 64, 64, 0 }; // "        " in EBCDIC (hex.40.40.40.40.40.40.40.40.00)
    private final String id6_str = "";
    private final byte[] id6_ebc = new byte[] { 0 }; // "" in EBCDIC (hex.00)
    private final String id7_str = "!@#$%^&*";
    private final byte[] id7_ebc = new byte[] { 90, 124, 123, 91, 108, 95, 80, 92, 0 }; // "!@#$%^&*" in EBCDIC (hex.5a.7c.7b.5b.6c.5f.50.5c.00)
    private final String id8_str = "LONGUSERID";
    private final byte[] id8_ebc = new byte[] { -45, -42, -43, -57, -28, -30, -59, -39, -55, -60, 0 }; // "LONGUSERID" in EBCDIC (hex.d3.d6.d5.c7.e4.e2.c5.d9.c9.c4.00)

    // Various passwords in EBCDIC.
    private final String p0_str = "pwd123usr";
    private final byte[] p0_ebc = new byte[] { -105, -90, -124, -15, -14, -13, -92, -94, -103, 0 }; // "pwd123usr" in EBCDIC (hex.97.a6.84.f1.f2.f3.a4.a2.99.00)
    private final byte[] p0_up_ebc = new byte[] { -41, -26, -60, -15, -14, -13, -28, -30, -39, 0 }; // "PWD123USR" in EBCDIC (hex.d7.e6.c4.f1.f2.f3.e4.e2.d9.00)

    private final String p1_str = "trailsp  ";
    private final byte[] p1_ebc = new byte[] { -93, -103, -127, -119, -109, -94, -105, 64, 64, 0 }; // "trailsp  " in EBCDIC (hex.a3.99.81.89.93.a2.97.40.40.00)
    private final String p1_up_str = "TRAILSP  ";
    private final byte[] p1_up_ebc = new byte[] { -29, -39, -63, -55, -45, -30, -41, 64, 64, 0 }; // "TRAILSP  " in EBCDIC (hex.a3.99.81.89.93.a2.97.40.40.00)
    private final String p2_str = "  leadsp";
    private final byte[] p2_ebc = new byte[] { 64, 64, -109, -123, -127, -124, -94, -105, 0 }; // "  leadsp" in EBCDIC (hex.40.40.93.85.81.84.a2.97.00)
    private final String p3_str = "emb space";
    private final byte[] p3_ebc = new byte[] { -123, -108, -126, 64, -94, -105, -127, -125, -123, 0 }; // "emb space" in EBCDIC (hex.85.94.82.40.a2.97.81.83.85.00)
    private final String p4_str = "        ";
    private final byte[] p4_ebc = new byte[] { 64, 64, 64, 64, 64, 64, 64, 64, 0 }; // "        " in EBCDIC (hex.40.40.40.40.40.40.40.40.00)
    private final String p5_str = "";
    private final byte[] p5_ebc = new byte[] { 0 }; // "" in EBCDIC (hex.00)
    private final String p6_str = "!@#$%^&*";
    private final byte[] p6_ebc = new byte[] { 90, 124, 123, 91, 108, 95, 80, 92, 0 }; // "!@#$%^&*" in EBCDIC (hex.5a.7c.7b.5b.6c.5f.50.5c.00)
    private final String p7_str = "emb\0null";
    private final byte[] p7_ebc = new byte[] { -123, -108, -126, 0, -107, -92, -109, -109, 0 }; // "emb\0null" in EBCDIC (hex.85.94.82.00.95.a4.93.93.00)
    private final String p8_str = "mIxEdCaSe";
    private final byte[] p8_ebc = new byte[] { -108, -55, -89, -59, -124, -61, -127, -30, -123, 0 }; // "mIxEdCaSe" in EBCDIC (hex.94.c9.a7.c5.84.c3.81.e2.85.00)
    private final String p9_str = "mIxEdCaS";
    private final byte[] p9_ebc = new byte[] { -108, -55, -89, -59, -124, -61, -127, -30, 0 }; // 8char length "mIxEdCaS" in EBCDIC (hex.94.c9.a7.c5.84.c3.81.e2.00)
    private final byte[] p9_up_ebc = new byte[] { -44, -55, -25, -59, -60, -61, -63, -30, 0 }; // 8char length "MIXEDCAS" in EBCDIC (hex.94.c9.a7.c5.84.c3.81.e2.00)

    private final String p10_str = "mIxEdCa";
    private final byte[] p10_ebc = new byte[] { -108, -55, -89, -59, -124, -61, -127, 0 }; // 7char length "mIxEdCa" in EBCDIC (hex.94.c9.a7.c5.84.c3.81.e2.00)
    private final byte[] p10_up_ebc = new byte[] { -44, -55, -25, -59, -60, -61, -63, 0 }; // 7char length "MIXEDCA" in EBCDIC (hex.94.c9.a7.c5.84.c3.81.e2.00)

    // Profile prefixes.
    private final static String pp0_str = "BBGZDFLT";
    private final byte[] pp0_ebc = new byte[] { -62, -62, -57, -23, -60, -58, -45, -29, 0 }; // "BBGZDFLT" in EBCDIC (hex.C2.C2.C7.E9.C4.C6.D3.E3.00)

    //saf result bytes containing return codes
    private final byte[] safResultBytes = new SAFServiceResult().getBytes();

    // audit strings
    private final String a0_str = null;
    private final String a1_str = "AUDIT";
    private final byte[] a1_ebc = new byte[] { -63, -28, -60, -55, -29, 0 }; // "AUDIT" in EBCDIC (hex.c1.e4.c4.c9.e3.00)
    private final String a2_str = "WebSphere Userid/Password Login";
    private final byte[] a2_ebc = new byte[] { -26, -123, -126, -30, -105, -120, -123, -103, -123, 64, -28, -94, -123, -103, -119, -124, 97, -41, -127, -94, -94, -90, -106, -103,
                                               -124, 64, -45, -106, -121, -119, -107, 0 }; // "WebSphere Userid/Password Login" in EBCDIC (hex.e6.85.82.e2.97.88.85.99.85.40.e4.a2.85.99.89.84.61.d7.81.a2.a2.a6.96.99.84.40.d3.96.87.89.95.00)

    private final String a3_str = "WebSphere Authorized Login";
    private final byte[] a3_ebc = new byte[] { -26, -123, -126, -30, -105, -120, -123, -103, -123, 64, -63, -92, -93, -120, -106, -103, -119, -87, -123, -124, 64, -45, -106, -121,
                                               -119, -107, 0 }; // "WebSphere Authorized Login" in EBCDIC (hex.e6.85.82.e2.97.88.85.99.85.40.c1.a4.a3.88.96.99.89.a9.85.84.40.d3.96.87.89.95.00)
    private final String a4_str = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private final byte[] a4_ebc = new byte[] { -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63,
                                               -63, -63,
                                               -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63,
                                               -63, -63,
                                               -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63,
                                               -63, -63,
                                               -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63,
                                               -63, -63,
                                               -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63,
                                               -63, -63,
                                               -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63,
                                               -63, -63,
                                               -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63,
                                               -63, -63,
                                               -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63,
                                               -63, -63,
                                               -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63, -63,
                                               -63, -63,
                                               -63, -63, -63, -63, 0 }; // "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" in EBCDIC (hex.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.c1.00)

    private final static String DEFAULT_CERTIFICATE_AUDIT_STRING = "WebSphere Certificate Login";

}
