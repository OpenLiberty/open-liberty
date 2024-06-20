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
package com.ibm.ws.zos.tx.internal;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.zos.tx.internal.rrs.BeginContextReturnType;
import com.ibm.ws.zos.tx.internal.rrs.RRSServices;
import com.ibm.ws.zos.tx.internal.rrs.RetrieveCurrentContextTokenReturnType;
import com.ibm.ws.zos.tx.internal.rrs.RetrieveSideInformationFastReturnType;
import com.ibm.ws.zos.tx.internal.rrs.SwitchContextReturnType;

/**
 * Unit tests for the context manager. These test the Java portion of the
 * context management, and do not exercise the native code at all.
 */
public class ContextManagerTest {
    /**
     * Mock environment for native methods.
     */
    private static final Mockery mockery = new JUnit4Mockery();

    /**
     * Create our artificial RRS services implementation
     */
    private static final RRSServices rrsServices = mockery.mock(RRSServices.class);

    /**
     * Test that the context factory interacts with the native code as expected.
     */
    @Test
    public void testContextFactory() throws Exception {
        // Define the input and output parms for the begin context method
        final byte[] rmToken = new byte[] { (byte) 0x12, (byte) 0x34 };
        final byte[] ctxToken = new byte[] { (byte) 0x34, (byte) 0x56 };
        final byte[] ctxRegToken = new byte[] { (byte) 0x78, (byte) 0x90 };

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        mockery.checking(new Expectations() {
            {
                oneOf(rrsServices).beginContext(with(rmToken));
                will(returnValue(new BeginContextReturnType(0, ctxToken, ctxRegToken)));
            }
        });

        // Call the context factory for begin.
        ContextManagerImpl.ContextFactory cf = new ContextManagerImpl.ContextFactory(rrsServices, rmToken);
        ContextManagerImpl.ContextImpl ctx = cf.create();
        assertTrue(ctx != null);
        assertTrue(Arrays.equals(ctx.getContextToken(), ctxToken));
        assertTrue(Arrays.equals(ctx.getContextRegistryToken(), ctxRegToken));

        mockery.assertIsSatisfied();
    }

    /**
     * Test that the context destroyer interacts with the native code as expected.
     */
    @Test
    public void testContextDestroyer() throws Exception {
        // A context to destroy
        final byte[] ctxToken = new byte[] { (byte) 0x01, (byte) 0x23 };
        final byte[] ctxRegToken = new byte[] { (byte) 0x45, (byte) 0x67 };
        final ContextManagerImpl.ContextImpl ctx = new ContextManagerImpl.ContextImpl(ctxToken, ctxRegToken);

        // Tell JMock that we expect end context to be called, and tell it what
        // to return.
        mockery.checking(new Expectations() {
            {
                oneOf(rrsServices).endContext(with(ctxRegToken), with(RRSServices.CTX_NORMAL_TERMINATION));
                will(returnValue(0));
            }
        });

        // Call the context destroyer.
        ContextManagerImpl.ContextDestroyer cd = new ContextManagerImpl.ContextDestroyer(rrsServices);
        cd.destroy(ctx);

        mockery.assertIsSatisfied();
    }

    /**
     * Test thread termination with a dirty UR. All other processing completes normally.
     */
    @Test
    public void testThreadTerminationWithDirtyUR() throws Exception {
        // Define the input and output parms for the context manager.
        final byte[] rmToken = new byte[] { (byte) 0x12, (byte) 0x34 };
        final ContextManagerImpl cm = createContextManager(rrsServices, rmToken);
        final byte[] ctxToken = new byte[] { (byte) 0x23, (byte) 0x45 };
        final byte[] ctxRegToken = new byte[] { (byte) 0x34, (byte) 0x56 };
        final byte[] oldCtxToken = ContextManagerImpl.ContextImpl.NATIVE_CONTEXT.getContextToken();

        UOWCoordinator mockUOW_tt = mockery.mock(UOWCoordinator.class, "mockUOW_tt");
        final Sequence sequence = mockery.sequence("sequence");
        mockery.checking(new Expectations() {
            {
                // Begin context expectations.
                oneOf(rrsServices).beginContext(with(rmToken));
                inSequence(sequence);
                will(returnValue(new BeginContextReturnType(0, ctxToken, ctxRegToken)));

                oneOf(rrsServices).contextSwitch(with(ctxRegToken));
                inSequence(sequence);
                will(returnValue(new SwitchContextReturnType(0, oldCtxToken)));

                // Thread termination: Retrieve environment info. No bits set.
                oneOf(rrsServices).retrieveSideInformationFast(with(ctxToken), with(0));
                inSequence(sequence);
                will(returnValue(new RetrieveSideInformationFastReturnType(0, 0)));

                // Thread termination: End the UR. It is not in reset.
                oneOf(rrsServices).endUR(with(RRSServices.ATR_ROLLBACK_ACTION), with(aNull(byte[].class)));
                inSequence(sequence);
                will(returnValue(RRSServices.ATR_OK));

                // Thread termination: Switch a native context
                oneOf(rrsServices).contextSwitch(with(ContextManagerImpl.ContextImpl.NATIVE_CONTEXT.getContextRegistryToken()));
                inSequence(sequence);
                will(returnValue(new SwitchContextReturnType(0, ctxToken)));
            }
        });

        // Begin a context.
        cm.begin(mockUOW_tt);

        // Simulate thread termination.
        cm.threadTerminating();

        // Verify that the associated UOW was reset.
        UOWCoordinator uowc = cm.getCurrentContext().getUnitOfWork();
        assertTrue("The context will be put back in the pool. The UOW should have been null. It was: " + uowc, uowc == null);

        // Verify that the state of the context is CLEAN (0).
        int state = cm.getCurrentContext().getState();
        assertTrue("The context will be put back in the pool. The local CTX state should have been CLEAN. It was: " + state, state == 0);

        mockery.assertIsSatisfied();
    }

