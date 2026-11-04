/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;
import java.util.Set;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 *
 */
public class WolaSecurityInterceptorTest {

    /**
     * Pushes/pops subjects on/off the thread.
     */
    final SubjectManager subjectManager = new SubjectManager();

    /**
     * Mock environment.
     */
    private Mockery mockery = null;

    /**
     * Create the mockery environment for each test. Setting up a new mockery
     * environment for each test helps isolate Expectation sets, making it easier to
     * debug when some Expectation fails and all the Expectations are dumped
     * to the error log.
     */
    @Before
    public void before() {
        // Needs to be ClassImposteriser in order to mock NativeRequestHandler and NativeWorkRequest.
        mockery = new JUnit4Mockery();

        clearSubjects();
    }

    /**
     * There are alternative ways to do this.
     * 1) Use @RunWith(JMock.class) (deprecated)
     * 2) Declare the field: @Rule public final JUnitRuleMockery context = new JUnitRuleMockery();
     * (this version of Junit is not in our codebase).
     *
     * Doing it the manual way for now.
     */
    @After
    public void after() {
        clearSubjects();

        mockery.assertIsSatisfied();
    }

    /**
     * Clear the subjects from the thread. Called pre/post test.
     */
    private void clearSubjects() {
        subjectManager.setCallerSubject(null);
        subjectManager.setInvocationSubject(null);
    }

    /**
     *
     */
    @Test
    public void testCreateSubjectWithAuthService() throws Exception {

        final AuthenticationService mockAuthService = mockery.mock(AuthenticationService.class);
        final UserRegistry mockUserRegistry = mockery.mock(UserRegistry.class);
        final String mvsUserId = "MSTONE1";
        final String realm = "WAS00";

        // Setup mock expectations for security
        mockery.checking(new Expectations() {
            {
                oneOf(mockAuthService).isAllowHashTableLoginWithIdOnly();
                will(returnValue(true));

                oneOf(mockUserRegistry).getRealm();
                will(returnValue(realm));
            }
        });

        // Create the Subject and assert its properties.
        WolaSecurityInterceptor wolaSecurityInterceptor = buildWolaSecurityInterceptor(mockAuthService);
        Subject subject = wolaSecurityInterceptor.createUserIdHashtableSubject(mvsUserId, mockUserRegistry);

        assertNotNull("WOLA security interceptor did not give us a subject", subject);
        Set<Object> publicCredentials = subject.getPublicCredentials();
        assertNotNull("No public credentials in the subject", publicCredentials);
        assertTrue("Public credentials size is not 1", publicCredentials.size() == 1);

        for (Object o : publicCredentials) {
            @SuppressWarnings("unchecked")
            Hashtable<String, Object> hashtable = (Hashtable<String, Object>) o;

            assertFalse("Hashtable should not contain id assertion key", hashtable.contains(AuthenticationConstants.INTERNAL_ASSERTION_KEY));

            assertTrue("Hashtable does not contain the user ID", hashtable.containsKey(AttributeNameConstants.WSCREDENTIAL_USERID));
            assertEquals("ID in subject is not correct", mvsUserId, hashtable.get(AttributeNameConstants.WSCREDENTIAL_USERID));

            assertTrue("Hashtable does not contain the realm", hashtable.containsKey(AttributeNameConstants.WSCREDENTIAL_REALM));
            assertEquals("Realm in subject is not correct", realm, hashtable.get(AttributeNameConstants.WSCREDENTIAL_REALM));
        }
    }

    /**
     *
     */
    @Test
    public void testCreateSubjectWithoutAuthService() throws Exception {

        final AuthenticationService mockAuthService = null;
        final UserRegistry mockUserRegistry = mockery.mock(UserRegistry.class);
        final String mvsUserId = "MSTONE1";
        final String realm = "WAS00";

        // Setup mock expectations for security
        mockery.checking(new Expectations() {
            {
                oneOf(mockUserRegistry).getRealm();
                will(returnValue(realm));
            }
        });

        // Create the Subject and assert its properties.
        WolaSecurityInterceptor wolaSecurityInterceptor = buildWolaSecurityInterceptor(mockAuthService);
        Subject subject = wolaSecurityInterceptor.createUserIdHashtableSubject(mvsUserId, mockUserRegistry);

        assertNotNull("WOLA security interceptor did not give us a subject", subject);
        Set<Object> publicCredentials = subject.getPublicCredentials();
        assertNotNull("No public credentials in the subject", publicCredentials);
        assertTrue("Public credentials size is not 1", publicCredentials.size() == 1);

        for (Object o : publicCredentials) {
            @SuppressWarnings("unchecked")
            Hashtable<String, Object> hashtable = (Hashtable<String, Object>) o;
            assertTrue("Hashtable does not contain id assertion key", hashtable.containsKey(AuthenticationConstants.INTERNAL_ASSERTION_KEY));
            assertTrue("ID assertion key is not true", (Boolean) hashtable.get(AuthenticationConstants.INTERNAL_ASSERTION_KEY));

            assertTrue("Hashtable does not contain the user ID", hashtable.containsKey(AttributeNameConstants.WSCREDENTIAL_USERID));
            assertEquals("ID in subject is not correct", mvsUserId, hashtable.get(AttributeNameConstants.WSCREDENTIAL_USERID));

            assertTrue("Hashtable does not contain the realm", hashtable.containsKey(AttributeNameConstants.WSCREDENTIAL_REALM));
            assertEquals("Realm in subject is not correct", realm, hashtable.get(AttributeNameConstants.WSCREDENTIAL_REALM));
        }
    }

