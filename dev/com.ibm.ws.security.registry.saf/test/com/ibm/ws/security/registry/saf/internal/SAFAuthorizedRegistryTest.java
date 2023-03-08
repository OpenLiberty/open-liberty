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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.security.cert.X509Certificate;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JMock;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.credentials.saf.internal.SAFCredentialsServiceImpl;
import com.ibm.ws.security.registry.CertificateMapFailedException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.saf.SAFException;
import com.ibm.ws.security.saf.SAFServiceResult;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.wsspi.security.credentials.saf.SAFCredential;

@RunWith(JMock.class)
public class SAFAuthorizedRegistryTest extends SAFRegistryTest {

    /**
     * NativeMethodManager for the the unit test.
     */
    protected NativeMethodManager _nativeMethodManager = null;

    /**
     * The mocked SAFCredentialsServiceImpl. This mock object handles all of SAFRegistry's
     * native methods.
     */
    protected SAFCredentialsServiceImpl _safCredentialsServiceImpl = null;

    /**
     * A mocked SAFCredential.
     */
    protected SAFCredential _safCred = null;

    /**
     * Create the mock object. Each mock object created by this test
     * needs to have a unique name. Just use a simple counter to
     * ensure uniqueness.
     *
     * @throws SAFException
     */
    @Override
    protected void createMockEnv() {
        super.createMockEnv();
        _safCredentialsServiceImpl = mockery.mock(SAFCredentialsServiceImpl.class, "SAFCredentialsServiceImpl" + uniqueMockNameCount);
        _nativeMethodManager = mockery.mock(NativeMethodManager.class, "NativeMethodManager" + uniqueMockNameCount);
        _safCred = mockery.mock(SAFCredential.class, "SAFCredential" + uniqueMockNameCount);
    }