    /**
     * Test that the call to destroy the context manager interacts well with a subsequent call to terminate a thread.
     * Context clean up should have been done by the call to destroy the context manager. All the thread termination
     * call should do is switch the native context on the thread.
     * No IllegalStateExceptions or NPEs are expected.
     */
    @Test
    public void testCtxMgrDestroyAndThreadTermination() throws Exception {
        // Define the input and output parms for the context manager.
        final byte[] rmToken = new byte[] { (byte) 0x12, (byte) 0x34 };
        final ContextManagerImpl cm = createContextManager(rrsServices, rmToken);
        final byte[] ctxToken = new byte[] { (byte) 0x23, (byte) 0x45 };
        final byte[] ctxRegToken = new byte[] { (byte) 0x34, (byte) 0x56 };
        final byte[] oldCtxToken = ContextManagerImpl.ContextImpl.NATIVE_CONTEXT.getContextToken();

        UOWCoordinator mockUOW_cdtt = mockery.mock(UOWCoordinator.class, "mockUOW_cdtt");
        final Sequence sequence = mockery.sequence("sequence");
        mockery.checking(new Expectations() {
            {
                // Begin context expectations.
                oneOf(rrsServices).beginContext(with(rmToken));
                inSequence(sequence);
                will(returnValue(new BeginContextReturnType(0, ctxToken, ctxRegToken)));

                oneOf(rrsServices).contextSwitch(with(ctxRegToken));
                inSequence(sequence);
                will(returnValue(new SwitchContextReturnType(0, oldCtxToken)));

                // DestroyContexMgr: processing
                oneOf(rrsServices).retrieveSideInformationFast(with(ctxToken), with(0));
                inSequence(sequence);
                will(returnValue(new RetrieveSideInformationFastReturnType(0, RRSServices.ATR_UR_STATE_IN_RESET_MASK)));

                oneOf(rrsServices).endContext(with(ctxRegToken), with(RRSServices.CTX_FORCED_END_OF_CONTEXT));
                inSequence(sequence);
                will(returnValue(0));

                // Thread termination: Switch a native context
                oneOf(rrsServices).contextSwitch(with(ContextManagerImpl.ContextImpl.NATIVE_CONTEXT.getContextRegistryToken()));
                inSequence(sequence);
                will(returnValue(new SwitchContextReturnType(0, oldCtxToken)));
            }
        });

        // Begin a context.
        cm.begin(mockUOW_cdtt);

        // Deactivate the context manager. This calls destroyContextManager().
        cm.deactivate();

        // Simulate thread termination.
        cm.threadTerminating();

        mockery.assertIsSatisfied();
    }

