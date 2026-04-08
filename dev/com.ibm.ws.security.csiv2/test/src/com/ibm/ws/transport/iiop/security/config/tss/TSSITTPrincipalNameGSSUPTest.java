/*
 * Copyright 2014, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.transport.iiop.security.config.tss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA.Any;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.Object;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CSI.GSS_NT_ExportedNameHelper;
import org.omg.CSI.ITTPrincipalName;
import org.omg.CSI.IdentityToken;
import org.omg.GSSUP.GSSUPMechOID;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.TypeMismatch;

import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.credentials.wscred.WSCredentialImpl;
import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.ws.transport.iiop.security.util.Util;

public class TSSITTPrincipalNameGSSUPTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private TSSITTPrincipalNameGSSUP tssITTPrincipalNameGSSUP;
    private final String realmName = "testRealm";
    private Authenticator authenticator;
    private IdentityToken identityToken;
    private Codec codec;
    private final Subject authenticatedSubject = new Subject();
    private final String principalName = "user1";
    private final byte[] principalNameEncoding = Util.encodeGSSExportName(GSSUPMechOID.value, principalName);

    @Before
    public void setUp() throws Exception {
        codec = mockery.mock(Codec.class);
        authenticator = mockery.mock(Authenticator.class);
        identityToken = new IdentityToken();
        identityToken.principal_name(principalNameEncoding);
        tssITTPrincipalNameGSSUP = new TSSITTPrincipalNameGSSUP(authenticator, realmName);
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSITTPrincipalNameGSSUP#getType()}.
     */
    @Test
    public void testGetType() {
        assertEquals("The token type must be set.", ITTPrincipalName.value, tssITTPrincipalNameGSSUP.getType());
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSITTPrincipalNameGSSUP#getOID()}.
     */
    @Test
    public void testGetOID() {
        assertEquals("The token OID must be set.", GSSUPMechOID.value.substring(4), tssITTPrincipalNameGSSUP.getOID());
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSITTPrincipalNameGSSUP#check(org.omg.CSI.IdentityToken, org.omg.IOP.Codec)}.
     */
    @Test
    public void testCheck() throws Exception {
        createDecodingExpectations();
        createAuthenticatorExpectations(new WSCredentialImpl("realmName", "user1", "user1UniqueSecurityName", "unauthenticated", "group", "user1AccessId", null, null));
        Subject identityAssertionSubject = tssITTPrincipalNameGSSUP.check(identityToken, codec);

        assertEquals("There must be an authenticated subject.", authenticatedSubject, identityAssertionSubject);
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSITTPrincipalNameGSSUP#check(org.omg.CSI.IdentityToken, org.omg.IOP.Codec)}.
     */
    @Test(expected = SASException.class)
    public void testCheckWithDecodingException() throws Exception {
        createDecodingExpectationsThrowsException();
        tssITTPrincipalNameGSSUP.check(identityToken, codec);

        fail("The check must throw a SASException when there is a decoding failure.");
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSITTPrincipalNameGSSUP#check(org.omg.CSI.IdentityToken, org.omg.IOP.Codec)}.
     */
    @Test
    public void testCheckWithAuthenticationExceptionThrowsException() throws Exception {
        createDecodingExpectations();
        createAuthenticatorExpectationsThrowsException();

        try {
            tssITTPrincipalNameGSSUP.check(identityToken, codec);
            fail("The check method must fail with a SASException.");
        } catch (Exception e) {
            assertSASException(e);
        }
    }

    @Test
    public void testToString() {
        String theToString = tssITTPrincipalNameGSSUP.toString();
        assertTrue("The toString must start with \"TSSITTPrincipalNameGSSUP: [\".",
                   theToString.startsWith("TSSITTPrincipalNameGSSUP: ["));
        assertTrue("The toString must contain the domain name.",
                   theToString.contains("domain: " + realmName));
        assertTrue("The toString must contain the realm name.",
                   theToString.contains("realm: " + realmName));
    }

    private void createDecodingExpectations() throws FormatMismatch, TypeMismatch {
        final Any anyObject = mockery.mock(Any.class);
        final InputStream inputStream = new InputStreamTestDouble();
        mockery.checking(new Expectations() {
            {
                one(codec).decode_value(principalNameEncoding, GSS_NT_ExportedNameHelper.type());
                will(returnValue(anyObject));
                one(anyObject).create_input_stream();
                will(returnValue(inputStream));
            }
        });
    }

    private void createDecodingExpectationsThrowsException() throws FormatMismatch, TypeMismatch {
        mockery.checking(new Expectations() {
            {
                one(codec).decode_value(principalNameEncoding, GSS_NT_ExportedNameHelper.type());
                will(throwException(new FormatMismatch()));
            }
        });
    }

    private void createAuthenticatorExpectations(WSCredential wsCredential) throws AuthenticationException {
        authenticatedSubject.getPublicCredentials().add(wsCredential);
        mockery.checking(new Expectations() {
            {
                one(authenticator).authenticate(principalName);
                will(returnValue(authenticatedSubject));
            }
        });
    }

    private void createAuthenticatorExpectationsThrowsException() throws AuthenticationException {
        mockery.checking(new Expectations() {
            {
                one(authenticator).authenticate(principalName);
                will(throwException(new AuthenticationException("")));
            }
        });
    }

    private void assertSASException(Exception e) {
        assertTrue("The exception thrown must be a SASException.", e instanceof SASException);
        SASException sasException = (SASException) e;
        assertEquals("The major code must be set.", 1, sasException.getMajor());
        assertEquals("The minor code must be set.", 1, sasException.getMinor());
        assertTrue("The cause exception must be a NO_PERMISSION exception.", sasException.getCause() instanceof NO_PERMISSION);
    }

    class InputStreamTestDouble extends InputStream {

        @Override
        public Object read_Object() {
            return null;
        }

        @Override
        public TypeCode read_TypeCode() {
            return null;
        }

        @Override
        public Any read_any() {
            return null;
        }

        @Override
        public boolean read_boolean() {
            return false;
        }

        @Override
        public void read_boolean_array(boolean[] value, int offset, int length) {}

        @Override
        public char read_char() {
            return 0;
        }

        @Override
        public void read_char_array(char[] value, int offset, int length) {}

        @Override
        public double read_double() {
            return 0;
        }

        @Override
        public void read_double_array(double[] value, int offset, int length) {}

        @Override
        public float read_float() {
            return 0;
        }

        @Override
        public void read_float_array(float[] value, int offset, int length) {}

        @Override
        public int read_long() {
            return principalNameEncoding.length;
        }

        @Override
        public void read_long_array(int[] value, int offset, int length) {}

        @Override
        public long read_longlong() {
            return 0;
        }

        @Override
        public void read_longlong_array(long[] value, int offset, int length) {}

        @Override
        public byte read_octet() {
            return 0;
        }

        @Override
        public void read_octet_array(byte[] value, int offset, int length) {
            for (int i = 0; i < length; i++) {
                value[i + offset] = principalNameEncoding[i];
            }
        }

        @Override
        public short read_short() {
            return 0;
        }

        @Override
        public void read_short_array(short[] value, int offset, int length) {}

        @Override
        public String read_string() {
            return null;
        }

        @Override
        public int read_ulong() {
            return 0;
        }

        @Override
        public void read_ulong_array(int[] value, int offset, int length) {}

        @Override
        public long read_ulonglong() {
            return 0;
        }

        @Override
        public void read_ulonglong_array(long[] value, int offset, int length) {}

        @Override
        public short read_ushort() {
            return 0;
        }

        @Override
        public void read_ushort_array(short[] value, int offset, int length) {}

        @Override
        public char read_wchar() {
            return 0;
        }

        @Override
        public void read_wchar_array(char[] value, int offset, int length) {}

        @Override
        public String read_wstring() {
            return null;
        }

    }
}
