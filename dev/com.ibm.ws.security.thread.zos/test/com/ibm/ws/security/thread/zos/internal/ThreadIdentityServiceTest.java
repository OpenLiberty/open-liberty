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
package com.ibm.ws.security.thread.zos.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.security.credentials.saf.SAFCredentialsService;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.webapp.WebAppConfiguration;
import com.ibm.wsspi.security.credentials.saf.SAFCredential;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;

/**
 * Unit test for the z/OS SYNC-TO-OS-THREAD ThreadIdentityService.
 *
 * Note: ThreadIdentityServiceImpl requires a z/OS native environment; however,
 * this unit test is designed to run on distributed platforms. The native environment
 * is mocked using JMock.
 *
 * Thus, this unit test is only focused on testing the Java code.
 */
@RunWith(JMock.class)
public class ThreadIdentityServiceTest {

    /**
     * Mock environment for NativeMethodManager and native methods.
     */
    private static Mockery mockery = null;

    /**
     * Counter for generating unique mock object names.
     */
    private static int uniqueMockNameCount = 1;

    /**
     * The mocked ThreadIdentityServiceImpl. This mock object handles all of
     * ThreadIdentityServiceImpl's native methods.
     */
    protected ThreadIdentityServiceImpl mockTISForNative = null;

    /**
     * Mock SAFCredentialsService.
     */
    protected SAFCredentialsService mockSAFCS = null;

    /**
     * Mock Server SAFCredential.
     */
    protected SAFCredential mockServerCred = null;

    /**
     * Mock User SAFCredential.
     */
    protected SAFCredential mockUserCred = null;