    /**
     * Test that the call to thread termination interacts well with a subsequent call to destroy the context manager.
     * Thread termination should do most of the work cleaning things up.
     * All the destroy context manager path does is to end the context.
     * No exceptions are expected.
     */
    @Test
    public void testThreadTerminationAndCtxMgrDestroy() throws Exception {
        // Define the input and output parms for the context manager.
        final byte[] rmToken = new byte[] { (byte) 0x12, (byte) 0x34 };
        final ContextManagerImpl cm = createContextManager(rrsServices, rmToken);
        final byte[] ctxToken = new byte[] { (byte) 0x23, (byte) 0x45 };
        final byte[] ctxRegToken = new byte[] { (byte) 0x34, (byte) 0x56 };
        final byte[] oldCtxToken = ContextManagerImpl.ContextImpl.NATIVE_CONTEXT.getContextToken();

        UOWCoordinator mockUOW_ttcd = mockery.mock(UOWCoordinator.class, "mockUOW_ttcd");
        final Sequence sequence = mockery.sequence("sequence");
        mockery.checking(new Expectations() {
            {
                // Begin context expectations.
                oneOf(rrsServices).beginContext(with(rmToken));
                inSequence(sequence);
                will(returnValue(new BeginContextReturnType(0, ctxToken, ctxRegToken)));

                oneOf(rrsServices).contextSwitch(with(ctxRegToken));
                inSequence(sequence);
                will(returnValue(new SwitchContextReturnType(0, oldCtxToken)));

                // Thread termination: Retrieve info.
                oneOf(rrsServices).retrieveSideInformationFast(with(ctxToken), with(0));
                inSequence(sequence);
                will(returnValue(new RetrieveSideInformationFastReturnType(0, RRSServices.ATR_UR_STATE_IN_RESET_MASK)));

                // Thread termination: Switch a native context
                oneOf(rrsServices).contextSwitch(with(ContextManagerImpl.ContextImpl.NATIVE_CONTEXT.getContextRegistryToken()));
                inSequence(sequence);
                will(returnValue(new SwitchContextReturnType(0, ctxToken)));

                // DestroyContexMgr: End the context
                oneOf(rrsServices).endContext(with(ctxRegToken), with(RRSServices.CTX_FORCED_END_OF_CONTEXT));
                inSequence(sequence);
                will(returnValue(0));
            }
        });

        // Begin a context.
        cm.begin(mockUOW_ttcd);

        // Simulate thread termination.
        cm.threadTerminating();

        // Deactivate the context manager. This calls destroyContextManager()
        cm.deactivate();

        mockery.assertIsSatisfied();
    }

    /**
     * Test that the call to terminate a thread interacts well with a subsequent call to destroy the context manager.
     * The thread termination processing should end the context after a failed attempt to
     * end the UR (UR_STATE_ERROR). The destroyContextManager call should have nothing to do.
     */
    @Test
    public void testThreadTerminationWithBadEndURRetCodeAndCtxMgrDestroy() throws Exception {
        // Define the input and output parms for the context manager.
        final byte[] rmToken = new byte[] { (byte) 0x12, (byte) 0x34 };
        final ContextManagerImpl cm = createContextManager(rrsServices, rmToken);
        final byte[] ctxToken = new byte[] { (byte) 0x23, (byte) 0x45 };
        final byte[] ctxRegToken = new byte[] { (byte) 0x34, (byte) 0x56 };
        final byte[] oldCtxToken = ContextManagerImpl.ContextImpl.NATIVE_CONTEXT.getContextToken();

        UOWCoordinator mockUOW_ttctcd = mockery.mock(UOWCoordinator.class, "mockUOW_ttctcd");
        final Sequence sequence = mockery.sequence("sequence");
        mockery.checking(new Expectations() {
            {
                // Begin context expectations.
                oneOf(rrsServices).beginContext(with(rmToken));
                inSequence(sequence);
                will(returnValue(new BeginContextReturnType(0, ctxToken, ctxRegToken)));

                oneOf(rrsServices).contextSwitch(with(ctxRegToken));
                inSequence(sequence);
                will(returnValue(new SwitchContextReturnType(0, oldCtxToken)));

                // Thread termination: Retrieve environment info. No bits set.
                oneOf(rrsServices).retrieveSideInformationFast(with(ctxToken), with(0));
                inSequence(sequence);
                will(returnValue(new RetrieveSideInformationFastReturnType(0, 0)));

                // Thread termination: End the UR. It is not in reset. Returns UR_STATE_ERROR.
                oneOf(rrsServices).endUR(with(RRSServices.ATR_ROLLBACK_ACTION), with(aNull(byte[].class)));
                inSequence(sequence);
                will(returnValue(RRSServices.ATR_UR_STATE_ERROR));

                // Thread termination: Switch a native context
                oneOf(rrsServices).contextSwitch(with(ContextManagerImpl.ContextImpl.NATIVE_CONTEXT.getContextRegistryToken()));
                inSequence(sequence);
                will(returnValue(new SwitchContextReturnType(0, ctxToken)));

                // ThreadTermination: End the context due to he bad return code from ATREND
                oneOf(rrsServices).endContext(with(ctxRegToken), with(RRSServices.CTX_NORMAL_TERMINATION));
                inSequence(sequence);
                will(returnValue(0));

                // DestroyContexMgr: Should have nothing to do.
            }
        });

        // Begin a context.
        cm.begin(mockUOW_ttctcd);

        // Simulate thread termination.
        cm.threadTerminating();

        // Deactivate the context manager. This calls destroyContextManager()
        cm.deactivate();

        mockery.assertIsSatisfied();
    }

