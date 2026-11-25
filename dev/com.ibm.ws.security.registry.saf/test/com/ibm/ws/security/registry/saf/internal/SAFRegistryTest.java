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

import java.lang.annotation.Annotation;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.registry.CertificateMapFailedException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.zos.jni.NativeMethodManager;

/**
 * Unit test for SAFRegistry. The SAFRegistry requires a z/OS native environment;
 * however, this unit test is designed to run on distributed platforms. Hence,
 * the native environment is mocked using JMock.
 *
 * Since the native environment is mocked, we're not actually running these tests
 * against an actual SAF database. Instead, the mockery environment is configured by
 * each test to receive and return expected values for every native call expected
 * during the test.
 *
 * Thus, this unit test is only focused on testing the Java code - which for the most
 * part consists of:
 * (1) translating input data to EBCDIC before passing to the native methods.
 * (2) translating/massaging/filtering the data it gets back from native before
 * returning to the caller.
 */
@RunWith(JMock.class)
public class SAFRegistryTest {

    /**
     * Mock environment for native methods.
     */
    protected static Mockery mockery = null;

    /**
     * Counter for generating unique mock object names.
     */
    protected static int uniqueMockNameCount = 1;

    /**
     * The mocked SAFRegistry. This mock object handles all of SAFRegistry's
     * native methods.
     */
    protected static SAFRegistry mock = null;

    /**
     * SAF config supplied to SAFRegistry CTOR.
     */
    protected SAFRegistryConfig config = new SAFRegistryConfig() {

        @Override
        public Class<? extends Annotation> annotationType() {
            return SAFRegistryConfig.class;
        }

        @Override
        public String realm() {
            return _realm;
        }

        @Override
        public boolean enableFailover() {
            return true;
        }

        @Override
        public String[] safCredentialService() {
            return new String[] { "scs" };
        }

        @Override
        public String safCredentialService_cardinality_minimum() {
            return "1";
        }

        @Override
        public String config_id() {
            return "com.ibm.ws.security.registry.saf.config";
        }

        @Override
        public boolean reportPasswordExpired() {
            return false;
        }

        @Override
        public boolean reportUserRevoked() {
            return false;
        }

        @Override
        public boolean includeSafGroups() {
            return false;
        }

        @Override
        public boolean reportPasswordChangeDetails() {
            return false;
        }

    };

    /**
     * realm
     */
    protected String _realm = "safTestRealm";

    /**
     * Final teardown work when class is exiting.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * Create the mockery environment. Should be called by each test
     * that writes mockery expectations. Setting up a new mockery environment
     * for each test helps isolate Expectation sets, making it easier to
     * debug when some Expectation fails and all the Expectations are dumped
     * to the error log.
     */
    protected void createMockEnv() {
        mockery = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        mock = mockery.mock(SAFRegistry.class, "SAFRegistry" + uniqueMockNameCount++);
    }