    /**
     * @return a WolaSecurityInterceptor object
     */
    private WolaSecurityInterceptor buildWolaSecurityInterceptor(AuthenticationService authService) {
        WolaSecurityInterceptor retMe = new WolaSecurityInterceptor();
        retMe.authenticationService = authService;
        return retMe;
    }

    /**
     *
     */
    @Test
    public void testIsEmpty() {
        assertTrue(new WolaSecurityInterceptor().isEmpty(""));
        assertTrue(new WolaSecurityInterceptor().isEmpty(null));
        assertFalse(new WolaSecurityInterceptor().isEmpty("x"));
    }

    /**
     *
     */
    @Test
    public void testSetRunAsSubject() {

        Subject subject = new Subject();

        WolaSecurityInterceptor.PreInvokeToken token = new WolaSecurityInterceptor().setRunAsSubject(subject);

        assertSame(subject, subjectManager.getInvocationSubject());
        assertSame(subject, subjectManager.getCallerSubject());

        assertNull(token.prevCallerSubject);
        assertNull(token.prevInvocationSubject);
    }

    /**
     *
     */
    @Test
    public void testSetRunAsSubjectWithPrevSubjectOnThread() {

        Subject subject0 = new Subject();

        subjectManager.setCallerSubject(subject0);
        subjectManager.setInvocationSubject(subject0);

        assertNotNull(subjectManager.getInvocationSubject());
        assertNotNull(subjectManager.getCallerSubject());

        Subject subject = new Subject();

        WolaSecurityInterceptor.PreInvokeToken token = new WolaSecurityInterceptor().setRunAsSubject(subject);

        assertSame(subject, subjectManager.getInvocationSubject());
        assertSame(subject, subjectManager.getCallerSubject());

        assertSame(subject0, token.prevCallerSubject);
        assertSame(subject0, token.prevInvocationSubject);
    }

    /**
     *
     */
    @Test
    public void testPostInvoke() {

        Subject subject0 = new Subject();

        subjectManager.setCallerSubject(subject0);
        subjectManager.setInvocationSubject(subject0);

        assertNotNull(subjectManager.getInvocationSubject());
        assertNotNull(subjectManager.getCallerSubject());

        // Create a token with null for prevCallerSubject and prevInvocationSubject
        WolaSecurityInterceptor.PreInvokeToken token = new WolaSecurityInterceptor.PreInvokeToken(null, null);

        // PostInvoke shall replace the current Subject on the thread (subject0)
        // with those of the PreInvokeToken (both null).
        new WolaSecurityInterceptor().postInvoke(token, null);

        assertNull(subjectManager.getInvocationSubject());
        assertNull(subjectManager.getCallerSubject());
    }

    /**
     *
     */
    @Test
    public void testPostInvokeWithPrevSubject() {

        Subject subject0 = new Subject();

        // Create a token with subject0 for prevCallerSubject and prevInvocationSubject
        WolaSecurityInterceptor.PreInvokeToken token = new WolaSecurityInterceptor.PreInvokeToken(subject0, subject0);

        // PostInvoke shall replace the current Subject on the thread (null)
        // with those of the PreInvokeToken (both subject0).
        new WolaSecurityInterceptor().postInvoke(token, null);

        assertSame(subject0, subjectManager.getInvocationSubject());
        assertSame(subject0, subjectManager.getCallerSubject());
    }

//    /**
//     *
//     */
//    @Test
//    public void testPreInvoke() throws Exception {
//
//        // Dummy up a WOLA message.
//        final String mvsUserId = "MSTONE1";
//        ByteBuffer bb = ByteBuffer.allocate(WolaMessage.HeaderSize);
//        bb.position(WolaMessage.MvsUserIdOffset);
//        bb.put((mvsUserId + "\0").getBytes(CodepageUtils.EBCDIC));
//        bb.rewind();
//
//        WolaMessage wolaMessage = new WolaMessage(new ByteBufferVector(bb.array()));
//        assertEquals(mvsUserId, wolaMessage.getMvsUserId());
//
//        // Install the mock SAFCredentialsService
//        final SAFCredentialsService mockSafCredentialsService = mockery.mock(SAFCredentialsService.class);
//        final SAFCredential mockSafCredential = mockery.mock(SAFCredential.class);
//
//        new WOLAChannelFactoryProvider().activate();
//        WOLAChannelFactoryProvider.getInstance().setSafCredentialsService(mockSafCredentialsService);
//
//        // Setup mock expectations for SAFCredentialsService
//        mockery.checking(new Expectations() {
//            {
//                oneOf(mockSafCredentialsService).createLazyAssertedCredential(mvsUserId, WolaSecurityInterceptor.SafAuditString);
//                will(returnValue(mockSafCredential));
//            }
//        });
//
//        // Run preInvoke.
//        Object token = new WolaSecurityInterceptor().preInvoke(wolaMessage);
//
//        // Asserts.
//        assertTrue(token instanceof WolaSecurityInterceptor.PreInvokeToken);
//        Subject invocationSubject = subjectManager.getInvocationSubject();
//        Subject callerSubject = subjectManager.getCallerSubject();
//
//        assertNotNull(invocationSubject);
//        assertSame(invocationSubject, callerSubject);
//
//        Set<Principal> principals = callerSubject.getPrincipals();
//        assertEquals(1, principals.size());
//        for (Principal principal : principals) {
//            assertEquals(mvsUserId, principal.getName());
//        }
//
//        Set<Object> creds = callerSubject.getPrivateCredentials();
//        assertEquals(1, creds.size());
//        for (Object cred : creds) {
//            assertTrue(cred instanceof SAFCredential);
//            assertSame(cred, mockSafCredential);
//        }
//
//    }

}