    /**
     * Test that the ContextFactory can deal with an invalid RM token.
     */
    @Test
    public void testContextFactoryInvalidRMToken() throws Exception {
        // Define the input and output parms for the begin context method
        final byte[] badRmToken = new byte[] { (byte) 0x12, (byte) 0x34 };

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        mockery.checking(new Expectations() {
            {
                oneOf(rrsServices).beginContext(with(badRmToken));
                will(returnValue(new BeginContextReturnType(256, null, null)));
            }
        });

        // Call the context factory for begin.
        ContextManagerImpl.ContextFactory cf = new ContextManagerImpl.ContextFactory(rrsServices, badRmToken);
        ContextManagerImpl.ContextImpl ctx = cf.create();
        assertTrue(ctx == null);

        mockery.assertIsSatisfied();
    }

    /**
     * Test that the ContextDestroyer can deal with bad parameters.
     */
    @Test
    public void testContextDestroyerInvalidParms() throws Exception {
        // Define the input and output parms for the begin context method
        final byte[] badCtxToken = new byte[] { (byte) 0x00, (byte) 0x00 };
        final byte[] badCtxRegToken = new byte[] { (byte) 0x23, (byte) 0x45 };
        ContextManagerImpl.ContextDestroyer cd = new ContextManagerImpl.ContextDestroyer(rrsServices);

        // Try to pass an invalid context token to the context destroyer.
        mockery.checking(new Expectations() {
            {
                oneOf(rrsServices).endContext(with(badCtxRegToken), with(RRSServices.CTX_NORMAL_TERMINATION));
                will(returnValue(256));
            }
        });

        boolean caughtException = false;
        try {
            cd.destroy(new ContextManagerImpl.ContextImpl(badCtxToken, badCtxRegToken));
        } catch (RuntimeException re) {
            caughtException = true;
        }
        assertTrue(caughtException);
        mockery.assertIsSatisfied();
    }

    /**
     * Test a normal context begin/end flow.
     */
    @Test
    public void testContextBeginEnd() throws Exception {
        // Define the input and output parms for the context manager.
        final byte[] rmToken = new byte[] { (byte) 0x12, (byte) 0x34 };
        final ContextManagerImpl cm = createContextManager(rrsServices, rmToken);
        final byte[] ctxToken = new byte[] { (byte) 0x23, (byte) 0x45 };
        final byte[] ctxRegToken = new byte[] { (byte) 0x34, (byte) 0x56 };
        final byte[] oldCtxToken = ContextManagerImpl.ContextImpl.NATIVE_CONTEXT.getContextToken();

        UOWCoordinator beginEndUowCoord = mockery.mock(UOWCoordinator.class, "beginEndUowCoord");

        mockery.checking(new Expectations() {
            {
                oneOf(rrsServices).beginContext(with(rmToken));
                will(returnValue(new BeginContextReturnType(0, ctxToken, ctxRegToken)));
                oneOf(rrsServices).contextSwitch(with(ctxRegToken));
                will(returnValue(new SwitchContextReturnType(0, oldCtxToken)));
                oneOf(rrsServices).retrieveSideInformationFast(with(ctxToken), with(0));
                will(returnValue(new RetrieveSideInformationFastReturnType(0, RRSServices.ATR_UR_STATE_IN_RESET_MASK)));
            }
        });

        cm.begin(beginEndUowCoord);
        cm.end(beginEndUowCoord);
        mockery.assertIsSatisfied();
    }

    /**
     * Test an alternative context begin/suspend/end flow.
     */
    @Test
    public void textContextBeginSuspendEnd() throws Exception {
        // Define the input and output parms for the context manager.
        final byte[] rmToken = new byte[] { (byte) 0x12, (byte) 0x34 };
        final ContextManagerImpl cm = createContextManager(rrsServices, rmToken);
        final byte[] ctxToken = new byte[] { (byte) 0x23, (byte) 0x45 };
        final byte[] ctxRegToken = new byte[] { (byte) 0x34, (byte) 0x56 };
        final byte[] ctxToken2 = new byte[] { (byte) 0x45, (byte) 0x67 };
        final byte[] ctxRegToken2 = new byte[] { (byte) 0x56, (byte) 0x78 };

        UOWCoordinator beginSuspendEndUowCoord = mockery.mock(UOWCoordinator.class, "beginSuspendEndUowCoord");

        mockery.checking(new Expectations() {
            {
                oneOf(rrsServices).beginContext(with(rmToken));
                will(returnValue(new BeginContextReturnType(0, ctxToken, ctxRegToken)));
                oneOf(rrsServices).contextSwitch(with(ctxRegToken));
                will(returnValue(new SwitchContextReturnType(0, ContextManagerImpl.ContextImpl.NATIVE_CONTEXT.getContextToken())));
                oneOf(rrsServices).beginContext(with(rmToken));
                will(returnValue(new BeginContextReturnType(0, ctxToken2, ctxRegToken2)));
                oneOf(rrsServices).contextSwitch(with(ctxRegToken2));
                will(returnValue(new SwitchContextReturnType(0, ctxToken)));
                oneOf(rrsServices).retrieveSideInformationFast(with(ctxToken), with(0));
                will(returnValue(new RetrieveSideInformationFastReturnType(0, RRSServices.ATR_UR_STATE_IN_RESET_MASK)));
            }
        });

        cm.begin(beginSuspendEndUowCoord);
        cm.suspend(beginSuspendEndUowCoord);
        cm.end(beginSuspendEndUowCoord);
        mockery.assertIsSatisfied();
    }