    /**
     * Create a SAFRegistry and Mockery environment for the unit test.
     * The SAFRegistry impl forwards all native method invocations (ntv_*) to the
     * SAFRegistry mock object in this class (mock).
     *
     * @return The SAFRegistry impl.
     */
    protected SAFRegistry createSAFRegistry() throws Exception {
        createMockEnv();
        return new SAFRegistryMockNative(config);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFRegistry#checkPassword(String, String)}.
     * Test that various odd password strings are handled properly.
     */
    @Test
    public void checkPassword_variousPasswords() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Create expectations of the mock SAFRegistry (i.e. which native
        // methods will be invoked and what they will return).
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_checkPassword(with(equal(TD.u1_ebc)),
                                              with(equal(TD.p1_ebc)),
                                              with(any(String.class)),
                                              with(any(byte[].class)));
                will(returnValue(true));
                oneOf(mock).ntv_checkPassword(with(equal(TD.u1_ebc)),
                                              with(equal(TD.p2_ebc)),
                                              with(any(String.class)),
                                              with(any(byte[].class)));
                will(returnValue(true));
                oneOf(mock).ntv_checkPassword(with(equal(TD.u1_ebc)),
                                              with(equal(TD.p3_ebc)),
                                              with(any(String.class)),
                                              with(any(byte[].class)));
                will(returnValue(true));
                oneOf(mock).ntv_checkPassword(with(equal(TD.u1_ebc)),
                                              with(equal(TD.p4_ebc)),
                                              with(any(String.class)),
                                              with(any(byte[].class)));
                will(returnValue(true));
                oneOf(mock).ntv_checkPassword(with(equal(TD.u1_ebc)),
                                              with(equal(TD.p6_ebc)),
                                              with(any(String.class)),
                                              with(any(byte[].class)));
                will(returnValue(false));
                oneOf(mock).ntv_checkPassword(with(equal(TD.u1_ebc)),
                                              with(equal(TD.p7_ebc)),
                                              with(any(String.class)),
                                              with(any(byte[].class)));
                will(returnValue(false));
                oneOf(mock).ntv_checkPassword(with(equal(TD.u1_ebc)),
                                              with(equal(TD.p8_ebc)),
                                              with(any(String.class)),
                                              with(any(byte[].class)));
                will(returnValue(false));
                oneOf(mock).ntv_checkPassword(with(equal(TD.u1_ebc)),
                                              with(equal(TD.p9_ebc)),
                                              with(any(String.class)),
                                              with(any(byte[].class)));
                will(returnValue(false));
            }
        });

        assertEquals(TD.u1_str, safRegistry.checkPassword(TD.u1_str, TD.p1_str));
        assertEquals(TD.u1_str, safRegistry.checkPassword(TD.u1_str, TD.p2_str));
        assertEquals(TD.u1_str, safRegistry.checkPassword(TD.u1_str, TD.p3_str));
        assertEquals(TD.u1_str, safRegistry.checkPassword(TD.u1_str, TD.p4_str));
        assertNull(safRegistry.checkPassword(TD.u1_str, TD.p6_str));
        assertNull(safRegistry.checkPassword(TD.u1_str, TD.p7_str));
        assertNull(safRegistry.checkPassword(TD.u1_str, TD.p8_str));
        assertNull(safRegistry.checkPassword(TD.u1_str, TD.p9_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFRegistry#checkPassword(String, String)}.
     * Test that various odd userid strings are handled properly.
     */
    @Test
    public void checkPassword_variousUserids() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_checkPassword(with(equal(TD.id1_ebc)),
                                              with(equal(TD.p1_ebc)),
                                              with(any(String.class)),
                                              with(any(byte[].class)));
                will(returnValue(true));
                oneOf(mock).ntv_checkPassword(with(equal(TD.id2_ebc)),
                                              with(equal(TD.p1_ebc)),
                                              with(any(String.class)),
                                              with(any(byte[].class)));
                will(returnValue(true));
                oneOf(mock).ntv_checkPassword(with(equal(TD.id3_ebc)),
                                              with(equal(TD.p1_ebc)),
                                              with(any(String.class)),
                                              with(any(byte[].class)));
                will(returnValue(true));
                oneOf(mock).ntv_checkPassword(with(equal(TD.id4_ebc)),
                                              with(equal(TD.p1_ebc)),
                                              with(any(String.class)),
                                              with(any(byte[].class)));
                will(returnValue(true));
                oneOf(mock).ntv_checkPassword(with(equal(TD.id5_ebc)),
                                              with(equal(TD.p1_ebc)),
                                              with(any(String.class)),
                                              with(any(byte[].class)));
                will(returnValue(false));
                oneOf(mock).ntv_checkPassword(with(equal(TD.id7_ebc)),
                                              with(equal(TD.p1_ebc)),
                                              with(any(String.class)),
                                              with(any(byte[].class)));
                will(returnValue(false));
            }
        });

        assertEquals(TD.id1_str, safRegistry.checkPassword(TD.id1_str, TD.p1_str));
        assertEquals(TD.id2_str, safRegistry.checkPassword(TD.id2_str, TD.p1_str));
        assertEquals(TD.id3_str, safRegistry.checkPassword(TD.id3_str, TD.p1_str));
        assertEquals(TD.id4_str, safRegistry.checkPassword(TD.id4_str, TD.p1_str));
        assertNull(safRegistry.checkPassword(TD.id5_str, TD.p1_str));
        assertNull(safRegistry.checkPassword(TD.id7_str, TD.p1_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFRegistry#checkPassword(String, String)}.
     * Passing a null user shall result in an IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void checkPassword_nullUser() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();
        safRegistry.checkPassword(null, "M00NTEST");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFRegistry#checkPassword(String, String)}.
     * Passing a "" user shall result in an IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void checkPassword_emptyUser() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();
        safRegistry.checkPassword("", "M00NTEST");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFRegistry#checkPassword(String, String)}.
     * Passing a null password shall result in a IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void checkPassword_nullPassword() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();
        safRegistry.checkPassword("MSTONE1", null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFRegistry#checkPassword(String, String)}.
     * Passing a "" password shall result in a IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void checkPassword_emptyPassword() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();
        safRegistry.checkPassword("MSTONE1", "");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFRegistry#isValidUser(String)}.
     * Test various userIds.
     */
    @Test
    public void isValidUser_variousUsers() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_isValidUser(with(equal(TD.id1_ebc)));
                will(returnValue(true));
                oneOf(mock).ntv_isValidUser(with(equal(TD.id2_ebc)));
                will(returnValue(true));
                oneOf(mock).ntv_isValidUser(with(equal(TD.id3_ebc)));
                will(returnValue(true));
                oneOf(mock).ntv_isValidUser(with(equal(TD.id4_ebc)));
                will(returnValue(true));
                oneOf(mock).ntv_isValidUser(with(equal(TD.id5_ebc)));
                will(returnValue(false));
                oneOf(mock).ntv_isValidUser(with(equal(TD.id7_ebc)));
                will(returnValue(false));
            }
        });

        assertTrue(safRegistry.isValidUser(TD.id1_str));
        assertTrue(safRegistry.isValidUser(TD.id2_str));
        assertTrue(safRegistry.isValidUser(TD.id3_str));
        assertTrue(safRegistry.isValidUser(TD.id4_str));
        assertFalse(safRegistry.isValidUser(TD.id5_str));
        assertFalse(safRegistry.isValidUser(TD.id7_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFRegistry#isValidUser(String)}.
     * Passing null to isValidUser shall raise a IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void isValidUser_nullUser() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();
        safRegistry.isValidUser(null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFRegistry#isValidUser(String)}.
     * Passing "" to isValidUser shall raise a IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void isValidUser_emptyUser() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();
        safRegistry.isValidUser("");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFRegistry#isValidGroup(String)}.
     * Test various groupIds.
     */
    @Test
    public void isValidGroup_variousGroups() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_isValidGroup(with(equal(TD.id1_ebc)));
                will(returnValue(true));
                oneOf(mock).ntv_isValidGroup(with(equal(TD.id2_ebc)));
                will(returnValue(true));
                oneOf(mock).ntv_isValidGroup(with(equal(TD.id3_ebc)));
                will(returnValue(true));
                oneOf(mock).ntv_isValidGroup(with(equal(TD.id4_ebc)));
                will(returnValue(true));
                oneOf(mock).ntv_isValidGroup(with(equal(TD.id5_ebc)));
                will(returnValue(false));
                oneOf(mock).ntv_isValidGroup(with(equal(TD.id7_ebc)));
                will(returnValue(false));
            }
        });

        assertTrue(safRegistry.isValidGroup(TD.id1_str));
        assertTrue(safRegistry.isValidGroup(TD.id2_str));
        assertTrue(safRegistry.isValidGroup(TD.id3_str));
        assertTrue(safRegistry.isValidGroup(TD.id4_str));
        assertFalse(safRegistry.isValidGroup(TD.id5_str));
        assertFalse(safRegistry.isValidGroup(TD.id7_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFRegistry#isValidGroup(String)}.
     * Passing null to isValidGroup shall raise a IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void isValidGroup_nullGroup() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();
        safRegistry.isValidGroup(null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFRegistry#isValidGroup(String)}.
     * Passing "" to isValidGroup shall raise a IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void isValidGroup_emptyGroup() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();
        safRegistry.isValidGroup("");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getGroupsForUser(String)}.
     * Verify that the getGroupsForUser Java code properly translates the List<byte[]>
     * returned from native into a List<String>.
     */
    @Test
    public void getGroupsForUser_verifyGroups() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Create list of groups to be returned by the mock call to ntv_getGroupsForUser.
        final List<byte[]> ntvGroups = new ArrayList<byte[]>();
        ntvGroups.add(TD.g1_ebc);
        ntvGroups.add(TD.g2_ebc);
        ntvGroups.add(TD.g3_ebc);
        ntvGroups.add(TD.g4_ebc);
        ntvGroups.add(TD.g5_ebc);

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_getGroupsForUser(with(equal(TD.u1_ebc)), with(any(List.class)));
                will(returnValue(ntvGroups));
            }
        });

        List<String> groups = safRegistry.getGroupsForUser(TD.u1_str);

        assertTrue(groups.size() == 5);
        assertTrue(groups.contains(TD.g1_str));
        assertTrue(groups.contains(TD.g2_str));
        assertTrue(groups.contains(TD.g3_str));
        assertTrue(groups.contains(TD.g4_str));
        assertTrue(groups.contains(TD.g5_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getGroupsForUser(String)}.
     * This tests the behavior when an unknown user is given. EntryNotFoundException is expected.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupsForUser_unknownUser() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_getGroupsForUser(with(equal(TD.u1_ebc)), with(any(List.class)));
                will(returnValue(null));
                oneOf(mock).ntv_isValidUser(with(equal(TD.u1_ebc)));
                will(returnValue(false));
            }
        });

        List<String> groups = safRegistry.getGroupsForUser(TD.u1_str);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getGroupsForUser(String)}.
     * This tests the behavior for unchecked exceptions. RegistryException is expected.
     */
    @Test(expected = RegistryException.class)
    public void getGroupsForUser_uncheckedException() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_getGroupsForUser(with(equal(TD.u1_ebc)), with(any(List.class)));
                will(throwException(new RuntimeException("unchecked exception")));
            }
        });

        List<String> groups = safRegistry.getGroupsForUser(TD.u1_str);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFRegistry#getGroupsForUser(String)}.
     * Passing null to getGroupsForUser shall raise a IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getGroupsForUser_nullUser() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();
        safRegistry.getGroupsForUser(null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getUniqueGroupIdsForUser(String)}.
     * Verify that the getUniqueGroupIdsForUser Java code properly translates the List<byte[]>
     * returned from native into a List<String>.
     */
    @Test
    public void getUniqueGroupIdsForUser_verifyGroups() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Create list of groups to be returned by the mock call to ntv_getGroupsForUser.
        final List<byte[]> ntvGroups = new ArrayList<byte[]>();
        ntvGroups.add(TD.g1_ebc);
        ntvGroups.add(TD.g2_ebc);
        ntvGroups.add(TD.g3_ebc);
        ntvGroups.add(TD.g4_ebc);
        ntvGroups.add(TD.g5_ebc);

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_getGroupsForUser(with(equal(TD.u1_ebc)), with(any(List.class)));
                will(returnValue(ntvGroups));
            }
        });

        List<String> groups = safRegistry.getUniqueGroupIdsForUser(TD.u1_str);

        assertTrue(groups.size() == 5);
        assertTrue(groups.contains(TD.g1_str));
        assertTrue(groups.contains(TD.g2_str));
        assertTrue(groups.contains(TD.g3_str));
        assertTrue(groups.contains(TD.g4_str));
        assertTrue(groups.contains(TD.g5_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getGroups(String, int)}.
     * Test basic function - match all groups and no limit.
     */
    @Test
    public void getGroups_testMatchAllZeroLimit() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_resetGroupsCursor();
                will(returnValue(true));
                atLeast(1).of(mock).ntv_getNextGroup();
                will(onConsecutiveCalls(
                                        returnValue(TD.g1_ebc),
                                        returnValue(TD.g2_ebc),
                                        returnValue(TD.g3_ebc),
                                        returnValue(TD.g4_ebc),
                                        returnValue(TD.g5_ebc),
                                        returnValue(TD.g6_ebc),
                                        returnValue(TD.g7_ebc),
                                        returnValue(null)));
                oneOf(mock).ntv_closeGroupsDB();
                will(returnValue(true));
            }
        });

        // A limit of 0 should get all groups.
        SearchResult res = safRegistry.getGroups(".*", 0);

        List<String> groups = res.getList();
        assertTrue(groups.size() == 7);
        assertTrue(groups.contains(TD.g1_str));
        assertTrue(groups.contains(TD.g2_str));
        assertTrue(groups.contains(TD.g3_str));
        assertTrue(groups.contains(TD.g4_str));
        assertTrue(groups.contains(TD.g5_str));
        assertTrue(groups.contains(TD.g6_str));
        assertTrue(groups.contains(TD.g7_str));
        assertFalse(res.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getGroups(String, int)}.
     * Test match all groups and negative limit (should return empty list).
     */
    @Test
    public void getGroups_testMatchAllNegativeLimit() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_resetGroupsCursor();
                will(returnValue(true));
                atLeast(1).of(mock).ntv_getNextGroup();
                will(onConsecutiveCalls(returnValue(TD.g1_ebc))); // match 1 to set hasMore.
                oneOf(mock).ntv_closeGroupsDB();
                will(returnValue(true));
            }
        });

        // A negative limit should return no groups.
        SearchResult res = safRegistry.getGroups(".*", -1);

        List<String> groups = res.getList();
        assertTrue(groups.isEmpty());
        assertTrue(res.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getGroups(String, int)}.
     * Test that the group list is filtered appropriately by the pattern.
     * Limit is set greater than number of matching groups (i.e. the limit doesn't
     * have any effect).
     */
    @Test
    public void getGroups_testPattern() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_resetGroupsCursor();
                will(returnValue(true));
                atLeast(1).of(mock).ntv_getNextGroup();
                will(onConsecutiveCalls(
                                        returnValue(TD.g1_ebc),
                                        returnValue(TD.g2_ebc),
                                        returnValue(TD.g3_ebc),
                                        returnValue(TD.g4_ebc),
                                        returnValue(TD.g5_ebc),
                                        returnValue(TD.g6_ebc),
                                        returnValue(TD.g7_ebc),
                                        returnValue(null)));
                oneOf(mock).ntv_closeGroupsDB();
                will(returnValue(true));
            }
        });

        SearchResult res = safRegistry.getGroups("M.*", 10);

        List<String> groups = res.getList();
        assertTrue(groups.size() == 2);
        assertTrue(groups.contains(TD.g6_str));
        assertTrue(groups.contains(TD.g7_str));
        assertFalse(res.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getGroups(String, int)}.
     * Test that the group list is filtered appropriately by the pattern
     * and that the limit is honored.
     */
    @Test
    public void getGroups_testPatternAndLimit() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_resetGroupsCursor();
                will(returnValue(true));
                atLeast(1).of(mock).ntv_getNextGroup();
                will(onConsecutiveCalls(
                                        returnValue(TD.g1_ebc),
                                        returnValue(TD.g2_ebc),
                                        returnValue(TD.g3_ebc),
                                        returnValue(TD.g4_ebc),
                                        returnValue(TD.g5_ebc),
                                        returnValue(TD.g6_ebc),
                                        returnValue(TD.g7_ebc))); // stops reading when we hit the limit + 1 (to set "hasMore").
                oneOf(mock).ntv_closeGroupsDB();
                will(returnValue(true));
            }
        });

        SearchResult res = safRegistry.getGroups("M.*", 1);

        List<String> groups = res.getList();
        assertTrue(groups.size() == 1);
        assertTrue(groups.contains(TD.g6_str));
        assertTrue(res.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getGroups(String,int)}.
     * Test ntv_resetGroupsCursor failure. RegistryException is expected.
     */
    @Test(expected = RegistryException.class)
    public void getGroups_resetFail() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_resetGroupsCursor();
                will(returnValue(false));
            }
        });

        SearchResult res = safRegistry.getGroups("ENYA", 10);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getGroups(String,int)}.
     * Test ntv_closeGroupsDB failure.
     */
    @Test(expected = RegistryException.class)
    public void getGroups_closeDBFail() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_resetGroupsCursor();
                will(returnValue(true));
                oneOf(mock).ntv_getNextGroup();
                will(returnValue(null));
                oneOf(mock).ntv_closeGroupsDB();
                will(returnValue(false));
            }
        });

        SearchResult res = safRegistry.getGroups("ENYA", 10);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getGroups(String,int)}.
     * getGroups() returns a list of groups that match a <i>pattern</i> in the registry.
     * The maximum number of groups returned is defined by the <i>limit</i>
     * argument.
     */
    @Test
    public void getGroups_noMatch() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_resetGroupsCursor();
                will(returnValue(true));
                oneOf(mock).ntv_getNextGroup();
                will(returnValue(null));
                oneOf(mock).ntv_closeGroupsDB();
                will(returnValue(true));
            }
        });

        SearchResult res = safRegistry.getGroups("ENYA", 10);

        List<String> groups = res.getList();
        assertTrue(groups.isEmpty());
        assertFalse(res.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFRegistry#getGroups(String, int)}.
     * Passing a null pattern to getGroups shall raise a IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getGroups_nullPattern() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();
        safRegistry.getGroups(null, 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFRegistry#getGroups(String, int)}.
     * Passing a "" pattern to getGroups shall raise a IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getGroups_emptyPattern() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();
        safRegistry.getGroups("", 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getGroupDisplayName(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupDisplayName_coverage() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_isValidGroup(with(equal(TD.g1_ebc)));
                will(returnValue(true));
                oneOf(mock).ntv_isValidGroup(with(equal(TD.g2_ebc)));
                will(returnValue(false));
            }
        });

        assertTrue(safRegistry.getGroupDisplayName(TD.g1_str).equals(TD.g1_str));
        safRegistry.getGroupDisplayName(TD.g2_str); // should throw EntryNotFoundException.
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getGroupSecurityName(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupSecurityName_coverage() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_isValidGroup(with(equal(TD.g1_ebc)));
                will(returnValue(true));
                oneOf(mock).ntv_isValidGroup(with(equal(TD.g2_ebc)));
                will(returnValue(false));
            }
        });

        assertTrue(safRegistry.getGroupSecurityName(TD.g1_str).equals(TD.g1_str));
        safRegistry.getGroupSecurityName(TD.g2_str); // should throw EntryNotFoundException.
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getUniqueGroupId(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueGroupId_coverage() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_isValidGroup(with(equal(TD.g1_ebc)));
                will(returnValue(true));
                oneOf(mock).ntv_isValidGroup(with(equal(TD.g2_ebc)));
                will(returnValue(false));
            }
        });

        assertTrue(safRegistry.getUniqueGroupId(TD.g1_str).equals(TD.g1_str));
        safRegistry.getUniqueGroupId(TD.g2_str); // should throw EntryNotFoundException.
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getUsers(String, int)}.
     * Test basic function - match all users with zero limit.
     */
    @Test
    public void getUsers_testMatchAllZeroLimit() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_resetUsersCursor();
                will(returnValue(true));
                atLeast(1).of(mock).ntv_getNextUser();
                will(onConsecutiveCalls(
                                        returnValue(TD.u1_ebc),
                                        returnValue(TD.u2_ebc),
                                        returnValue(TD.u3_ebc),
                                        returnValue(TD.u4_ebc),
                                        returnValue(TD.u5_ebc),
                                        returnValue(TD.u6_ebc),
                                        returnValue(TD.u7_ebc),
                                        returnValue(null)));
                oneOf(mock).ntv_closeUsersDB();
                will(returnValue(true));
            }
        });

        SearchResult res = safRegistry.getUsers(".*", 0);

        List<String> users = res.getList();
        assertTrue(users.size() == 7);
        assertTrue(users.contains(TD.u1_str));
        assertTrue(users.contains(TD.u2_str));
        assertTrue(users.contains(TD.u3_str));
        assertTrue(users.contains(TD.u4_str));
        assertTrue(users.contains(TD.u5_str));
        assertTrue(users.contains(TD.u6_str));
        assertTrue(users.contains(TD.u7_str));
        assertFalse(res.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getUsers(String, int)}.
     * Match all users with a negative limit. Should return an empty list.
     */
    @Test
    public void getUsers_testMatchAllNegativeLimit() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_resetUsersCursor();
                will(returnValue(true));
                atLeast(1).of(mock).ntv_getNextUser();
                will(onConsecutiveCalls(returnValue(TD.u1_ebc))); // stops reading after 1 match to set hasMore.
                oneOf(mock).ntv_closeUsersDB();
                will(returnValue(true));
            }
        });

        SearchResult res = safRegistry.getUsers(".*", -1);

        List<String> users = res.getList();
        assertTrue(users.isEmpty());
        assertTrue(res.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getUsers(String, int)}.
     * Test that the user list is filtered appropriately by the pattern.
     * Limit is set greater than number of matching users (i.e. the limit doesn't
     * have any effect).
     */
    @Test
    public void getUsers_testPattern() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_resetUsersCursor();
                will(returnValue(true));
                atLeast(1).of(mock).ntv_getNextUser();
                will(onConsecutiveCalls(
                                        returnValue(TD.u1_ebc),
                                        returnValue(TD.u2_ebc),
                                        returnValue(TD.u3_ebc),
                                        returnValue(TD.u4_ebc),
                                        returnValue(TD.u5_ebc),
                                        returnValue(TD.u6_ebc),
                                        returnValue(TD.u7_ebc),
                                        returnValue(null)));
                oneOf(mock).ntv_closeUsersDB();
                will(returnValue(true));
            }
        });

        SearchResult res = safRegistry.getUsers("W.*", 10);

        List<String> users = res.getList();
        assertTrue(users.size() == 2);
        assertTrue(users.contains(TD.u5_str));
        assertTrue(users.contains(TD.u6_str));
        assertFalse(res.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getUsers(String, int)}.
     * Test that the user list is filtered appropriately by the pattern,
     * and that the limit is honored.
     */
    @Test
    public void getUsers_testPatternAndLimit() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_resetUsersCursor();
                will(returnValue(true));
                atLeast(1).of(mock).ntv_getNextUser();
                will(onConsecutiveCalls(
                                        returnValue(TD.u1_ebc),
                                        returnValue(TD.u2_ebc),
                                        returnValue(TD.u3_ebc),
                                        returnValue(TD.u4_ebc),
                                        returnValue(TD.u5_ebc),
                                        returnValue(TD.u6_ebc))); // stops reading when limit + 1 has been matched (to set hasMore).
                oneOf(mock).ntv_closeUsersDB();
                will(returnValue(true));
            }
        });

        SearchResult res = safRegistry.getUsers("W.*", 1);

        List<String> users = res.getList();
        assertTrue(users.size() == 1);
        assertTrue(users.contains(TD.u5_str));
        assertTrue(res.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getUsers(String,int)}.
     * Handle a ntv_resetUsersCursor failure.
     */
    @Test(expected = RegistryException.class)
    public void getUsers_rewindFail() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_resetUsersCursor();
                will(returnValue(false));
            }
        });

        SearchResult res = safRegistry.getUsers("ENYA", 10);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getUsers(String,int)}.
     * Handle a ntv_closeUsersDB failure.
     */
    @Test(expected = RegistryException.class)
    public void getUsers_closeDBFail() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_resetUsersCursor();
                will(returnValue(true));
                oneOf(mock).ntv_getNextUser();
                will(returnValue(null));
                oneOf(mock).ntv_closeUsersDB();
                will(returnValue(false));
            }
        });

        SearchResult res = safRegistry.getUsers("ENYA", 10);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getUsers(String,int)}.
     * Verify that the Java code returns a SearchResult containing an empty
     * list when the native code returns no users.
     */
    @Test
    public void getUsers_noMatch() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_resetUsersCursor();
                will(returnValue(true));
                oneOf(mock).ntv_getNextUser();
                will(returnValue(null));
                oneOf(mock).ntv_closeUsersDB();
                will(returnValue(true));
            }
        });

        SearchResult res = safRegistry.getUsers("ENYA", 10);

        List<String> users = res.getList();
        assertTrue(users.isEmpty());
        assertFalse(res.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFRegistry#getUsers(String, int)}.
     * Passing a null pattern to getUsers shall raise a IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getUsers_nullPattern() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();
        safRegistry.getUsers(null, 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFRegistry#getUsers(String, int)}.
     * Passing a "" pattern to getUsers shall raise a IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getUsers_emptyPattern() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();
        safRegistry.getUsers("", 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getUserDisplayName(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUserDisplayName_coverage() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_isValidUser(with(equal(TD.u1_ebc)));
                will(returnValue(true));
                oneOf(mock).ntv_isValidUser(with(equal(TD.u2_ebc)));
                will(returnValue(false));
            }
        });

        assertTrue(safRegistry.getUserDisplayName(TD.u1_str).equals(TD.u1_str));
        safRegistry.getUserDisplayName(TD.u2_str); // should throw EntryNotFoundException.
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getUserSecurityName(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUserSecurityName_coverage() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_isValidUser(with(equal(TD.u1_ebc)));
                will(returnValue(true));
                oneOf(mock).ntv_isValidUser(with(equal(TD.u2_ebc)));
                will(returnValue(false));
            }
        });

        assertTrue(safRegistry.getUserSecurityName(TD.u1_str).equals(TD.u1_str));
        safRegistry.getUserSecurityName(TD.u2_str); // should throw EntryNotFoundException.
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getUniqueUserId(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueUserId_coverage() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_isValidUser(with(equal(TD.u1_ebc)));
                will(returnValue(true));
                oneOf(mock).ntv_isValidUser(with(equal(TD.u2_ebc)));
                will(returnValue(false));
            }
        });

        assertTrue(safRegistry.getUniqueUserId(TD.u1_str).equals(TD.u1_str));
        safRegistry.getUniqueUserId(TD.u2_str); // should throw EntryNotFoundException.
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getRealm()}.
     * Test realm defined in config.
     */
    @Test
    public void getRealm_testBasic() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();
        assertTrue(safRegistry.getRealm().equals(_realm));
        assertTrue(safRegistry.getRealm().equals(_realm)); // test non-null path also
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getRealm()}.
     * Test expected default realm value when no realm is defined in config.
     */
    @Test
    public void getRealm_nullRealmInConfig() throws Exception {
        _realm = null; //This results in config._realm=null
        SAFRegistry safRegistry = createSAFRegistry();

        // Set up Expectations of native method calls for the mock SAFRegistry.
        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_getPlexName();
                will(returnValue(TD.r2_ebc));
            }
        });
        assertTrue("Should return NTVPLEXNAME", safRegistry.getRealm().equals(TD.r2_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistry#getRealm()}.
     * Test expected default realm value when no realm is defined in config.
     */
    @Test
    public void getRealm_noRealmInConfig() throws Exception {
        _realm = "";
        SAFRegistry safRegistry = createSAFRegistry();
        mockery.checking(new Expectations() {
            {
                atLeast(1).of(mock).ntv_getPlexName();
                will(returnValue(TD.r2_ebc));

            }
        });
        assertTrue("Should return NTVPLEXNAME", safRegistry.getRealm().equals(safRegistry.getDefaultRealm()));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.SAFRegistryFactory.getUserRegistry()}.
     * Coverage test for SAFRegistryFactory.getUserRegistry. Testing that getUserRegistry
     * returns a non-null UserRegistry.
     */
    @Test
    public void SAFRegistryFactory_getUserRegistry_coverage() throws Exception {
        createMockEnv();
        final NativeMethodManager mockNmm = mockery.mock(NativeMethodManager.class);

        // Set up Expectations of method calls for the mock NativeMethodManager.
        mockery.checking(new Expectations() {
            {
                oneOf(mockNmm).registerNatives(with(equal(SAFRegistry.class)));
            }
        });

        SAFDelegatingUserRegistry ur = new SAFDelegatingUserRegistry();
        ur.nativeMethodManager = mockNmm;

        ur.activate(config);

        assertNotNull(ur.delegate);

        assertTrue(ur.delegate instanceof SAFRegistry);

        assertFalse(ur.delegate instanceof SAFAuthorizedRegistry);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFRegistry#mapCertificate(X509Certificate)}.
     * Test the business as usual path
     */
    @Test
    public void mapCertificate_test() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();
        final X509Certificate cert = mockery.mock(X509Certificate.class, "X509Certificate" + uniqueMockNameCount++);

        mockery.checking(new Expectations() {
            {
                exactly(2).of(cert).getEncoded();
                will(returnValue(new byte[] { 'b', 'l', 'a', 'h' }));
            }
        });

        final byte[] ecert = cert.getEncoded();

        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_mapCertificate(ecert, ecert.length);
                will(returnValue(TD.u1_ebc));
            }
        });

        assertTrue(safRegistry.mapCertificate(new X509Certificate[] { cert }).equals(TD.u1_str));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFRegistry#mapCertificate(X509Certificate)}.
     * Test the null certificate path
     */
    @Test(expected = IllegalArgumentException.class)
    public void mapCertificate_testNullCert() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();
        safRegistry.mapCertificate((X509Certificate[]) null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.saf.internal.SAFRegistry#mapCertificate(X509Certificate)}.
     * Test the path where ntv_mapCertificate returns null
     */
    @Test(expected = CertificateMapFailedException.class)
    public void mapCertificate_testMapFail() throws Exception {
        SAFRegistry safRegistry = createSAFRegistry();

        final X509Certificate cert = mockery.mock(X509Certificate.class, "X509Certificate" + uniqueMockNameCount++);

        mockery.checking(new Expectations() {
            {
                exactly(2).of(cert).getEncoded();
                will(returnValue(new byte[] { 'b', 'l', 'a', 'h' }));
            }
        });

        final byte[] ecert = cert.getEncoded();

        mockery.checking(new Expectations() {
            {
                oneOf(mock).ntv_mapCertificate(ecert, ecert.length);
                will(returnValue(null));
            }
        });

        safRegistry.mapCertificate(new X509Certificate[] { cert });
    }

    /**
     * Test that checkPasswordHelper doesn't throw an Exception when it's not supposed to.
     */
    @Test
    public void testCheckPasswordHelperNoException() throws Exception {

        SAFRegistryErrno safRegistry = new SAFRegistryErrno(config);
        safRegistry.errno = PasswdResult.Errno.ESRCH.errno;
        safRegistry.errno2 = 0x1234;
        assertFalse(safRegistry.checkPasswordHelper("someuser", "somepass"));

        safRegistry.errno = PasswdResult.Errno.EACCES.errno;
        assertFalse(safRegistry.checkPasswordHelper("someuser", "somepass"));

        safRegistry.errno = PasswdResult.Errno.EINVAL.errno;
        assertFalse(safRegistry.checkPasswordHelper("someuser", "somepass"));

        safRegistry.errno = PasswdResult.Errno.EMVSEXPIRE.errno;
        assertFalse(safRegistry.checkPasswordHelper("someuser", "somepass"));

        safRegistry.errno = PasswdResult.Errno.EMVSPASSWORD.errno;
        assertFalse(safRegistry.checkPasswordHelper("someuser", "somepass"));
    }

    /**
     * Test that checkPasswordHelper throws an Exception when it's supposed to.
     */
    @Test
    public void testCheckPasswordHelperException() throws Exception {

        SAFRegistryErrno safRegistry = new SAFRegistryErrno(config);

        // Test 1
        {
            RegistryException expectedEx = null;
            try {
                safRegistry.errno = PasswdResult.Errno.EMVSERR.errno;
                safRegistry.errno2 = 0x1234;
                safRegistry.checkPasswordHelper("someuser", "somepass");
            } catch (RegistryException re) {
                expectedEx = re;
            }

            assertNotNull(expectedEx);
            assertEquals("Unix System Service __passwd failed for user someuser with errno " + safRegistry.errno + " (EMVSERR) and errno2 x1234", expectedEx.getMessage());
        }

        // Test 2
        {
            RegistryException expectedEx = null;
            try {

                safRegistry.errno = PasswdResult.Errno.EMVSSAF2ERR.errno;
                safRegistry.errno2 = 0x4567;
                safRegistry.checkPasswordHelper("someuser", "somepass");
            } catch (RegistryException re) {
                expectedEx = re;
            }

            assertNotNull(expectedEx);
            assertEquals("Unix System Service __passwd failed for user someuser with errno " + safRegistry.errno + " (EMVSSAF2ERR) and errno2 x4567", expectedEx.getMessage());
        }

        // Test 3
        {
            RegistryException expectedEx = null;
            try {

                safRegistry.errno = PasswdResult.Errno.EMVSSAFEXTRERR.errno;
                safRegistry.errno2 = 0x4567;
                safRegistry.checkPasswordHelper("someuser", "somepass");
            } catch (RegistryException re) {
                expectedEx = re;
            }

            assertNotNull(expectedEx);
            assertEquals("Unix System Service __passwd failed for user someuser with errno " + safRegistry.errno + " (EMVSSAFEXTRERR) and errno2 x4567", expectedEx.getMessage());
        }

        // Test 4
        {
            RegistryException expectedEx = null;
            try {

                safRegistry.errno = 777;
                safRegistry.errno2 = 0x4567;
                safRegistry.checkPasswordHelper("someuser", "somepass");
            } catch (RegistryException re) {
                expectedEx = re;
            }

            assertNotNull(expectedEx);
            assertEquals("Unix System Service __passwd failed for user someuser with errno " + safRegistry.errno + " (UNKNOWN) and errno2 x4567", expectedEx.getMessage());
        }

    }
}

/**
 * Helper class for setting errno/errno2 values for mocked call to ntv_checkPassword.
 */
class SAFRegistryErrno extends SAFRegistry {
    public int errno;
    public int errno2;

    public SAFRegistryErrno(SAFRegistryConfig config) {
        super(config);
    }

    @Override
    protected boolean ntv_checkPassword(byte[] user, byte[] pwd, String applid, byte[] passwdResult) {

        IntBuffer buff = ByteBuffer.wrap(passwdResult).asIntBuffer();
        buff.put(errno);
        buff.put(errno2);

        return false;
    }
}
