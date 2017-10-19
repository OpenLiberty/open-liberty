/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.security.thread;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.wsspi.kernel.security.thread.ThreadIdentityService;

public class ThreadIdentityManagerTest {

    private final Mockery mockery = new JUnit4Mockery();
    private ThreadIdentityService tis1;
    private ThreadIdentityService tis2;
    private ThreadIdentityService tis3;
    private J2CIdentityService j2cIs1;
    private J2CIdentityService j2cIs2;
    private J2CIdentityService j2cIs3;
    private Subject invocationSubject;
    private final String expectedRE = "java.lang.RuntimeException: ";
    private final String setRE = "ThreadIdentityService.set() blew up.";
    private final String setREJ2C = "J2CIdentityService.set() blew up.";
    private final String resetREJ2C = "J2CIdentityService.reset() blew up.";
    private final String expectedSetRE = expectedRE + setRE;
    private final String expectedSetREJ2C = expectedRE + setREJ2C;

    @Before
    public void setUp() {
        tis1 = mockery.mock(ThreadIdentityService.class, "tis1");
        tis2 = mockery.mock(ThreadIdentityService.class, "tis2");
        tis3 = mockery.mock(ThreadIdentityService.class, "tis3");
        j2cIs1 = mockery.mock(J2CIdentityService.class, "j2cIs1");
        j2cIs2 = mockery.mock(J2CIdentityService.class, "j2cIs2");
        j2cIs3 = mockery.mock(J2CIdentityService.class, "j2cIs3");
        invocationSubject = new Subject();
    }

    @After
    public void tearDown() {
        ThreadIdentityManager.removeAllThreadIdentityServices();
        ThreadIdentityManager.removeAllJ2CIdentityServices();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        ThreadIdentityManager.removeAllThreadIdentityServices();
    }

    @Test
    public void testIsAppThreadIdentityEnabled() {
        setAppThreadIdentityEnabledExpectation(tis1, true);
        ThreadIdentityManager.addThreadIdentityService(tis1);
        assertTrue("The application thread identity must be enabled", ThreadIdentityManager.isAppThreadIdentityEnabled());
    }

    @Test
    public void testIsAppThreadIdentityEnabled_oneTIS() {
        setAppThreadIdentityEnabledExpectation(tis1, true);
        ThreadIdentityManager.addThreadIdentityService(tis1);
        assertTrue("The application thread identity must be enabled", ThreadIdentityManager.isAppThreadIdentityEnabled());
    }

    @Test
    public void testIsAppThreadIdentityEnabled_multipleTIS() {
        setAppThreadIdentityEnabledExpectation(tis1, true);
        setAppThreadIdentityEnabledExpectation(tis2, false);
        addTwoTIS();
        assertTrue("The application thread identity must be enabled", ThreadIdentityManager.isAppThreadIdentityEnabled());
    }

    @Test
    public void testIsAppThreadIdentityEnabled_bothTISFalse() {
        setAppThreadIdentityEnabledExpectation(tis1, false);
        setAppThreadIdentityEnabledExpectation(tis2, false);
        addTwoTIS();
        assertFalse("The application thread identity must not be enabled", ThreadIdentityManager.isAppThreadIdentityEnabled());
    }

    @Test
    public void testAppThreadIdentity() throws ThreadIdentityException {
        setAppFlowExpectations(tis1, true);
        ThreadIdentityManager.addThreadIdentityService(tis1);
        Object token = ThreadIdentityManager.setAppThreadIdentity(invocationSubject);
        ThreadIdentityManager.resetChecked(token);
    }

    @Test
    public void testAppThreadIdentityWithMultipleTIS() throws ThreadIdentityException {
        setAppFlowExpectations(tis1, true);
        setAppFlowExpectations(tis2, true);
        setAndResetAppSubjectForTwoTIS();
    }