    /**
     * Test an alternative context begin/suspend/end flow with a dirty UR.
     */
    @Test
    public void textContextBeginSuspendEndWithDirtyUR() throws Exception {
        // Define the input and output parms for the context manager.
        final byte[] rmToken = new byte[] { (byte) 0x12, (byte) 0x34 };
        final ContextManagerImpl cm = createContextManager(rrsServices, rmToken);
        final byte[] ctxToken = new byte[] { (byte) 0x23, (byte) 0x45 };
        final byte[] ctxRegToken = new byte[] { (byte) 0x34, (byte) 0x56 };
        final byte[] ctxToken2 = new byte[] { (byte) 0x45, (byte) 0x67 };
        final byte[] ctxRegToken2 = new byte[] { (byte) 0x56, (byte) 0x78 };

        UOWCoordinator beginSuspendEndDirtyURUowCoord = mockery.mock(UOWCoordinator.class, "beginSuspendEndDirtyURUowCoord");

        mockery.checking(new Expectations() {
            {
                // Begin:
                oneOf(rrsServices).beginContext(with(rmToken));
                will(returnValue(new BeginContextReturnType(0, ctxToken, ctxRegToken)));

                // Suspend:
                oneOf(rrsServices).contextSwitch(with(ctxRegToken));
                will(returnValue(new SwitchContextReturnType(0, ContextManagerImpl.ContextImpl.NATIVE_CONTEXT.getContextToken())));
                oneOf(rrsServices).beginContext(with(rmToken));
                will(returnValue(new BeginContextReturnType(0, ctxToken2, ctxRegToken2)));
                oneOf(rrsServices).contextSwitch(with(ctxRegToken2));
                will(returnValue(new SwitchContextReturnType(0, ctxToken)));

                // End: Check for UR interests danglers
                oneOf(rrsServices).retrieveSideInformationFast(with(ctxToken), with(0));
                will(returnValue(new RetrieveSideInformationFastReturnType(0, RRSServices.ATR_ONE_INTEREST_COUNT_MASK)));

                // Found UR interest danglers: Switch suspended context onto thread to be able to call endUR.
                oneOf(rrsServices).contextSwitch(with(ctxRegToken));
                will(returnValue(new SwitchContextReturnType(0, ctxToken2)));

                // Found UR interest danglers: Backout the UR.
                oneOf(rrsServices).endUR(with(RRSServices.ATR_ROLLBACK_ACTION), with(aNull(byte[].class)));
                will(returnValue(RRSServices.ATR_OK));

                // Found UR interest danglers: Switch contexts back to pre-end state.
                oneOf(rrsServices).contextSwitch(with(ctxRegToken2));
                will(returnValue(new SwitchContextReturnType(0, ctxToken)));

                // End: last steps.
            }
        });

        cm.begin(beginSuspendEndDirtyURUowCoord);
        cm.suspend(beginSuspendEndDirtyURUowCoord);
        cm.end(beginSuspendEndDirtyURUowCoord);
        mockery.assertIsSatisfied();
    }

    /**
     * Test an alternative context begin/suspend/resume/end flow.
     */
    @Test
    public void textContextBeginSuspendResumeEnd() throws Exception {
        // Define the input and output parms for the context manager.
        final byte[] rmToken = new byte[] { (byte) 0x12, (byte) 0x34 };
        final ContextManagerImpl cm = createContextManager(rrsServices, rmToken);
        final byte[] ctxToken = new byte[] { (byte) 0x23, (byte) 0x45 };
        final byte[] ctxRegToken = new byte[] { (byte) 0x34, (byte) 0x56 };
        final byte[] ctxToken2 = new byte[] { (byte) 0x45, (byte) 0x67 };
        final byte[] ctxRegToken2 = new byte[] { (byte) 0x56, (byte) 0x78 };

        UOWCoordinator beginSuspendResumeEndUowCoord = mockery.mock(UOWCoordinator.class, "beginSuspendResumeEndUowCoord");

        mockery.checking(new Expectations() {
            {
                oneOf(rrsServices).beginContext(with(rmToken));
                will(returnValue(new BeginContextReturnType(0, ctxToken, ctxRegToken)));
                oneOf(rrsServices).contextSwitch(with(ctxRegToken));
                will(returnValue(new SwitchContextReturnType(0, ContextManagerImpl.ContextImpl.NATIVE_CONTEXT.getContextToken())));
                oneOf(rrsServices).beginContext(with(rmToken));
                will(returnValue(new BeginContextReturnType(0, ctxToken2, ctxRegToken2)));
                oneOf(rrsServices).contextSwitch(with(ctxRegToken2));
                will(returnValue(new SwitchContextReturnType(0, ctxToken)));
                oneOf(rrsServices).contextSwitch(with(ctxRegToken));
                will(returnValue(new SwitchContextReturnType(0, ctxToken2)));
                oneOf(rrsServices).retrieveSideInformationFast(with(ctxToken), with(0));
                will(returnValue(new RetrieveSideInformationFastReturnType(0, RRSServices.ATR_UR_STATE_IN_RESET_MASK)));
            }
        });

        cm.begin(beginSuspendResumeEndUowCoord);
        cm.suspend(beginSuspendResumeEndUowCoord);
        cm.resume(beginSuspendResumeEndUowCoord);
        cm.end(beginSuspendResumeEndUowCoord);
        mockery.assertIsSatisfied();
    }