    /**
     * Create the SAFRegistry object for the unit test.
     */
    @Override
    protected SAFRegistry createSAFRegistry() {
        createMockEnv();

        // Add NativeMethodManager.registerNatives, which is called by the SAFRegistry CTOR.
        mockery.checking(new Expectations() {
            {
                oneOf(_nativeMethodManager).registerNatives(with(equal(SAFRegistry.class)));
            }
        });

        return new SAFAuthorizedRegistryMockNative(config, _nativeMethodManager, _safCredentialsServiceImpl);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getGroupsForUser(String)}.
     * This tests the behavior when an unknown user is given. EntryNotFoundException is expected.
     */
    @Override
    @Test(expected = EntryNotFoundException.class)
    public void getGroupsForUser_unknownUser() throws Exception {
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();
        final SAFServiceResult safResult = new SAFServiceResult();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_getGroupsForUser(with(equal(TD.u1_ebc)), with(any(List.class)));
                will(returnValue(null));
                oneOf(_safCredentialsServiceImpl).createAssertedCredential(TD.u1_str, null, 1);
                will(throwException(new SAFException(safResult)));
            }
        });

        safAuthorizedRegistry.getGroupsForUser(TD.u1_str);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getUserDisplayName(String)}.
     */
    @Override
    @Test(expected = EntryNotFoundException.class)
    public void getUserDisplayName_coverage() throws Exception {
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();
        final SAFServiceResult safResult = new SAFServiceResult();

        mockery.checking(new Expectations() {
            {
                oneOf(_safCredentialsServiceImpl).createAssertedCredential(TD.u2_str, null, 1);
                will(throwException(new SAFException(safResult)));
            }
        });

        safAuthorizedRegistry.getUserDisplayName(TD.u2_str); // should throw EntryNotFoundException.
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getUserSecurityName(String)}.
     */
    @Override
    @Test(expected = EntryNotFoundException.class)
    public void getUserSecurityName_coverage() throws Exception {
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();
        final SAFServiceResult safResult = new SAFServiceResult();

        mockery.checking(new Expectations() {
            {
                oneOf(_safCredentialsServiceImpl).createAssertedCredential(TD.u1_str, null, 1);
                will(returnValue(_safCred));
                oneOf(_safCredentialsServiceImpl).deleteCredential(_safCred);
                oneOf(_safCredentialsServiceImpl).createAssertedCredential(TD.u2_str, null, 1);
                will(throwException(new SAFException(safResult)));
            }
        });

        assertTrue(safAuthorizedRegistry.getUserSecurityName(TD.u1_str).equals(TD.u1_str));
        safAuthorizedRegistry.getUserSecurityName(TD.u2_str); // should throw EntryNotFoundException.
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getUniqueUserId(String)}.
     */
    @Override
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueUserId_coverage() throws Exception {
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();
        final SAFServiceResult safResult = new SAFServiceResult();

        mockery.checking(new Expectations() {
            {
                oneOf(_safCredentialsServiceImpl).createAssertedCredential(TD.u1_str, null, 1);
                will(returnValue(_safCred));
                oneOf(_safCredentialsServiceImpl).deleteCredential(_safCred);
                oneOf(_safCredentialsServiceImpl).createAssertedCredential(TD.u2_str, null, 1);
                will(throwException(new SAFException(safResult)));
            }
        });

        assertTrue(safAuthorizedRegistry.getUniqueUserId(TD.u1_str).equals(TD.u1_str));
        safAuthorizedRegistry.getUniqueUserId(TD.u2_str); // should throw EntryNotFoundException.
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFAuthorizedRegistry#getRealm()}.
     * Test realm defined in config.
     * This test simulates:
     * 1) Server.xml: <safRegistry id="saf" realm="safTestRealm"/>
     */
    @Override
    @Test
    public void getRealm_testBasic() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        assertTrue("Should return safTestRealm", safRegistry.getRealm().equals(_realm));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getRealm()}.
     * Test handling of ntv_getRealm data, which is called when no realm is defined
     * in the config.
     * This test simulates:
     * 1) Server.xml: <safRegistry id="saf" realm=""/>
     * 2) Assumes that the APPLDATA field in the SAFDFLT profile under the REALM class is found..
     * This should result in the APPLDATA is being returned.
     */
    @Override
    @Test
    public void getRealm_noRealmInConfig() throws Exception {
        _realm = ""; //SAFRegistryConfig.realm()
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_getRealm();
                will(returnValue(TD.r1_ebc));
            }
        });

        assertTrue("Should return NTVREALM", safAuthorizedRegistry.getRealm().equals(TD.r1_str));
    }

    /*
     * This test simulates:
     * 1) Server.xml: <safRegistry id="saf"/>
     * 2) Assumes that the APPLDATA field in the SAFDFLT profile under the REALM class is found.
     * This should result in the APPLDATA being returned.
     */
    @Override
    @Test
    public void getRealm_nullRealmInConfig() throws Exception {
        _realm = null; //SAFRegistryConfig.realm()
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_getRealm();
                will(returnValue(TD.r1_ebc));
            }
        });

        assertTrue("Should return NTVREALM", safAuthorizedRegistry.getRealm().equals(TD.r1_str));
    }

    /*
     * This test simulates:
     * 1) Server.xml: <safRegistry id="saf"/>
     * 2) Assumes that the APPLDATA field in the SAFDFLT profile under the REALM class is NOT found.
     * This should result in the Plexname being returned.
     */
    @Test
    public void getRealm_noAppldataInRACF() throws Exception {
        _realm = null; //SAFRegistryConfig.realm()
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_getRealm();
                will(returnValue(TD.r3_ebc));
                atLeast(1).of(mock).ntv_getPlexName();
                will(returnValue(TD.r2_ebc));
            }
        });

        assertTrue("Should return NTVPLEXNAME", safAuthorizedRegistry.getRealm().equals(TD.r2_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistryFactory.getUserRegistry()}.
     * Coverage test for SAFRegistryFactory.getUserRegistry. Testing that getUserRegistry
     * returns a non-null UserRegistry.
     */
    @Override
    @Test
    public void SAFRegistryFactory_getUserRegistry_coverage() throws Exception {
        createMockEnv();

        // Set up Expectations of method calls for the mock NativeMethodManager.
        mockery.checking(new Expectations() {
            {
                oneOf(_nativeMethodManager).registerNatives(with(equal(SAFRegistry.class)));
            }
        });

        SAFDelegatingUserRegistry ur = new SAFDelegatingUserRegistry();
        ur.nativeMethodManager = _nativeMethodManager;
        ur.safCredentialsService = _safCredentialsServiceImpl;

        ur.activate(config);

        assertNotNull(ur.delegate);

        assertTrue(ur.delegate instanceof SAFAuthorizedRegistry);
    }

    /**
     * NO-OP test for SAFAuthorizedRegistry.
     */
    @Override
    public void checkPassword_variousPasswords() throws Exception {
    }

    /**
     * NO-OP test for SAFAuthorizedRegistry.
     */
    @Override
    public void checkPassword_variousUserids() throws Exception {
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFAuthorizedRegistry#checkPassword(String, String)}.
     * Test good credentials.
     */
    @Test
    public void checkPassword_nonNullSAFCredential() throws Exception {
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();

        mockery.checking(new Expectations() {
            {
                oneOf(_safCredentialsServiceImpl).createPasswordCredential(TD.u1_str, TD.p9_str, null);
                will(returnValue(_safCred));
                oneOf(_safCredentialsServiceImpl).getSAFCredentialTokenKey(_safCred);
                will(returnValue(null));
            }
        });
        assertEquals("checkPassword(" + TD.u1_str + ",'" + TD.p9_str + "')", TD.u1_str, safAuthorizedRegistry.checkPassword(TD.u1_str, TD.p9_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFAuthorizedRegistry#checkPassword(String, String)}.
     * Test bad credentials.
     */
    @Test
    public void checkPassword_nullSAFCredential() throws Exception {
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();
        mockery.checking(new Expectations() {
            {
                oneOf(_safCredentialsServiceImpl).createPasswordCredential(TD.u1_str, TD.p8_str, null);
                will(returnValue(null));
            }
        });
        assertNull("!checkPassword(" + TD.u1_str + ",'" + TD.p8_str + "')", safAuthorizedRegistry.checkPassword(TD.u1_str, TD.p8_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFAuthorizedRegistry#checkPassword(String, String)}.
     * Force a SAFException in createPasswordCredential
     */
    @Test(expected = RegistryException.class)
    public void checkPassword_SAFException() throws Exception {
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();
        mockery.checking(new Expectations() {
            {
                oneOf(_safCredentialsServiceImpl).createPasswordCredential(TD.u3_str, TD.p9_str, null);
                will(throwException(new SAFException("SAFException for unit test")));
            }
        });

        safAuthorizedRegistry.checkPassword(TD.u3_str, TD.p9_str);
    }

    /**
     * NO-OP test for SAFAuthorizedRegistry.
     */
    @Override
    public void isValidUser_variousUsers() throws Exception {
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFAuthorizedRegistry#isValidUser(String)}.
     * isValidUser shall return true if the user exists in the registry.
     */
    @Test
    public void isValidUser_validUser() throws Exception {
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();
        mockery.checking(new Expectations() {
            {
                oneOf(_safCredentialsServiceImpl).createAssertedCredential(TD.u1_str, null, 1);
                will(returnValue(_safCred));
                oneOf(_safCredentialsServiceImpl).deleteCredential(_safCred);
            }
        });

        assertTrue("isValidUser(" + TD.u1_str + ")", safAuthorizedRegistry.isValidUser(TD.u1_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFAuthorizedRegistry#isValidUser(String)}.
     * isValidUser shall return true if the user exists in the registry.
     */
    @Test
    public void isValidUser_invalidUser() throws Exception {
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();
        final SAFServiceResult safResult = new SAFServiceResult();
        mockery.checking(new Expectations() {
            {
                oneOf(_safCredentialsServiceImpl).createAssertedCredential(TD.u2_str, null, 1);
                will(throwException(new SAFException(safResult)));
            }
        });
        assertFalse("!isValidUser(" + TD.u2_str + ")", safAuthorizedRegistry.isValidUser(TD.u2_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFAuthorizedRegistry#isValidUser(String)}.
     * Test valid userSecurityName that contains safCredTokenKey.
     */
    @Test
    public void isValidUser_valdSafCredTokenKey() throws Exception {
        final String safCredTokenKey = "blahblahblah";
        String userSecurityName = "mstone1::" + safCredTokenKey;
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();
        mockery.checking(new Expectations() {
            {
                oneOf(_safCredentialsServiceImpl).getCredentialFromKey(safCredTokenKey);
                will(returnValue(_safCred));
            }
        });

        assertTrue("isValidUser(" + userSecurityName + ")", safAuthorizedRegistry.isValidUser(userSecurityName));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFAuthorizedRegistry#isValidUser(String)}.
     * Test valid userSecurityName that contains an invalid safCredTokenKey.
     */
    @Test
    public void isValidUser_invalidSafCredTokenKey() throws Exception {
        final String safCredTokenKey = "blahblahblah";
        String userSecurityName = "mstone1::" + safCredTokenKey;
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();
        mockery.checking(new Expectations() {
            {
                oneOf(_safCredentialsServiceImpl).getCredentialFromKey(safCredTokenKey);
                will(returnValue(null));
            }
        });

        assertFalse("isValidUser(" + userSecurityName + ")", safAuthorizedRegistry.isValidUser(userSecurityName));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFAuthorizedRegistry#isValidUser(String)}.
     * isValidUser shall return true if the user exists in the registry.
     */
    @Test(expected = RegistryException.class)
    public void isValidUser_SAFException() throws Exception {
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();
        mockery.checking(new Expectations() {
            {
                oneOf(_safCredentialsServiceImpl).createAssertedCredential(TD.u3_str, null, 1);
                will(throwException(new SAFException("SAFException for unit test")));
            }
        });

        assertFalse("!isValidUser(" + TD.u3_str + ")", safAuthorizedRegistry.isValidUser(TD.u3_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFAuthorizedRegistry#mapCertificate(X509Certificate)}.
     * Test the business as usual path.
     */
    @Override
    @Test
    public void mapCertificate_test() throws Exception {
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();
        final X509Certificate cert = mockery.mock(X509Certificate.class, "X509Certificate" + uniqueMockNameCount++);

        mockery.checking(new Expectations() {
            {
                oneOf(_safCredentialsServiceImpl).createCertificateCredential(cert, null);
                will(returnValue(_safCred));
                oneOf(_safCred).getUserId();
                will(returnValue(TD.u1_str));
                oneOf(_safCredentialsServiceImpl).getSAFCredentialTokenKey(_safCred);
                will(returnValue(null));
            }
        });

        assertTrue(safAuthorizedRegistry.mapCertificate(new X509Certificate[] { cert }).equals(TD.u1_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFAuthorizedRegistry#mapCertificate(X509Certificate)}.
     * test CertificateMapFailedExcpetion path when SAFCredentialsService.createCertificateCredential returns null
     */
    @Test(expected = CertificateMapFailedException.class)
    public void mapCertificate_testNullFromSAFCredentialsService() throws Exception {
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();
        final X509Certificate cert = mockery.mock(X509Certificate.class, "X509Certificate" + uniqueMockNameCount++);

        mockery.checking(new Expectations() {
            {
                oneOf(_safCredentialsServiceImpl).createCertificateCredential(cert, null);
                will(returnValue(null));
            }
        });

        safAuthorizedRegistry.mapCertificate(new X509Certificate[] { cert });
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFAuthorizedRegistry#mapCertificate(X509Certificate)}.
     * test CertificateMapFailedExcpetion path when SAFCredentialsService.createCertificateCredential throws a SAFException
     */
    @Test(expected = CertificateMapFailedException.class)
    public void mapCertificate_testSAFException() throws Exception {
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();
        final X509Certificate cert = mockery.mock(X509Certificate.class, "X509Certificate" + uniqueMockNameCount++);

        mockery.checking(new Expectations() {
            {
                oneOf(_safCredentialsServiceImpl).createCertificateCredential(cert, null);
                will(throwException(new SAFException("testing SAFException")));
            }
        });

        safAuthorizedRegistry.mapCertificate(new X509Certificate[] { cert });
    }

    /**
     * NO-OP test for SAFAuthorizedRegistry.
     */
    @Override
    @Test
    public void mapCertificate_testMapFail() throws Exception {
        createMockEnv();
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFAuthorizedRegistry#isValidGroup(String)}.
     * Test various groupIds.
     */
    @Override
    @Test(expected = IllegalArgumentException.class)
    public void isValidGroup_variousGroups() throws Exception {
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();
        final SAFServiceResult safResult = new SAFServiceResult();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                String safProfile = allowing(_safCredentialsServiceImpl).getProfilePrefix();
                will(returnValue(safProfile));
                oneOf(mock).ntv_isValidGroupAuthorized(with(equal(TD.g1_ebc)), with(equal(TD.empty_ebc)), with(equal(safResult.getBytes())));
                will(returnValue(true));
                oneOf(mock).ntv_isValidGroupAuthorized(with(equal(TD.g2_ebc)), with(equal(TD.empty_ebc)), with(equal(safResult.getBytes())));
                will(returnValue(true));
                oneOf(mock).ntv_isValidGroupAuthorized(with(equal(TD.g3_ebc)), with(equal(TD.empty_ebc)), with(equal(safResult.getBytes())));
                will(returnValue(true));
                oneOf(mock).ntv_isValidGroupAuthorized(with(equal(TD.g4_ebc)), with(equal(TD.empty_ebc)), with(equal(safResult.getBytes())));
                will(returnValue(true));
                oneOf(mock).ntv_isValidGroupAuthorized(with(equal(TD.g5_ebc)), with(equal(TD.empty_ebc)), with(equal(safResult.getBytes())));
                will(returnValue(false));
                oneOf(mock).ntv_isValidGroupAuthorized(with(equal(TD.g6_ebc)), with(equal(TD.empty_ebc)), with(equal(safResult.getBytes())));
                will(returnValue(false));
            }
        });

        assertTrue(safAuthorizedRegistry.isValidGroup(TD.g1_str));
        assertTrue(safAuthorizedRegistry.isValidGroup(TD.g2_str));
        assertTrue(safAuthorizedRegistry.isValidGroup(TD.g3_str));
        assertTrue(safAuthorizedRegistry.isValidGroup(TD.g4_str));
        assertFalse(safAuthorizedRegistry.isValidGroup(TD.g5_str));
        // check that isGroupValid capitalizes entire group name
        assertFalse(safAuthorizedRegistry.isValidGroup(TD.g6_str.toLowerCase()));
        safAuthorizedRegistry.isValidGroup(null); // should throw IllegalArgumentException
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFAuthorizedRegistry#getGroupDisplayName(String)}.
     */
    @Override
    @Test(expected = EntryNotFoundException.class)
    public void getGroupDisplayName_coverage() throws Exception {
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();
        final SAFServiceResult safResult = new SAFServiceResult();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                String safProfile = allowing(_safCredentialsServiceImpl).getProfilePrefix();
                will(returnValue(safProfile));
                oneOf(mock).ntv_isValidGroupAuthorized(with(equal(TD.g1_ebc)), with(equal(TD.empty_ebc)), with(equal(safResult.getBytes())));
                will(returnValue(true));
                oneOf(mock).ntv_isValidGroupAuthorized(with(equal(TD.g2_ebc)), with(equal(TD.empty_ebc)), with(equal(safResult.getBytes())));
                will(returnValue(false));
            }
        });

        assertTrue(safAuthorizedRegistry.getGroupDisplayName(TD.g1_str).equals(TD.g1_str));
        safAuthorizedRegistry.getGroupDisplayName(TD.g2_str); // should throw EntryNotFoundException.
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFAuthorizedRegistry#getGroupSecurityName(String)}.
     */
    @Override
    @Test(expected = EntryNotFoundException.class)
    public void getGroupSecurityName_coverage() throws Exception {
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();
        final SAFServiceResult safResult = new SAFServiceResult();

        mockery.checking(new Expectations() {
            {
                String safProfile = allowing(_safCredentialsServiceImpl).getProfilePrefix();
                will(returnValue(safProfile));
                oneOf(mock).ntv_isValidGroupAuthorized(with(equal(TD.g1_ebc)), with(equal(TD.empty_ebc)), with(equal(safResult.getBytes())));
                will(returnValue(true));
                oneOf(mock).ntv_isValidGroupAuthorized(with(equal(TD.g2_ebc)), with(equal(TD.empty_ebc)), with(equal(safResult.getBytes())));
                will(returnValue(false));
            }
        });

        assertTrue(safAuthorizedRegistry.getGroupSecurityName(TD.g1_str).equals(TD.g1_str));
        safAuthorizedRegistry.getGroupSecurityName(TD.g2_str); // should throw EntryNotFoundException.
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFAuthorizedRegistry#getUniqueGroupId(String)}.
     */
    @Override
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueGroupId_coverage() throws Exception {
        SAFAuthorizedRegistry safAuthorizedRegistry = (SAFAuthorizedRegistry) createSAFRegistry();
        final SAFServiceResult safResult = new SAFServiceResult();

        mockery.checking(new Expectations() {
            {
                String safProfile = allowing(_safCredentialsServiceImpl).getProfilePrefix();
                will(returnValue(safProfile));
                oneOf(mock).ntv_isValidGroupAuthorized(with(equal(TD.g1_ebc)), with(equal(TD.empty_ebc)), with(equal(safResult.getBytes())));
                will(returnValue(true));
                oneOf(mock).ntv_isValidGroupAuthorized(with(equal(TD.g2_ebc)), with(equal(TD.empty_ebc)), with(equal(safResult.getBytes())));
                will(returnValue(false));
            }
        });

        assertTrue(safAuthorizedRegistry.getUniqueGroupId(TD.g1_str).equals(TD.g1_str));
        safAuthorizedRegistry.getUniqueGroupId(TD.g2_str); // should throw EntryNotFoundException.
    }
}