    /**
     * Mock User SAFCredential.
     */
    protected SAFCredential mockUser2Cred = null;

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
        mockTISForNative = mockery.mock(ThreadIdentityServiceImpl.class, "ThreadIdentityServiceImpl" + uniqueMockNameCount);
        mockSAFCS = mockery.mock(SAFCredentialsService.class, "SAFCredentialsService" + uniqueMockNameCount);
        mockServerCred = mockery.mock(SAFCredential.class, "ServerSAFCredential" + uniqueMockNameCount);
        mockUserCred = mockery.mock(SAFCredential.class, "UserSAFCredential" + uniqueMockNameCount);
        mockUser2Cred = mockery.mock(SAFCredential.class, "User2SAFCredential" + uniqueMockNameCount);
    }

    /**
     * Create a ThreadIdentityServiceImpl for the unit test. The ThreadIdentityServiceImpl
     * returned by this method forwards all native method invocations (ntv_*) to the
     * ThreadIdentityServiceImpl mock object (mockTISForNative).
     */
    protected ThreadIdentityServiceImpl getThreadIdentityServiceImplMockNative() throws Exception {
        return new ThreadIdentityServiceImpl() {
            @Override
            protected int ntv_setThreadSecurityEnvironment(byte[] safCredToken,
                                                           byte[] profilePrefixEbcdic,
                                                           byte[] safResult) {
                // -rx- // Copy the SAFServiceResult bytes from the test into safResult,
                // -rx- // which ThreadIdentityServiceImpl will use to detect SAF failures.
                // -rx- // System.arraycopy(mockSSR.getBytes(), 0, safResult, 0, safResult.length);

                return mockTISForNative.ntv_setThreadSecurityEnvironment(safCredToken, profilePrefixEbcdic, safResult);
            }

            @Override
            protected boolean ntv_isSyncToThreadEnabled(byte[] profilePrefixEbcdic) {
                return mockTISForNative.ntv_isSyncToThreadEnabled(profilePrefixEbcdic);
            }

            @Override
            protected void ntv_resetIsNativeEnabledCache() {
                mockTISForNative.ntv_resetIsNativeEnabledCache();
            }
        };
    }

    /**
     * Create Map object containing the config.
     */
    protected Map<String, Object> createConfigMap(boolean appEnabled, boolean j2cEnabled) {
        final Map<String, Object> config = new HashMap<String, Object>();
        config.put(ThreadIdentityServiceImpl.APP_ENABLED_KEY, Boolean.valueOf(appEnabled));
        config.put(ThreadIdentityServiceImpl.J2C_ENABLED_KEY, Boolean.valueOf(j2cEnabled));
        return config;
    }

    /**
     * New up a ThreadIdentityServiceImpl and set its service references (which are mocked).
     */
    protected ThreadIdentityServiceImpl createThreadIdentityServiceImpl() throws Exception {
        return createThreadIdentityServiceImpl(createConfigMap(true, false));
    }

    /**
     * New up a ThreadIdentityServiceImpl and set its service references (which are mocked).
     *
     * @param appEnabled Config value for appEnabled
     * @param j2cEnabled Config value for j2cEnabled
     */
    protected ThreadIdentityServiceImpl createThreadIdentityServiceImpl(Map<String, Object> config) throws Exception {
        createMockEnv();

        // Expectations on the mock objects...
        mockery.checking(new Expectations() {
            {
                oneOf(mockSAFCS).getServerCredential();
                will(returnValue(mockServerCred));

                oneOf(mockTISForNative).ntv_resetIsNativeEnabledCache();
            }
        });

        ThreadIdentityServiceImpl tis = getThreadIdentityServiceImplMockNative();
        tis.setSafCredentialsService(mockSAFCS);
        tis.activate(null, config);

        return tis;
    }

    /**
     * Test for basic lifecycle operations.
     */
    @Test
    public void testBasicLifecycle() throws Exception {
        ThreadIdentityServiceImpl tis = createThreadIdentityServiceImpl();
        tis.deactivate(null);
    }

    /**
     * Setup common mockery Expectations for ThreadIdentityServiceImpl.setThreadSecurityEnvironment.
     */
    protected void setupSetThreadSecurityEnvironmentExpectations(final SAFCredential mockCred,
                                                                 final int rc) throws Exception {

        final String profilePrefix = "BBGZDFLT";
        final byte[] safCredToken = new byte[0];

        mockery.checking(new Expectations() {
            {
                oneOf(mockSAFCS).getSAFCredentialTokenBytes(with(equal(mockCred)));
                will(returnValue(safCredToken));

                oneOf(mockSAFCS).getProfilePrefix();
                will(returnValue(profilePrefix));

                allowing(mockCred).getUserId();
                will(returnValue("mockCredUserId"));

                oneOf(mockTISForNative).ntv_setThreadSecurityEnvironment(with(equal(safCredToken)),
                                                                         with(any(byte[].class)),
                                                                         with(any(byte[].class)));
                will(returnValue(rc));
            }
        });
    }

    /**
     * Setup common mockery Expectations for ThreadIdentityServiceImpl.set.
     */
    protected void setupSetExpectations(final SAFCredential mockCred) {
        mockery.checking(new Expectations() {
            {
                oneOf(mockSAFCS).getSAFCredentialFromSubject(with(any(Subject.class)));
                will(returnValue(mockCred));
            }
        });
    }

    /**
     * Setup common mockery Expectations for ThreadIdentityServiceImpl.runAsServer.
     */
    protected void setupRunAsServerExpectations() {
        mockery.checking(new Expectations() {
            {
                oneOf(mockSAFCS).getServerCredential();
                will(returnValue(mockServerCred));
            }
        });
    }

    /**
     * Setup common mockery Expectations for ThreadIdentityServiceImpl.isAppThreadIdentityEnabled/
     * isJ2CThreadIdentityEnabled.
     */
    protected void setupIsEnabledExpectations(final boolean retVal) throws Exception {

        final String profilePrefix = "BBGZDFLT";

        mockery.checking(new Expectations() {
            {
                oneOf(mockSAFCS).getProfilePrefix();
                will(returnValue(profilePrefix));

                oneOf(mockTISForNative).ntv_isSyncToThreadEnabled(with(any(byte[].class)));
                will(returnValue(retVal));
            }
        });
    }

    /**
     * Setup common mockery Expectations for ThreadIdentityServiceImpl.modify.
     */
    protected void setupModifyExpectations() throws Exception {
        mockery.checking(new Expectations() {
            {
                oneOf(mockTISForNative).ntv_resetIsNativeEnabledCache();
            }
        });
    }

    /**
     * Basic test of ThreadIdentityServiceImpl.set and reset.
     */
    @Test
    public void testSuccessfulSetAndReset() throws Exception {
        ThreadIdentityServiceImpl tis = createThreadIdentityServiceImpl();

        // ThreadIdentityServiceImpl starts with the serverCred as the "current" identity.
        // This test will replace it with the mockUserCred.
        setupSetExpectations(mockUserCred);
        setupSetThreadSecurityEnvironmentExpectations(mockUserCred, 0);
        Object token = tis.set(new Subject());

        // The token returned should refer to the serverCred.
        assertEquals(token, mockServerCred);

        // Now we will "reset" the thread identity by passing in the previously returned token.
        // This token represents the serverCred, so set the expectations for the mockServerCred.
        setupSetThreadSecurityEnvironmentExpectations(mockServerCred, 0);
        tis.reset(token);
    }

    /**
     * Test nested calls of set/reset.
     */
    @Test
    public void testNestedSetAndReset() throws Exception {
        ThreadIdentityServiceImpl tis = createThreadIdentityServiceImpl();

        // ThreadIdentityServiceImpl starts with the serverCred as the "current" identity.
        // Replace it with the mockUserCred.
        setupSetExpectations(mockUserCred);
        setupSetThreadSecurityEnvironmentExpectations(mockUserCred, 0);
        Object token = tis.set(new Subject());

        // The token returned should refer to the serverCred.
        assertEquals(token, mockServerCred);

        // Now set user2 onto the thread; i.e. the app invoked another app using a different
        // invocation Subject.
        setupSetExpectations(mockUser2Cred);
        setupSetThreadSecurityEnvironmentExpectations(mockUser2Cred, 0);
        Object token1 = tis.set(new Subject());

        // token1 should refer to mockUserCred.
        assertEquals(token1, mockUserCred);

        // Now call runAsServer.  This method is called by the wrapper code around operations
        // that must be performed using the server's identity.
        setupRunAsServerExpectations();
        setupSetThreadSecurityEnvironmentExpectations(mockServerCred, 0);
        Object token2 = tis.runAsServer();

        // token2 should refer to mockUser2Cred.
        assertEquals(token2, mockUser2Cred);

        // Now call reset, as the wrapper would do.  mockUser2Cred will be re-assigned to the thread.
        setupSetThreadSecurityEnvironmentExpectations(mockUser2Cred, 0);
        tis.reset(token2);

        // Call reset again.  This time mockUserCred will be re-assigned to the thread as we
        // unwind the stack.
        setupSetThreadSecurityEnvironmentExpectations(mockUserCred, 0);
        tis.reset(token1);

        // Now the final reset will re-assign the server cred to the thread.
        setupSetThreadSecurityEnvironmentExpectations(mockServerCred, 0);
        tis.reset(token);
    }

    /**
     * Test symmetry of ThreadIdentityServiceImpl.setThreadSecurityEnvironment
     */
    @Test
    public void testSetThreadSecurityEnvironmentSymmetry() throws Exception {
        ThreadIdentityServiceImpl tis = createThreadIdentityServiceImpl();

        // ThreadIdentityServiceImpl starts with the serverCred as the "current" identity.
        // This test will replace it with the mockUserCred.
        setupSetThreadSecurityEnvironmentExpectations(mockUserCred, 0);
        SAFCredential prev = tis.setThreadSecurityEnvironment(mockUserCred);

        // The prev credential should refer to the serverCred.
        assertEquals(prev, mockServerCred);

        // Now "reset" the thread identity by passing in the previously returned credential.
        // This token represents the serverCred, so set the expectations for the mockServerCred.
        setupSetThreadSecurityEnvironmentExpectations(mockServerCred, 0);
        prev = tis.setThreadSecurityEnvironment(prev);

        // The prev credential should now refer to the userCred.
        assertEquals(prev, mockUserCred);

        // Set the serverCred again. Since the serverCred is already current, the call should
        // basically be a NO-OP, so no Expectations need to be set.
        prev = tis.setThreadSecurityEnvironment(mockServerCred);

        // The prev credential should refer to the serverCred.
        assertEquals(prev, mockServerCred);

        // Set the serverCred one more time, for good measure.
        prev = tis.setThreadSecurityEnvironment(mockServerCred);
        assertEquals(prev, mockServerCred);
    }

    /**
     * Test isAppThreadIdentityEnabled. Verify caching. Verify modify() clears the cache.
     */
    @Test
    public void testIsAppEnabledAndModify() throws Exception {
        ThreadIdentityServiceImpl tis = createThreadIdentityServiceImpl(createConfigMap(true, false));
        setTestWebModuleMetaDataOnThread("true");

        // Setup the mock native method to return true.
        setupIsEnabledExpectations(true);
        assertTrue(tis.isAppThreadIdentityEnabled());

        // Verify that the value is cached by calling again with no Expectations.
        assertTrue(tis.isAppThreadIdentityEnabled());

        // Call modify, which should reset the cached value.
        setupModifyExpectations();
        tis.modify(createConfigMap(true, false));

        // Setup the mock native method to return false.
        setupIsEnabledExpectations(false);
        assertFalse(tis.isAppThreadIdentityEnabled());

        // Verify that the value is cached by calling again with no Expectations.
        assertFalse(tis.isAppThreadIdentityEnabled());
    }

    /**
     * Test isAppThreadIdentityEnabled returns false with component disabled.
     */
    @Test
    public void testIsAppEnabledComponentDisabled() throws Exception {
        ThreadIdentityServiceImpl tis = createThreadIdentityServiceImpl(createConfigMap(true, false));
        setTestWebModuleMetaDataOnThread("false");

        // Setup the mock native method to return true.
        setupIsEnabledExpectations(true);
        assertFalse(tis.isAppThreadIdentityEnabled());
    }

    private void setTestWebModuleMetaDataOnThread(final String componentEnabled) {
        final WebModuleMetaData webModuleMetaData = createWebModuleMetaDataMock(componentEnabled);
        WebComponentMetaData webComponentMetaData = createTestWebComponentMetaData(webModuleMetaData);
        ComponentMetaDataAccessorImpl cmda = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        cmda.beginContext(webComponentMetaData);
    }

    private WebModuleMetaData createWebModuleMetaDataMock(final String componentEnabled) {
        final WebModuleMetaData webModuleMetaData = mockery.mock(WebModuleMetaData.class);
        final WebAppConfiguration webAppConfig = mockery.mock(WebAppConfiguration.class);
        final EnvEntry syncToOSThreadEntry = mockery.mock(EnvEntry.class);
        final List<EnvEntry> envEntries = new ArrayList<EnvEntry>();
        envEntries.add(syncToOSThreadEntry);
        mockery.checking(new Expectations() {
            {
                allowing(webModuleMetaData).getConfiguration();
                will(returnValue(webAppConfig));
                allowing(webAppConfig).getEnvEntries();
                will(returnValue(envEntries));
                allowing(syncToOSThreadEntry).getName();
                will(returnValue("com.ibm.websphere.security.SyncToOSThread"));
                allowing(syncToOSThreadEntry).getValue();
                will(returnValue(componentEnabled));
            }
        });
        return webModuleMetaData;
    }

    private WebComponentMetaData createTestWebComponentMetaData(final WebModuleMetaData webModuleMetaData) {
        final WebComponentMetaData webComponentMetaData = mockery.mock(WebComponentMetaData.class);

        mockery.checking(new Expectations() {
            {
                allowing(webComponentMetaData).getModuleMetaData();
                will(returnValue(webModuleMetaData));
            }
        });
        return webComponentMetaData;
    }

    /**
     * Test null parms.
     */
    @Test
    public void testNullParms() throws Exception {
        ThreadIdentityServiceImpl tis = createThreadIdentityServiceImpl();

        // Leaving this code here in case we change the null scenario back to no-op.
        // // Passing null to set() is NO-OP.  Null returned.
        // Object token = tis.set(null);
        // assertNull(token);
        // tis.reset(token);

        // The previously current credential is returned (the server).
        SAFCredential prev = tis.setThreadSecurityEnvironment(null);

        // Previous credential is server's.
        assertEquals(prev, mockServerCred);

        // "Reset" serverCred (it was never actually removed).
        prev = tis.setThreadSecurityEnvironment(mockServerCred);

        // Previous credential is still server's.  Null is never set as current.
        assertEquals(prev, mockServerCred);
    }
}