    /*
     * Check to make sure that it still works if someone passes a null TIS
     * by mistake. The code must not be passing a null TIS, but it must not fail.
     */
    @Test
    public void testAppThreadIdentityWithMultipleTIS_oneNull() throws ThreadIdentityException {
        setAppFlowExpectations(tis1, true);
        ThreadIdentityManager.addThreadIdentityService(tis1);
        ThreadIdentityManager.addThreadIdentityService(null);
        Object token = ThreadIdentityManager.setAppThreadIdentity(invocationSubject);
        ThreadIdentityManager.resetChecked(token);
    }

    @Test
    public void testAppThreadIdentityWithMultipleTIS_oneWithAppThreadDisabled() throws ThreadIdentityException {
        setAppFlowExpectations(tis1, true);
        setAppFlowExpectations(tis2, false);
        setAndResetAppSubjectForTwoTIS();
    }

    @Test
    public void testIsJ2CThreadIdentityEnabled_oneJ2CIS() {
        setJ2CThreadIdentityEnabledExpectation(j2cIs1, true);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs1);
        assertTrue("The J2C thread identity must be enabled", ThreadIdentityManager.isJ2CThreadIdentityEnabled());
    }

    @Test
    public void testIsJ2CThreadIdentityEnabled_multipleJ2CIS() {
        setJ2CThreadIdentityEnabledExpectation(j2cIs1, true);
        setJ2CThreadIdentityEnabledExpectation(j2cIs2, false);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs1);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs2);
        assertTrue("The J2C thread identity must be enabled", ThreadIdentityManager.isJ2CThreadIdentityEnabled());
    }

    @Test
    public void testIsJ2CThreadIdentityEnabled_bothJ2CISFalse() {
        setJ2CThreadIdentityEnabledExpectation(j2cIs1, false);
        setJ2CThreadIdentityEnabledExpectation(j2cIs2, false);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs1);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs2);
        assertFalse("The J2C thread identity must not be enabled", ThreadIdentityManager.isJ2CThreadIdentityEnabled());
    }

    @Test
    public void testJ2CThreadIdentity() throws ThreadIdentityException {
        setJ2CFlowExpectations(j2cIs1, true);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs1);
        Object token = ThreadIdentityManager.setJ2CThreadIdentity(invocationSubject);
        ThreadIdentityManager.resetChecked(token);
    }

    @Test
    public void testJ2CThreadIdentityWithMultipleJ2CIS() throws ThreadIdentityException {
        setJ2CFlowExpectations(j2cIs1, true);
        setJ2CFlowExpectations(j2cIs2, true);
        setAndResetJ2CSubjectForTwoTIS();
    }

    @Test
    public void testJ2CThreadIdentityWithMultipleJ2CIS_oneWithJ2CThreadDisabled() throws ThreadIdentityException {
        setJ2CFlowExpectations(j2cIs1, true);
        setJ2CFlowExpectations(j2cIs2, false);
        setAndResetJ2CSubjectForTwoTIS();
    }

    @Test
    public void testIsThreadIdentityEnabled() {
        setAppThreadIdentityEnabledExpectation(tis1, true);
        ThreadIdentityManager.addThreadIdentityService(tis1);
        assertTrue("The thread identity must be enabled.", ThreadIdentityManager.isThreadIdentityEnabled());
    }

    @Test
    public void testIsThreadIdentityEnabled_multipleIS_oneTrue() {
        setAppThreadIdentityEnabledExpectation(tis1, false);
        setJ2CThreadIdentityEnabledExpectation(j2cIs1, false);
        setAppThreadIdentityEnabledExpectation(tis2, false);
        setJ2CThreadIdentityEnabledExpectation(j2cIs2, true);
        addTwoTIS();
        addTwoJ2CIdentityServices();
        assertTrue("The thread identity must be enabled.", ThreadIdentityManager.isThreadIdentityEnabled());
    }

    @Test
    public void testRemovingOneTIS() throws ThreadIdentityException {
        setAppFlowExpectations(tis2, true);
        addTwoTIS();
        ThreadIdentityManager.removeThreadIdentityService(tis1);
        Object token = ThreadIdentityManager.setAppThreadIdentity(invocationSubject);
        ThreadIdentityManager.resetChecked(token);
    }

    @Test
    public void testRemovingOneTIS_afterSetAppThreadIdentity() throws ThreadIdentityException {
        setAppFlowExpectations(tis1, true);
        setAppFlowExpectations(tis2, true);
        addTwoTIS();
        Object token = ThreadIdentityManager.setAppThreadIdentity(invocationSubject);
        ThreadIdentityManager.removeThreadIdentityService(tis1);
        ThreadIdentityManager.resetChecked(token);
    }

    /*
     * This would be odd, but the ThreadIdentityService.set(Subject subject)
     * javadoc indicates that the method must return null if threadIdentity support is
     * disabled, but set would not be called if isAppThreadIdentityEnabled is false or if
     * isJ2CThreadIdentityEnabled is false.
     */
    @Test
    public void testWithNullCredential() throws ThreadIdentityException {
        mockery.checking(new Expectations() {
            {
                allowing(tis1).isAppThreadIdentityEnabled();
                will(returnValue(true));
                one(tis1).set(invocationSubject);
                will(returnValue(null));
                never(tis1).reset(null);
            }
        });

        setAppFlowExpectations(tis2, true);
        addTwoTIS();
        Object token = ThreadIdentityManager.setAppThreadIdentity(invocationSubject);
        ThreadIdentityManager.resetChecked(token);
    }

    // Test runAsServer
    @Test
    public void testRunAsServer() throws ThreadIdentityException {
        setRunAsServerFlowExpectations(tis1, true);
        setAppThreadIdentityEnabledExpectation(tis1, true);
        ThreadIdentityManager.addThreadIdentityService(tis1);
        Object token = ThreadIdentityManager.runAsServer();
        ThreadIdentityManager.resetChecked(token);
    }

    @Test
    public void testRunAsServerWithMultipleTIS() throws ThreadIdentityException {
        setRunAsServerFlowExpectations(tis1, true);
        setRunAsServerFlowExpectations(tis2, true);
        setAppThreadIdentityEnabledExpectation(tis1, true);
        setAppThreadIdentityEnabledExpectation(tis2, true);
        setAndResetRunAsServerForTwoTIS();
    }

    @Test
    public void testRunAsServerWithMultipleTIS_oneFalse() throws ThreadIdentityException {
        setRunAsServerFlowExpectations(tis1, true);
        setRunAsServerFlowExpectations(tis2, false);
        setAppThreadIdentityEnabledExpectation(tis1, true);
        setAppThreadIdentityEnabledExpectation(tis2, false);
        setAndResetRunAsServerForTwoTIS();
    }

    @Test
    public void testGetJ2CInvocationSubject() {
        setGetJ2CInvocationSubjectExpectation(j2cIs1, invocationSubject);
        setJ2CThreadIdentityEnabledExpectation(j2cIs1, true);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs1);
        assertNotNull("There must be a J2C subject", ThreadIdentityManager.getJ2CInvocationSubject());
    }

    @Test
    public void testGetJ2CInvocationSubjectWithMultipleJ2CIS() {
        setGetJ2CInvocationSubjectExpectation(j2cIs1, invocationSubject);
        setGetJ2CInvocationSubjectExpectation(j2cIs2, invocationSubject);
        setJ2CThreadIdentityEnabledExpectation(j2cIs1, true);
        setJ2CThreadIdentityEnabledExpectation(j2cIs2, true);
        addTwoJ2CIdentityServices();
        assertNotNull("There must be a J2C subject", ThreadIdentityManager.getJ2CInvocationSubject());
    }

    @Test
    public void testGetJ2CInvocationSubjectWithMultipleJ2CIS_oneNull() {
        setGetJ2CInvocationSubjectExpectation(j2cIs1, invocationSubject);
        setGetJ2CInvocationSubjectExpectation(j2cIs2, null);
        setJ2CThreadIdentityEnabledExpectation(j2cIs1, true);
        setJ2CThreadIdentityEnabledExpectation(j2cIs2, true);
        addTwoJ2CIdentityServices();
        assertNotNull("There must be a J2C subject", ThreadIdentityManager.getJ2CInvocationSubject());
    }

    @Test
    public void testGetJ2CInvocationSubjectWithMultipleJ2CIS_bothNull() {
        setGetJ2CInvocationSubjectExpectation(j2cIs1, null);
        setGetJ2CInvocationSubjectExpectation(j2cIs2, null);
        setJ2CThreadIdentityEnabledExpectation(j2cIs1, true);
        setJ2CThreadIdentityEnabledExpectation(j2cIs2, true);
        addTwoJ2CIdentityServices();
        assertNull("There must be not be a J2C subject", ThreadIdentityManager.getJ2CInvocationSubject());
    }

    // @Test - Disable test until messages are added
    public void testMessages_adding() throws Exception {
        SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=info");
        outputMgr.captureStreams();

        try {
            setTracingExpectations(tis1, false);
            ThreadIdentityManager.addThreadIdentityService(tis1);

            String msg = "CWWKS2200I";
            assertTrue("Unable to find registration message", outputMgr.checkForMessages(msg));
        } finally {
            outputMgr.resetStreams();
            outputMgr.restoreStreams();
        }
    }

    // @Test - Disable test until messages are added
    public void testMessages_removing() throws Exception {
        SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=info");
        outputMgr.captureStreams();

        try {
            setTracingExpectations(tis1, false);
            ThreadIdentityManager.addThreadIdentityService(tis1);
            ThreadIdentityManager.removeThreadIdentityService(tis1);

            String msg = "CWWKS2201I";
            assertTrue("Unable to find deregistration message", outputMgr.checkForMessages(msg));
        } finally {
            outputMgr.resetStreams();
            outputMgr.restoreStreams();
        }
    }

    private void setTracingExpectations(final ThreadIdentityService tis, final boolean appThreadIdentityEnabled) {
        final Object tracingToken = new Object();
        mockery.checking(new Expectations() {
            {
                allowing(tis).isAppThreadIdentityEnabled();
                will(returnValue(appThreadIdentityEnabled));
            }
        });
        if (appThreadIdentityEnabled) {
            mockery.checking(new Expectations() {
                {
                    allowing(tis).runAsServer();
                    will(returnValue(tracingToken));
                    allowing(tis).reset(tracingToken);
                }
            });
        }
    }

    private void setAndResetAppSubjectForTwoTIS() throws ThreadIdentityException {
        addTwoTIS();
        Object token = ThreadIdentityManager.setAppThreadIdentity(invocationSubject);
        ThreadIdentityManager.resetChecked(token);
    }

    private void addTwoTIS() {
        ThreadIdentityManager.addThreadIdentityService(tis1);
        ThreadIdentityManager.addThreadIdentityService(tis2);
    }

    private void addTwoJ2CIdentityServices() {
        ThreadIdentityManager.addJ2CIdentityService(j2cIs1);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs2);
    }

    private void setAndResetJ2CSubjectForTwoTIS() throws ThreadIdentityException {
        addTwoJ2CIdentityServices();
        Object token = ThreadIdentityManager.setJ2CThreadIdentity(invocationSubject);
        ThreadIdentityManager.resetChecked(token);
    }

    private void setAndResetRunAsServerForTwoTIS() throws ThreadIdentityException {
        addTwoTIS();
        Object token = ThreadIdentityManager.runAsServer();
        ThreadIdentityManager.resetChecked(token);
    }

    private void setAppThreadIdentityEnabledExpectation(final ThreadIdentityService tis, final boolean value) {
        mockery.checking(new Expectations() {
            {
                allowing(tis).isAppThreadIdentityEnabled();
                will(returnValue(value));
            }
        });
    }

    private void setJ2CThreadIdentityEnabledExpectation(final J2CIdentityService j2cIdentityService, final boolean value) {
        mockery.checking(new Expectations() {
            {
                allowing(j2cIdentityService).isJ2CThreadIdentityEnabled();
                will(returnValue(value));
            }
        });
    }

    private void setAppFlowExpectations(final ThreadIdentityService tis, final boolean appThreadEnabled) {
        if (appThreadEnabled) {
            final Object token = new Object();
            mockery.checking(new Expectations() {
                {
                    one(tis).isAppThreadIdentityEnabled();
                    will(returnValue(appThreadEnabled));
                    one(tis).set(invocationSubject);
                    will(returnValue(token));
                    one(tis).reset(token);
                }
            });
        } else {
            mockery.checking(new Expectations() {
                {
                    one(tis).isAppThreadIdentityEnabled();
                    will(returnValue(appThreadEnabled));
                    never(tis).set(invocationSubject);
                    never(tis).reset(with(anything()));
                }
            });
        }
    }

    private void setJ2CFlowExpectations(final J2CIdentityService j2cIdentityService, final boolean j2cThreadEnabled) {
        if (j2cThreadEnabled) {
            final Object token = new Object();
            mockery.checking(new Expectations() {
                {
                    one(j2cIdentityService).isJ2CThreadIdentityEnabled();
                    will(returnValue(j2cThreadEnabled));
                    one(j2cIdentityService).set(invocationSubject);
                    will(returnValue(token));
                    one(j2cIdentityService).reset(token);
                }
            });
        } else {
            mockery.checking(new Expectations() {
                {
                    one(j2cIdentityService).isJ2CThreadIdentityEnabled();
                    will(returnValue(j2cThreadEnabled));
                    never(j2cIdentityService).set(invocationSubject);
                    never(j2cIdentityService).reset(with(anything()));
                }
            });
        }
    }

    private void setRunAsServerFlowExpectations(final ThreadIdentityService tis, boolean runAsExpected) {
        if (runAsExpected) {
            final Object runAsToken = new Object();
            mockery.checking(new Expectations() {
                {
                    one(tis).runAsServer();
                    will(returnValue(runAsToken));
                    one(tis).reset(runAsToken);
                }
            });
        } else {
            mockery.checking(new Expectations() {
                {
                    never(tis).runAsServer();
                    never(tis).reset(with(anything()));
                }
            });
        }
    }

    private void setGetJ2CInvocationSubjectExpectation(final J2CIdentityService j2cIdentityService, final Subject subject) {
        mockery.checking(new Expectations() {
            {
                allowing(j2cIdentityService).getJ2CInvocationSubject();
                will(returnValue(subject));
            }
        });
    }

    private void setAppFlowExpectations_Exception(final ThreadIdentityService tis) {
        mockery.checking(new Expectations() {
            {
                one(tis).isAppThreadIdentityEnabled();
                will(returnValue(true));
                one(tis).set(invocationSubject);
                will(throwException(new RuntimeException("ThreadIdentityService.set() blew up.")));
                never(tis).reset(with(anything()));
            }
        });
    }

    private void setJ2CFlowExpectations_Exception(final J2CIdentityService j2cIdentityService) {
        mockery.checking(new Expectations() {
            {
                one(j2cIdentityService).isJ2CThreadIdentityEnabled();
                will(returnValue(true));
                one(j2cIdentityService).set(invocationSubject);
                will(throwException(new RuntimeException("J2CIdentityService.set() blew up.")));
                never(j2cIdentityService).reset(with(anything()));
            }
        });
    }

    private void setAppFlowExpectations_PostException(final ThreadIdentityService tis) {
        mockery.checking(new Expectations() {
            {
                never(tis).isAppThreadIdentityEnabled();
                never(tis).set(with(any(Subject.class)));
                never(tis).reset(with(anything()));
            }
        });
    }

    private void setJ2CFlowExpectations_PostException(final J2CIdentityService jis) {
        mockery.checking(new Expectations() {
            {
                never(jis).isJ2CThreadIdentityEnabled();
                never(jis).set(with(any(Subject.class)));
                never(jis).reset(with(anything()));
            }
        });
    }

    private void resetAppFlowExpectations_Exception(final ThreadIdentityService tis, final String msg) {
        final Object token = new Object();
        mockery.checking(new Expectations() {
            {
                one(tis).isAppThreadIdentityEnabled();
                will(returnValue(true));
                one(tis).set(invocationSubject);
                will(returnValue(token));
                one(tis).reset(token);
                will(throwException(new RuntimeException(msg)));
            }
        });
    }

    private void resetJ2CFlowExpectations_Exception(final J2CIdentityService jis, final String msg) {
        final Object token = new Object();
        mockery.checking(new Expectations() {
            {
                one(jis).isJ2CThreadIdentityEnabled();
                will(returnValue(true));
                one(jis).set(invocationSubject);
                will(returnValue(token));
                one(jis).reset(token);
                will(throwException(new RuntimeException(msg)));
            }
        });
    }

    private void resetAppFlowExpectations_Exception(final ThreadIdentityService tis) {
        resetAppFlowExpectations_Exception(tis, "ThreadIdentityService.reset() blew up.");
    }

    private void resetJ2CFlowExpectations_Exception(final J2CIdentityService jis) {
        resetJ2CFlowExpectations_Exception(jis, resetREJ2C);
    }

    @Test(expected = ThreadIdentityException.class)
    public void exceptionDuringSet_First() throws ThreadIdentityException {
        setAppFlowExpectations_Exception(tis1);
        setAppFlowExpectations_PostException(tis2);
        setAppFlowExpectations_PostException(tis3);

        ThreadIdentityManager.addThreadIdentityService(tis1);
        ThreadIdentityManager.addThreadIdentityService(tis2);
        ThreadIdentityManager.addThreadIdentityService(tis3);

        ThreadIdentityManager.setAppThreadIdentity(invocationSubject);
    }

    @Test(expected = ThreadIdentityException.class)
    public void exceptionDuringSet_Second() throws ThreadIdentityException {
        setAppFlowExpectations(tis1, true);
        setAppFlowExpectations_Exception(tis2);
        setAppFlowExpectations_PostException(tis3);

        ThreadIdentityManager.addThreadIdentityService(tis1);
        ThreadIdentityManager.addThreadIdentityService(tis2);
        ThreadIdentityManager.addThreadIdentityService(tis3);

        Object token = ThreadIdentityManager.setAppThreadIdentity(invocationSubject);
        ThreadIdentityManager.resetChecked(token);
    }

    @Test(expected = ThreadIdentityException.class)
    public void exceptionDuringSet_Third() throws ThreadIdentityException {
        setAppFlowExpectations(tis1, true);
        setAppFlowExpectations(tis2, true);
        setAppFlowExpectations_Exception(tis3);

        ThreadIdentityManager.addThreadIdentityService(tis1);
        ThreadIdentityManager.addThreadIdentityService(tis2);
        ThreadIdentityManager.addThreadIdentityService(tis3);

        Object token = ThreadIdentityManager.setAppThreadIdentity(invocationSubject);
        ThreadIdentityManager.resetChecked(token);
    }

    @Test(expected = ThreadIdentityException.class)
    public void exceptionDuringreset_First() throws ThreadIdentityException {
        resetAppFlowExpectations_Exception(tis1);
        setAppFlowExpectations(tis2, true);
        setAppFlowExpectations(tis3, true);

        ThreadIdentityManager.addThreadIdentityService(tis1);
        ThreadIdentityManager.addThreadIdentityService(tis2);
        ThreadIdentityManager.addThreadIdentityService(tis3);

        Object token = ThreadIdentityManager.setAppThreadIdentity(invocationSubject);
        ThreadIdentityManager.resetChecked(token);
    }

    @Test(expected = ThreadIdentityException.class)
    public void exceptionDuringReset_Second() throws ThreadIdentityException {
        setAppFlowExpectations(tis1, true);
        resetAppFlowExpectations_Exception(tis2);
        setAppFlowExpectations(tis3, true);

        ThreadIdentityManager.addThreadIdentityService(tis1);
        ThreadIdentityManager.addThreadIdentityService(tis2);
        ThreadIdentityManager.addThreadIdentityService(tis3);

        Object token = ThreadIdentityManager.setAppThreadIdentity(invocationSubject);
        ThreadIdentityManager.resetChecked(token);

    }

    @Test(expected = ThreadIdentityException.class)
    public void exceptionDuringReset_Third() throws ThreadIdentityException {
        setAppFlowExpectations(tis1, true);
        setAppFlowExpectations(tis2, true);
        resetAppFlowExpectations_Exception(tis3);

        ThreadIdentityManager.addThreadIdentityService(tis1);
        ThreadIdentityManager.addThreadIdentityService(tis2);
        ThreadIdentityManager.addThreadIdentityService(tis3);

        Object token = ThreadIdentityManager.setAppThreadIdentity(invocationSubject);
        ThreadIdentityManager.resetChecked(token);
    }

    @Test(expected = ThreadIdentityException.class)
    public void exceptionDuringSetAndReset() throws ThreadIdentityException {
        // successful set, but throw in reset after the second token throws in set
        resetAppFlowExpectations_Exception(tis1);
        // throw in set
        setAppFlowExpectations_Exception(tis2);
        // never handle this one
        setAppFlowExpectations_PostException(tis3);

        ThreadIdentityManager.addThreadIdentityService(tis1);
        ThreadIdentityManager.addThreadIdentityService(tis2);
        ThreadIdentityManager.addThreadIdentityService(tis3);

        try {
            ThreadIdentityManager.setAppThreadIdentity(invocationSubject);
        } catch (ThreadIdentityException tie) {
            assertEquals(tie.getMessage(), expectedSetRE);
            throw tie;
        }
    }

    @Test(expected = ThreadIdentityException.class)
    public void exceptionsFromTwoResets() throws ThreadIdentityException {
        String msg = "ThreadIdentityService.reset() blew up on anotherTIS.";

        // succeed with set, blow up in reset on the way back out for the first two
        resetAppFlowExpectations_Exception(tis1);
        resetAppFlowExpectations_Exception(tis2, msg);
        // business as usual for the third one
        setAppFlowExpectations(tis3, true);

        ThreadIdentityManager.addThreadIdentityService(tis1);
        ThreadIdentityManager.addThreadIdentityService(tis2);
        ThreadIdentityManager.addThreadIdentityService(tis3);

        try {
            Object token = ThreadIdentityManager.setAppThreadIdentity(invocationSubject);
            ThreadIdentityManager.resetChecked(token);
        } catch (ThreadIdentityException tie) {
            // verify we got the message from the second TIS's reset
            assertEquals(tie.getMessage(), "java.lang.RuntimeException: " + msg);
            throw tie;
        }
        assertTrue("We should never get here.", false);
    }

    @Test(expected = ThreadIdentityException.class)
    public void exceptionDuringSetJ2C_First() throws ThreadIdentityException {
        // blow up in set, never reset
        setJ2CFlowExpectations_Exception(j2cIs1);
        // do nothing with these two
        setJ2CFlowExpectations_PostException(j2cIs2);
        setJ2CFlowExpectations_PostException(j2cIs3);

        ThreadIdentityManager.addJ2CIdentityService(j2cIs1);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs2);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs3);

        ThreadIdentityManager.setJ2CThreadIdentity(invocationSubject);
    }

    @Test(expected = ThreadIdentityException.class)
    public void exceptionDuringSetJ2C_Second() throws ThreadIdentityException {
        // full cycle for the first one
        setJ2CFlowExpectations(j2cIs1, true);
        // blow up in set for the second one
        setJ2CFlowExpectations_Exception(j2cIs2);
        // neither set nor reset the third one
        setJ2CFlowExpectations_PostException(j2cIs3);

        ThreadIdentityManager.addJ2CIdentityService(j2cIs1);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs2);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs3);

        ThreadIdentityManager.setJ2CThreadIdentity(invocationSubject);
    }

    @Test(expected = ThreadIdentityException.class)
    public void exceptionDuringSetJ2C_Third() throws ThreadIdentityException {
        // full cycle for the first two
        setJ2CFlowExpectations(j2cIs1, true);
        setJ2CFlowExpectations(j2cIs2, true);
        // blow up in set on the third one, reset everyone else
        setJ2CFlowExpectations_Exception(j2cIs3);

        ThreadIdentityManager.addJ2CIdentityService(j2cIs1);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs2);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs3);

        ThreadIdentityManager.setJ2CThreadIdentity(invocationSubject);
    }

    @Test(expected = ThreadIdentityException.class)
    public void exceptionDuringresetJ2C_First() throws ThreadIdentityException {
        // set and blow up in reset
        resetJ2CFlowExpectations_Exception(j2cIs1);
        // business as usual for the other two
        setJ2CFlowExpectations(j2cIs2, true);
        setJ2CFlowExpectations(j2cIs3, true);

        ThreadIdentityManager.addJ2CIdentityService(j2cIs1);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs2);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs3);

        Object token = ThreadIdentityManager.setJ2CThreadIdentity(invocationSubject);
        ThreadIdentityManager.resetChecked(token);
    }

    @Test(expected = ThreadIdentityException.class)
    public void exceptionDuringResetJ2C_Second() throws ThreadIdentityException {
        // business as usual for the first one
        setJ2CFlowExpectations(j2cIs1, true);
        // blow up in reset for the second one
        resetJ2CFlowExpectations_Exception(j2cIs2);
        // business as usual for the third one
        setJ2CFlowExpectations(j2cIs3, true);

        ThreadIdentityManager.addJ2CIdentityService(j2cIs1);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs2);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs3);

        Object token = ThreadIdentityManager.setJ2CThreadIdentity(invocationSubject);
        ThreadIdentityManager.resetChecked(token);
    }

    @Test(expected = ThreadIdentityException.class)
    public void exceptionDuringResetJ2C_Third() throws ThreadIdentityException {
        // business as usual for the first two
        setJ2CFlowExpectations(j2cIs1, true);
        setJ2CFlowExpectations(j2cIs2, true);
        // blow up in reset for the third one
        resetJ2CFlowExpectations_Exception(j2cIs3);

        ThreadIdentityManager.addJ2CIdentityService(j2cIs1);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs2);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs3);

        Object token = ThreadIdentityManager.setJ2CThreadIdentity(invocationSubject);
        ThreadIdentityManager.resetChecked(token);
    }

    @Test(expected = ThreadIdentityException.class)
    public void exceptionDuringSetJ2CAndResetJ2C() throws ThreadIdentityException {
        // successful set for the first one, but blow up in reset on the way out
        resetJ2CFlowExpectations_Exception(j2cIs1);
        // blow up in set for the second one
        setJ2CFlowExpectations_Exception(j2cIs2);
        // never handle the third one
        setJ2CFlowExpectations_PostException(j2cIs3);

        ThreadIdentityManager.addJ2CIdentityService(j2cIs1);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs2);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs3);

        try {
            Object token = ThreadIdentityManager.setJ2CThreadIdentity(invocationSubject);
            ThreadIdentityManager.resetChecked(token);
        } catch (ThreadIdentityException tie) {
            // make sure we got the exception from the second TIS' set call
            assertEquals(tie.getMessage(), expectedSetREJ2C);
            throw tie;
        }
        assertTrue("We should never get here.", false);
    }

    @Test(expected = ThreadIdentityException.class)
    public void exceptionsFromTwoJ2CResets() throws ThreadIdentityException {
        String msg = "J2CIdentityService.reset() blew up on anotherJ2CIdentityService.";

        // succeed in set, but blow up on reset on the way out for the first two
        resetJ2CFlowExpectations_Exception(j2cIs1);
        resetJ2CFlowExpectations_Exception(j2cIs2, msg);
        // set and reset as usual
        setJ2CFlowExpectations(j2cIs3, true);

        ThreadIdentityManager.addJ2CIdentityService(j2cIs1);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs2);
        ThreadIdentityManager.addJ2CIdentityService(j2cIs3);

        try {
            Object token = ThreadIdentityManager.setJ2CThreadIdentity(invocationSubject);
            ThreadIdentityManager.resetChecked(token);
        } catch (ThreadIdentityException tie) {
            // make sure we got the exception from the second TIS' reset call
            assertEquals(tie.getMessage(), expectedRE + msg);
            throw tie;
        }
        assertTrue("We should never get here.", false);
    }
}