    /**
     * Test that a double begin fails.
     */
    @Test
    public void textContextDoubleBeginFail() throws Exception {
        // Define the input and output parms for the context manager.
        final byte[] rmToken = new byte[] { (byte) 0x12, (byte) 0x34 };
        final ContextManagerImpl cm = createContextManager(rrsServices, rmToken);
        final byte[] ctxToken = new byte[] { (byte) 0x23, (byte) 0x45 };
        final byte[] ctxRegToken = new byte[] { (byte) 0x34, (byte) 0x56 };

        UOWCoordinator doubleBeginUowCoord = mockery.mock(UOWCoordinator.class, "doubleBeginUowCoord");
        UOWCoordinator doubleBeginUowCoord2 = mockery.mock(UOWCoordinator.class, "doubleBeginUowCoord2");

        mockery.checking(new Expectations() {
            {
                oneOf(rrsServices).beginContext(with(rmToken));
                will(returnValue(new BeginContextReturnType(0, ctxToken, ctxRegToken)));
                oneOf(rrsServices).contextSwitch(with(ctxRegToken));
                will(returnValue(new SwitchContextReturnType(0, ContextManagerImpl.ContextImpl.NATIVE_CONTEXT.getContextToken())));
            }
        });

        cm.begin(doubleBeginUowCoord);
        boolean caughtException = false;
        try {
            cm.suspend(doubleBeginUowCoord2);
        } catch (IllegalStateException ise) {
            caughtException = true;
        }
        assertTrue(caughtException);
        mockery.assertIsSatisfied();
    }

    /**
     * Test that a double end fails. the current context will be in the wrong
     * state.
     */
    @Test
    public void textContextDoubleEndFail() throws Exception {
        // Define the input and output parms for the context manager.
        final byte[] rmToken = new byte[] { (byte) 0x12, (byte) 0x34 };
        final ContextManagerImpl cm = createContextManager(rrsServices, rmToken);
        final byte[] ctxToken = new byte[] { (byte) 0x23, (byte) 0x45 };
        final byte[] ctxRegToken = new byte[] { (byte) 0x34, (byte) 0x56 };

        UOWCoordinator doubleEndUowCoord = mockery.mock(UOWCoordinator.class, "doubleEndUowCoord");
        UOWCoordinator doubleEndUowCoord2 = mockery.mock(UOWCoordinator.class, "doubleEndUowCoord2");

        mockery.checking(new Expectations() {
            {
                oneOf(rrsServices).beginContext(with(rmToken));
                will(returnValue(new BeginContextReturnType(0, ctxToken, ctxRegToken)));
                oneOf(rrsServices).contextSwitch(with(ctxRegToken));
                will(returnValue(new SwitchContextReturnType(0, ContextManagerImpl.ContextImpl.NATIVE_CONTEXT.getContextToken())));
                oneOf(rrsServices).retrieveSideInformationFast(with(ctxToken), with(0));
                will(returnValue(new RetrieveSideInformationFastReturnType(0, RRSServices.ATR_UR_STATE_IN_RESET_MASK)));
            }
        });

        cm.begin(doubleEndUowCoord);
        cm.end(doubleEndUowCoord);
        boolean caughtException = false;
        try {
            cm.end(doubleEndUowCoord2);
        } catch (IllegalStateException ise) {
            caughtException = true;
        }
        assertTrue(caughtException);
        mockery.assertIsSatisfied();
    }

    /**
     * Test that an end for the wrong coordinator fails. The current context
     * will be associated with the wrong coordinator.
     */
    @Test
    public void textContextWrongEndFail() throws Exception {
        // Define the input and output parms for the context manager.
        final byte[] rmToken = new byte[] { (byte) 0x12, (byte) 0x34 };
        final ContextManagerImpl cm = createContextManager(rrsServices, rmToken);
        final byte[] ctxToken = new byte[] { (byte) 0x23, (byte) 0x45 };
        final byte[] ctxRegToken = new byte[] { (byte) 0x34, (byte) 0x56 };

        UOWCoordinator wrongEndUowCoord = mockery.mock(UOWCoordinator.class, "wrongEndUowCoord");
        UOWCoordinator wrongEndUowCoord2 = mockery.mock(UOWCoordinator.class, "wrongEndUowCoord2");

        mockery.checking(new Expectations() {
            {
                oneOf(rrsServices).beginContext(with(rmToken));
                will(returnValue(new BeginContextReturnType(0, ctxToken, ctxRegToken)));
                oneOf(rrsServices).contextSwitch(with(ctxRegToken));
                will(returnValue(new SwitchContextReturnType(0, ContextManagerImpl.ContextImpl.NATIVE_CONTEXT.getContextToken())));
            }
        });

        cm.begin(wrongEndUowCoord);
        boolean caughtException = false;
        try {
            cm.end(wrongEndUowCoord2);
        } catch (IllegalStateException ise) {
            caughtException = true;
        }
        assertTrue(caughtException);
        mockery.assertIsSatisfied();
    }

    /**
     * Test that a suspend with no work fails.
     */
    @Test
    public void testSuspendNoWorkFail() throws Exception {
        // Define the input and output parms for the context manager.
        final byte[] rmToken = new byte[] { (byte) 0x12, (byte) 0x34 };
        final ContextManagerImpl cm = createContextManager(rrsServices, rmToken);
        final byte[] ctxToken = new byte[] { (byte) 0x13, (byte) 0x35 };
        final byte[] ctxRegToken = new byte[] { (byte) 0x24, (byte) 0x46 };
        final byte[] nativeCtxToken = new byte[] { (byte) 0x35, (byte) 0x57 };
        UOWCoordinator suspendNoWorkUowCoord = mockery.mock(UOWCoordinator.class, "suspendNoWorkUowCoord");

        // --------------------------------------------------------------------
        // By default the native context is on the thread.  We need to get the
        // native context off the thread because we will allow the native
        // context to be suspended even though there is no work (to support the
        // case where the RRS integration feature is installed midway through
        // a unit of work).
        // --------------------------------------------------------------------
        mockery.checking(new Expectations() {
            {
                // Suspend the native context, replaced with this new context
                oneOf(rrsServices).beginContext(with(rmToken));
                will(returnValue(new BeginContextReturnType(0, ctxToken, ctxRegToken)));
                oneOf(rrsServices).contextSwitch(with(ctxRegToken));
                will(returnValue(new SwitchContextReturnType(0, ContextManagerImpl.ContextImpl.NATIVE_CONTEXT.getContextToken())));
                // End the native context
                oneOf(rrsServices).retrieveCurrentContextToken();
                will(returnValue(new RetrieveCurrentContextTokenReturnType(0, nativeCtxToken)));
                oneOf(rrsServices).retrieveSideInformationFast(with(nativeCtxToken), with(0));
                will(returnValue(new RetrieveSideInformationFastReturnType(0, RRSServices.ATR_UR_STATE_IN_RESET_MASK)));
            }
        });

        // Suspend the native context, and get rid of it.
        cm.suspend(suspendNoWorkUowCoord);
        cm.end(suspendNoWorkUowCoord);

        // Now try to suspend the new clean context which replaced the native context.
        boolean caughtException = false;
        try {
            cm.suspend(suspendNoWorkUowCoord);
        } catch (IllegalStateException ise) {
            caughtException = true;
        }
        assertTrue(caughtException);
        mockery.assertIsSatisfied();
    }

    /**
     * Test that a suspend for a context in the wrong state fails.
     */
    @Test
    public void testSuspendWrongStateFail() throws Exception {
        // Define the input and output parms for the context manager.
        final byte[] rmToken = new byte[] { (byte) 0x12, (byte) 0x34 };
        final ContextManagerImpl cm = createContextManager(rrsServices, rmToken);
        final byte[] ctxToken = new byte[] { (byte) 0x23, (byte) 0x45 };
        final byte[] ctxRegToken = new byte[] { (byte) 0x34, (byte) 0x56 };

        UOWCoordinator suspendWrongStateUowCoord = mockery.mock(UOWCoordinator.class, "suspendWrongStateUowCoord");

        mockery.checking(new Expectations() {
            {
                oneOf(rrsServices).beginContext(with(rmToken));
                will(returnValue(new BeginContextReturnType(0, ctxToken, ctxRegToken)));
                oneOf(rrsServices).contextSwitch(with(ctxRegToken));
                will(returnValue(new SwitchContextReturnType(0, ContextManagerImpl.ContextImpl.NATIVE_CONTEXT.getContextToken())));
                oneOf(rrsServices).retrieveSideInformationFast(with(ctxToken), with(0));
                will(returnValue(new RetrieveSideInformationFastReturnType(0, RRSServices.ATR_UR_STATE_IN_RESET_MASK)));
            }
        });

        cm.begin(suspendWrongStateUowCoord);
        cm.end(suspendWrongStateUowCoord);
        boolean caughtException = false;
        try {
            cm.suspend(suspendWrongStateUowCoord);
        } catch (IllegalStateException ise) {
            caughtException = true;
        }
        assertTrue(caughtException);
        mockery.assertIsSatisfied();
    }

    /**
     * Test that a suspend for the wrong coordinator fails.
     */
    @Test
    public void testSuspendWrongCoordFail() throws Exception {
        // Define the input and output parms for the context manager.
        final byte[] rmToken = new byte[] { (byte) 0x12, (byte) 0x34 };
        final ContextManagerImpl cm = createContextManager(rrsServices, rmToken);
        final byte[] ctxToken = new byte[] { (byte) 0x23, (byte) 0x45 };
        final byte[] ctxRegToken = new byte[] { (byte) 0x34, (byte) 0x56 };

        UOWCoordinator suspendWrongCoordUowCoord = mockery.mock(UOWCoordinator.class, "suspendWrongCoordUowCoord");
        UOWCoordinator suspendWrongCoordUowCoord2 = mockery.mock(UOWCoordinator.class, "suspendWrongCoordUowCoord2");

        mockery.checking(new Expectations() {
            {
                oneOf(rrsServices).beginContext(with(rmToken));
                will(returnValue(new BeginContextReturnType(0, ctxToken, ctxRegToken)));
                oneOf(rrsServices).contextSwitch(with(ctxRegToken));
                will(returnValue(new SwitchContextReturnType(0, ContextManagerImpl.ContextImpl.NATIVE_CONTEXT.getContextToken())));
            }
        });

        cm.begin(suspendWrongCoordUowCoord);
        boolean caughtException = false;
        try {
            cm.suspend(suspendWrongCoordUowCoord2);
        } catch (IllegalStateException ise) {
            caughtException = true;
        }
        assertTrue(caughtException);
        mockery.assertIsSatisfied();
    }

    /**
     * Test that a resume with active work on the thread fails.
     */
    @Test
    public void testResumeActiveWorkFail() throws Exception {
        // Define the input and output parms for the context manager.
        final byte[] rmToken = new byte[] { (byte) 0x12, (byte) 0x34 };
        final ContextManagerImpl cm = createContextManager(rrsServices, rmToken);
        final byte[] ctxToken = new byte[] { (byte) 0x23, (byte) 0x45 };
        final byte[] ctxRegToken = new byte[] { (byte) 0x34, (byte) 0x56 };

        UOWCoordinator resumeActiveWorkCoord = mockery.mock(UOWCoordinator.class, "resumeActiveWorkCoord");
        UOWCoordinator resumeActiveWorkCoord2 = mockery.mock(UOWCoordinator.class, "resumeActiveWorkCoord2");

        mockery.checking(new Expectations() {
            {
                oneOf(rrsServices).beginContext(with(rmToken));
                will(returnValue(new BeginContextReturnType(0, ctxToken, ctxRegToken)));
                oneOf(rrsServices).contextSwitch(with(ctxRegToken));
                will(returnValue(new SwitchContextReturnType(0, ContextManagerImpl.ContextImpl.NATIVE_CONTEXT.getContextToken())));
            }
        });

        cm.begin(resumeActiveWorkCoord);
        boolean caughtException = false;
        try {
            cm.resume(resumeActiveWorkCoord2);
        } catch (IllegalStateException ise) {
            caughtException = true;
        }
        assertTrue(caughtException);
        mockery.assertIsSatisfied();
    }

    /**
     * Test that a resume for an unknown coord fails.
     */
    @Test
    public void testResumeUnknownFail() throws Exception {
        // Define the input and output parms for the context manager.
        final byte[] rmToken = new byte[] { (byte) 0x12, (byte) 0x34 };
        final ContextManagerImpl cm = createContextManager(rrsServices, rmToken);

        UOWCoordinator resumeUnknownCoord = mockery.mock(UOWCoordinator.class, "resumeUnknownCoord");

        boolean caughtException = false;
        try {
            cm.resume(resumeUnknownCoord);
        } catch (IllegalStateException ise) {
            caughtException = true;
        }
        assertTrue(caughtException);
        mockery.assertIsSatisfied();
    }

    /** Creates a ContextManagerImpl. */
    private ContextManagerImpl createContextManager(RRSServices rrsServices, byte[] rmToken) {
        ContextManagerImpl cmi = new ContextManagerImpl();
        cmi.setRRSServices(rrsServices);
        cmi.activate();
        cmi.initialize(rmToken);

        return cmi;
    }
}
