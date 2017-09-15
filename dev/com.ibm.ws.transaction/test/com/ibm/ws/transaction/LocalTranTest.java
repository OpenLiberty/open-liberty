/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction;

import java.lang.reflect.Method;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.tx.jta.OnePhaseXAResource;
import com.ibm.tx.jta.impl.UserTransactionImpl;
import com.ibm.tx.ltc.impl.LTCUOWCallback;
import com.ibm.tx.ltc.impl.LocalTranCoordImpl;
import com.ibm.tx.ltc.impl.LocalTranCurrentSet;
import com.ibm.ws.LocalTransaction.ContainerSynchronization;
import com.ibm.ws.LocalTransaction.InconsistentLocalTranException;
import com.ibm.ws.LocalTransaction.LTCSystemException;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.LocalTransaction.RolledbackException;
import com.ibm.ws.uow.UOWScopeCallback;

public class LocalTranTest {

    private final Mockery mockery = new Mockery();

    private UserTransaction _ut;

    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty("com.ibm.tx.config.ConfigurationProviderClassName", "com.ibm.tx.jta.config.DefaultConfigurationProvider");
        UOWScopeCallback cb = LTCUOWCallback.createUserTransactionCallback();
        if (cb != null) // if it's null then we've already done this!
        {
            UserTransactionImpl.instance().registerCallback(cb);
        }

        final LocalTransactionCoordinator coordinator = LocalTranCurrentSet.instance().getLocalTranCoord();

        if (coordinator != null) {
            try {
                coordinator.cleanup();
            } catch (Exception e) {
            }
        }

        //assertNull(LocalTranCurrentSet.instance().getLocalTranCoord());
        if (LocalTranCurrentSet.instance().getLocalTranCoord() != null) {
            System.out.println("setup - error, LocalTranCurrentSet.instance().getLocalTranCoord() is not null");
            throw new Exception();
        }

    }

    public void tearDown() throws Exception {
        // com.ibm.tx.util.TMHelper.shutdown(-1);
    }

    @Test
    public void testCleanupWithResourcesEnlisted() throws Exception {
        LocalTranCurrentSet.instance().begin();

        // Mock mockSync = enlistSync();
        final Synchronization mockSync = enlistSync();

        // Don't set any expectations on the resource as it shouldn't be
        // called; it's been enlisted normally and the LTC is being
        // cleaned up so it should only drive resources that have been
        // enlisted for cleanup.
        enlistResource(true, "resource1");

        // mockSync.expects(once()).method("beforeCompletion");
        mockery.checking(new Expectations() {
            {
                oneOf(mockSync).beforeCompletion();
            }
        });
        // mockSync.expects(once()).method("afterCompletion");
        mockery.checking(new Expectations() {
            {
                oneOf(mockSync).afterCompletion(javax.transaction.Status.STATUS_COMMITTED); // Status
                                                                                            // Committedtest
            }
        });

        try {
            LocalTranCurrentSet.instance().cleanup();
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testCleanupWithResourcesEnlisted unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        } catch (RolledbackException re) {
            System.out.println("testCleanupWithResourcesEnlisted unexpectedly caught RolledbackException: "
                               + re);
            re.printStackTrace(System.out);
            throw new Exception();
        }

        mockery.assertIsSatisfied();
    }

    @Test
    public void testCleanupWithResourcesEnlistedForCleanupAndUnresolvedActionOfRollback() throws Exception {
        LocalTranCurrentSet.instance().begin(false, false, false);

        final Synchronization mockSync = enlistSync();

        // The unresolved action is rollback so the resource should be
        // rolledback
        // and the sync should only be called for after completion.
        final OnePhaseXAResource mockResource = enlistResourceForCleanup("resource1");

        mockery.checking(new Expectations() {
            {
                oneOf(mockResource).rollback(null);
            }
        });

        mockery.checking(new Expectations() {
            {
                oneOf(mockResource).getResourceName();
            }
        });

        mockery.checking(new Expectations() {
            {
                oneOf(mockSync).afterCompletion(
                                                javax.transaction.Status.STATUS_ROLLEDBACK);
            }
        });
        try {
            LocalTranCurrentSet.instance().cleanup();
            System.out.println("testCleanupWithResourcesEnlistedForCleanupAndUnresolvedActionOfRollback didn't throw a rolledbackexception");
            throw new Exception();
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testCleanupWithResourcesEnlistedForCleanupAndUnresolvedActionOfRollback unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        } catch (RolledbackException re) {
        }

        mockery.assertIsSatisfied();
    }

    @Test
    public void testCleanupWithResourcesEnlistedForCleanupAndUnresolvedActionOfCommit() throws Exception {
        LocalTranCurrentSet.instance().begin(false, true, false);

        final Synchronization mockSync = enlistSync();

        // The unresolved action is commit so the sync should be driven
        // for beforeCompletion the resource directed to commit and then
        // the sync driven for afterCompletion.
        final OnePhaseXAResource mockResource = enlistResourceForCleanup("resource1");

        mockery.checking(new Expectations() {
            {
                oneOf(mockSync).beforeCompletion();
            }
        });

        mockery.checking(new Expectations() {
            {
                oneOf(mockResource).commit(null, true);
            }
        });

        mockery.checking(new Expectations() {
            {
                oneOf(mockSync).afterCompletion(javax.transaction.Status.STATUS_COMMITTED);
            }
        });

        try {
            LocalTranCurrentSet.instance().cleanup();
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testCleanupWithResourcesEnlistedForCleanupAndUnresolvedActionOfCommit unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        } catch (RolledbackException re) {
            System.out.println("testCleanupWithResourcesEnlistedForCleanupAndUnresolvedActionOfCommit unexpectedly caught RolledbackException: "
                               + re);
            re.printStackTrace(System.out);
            throw new Exception();
        }

        mockery.assertIsSatisfied();
    }

    @Test
    public void testCompleteWithCommitAndResourcesEnlisted() throws Exception {
        LocalTranCurrentSet.instance().begin();

        final Synchronization mockSync = enlistSync();
        final OnePhaseXAResource mockResource = enlistResource(true,
                                                               "resource1");
        mockery.checking(new Expectations() {
            {
                oneOf(mockSync).beforeCompletion();
            }
        });

        mockery.checking(new Expectations() {
            {
                oneOf(mockResource).commit(null, true);
            }
        });

        mockery.checking(new Expectations() {
            {
                oneOf(mockSync).afterCompletion(
                                                javax.transaction.Status.STATUS_COMMITTED);
            }
        });

        try {
            LocalTranCurrentSet.instance().complete(
                                                    LocalTransactionCoordinator.EndModeCommit);
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testCleanupWithResourcesEnlistedForCleanupAndUnresolvedActionOfCommit unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        } catch (RolledbackException re) {
            System.out.println("testCleanupWithResourcesEnlistedForCleanupAndUnresolvedActionOfCommit unexpectedly caught RolledbackException: "
                               + re);
            re.printStackTrace(System.out);
            throw new Exception();
        }

        mockery.assertIsSatisfied();
    }

    @Test
    public void testCompleteWithRollbackAndResourcesEnlisted() throws Exception {
        LocalTranCurrentSet.instance().begin();

        final Synchronization mockSync = enlistSync();
        final OnePhaseXAResource mockResource = enlistResource(true,
                                                               "resource1");

        mockery.checking(new Expectations() {
            {
                oneOf(mockResource).rollback(null);
            }
        });

        mockery.checking(new Expectations() {
            {
                oneOf(mockSync).afterCompletion(
                                                javax.transaction.Status.STATUS_ROLLEDBACK);
            }
        });

        try {
            LocalTranCurrentSet.instance().complete(
                                                    LocalTransactionCoordinator.EndModeRollBack);
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testCompleteWithRollbackAndResourcesEnlisted unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        } catch (RolledbackException re) {
            // The rollback outcome has been explicitly requested so the
            // rollback
            // occurring should not cause an RolledbackException to be thrown.
            System.out.println("testCompleteWithRollbackAndResourcesEnlisted unexpectedly caught InconsistentLocalTranException: "
                               + re);
            re.printStackTrace(System.out);
            throw new Exception();
        }

        mockery.assertIsSatisfied();
    }

    @Test
    public void testCompleteWithResourcesEnlistedForCleanup() throws Exception {
        LocalTranCurrentSet.instance().begin();

        final Synchronization mockSync = enlistSync();

        // The resource has been enlisted for cleanup yet we're being completed
        // normally. Check that the resource is never called by not setting
        // any expectations.
        enlistResourceForCleanup("resource1");

        mockery.checking(new Expectations() {
            {
                oneOf(mockSync).beforeCompletion();
            }
        });

        mockery.checking(new Expectations() {
            {
                oneOf(mockSync).afterCompletion(javax.transaction.Status.STATUS_COMMITTED);
            }
        });

        try {
            LocalTranCurrentSet.instance().complete(
                                                    LocalTransactionCoordinator.EndModeCommit);
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testCompleteWithResourcesEnlistedForCleanup unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        } catch (RolledbackException re) {
            System.out.println("testCompleteWithResourcesEnlistedForCleanup unexpectedly caught InconsistentLocalTranException: "
                               + re);
            re.printStackTrace(System.out);
            throw new Exception();
        }

        mockery.assertIsSatisfied();
    }

    @Test
    public void testGetConfiguredBoundaryWhenBoundaryIsBeanMethod() throws Exception {
        // Set the boundary to be bean method and check it.
        LocalTranCurrentSet.instance().begin(false);

        // assertEquals(((LocalTranCoordImpl)
        // LocalTranCurrentSet.instance().getLocalTranCoord()).getConfiguredBoundary(),
        // LocalTranCoordImpl.BOUNDARY_BEAN_METHOD);
        if (!(((LocalTranCoordImpl) LocalTranCurrentSet.instance().getLocalTranCoord()).getConfiguredBoundary()).equals(LocalTranCoordImpl.BOUNDARY_BEAN_METHOD)) {
            System.out.println("testGetConfiguredBoundaryWhenBoundaryIsBeanMethod - not equal");
            throw new Exception();
        }

        try {
            LocalTranCurrentSet.instance().complete(LocalTransactionCoordinator.EndModeCommit);
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testGetConfiguredBoundaryWhenBoundaryIsBeanMethod unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        } catch (RolledbackException re) {
            System.out.println("testGetConfiguredBoundaryWhenBoundaryIsBeanMethod unexpectedly caught InconsistentLocalTranException: "
                               + re);
            re.printStackTrace(System.out);
            throw new Exception();
        }

        mockery.assertIsSatisfied();
    }

    @Test
    public void testGetConfiguredBoundaryWhenBoundaryIsActivitySessionButNoActivitySessionOnThread() throws Exception {
        // Set the boundary to ActivitySession but with no ActivitySession
        // on the thread and check it.
        LocalTranCurrentSet.instance().begin(true);
        // assertEquals(((LocalTranCoordImpl)LocalTranCurrentSet.instance().getLocalTranCoord()).getConfiguredBoundary(),
        // LocalTranCoordImpl.BOUNDARY_ACTIVITYSESSION);
        if (!(((LocalTranCoordImpl) LocalTranCurrentSet.instance().getLocalTranCoord()).getConfiguredBoundary()).equals(LocalTranCoordImpl.BOUNDARY_ACTIVITYSESSION)) {
            System.out.println("testGetConfiguredBoundaryWhenBoundaryIsActivitySessionButNoActivitySessionOnThread - not equal");
            throw new Exception();
        }

        try {
            LocalTranCurrentSet.instance().complete(
                                                    LocalTransactionCoordinator.EndModeCommit);
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testGetConfiguredBoundaryWhenBoundaryIsActivitySessionButNoActivitySessionOnThread unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        } catch (RolledbackException re) {
            System.out.println("testGetConfiguredBoundaryWhenBoundaryIsActivitySessionButNoActivitySessionOnThread unexpectedly caught InconsistentLocalTranException: "
                               + re);
            re.printStackTrace(System.out);
            throw new Exception();
        }

        mockery.assertIsSatisfied();
    }

    @Test
    public void testHasOutstandingWork() throws Exception {
        LocalTranCurrentSet.instance().begin();
        // assertFalse(LocalTranCurrentSet.instance().hasOutstandingWork());
        if (LocalTranCurrentSet.instance().hasOutstandingWork()) {
            System.out.println("testHasOutstandingWork - error, there is outstanding work");
            throw new Exception();
        }

        enlistDummyResource();

        // assertTrue(LocalTranCurrentSet.instance().hasOutstandingWork());
        if (!LocalTranCurrentSet.instance().hasOutstandingWork()) {
            System.out.println("testHasOutstandingWork - error, there should be outstanding work");
            throw new Exception();
        }

        try {
            LocalTranCurrentSet.instance().complete(LocalTransactionCoordinator.EndModeCommit);
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testHasOutstandingWork unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        } catch (RolledbackException re) {
            System.out.println("testHasOutstandingWork unexpectedly caught InconsistentLocalTranException: "
                               + re);
            re.printStackTrace(System.out);
            throw new Exception();
        }

        LocalTranCurrentSet.instance().begin();
        // assertFalse(LocalTranCurrentSet.instance().hasOutstandingWork());
        if (LocalTranCurrentSet.instance().hasOutstandingWork()) {
            System.out.println("testHasOutstandingWork - error, there is outstanding work");
            throw new Exception();
        }

        enlistDummyResourceForCleanup();

        // assertTrue(LocalTranCurrentSet.instance().hasOutstandingWork());
        if (!LocalTranCurrentSet.instance().hasOutstandingWork()) {
            System.out.println("testHasOutstandingWork - error, there should be outstanding work");
            throw new Exception();
        }

        try {
            LocalTranCurrentSet.instance().complete(
                                                    LocalTransactionCoordinator.EndModeRollBack);
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testHasOutstandingWork unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        } catch (RolledbackException re) {
            System.out.println("testHasOutstandingWork unexpectedly caught InconsistentLocalTranException: "
                               + re);
            re.printStackTrace(System.out);
            throw new Exception();
        }
    }

    @Test
    public void testCompleteOfLTCThatHasAlreadyBeenCompleted() throws Exception {
        LocalTranCurrentSet.instance().begin();

        final LocalTransactionCoordinator coordinator = LocalTranCurrentSet.instance().getLocalTranCoord();

        try {
            coordinator.complete(LocalTransactionCoordinator.EndModeRollBack);
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testCompleteOfLTCThatHasAlreadyBeenCompleted unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        } catch (RolledbackException re) {
            System.out.println("testCompleteOfLTCThatHasAlreadyBeenCompleted unexpectedly caught InconsistentLocalTranException: "
                               + re);
            re.printStackTrace(System.out);
            throw new Exception();
        }

        try {
            coordinator.complete(LocalTransactionCoordinator.EndModeRollBack);
            System.out.println("testCompleteOfLTCThatHasAlreadyBeenCompleted didn't throw an IllegalStateException");
            throw new Exception();
        } catch (IllegalStateException ise) {

        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testCompleteOfLTCThatHasAlreadyBeenCompleted unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        } catch (RolledbackException re) {
            System.out.println("testCompleteOfLTCThatHasAlreadyBeenCompleted unexpectedly caught InconsistentLocalTranException: "
                               + re);
            re.printStackTrace(System.out);
            throw new Exception();
        }
    }

    @Test
    public void testCleanupOfLTCThatHasAlreadyBeenCompleted() throws Exception {
        LocalTranCurrentSet.instance().begin();

        final LocalTransactionCoordinator coordinator = LocalTranCurrentSet.instance().getLocalTranCoord();

        try {
            coordinator.complete(LocalTransactionCoordinator.EndModeRollBack);
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testCleanupOfLTCThatHasAlreadyBeenCompleted unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        } catch (RolledbackException re) {
            System.out.println("testCleanupOfLTCThatHasAlreadyBeenCompleted unexpectedly caught InconsistentLocalTranException: "
                               + re);
            re.printStackTrace(System.out);
            throw new Exception();
        }

        try {
            coordinator.cleanup();
            System.out.println("testCleanupOfLTCThatHasAlreadyBeenCompleted didn't throw an IllegalStateException");
            throw new Exception();
        } catch (IllegalStateException ise) {

        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testCleanupOfLTCThatHasAlreadyBeenCompleted unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        } catch (RolledbackException re) {
            System.out.println("testCleanupOfLTCThatHasAlreadyBeenCompleted unexpectedly caught RolledbackException: "
                               + re);
            re.printStackTrace(System.out);
            throw new Exception();
        }
    }

    @Test
    public void testInconsistentOutcomeFromComplete() throws Exception {
        LocalTranCurrentSet.instance().begin(false, false, true);
        final OnePhaseXAResource resource1 = enlistResource(true, "resource1");
        final OnePhaseXAResource resource2 = enlistResource(true, "resource2");
        final OnePhaseXAResource resource3 = enlistResource(true, "resource3");
        final OnePhaseXAResource resource4 = enlistResource(true, "resource4");

        // resource1.expects(once()).method("commit");
        mockery.checking(new Expectations() {
            {
                oneOf(resource1).commit(null, true);
            }
        });
        // resource2.expects(once()).method("commit").will(throwException(new
        // XAException(XAException.XA_RBOTHER)));
        mockery.checking(new Expectations() {
            {
                oneOf(resource2).commit(null, true);
                will(throwException(new XAException(XAException.XA_RBOTHER)));
            }
        });

        // resource2.expects(atLeast()).method("getResourceName").will(returnValue("resource2"));
        mockery.checking(new Expectations() {
            {
                atLeast(1).of(resource2).getResourceName();
                will(returnValue("resource2"));
            }
        });

        // resource3.expects(once()).method("commit").will(throwException(new
        // XAException(XAException.XA_RBOTHER)));
        mockery.checking(new Expectations() {
            {
                oneOf(resource3).commit(null, true);
                will(throwException(new XAException(XAException.XA_RBOTHER)));
            }
        });

        // resource3.expects(atLeast()).method("getResourceName").will(returnValue("resource3"));
        mockery.checking(new Expectations() {
            {
                atLeast(1).of(resource3).getResourceName();
                will(returnValue("resource3"));
            }
        });

        // resource4.expects(once()).method("commit");
        mockery.checking(new Expectations() {
            {
                oneOf(resource4).commit(null, true);
            }
        });

        try {
            LocalTranCurrentSet.instance().complete(LocalTransactionCoordinator.EndModeCommit);
            System.out.println("testInconsistentOutcomeFromComplete didn't throw an InconsistentLocalTranException");
            throw new Exception();
        } catch (InconsistentLocalTranException ilte) {
            final String[] failingResources = ilte.getFailingResources();

            // assertTrue(failingResources.length == 2);
            if (!(failingResources.length == 2)) {
                System.out.println("testInconsistentOutcomeFromComplete - error, there should be 2 failing resources");
                throw new Exception();
            }

            if (!(failingResources[0]).equals("resource2")) {
                System.out.println("testInconsistentOutcomeFromComplete - no failing resource2");
                throw new Exception();
            }

            if (!(failingResources[1]).equals("resource3")) {
                System.out.println("testInconsistentOutcomeFromComplete - no failing resource2");
                throw new Exception();
            }
        } catch (RolledbackException re) {
            System.out.println("testInconsistentOutcomeFromComplete unexpectedly caught RolledbackException: "
                               + re);
            re.printStackTrace(System.out);
            throw new Exception();
        }
    }

    @Test
    public void testInconsistentOutcomeFromCleanup() throws Exception {
        LocalTranCurrentSet.instance().begin(false, false, false);
        final OnePhaseXAResource resource1 = enlistResourceForCleanup("resource1");
        final OnePhaseXAResource resource2 = enlistResourceForCleanup("resource2");
        final OnePhaseXAResource resource3 = enlistResourceForCleanup("resource3");
        final OnePhaseXAResource resource4 = enlistResourceForCleanup("resource4");

        // resource1.expects(once()).method("rollback");
        mockery.checking(new Expectations() {
            {
                oneOf(resource1).rollback(null);
            }
        });
        // resource1.expects(once()).method("getResourceName");
        mockery.checking(new Expectations() {
            {
                oneOf(resource1).getResourceName();
            }
        });
        // resource2.expects(once()).method("rollback").will(throwException(new
        // XAException(XAException.XA_RBOTHER)));
        mockery.checking(new Expectations() {
            {
                oneOf(resource2).rollback(null);
                will(throwException(new XAException(XAException.XA_RBOTHER)));
            }
        });
        // resource2.expects(atLeast()).method("getResourceName").will(returnValue("resource2"));
        mockery.checking(new Expectations() {
            {
                atLeast(1).of(resource2).getResourceName();
                will(returnValue("resource2"));
            }
        });
        // resource3.expects(once()).method("rollback").will(throwException(new
        // XAException(XAException.XA_RBOTHER)));
        mockery.checking(new Expectations() {
            {
                oneOf(resource3).rollback(null);
                will(throwException(new XAException(XAException.XA_RBOTHER)));
            }
        });
        // resource3.expects(atLeast()).method("getResourceName").will(returnValue("resource3"));
        mockery.checking(new Expectations() {
            {
                atLeast(1).of(resource3).getResourceName();
                will(returnValue("resource3"));
            }
        });
        // resource4.expects(once()).method("rollback");
        mockery.checking(new Expectations() {
            {
                oneOf(resource4).rollback(null);
            }
        });
        // resource4.expects(once()).method("getResourceName");
        mockery.checking(new Expectations() {
            {
                oneOf(resource4).getResourceName();
            }
        });

        try {
            LocalTranCurrentSet.instance().cleanup();
            System.out.println("testInconsistentOutcomeFromCleanup didn't throw an InconsistentLocalTranException");
            throw new Exception();
        } catch (InconsistentLocalTranException ilte) {
            final String[] failingResources = ilte.getFailingResources();

            // assertTrue(failingResources.length == 2);
            if (!(failingResources.length == 2)) {
                System.out.println("testInconsistentOutcomeFromCleanup - error, there should be 2 failing resources");
                throw new Exception();
            }

            if (!(failingResources[0]).equals("resource2")) {
                System.out.println("testInconsistentOutcomeFromCleanup - no failing resource2");
                throw new Exception();
            }

            if (!(failingResources[1]).equals("resource3")) {
                System.out.println("testInconsistentOutcomeFromCleanup - no failing resource2");
                throw new Exception();
            }
        } catch (RolledbackException re) {
            System.out.println("testInconsistentOutcomeFromCleanup unexpectedly caught RolledbackException: "
                               + re);
            re.printStackTrace(System.out);
            throw new Exception();
        }
    }

    @Test
    public void testSuspendAndResume() throws Exception {
        LocalTranCurrentSet.instance().begin();

        // Having begun an LTC verify that there actually is one on the thread
        LocalTransactionCoordinator coordFromBegin = LocalTranCurrentSet.instance().getLocalTranCoord();
        // assertNotNull(coordFromBegin);
        if (coordFromBegin == null) {
            System.out.println("testSuspendAndResume - error, coordFromBegin is null");
            throw new Exception();
        }

        // Now suspend the LTC and verify that suspend returns us the LTC that's
        // on the thread
        LocalTransactionCoordinator coordFromSuspend = LocalTranCurrentSet.instance().suspend();
        // assertEquals(coordFromBegin, coordFromSuspend);
        if (!coordFromBegin.equals(coordFromSuspend)) {
            System.out.println("testSuspendAndResume - coordFromBegin, coordFromSuspend not equal");
            throw new Exception();
        }

        // Having suspend the LTC there should be no LTC on the thread and
        // calling suspend
        // should return null.
        // assertNull(LocalTranCurrentSet.instance().getLocalTranCoord());
        if (LocalTranCurrentSet.instance().getLocalTranCoord() != null) {
            System.out.println("testSuspendAndResume - error, LocalTranCurrentSet.instance().getLocalTranCoord() is not null");
            throw new Exception();
        }
        // assertNull(LocalTranCurrentSet.instance().suspend());
        if (LocalTranCurrentSet.instance().suspend() != null) {
            System.out.println("testSuspendAndResume - error, LocalTranCurrentSet.instance().suspend() is not null");
            throw new Exception();
        }

        // Resume the LTC and check that the LTC that ends up on the thread is
        // the one that we asked to be resumed.
        LocalTranCurrentSet.instance().resume(coordFromSuspend);
        // assertEquals(coordFromSuspend,
        // LocalTranCurrentSet.instance().getLocalTranCoord());
        if (!coordFromSuspend.equals(LocalTranCurrentSet.instance().getLocalTranCoord())) {
            System.out.println("testSuspendAndResume - coordFromSuspend, LocalTranCurrentSet.instance().getLocalTranCoord() not equal");
            throw new Exception();
        }

        // With an LTC on the thread check that a call to resume throws an
        // IllegalStateException
        try {
            LocalTranCurrentSet.instance().resume(coordFromSuspend);
            System.out.println("testSuspendAndResume didn't throw an IllegalStateException");
            throw new Exception();
        } catch (IllegalStateException ise) {
            // An IllegalStateException is expected when a resume call
            // is made with an LTC already on the thread.
        }

        // Complete the LTC which should leave the thread empty
        try {
            LocalTranCurrentSet.instance().complete(
                                                    LocalTranCurrentSet.EndModeCommit);
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testSuspendAndResume unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        } catch (RolledbackException re) {
            System.out.println("testSuspendAndResume unexpectedly caught RolledbackException: "
                               + re);
            re.printStackTrace(System.out);
            throw new Exception();
        }

        // Check that the thread is empty after LTC completion
        // assertNull(LocalTranCurrentSet.instance().getLocalTranCoord());
        if (LocalTranCurrentSet.instance().getLocalTranCoord() != null) {
            System.out.println("testSuspendAndResume - error, LocalTranCurrentSet.instance().getLocalTranCoord() is not null");
            throw new Exception();
        }
        mockery.assertIsSatisfied();
    }

    @Test
    public void testEnlistForCleanupWithNullResource() throws Exception {
        LocalTranCurrentSet.instance().begin();

        try {
            LocalTranCurrentSet.instance().getLocalTranCoord().enlistForCleanup(null);
            System.out.println("testEnlistForCleanupWithNullResource didn't throw an IllegalStateException");
            throw new Exception();
        } catch (IllegalStateException ise) {

        } catch (LTCSystemException ltcse) {
            System.out.println("testEnlistForCleanupWithNullResource unexpectedly caught LTCSystemException: "
                               + ltcse);
            ltcse.printStackTrace(System.out);
            throw new Exception();
        }

        try {
            LocalTranCurrentSet.instance().end(
                                               LocalTranCurrentSet.EndModeCommit);
        } catch (RolledbackException rbe) {
            System.out.println("testEnlistForCleanupWithNullResource unexpectedly caught RolledbackException: "
                               + rbe);
            rbe.printStackTrace(System.out);
            throw new Exception();
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testEnlistForCleanupWithNullResource unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }

        mockery.assertIsSatisfied();
    }

    @Test
    public void testEnlistWithNullResource() throws Exception {
        LocalTranCurrentSet.instance().begin(false, true, true);

        try {
            LocalTranCurrentSet.instance().getLocalTranCoord().enlist(null);
            System.out.println("testEnlistWithNullResource didn't throw an IllegalStateException");
            throw new Exception();
        } catch (IllegalStateException ise) {

        } catch (LTCSystemException ltcse) {
            System.out.println("testEnlistWithNullResource unexpectedly caught LTCSystemException: "
                               + ltcse);
            ltcse.printStackTrace(System.out);
            throw new Exception();
        }

        try {
            LocalTranCurrentSet.instance().end(LocalTranCurrentSet.EndModeCommit);
        } catch (RolledbackException rbe) {
            System.out.println("testEnlistForCleanupWithNullResource unexpectedly caught RolledbackException: "
                               + rbe);
            rbe.printStackTrace(System.out);
            throw new Exception();
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testEnlistForCleanupWithNullResource unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }

        mockery.assertIsSatisfied();
    }

    @Test
    public void testEnlistForCleanupWhenLTCIsMarkedRollbackOnly() throws Exception {
        LocalTranCurrentSet.instance().begin();
        LocalTranCurrentSet.instance().getLocalTranCoord().setRollbackOnly();

        try {
            enlistDummyResourceForCleanup();
            System.out.println("testEnlistForCleanupWhenLTCIsMarkedRollbackOnly didn't throw an IllegalStateException");
            throw new Exception();
        } catch (IllegalStateException ise) {

        }

        try {
            LocalTranCurrentSet.instance().cleanup();
            System.out.println("testEnlistForCleanupWhenLTCIsMarkedRollbackOnly didn't throw a RolledbackException");
            throw new Exception();
        } catch (RolledbackException rbe) {
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testEnlistForCleanupWithNullResource unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }
    }

    @Test
    public void testEnlistWhenLTCIsMarkedRollbackOnly() throws Exception {
        LocalTranCurrentSet.instance().begin();
        LocalTranCurrentSet.instance().getLocalTranCoord().setRollbackOnly();

        try {
            enlistDummyResource();
            System.out.println("testEnlistWhenLTCIsMarkedRollbackOnly didn't throw an IllegalStateException");
            throw new Exception();
        } catch (IllegalStateException ise) {

        }

        try {
            LocalTranCurrentSet.instance().cleanup();
            System.out.println("testEnlistWhenLTCIsMarkedRollbackOnly didn't throw a RolledbackException");
            throw new Exception();
        } catch (RolledbackException rbe) {
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testEnlistWhenLTCIsMarkedRollbackOnly unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }
    }

    @Test
    public void testEnlistForCleanupWithResourceEnlisted() throws Exception {
        LocalTranCurrentSet.instance().begin();
        enlistDummyResource();

        try {
            LocalTranCurrentSet.instance().getLocalTranCoord().enlistForCleanup(new DummyOnePhaseXAResource());
            System.out.println("testEnlistForCleanupWithResourceEnlisted didn't throw an IllegalStateException");
            throw new Exception();
        } catch (IllegalStateException ise) {

        } catch (LTCSystemException ltcse) {
            System.out.println("testEnlistForCleanupWithResourceEnlisted unexpectedly caught LTCSystemException: "
                               + ltcse);
            ltcse.printStackTrace(System.out);
            throw new Exception();
        }

        try {
            LocalTranCurrentSet.instance().complete(LocalTransactionCoordinator.EndModeCommit);
        } catch (RolledbackException rbe) {
            System.out.println("testEnlistForCleanupWithResourceEnlisted unexpectedly caught RolledbackException: "
                               + rbe);
            rbe.printStackTrace(System.out);
            throw new Exception();
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testEnlistForCleanupWithResourceEnlisted unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }
    }

    @Test
    public void testEnlistWithResourceEnlistedForCleanup() throws Exception {
        LocalTranCurrentSet.instance().begin();
        enlistDummyResourceForCleanup();

        try {
            LocalTranCurrentSet.instance().getLocalTranCoord().enlist(new DummyOnePhaseXAResource());
            System.out.println("testEnlistWithResourceEnlistedForCleanup didn't throw an IllegalStateException");
            throw new Exception();
        } catch (IllegalStateException ise) {

        } catch (LTCSystemException ltcse) {
            System.out.println("testEnlistWithResourceEnlistedForCleanup unexpectedly caught LTCSystemException: "
                               + ltcse);
            ltcse.printStackTrace(System.out);
            throw new Exception();
        }

        try {
            LocalTranCurrentSet.instance().complete(
                                                    LocalTransactionCoordinator.EndModeCommit);
        } catch (RolledbackException rbe) {
            System.out.println("testEnlistWithResourceEnlistedForCleanup unexpectedly caught RolledbackException: "
                               + rbe);
            rbe.printStackTrace(System.out);
            throw new Exception();
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testEnlistWithResourceEnlistedForCleanup unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }
    }

    @Test
    public void testEnlistForCleanupDuringAfterCompletion() throws Exception {
        LocalTranCurrentSet.instance().begin();

        LocalTranCurrentSet.instance().getLocalTranCoord().enlistSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {

            }

            @Override
            public void afterCompletion(int arg0) {
                try {
                    // LocalTranCurrentSet.instance().getLocalTranCoord().enlistForCleanup((OnePhaseXAResource)
                    // newDummy(OnePhaseXAResource.class));
                    LocalTranCurrentSet.instance().getLocalTranCoord().enlistForCleanup(
                                                                                        new DummyOnePhaseXAResource());
                    System.out.println("testEnlistForCleanupDuringAfterCompletion didn't throw an IllegalStateException");
                    // throw new Exception();
                } catch (IllegalStateException ise) {

                } catch (LTCSystemException ltcse) {
                    System.out.println("testEnlistForCleanupDuringAfterCompletion unexpectedly caught LTCSystemException: "
                                       + ltcse);
                    ltcse.printStackTrace(System.out);
                    // throw new Exception();
                }
            }
        });

        try {
            LocalTranCurrentSet.instance().complete(LocalTransactionCoordinator.EndModeCommit);
        } catch (RolledbackException rbe) {
            System.out.println("testEnlistForCleanupDuringAfterCompletion unexpectedly caught RolledbackException: "
                               + rbe);
            rbe.printStackTrace(System.out);
            throw new Exception();
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testEnlistForCleanupDuringAfterCompletion unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }
    }

    @Test
    public void testEnlistDuringAfterCompletion() throws Exception {
        LocalTranCurrentSet.instance().begin();

        LocalTranCurrentSet.instance().getLocalTranCoord().enlistSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {

            }

            @Override
            public void afterCompletion(int arg0) {
                try {
                    // LocalTranCurrentSet.instance().getLocalTranCoord().enlist((OnePhaseXAResource)
                    // newDummy(OnePhaseXAResource.class));
                    LocalTranCurrentSet.instance().getLocalTranCoord().enlist(new DummyOnePhaseXAResource());
                    System.out.println("testEnlistDuringAfterCompletion didn't throw an IllegalStateException");
                    // throw new Exception();
                } catch (IllegalStateException ise) {

                } catch (LTCSystemException ltcse) {
                    System.out.println("testEnlistDuringAfterCompletion unexpectedly caught LTCSystemException: "
                                       + ltcse);
                    ltcse.printStackTrace(System.out);
                    // throw new Exception();
                }
            }
        });

        try {
            LocalTranCurrentSet.instance().complete(LocalTransactionCoordinator.EndModeCommit);
        } catch (RolledbackException rbe) {
            System.out.println("testEnlistDuringAfterCompletion unexpectedly caught RolledbackException: "
                               + rbe);
            rbe.printStackTrace(System.out);
            throw new Exception();
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testEnlistDuringAfterCompletion unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }
    }

    @Test
    public void testDelistFromCleanupDuringAfterCompletion() throws Exception {
        LocalTranCurrentSet.instance().begin();
        // final OnePhaseXAResource resource = (OnePhaseXAResource)
        // newDummy(OnePhaseXAResource.class);
        final OnePhaseXAResource resource = new DummyOnePhaseXAResource();

        try {
            LocalTranCurrentSet.instance().getLocalTranCoord().enlistForCleanup(resource);
        } catch (LTCSystemException ltcse) {
            System.out.println("testDelistFromCleanupDuringAfterCompletion unexpectedly caught LTCSystemException: "
                               + ltcse);
            ltcse.printStackTrace(System.out);
            throw new Exception();
        }

        LocalTranCurrentSet.instance().getLocalTranCoord().enlistSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {

            }

            @Override
            public void afterCompletion(int arg0) {
                try {
                    LocalTranCurrentSet.instance().getLocalTranCoord().delistFromCleanup(resource);
                    System.out.println("testDelistFromCleanupDuringAfterCompletion didn't throw an IllegalStateException");
                    // throw new Exception();
                } catch (IllegalStateException ise) {

                }
            }
        });

        try {
            LocalTranCurrentSet.instance().complete(LocalTransactionCoordinator.EndModeCommit);
        } catch (RolledbackException rbe) {
            System.out.println("testDelistFromCleanupDuringAfterCompletion unexpectedly caught RolledbackException: "
                               + rbe);
            rbe.printStackTrace(System.out);
            throw new Exception();
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testDelistFromCleanupDuringAfterCompletion unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }
    }

    @Test
    public void testDelistFromCleanupWithNoResourcesEnlistedForCleanup() throws Exception {
        LocalTranCurrentSet.instance().begin();

        final OnePhaseXAResource resource = new DummyOnePhaseXAResource();

        try {
            LocalTranCurrentSet.instance().getLocalTranCoord().delistFromCleanup(resource);
            System.out.println("testDelistFromCleanupWithNoResourcesEnlistedForCleanup didn't throw an IllegalStateException");
            throw new Exception();
        } catch (IllegalStateException ise) {

        }

        try {
            LocalTranCurrentSet.instance().cleanup();
        } catch (RolledbackException rbe) {
            System.out.println("testDelistFromCleanupWithNoResourcesEnlistedForCleanup unexpectedly caught RolledbackException: "
                               + rbe);
            rbe.printStackTrace(System.out);
            throw new Exception();
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testDelistFromCleanupWithNoResourcesEnlistedForCleanup unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }
    }

    @Test
    public void testDelistFromCleanupWhenResourceHasNotBeenEnlistedForCleanup() throws Exception {
        LocalTranCurrentSet.instance().begin();

        try {
            LocalTranCurrentSet.instance().getLocalTranCoord().enlistForCleanup(new DummyOnePhaseXAResource());
        } catch (LTCSystemException ltcse) {
            System.out.println("testDelistFromCleanupWhenResourceHasNotBeenEnlistedForCleanup unexpectedly caught LTCSystemException: "
                               + ltcse);
            ltcse.printStackTrace(System.out);
            throw new Exception();
        }

        final OnePhaseXAResource resource = new DummyOnePhaseXAResource();

        try {
            LocalTranCurrentSet.instance().getLocalTranCoord().delistFromCleanup(resource);
            System.out.println("testDelistFromCleanupWhenResourceHasNotBeenEnlistedForCleanup didn't throw an IllegalStateException");
            throw new Exception();
        } catch (IllegalStateException ise) {

        }

        try {
            LocalTranCurrentSet.instance().cleanup();
            System.out.println("testDelistFromCleanupWhenResourceHasNotBeenEnlistedForCleanup didn't throw a RolledbackException");
            throw new Exception();
        } catch (RolledbackException rbe) {

        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testDelistFromCleanupWhenResourceHasNotBeenEnlistedForCleanup unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }
    }

    @Test
    public void testDelistFromCleanup() throws Exception {
        LocalTranCurrentSet.instance().begin();

        try {
            LocalTranCurrentSet.instance().getLocalTranCoord().enlistForCleanup(new DummyOnePhaseXAResource());

            // Enlist a new resource for cleanup but don't set any expectations
            // on it.
            // This allows us to check that the resource is actually delisted
            // and
            // doesn't get called to rollback when the cleanup the LTC.
            final OnePhaseXAResource resource = enlistResourceForCleanup("resource");
            LocalTranCurrentSet.instance().getLocalTranCoord().delistFromCleanup(resource);
        } catch (LTCSystemException ltcse) {
            System.out.println("testDelistFromCleanup unexpectedly caught LTCSystemException: "
                               + ltcse);
            ltcse.printStackTrace(System.out);
            throw new Exception();
        }

        try {
            LocalTranCurrentSet.instance().cleanup();
            System.out.println("testDelistFromCleanup didn't throw n RolledbackException");
            throw new Exception();
        } catch (RolledbackException rbe) {
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testDelistFromCleanup unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }
    }

    @Test
    public void testCompleteOfLTCMarkedRollbackOnly() throws Exception {
        LocalTranCurrentSet.instance().begin();
        final OnePhaseXAResource resource1 = enlistResource(true, "resource1");
        final OnePhaseXAResource resource2 = enlistResource(true, "resource2");

        // resource1.expects(once()).method("rollback");
        mockery.checking(new Expectations() {
            {
                oneOf(resource1).rollback(null);
            }
        });

        // resource2.expects(once()).method("rollback");
        mockery.checking(new Expectations() {
            {
                oneOf(resource2).rollback(null);
            }
        });

        LocalTranCurrentSet.instance().getLocalTranCoord().setRollbackOnly();

        try {
            LocalTranCurrentSet.instance().complete(
                                                    LocalTransactionCoordinator.EndModeCommit);
            System.out.println("testCompleteOfLTCMarkedRollbackOnly didn't throw a RolledbackException");
            throw new Exception();
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testCompleteOfLTCMarkedRollbackOnly unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        } catch (RolledbackException re) {

        }
    }

    @Test
    public void testContainerSynchronizationForCommit() throws Exception {
        LocalTranCurrentSet.instance().begin();

        final ContainerSynchronization containerSync = enlistContainerSync();
        // containerSync.expects(once()).method("setCompleting").with(eq(true));
        mockery.checking(new Expectations() {
            {
                oneOf(containerSync).setCompleting(true);
            }
        });
        // containerSync.expects(once()).method("beforeCompletion");
        mockery.checking(new Expectations() {
            {
                oneOf(containerSync).beforeCompletion();
            }
        });
        // containerSync.expects(once()).method("setCompleting").with(eq(true));
        mockery.checking(new Expectations() {
            {
                oneOf(containerSync).setCompleting(true);
            }
        });
        // containerSync.expects(once()).method("afterCompletion")
        // .with(eq(Status.STATUS_COMMITTED));
        mockery.checking(new Expectations() {
            {
                oneOf(containerSync).afterCompletion(Status.STATUS_COMMITTED);
            }
        });

        try {
            LocalTranCurrentSet.instance().end(LocalTransactionCoordinator.EndModeCommit);
        } catch (RolledbackException rbe) {
            System.out.println("testContainerSynchronizationForCommit unexpectedly caught InconsistentLocalTranException: "
                               + rbe);
            rbe.printStackTrace(System.out);
            throw new Exception();
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testContainerSynchronizationForCommit unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }

        mockery.assertIsSatisfied();
    }

    @Test
    public void testExceptionDuringBeforeCompletionCausesRollback() throws Exception {
        LocalTranCurrentSet.instance().begin();

        final Synchronization sync = enlistSync();
        // sync.expects(once()).method("beforeCompletion").will(throwException(new
        // RuntimeException()));
        mockery.checking(new Expectations() {
            {
                oneOf(sync).beforeCompletion();
                will(throwException(new RuntimeException()));
            }
        });

        // sync.expects(once()).method("afterCompletion")
        // .with(eq(Status.STATUS_ROLLEDBACK));
        mockery.checking(new Expectations() {
            {
                oneOf(sync).afterCompletion(Status.STATUS_ROLLEDBACK);
                will(throwException(new RuntimeException()));
            }
        });

        // final Mock resource = enlistResource(true);
        final OnePhaseXAResource resource = enlistResource(true, "resource");
        // resource.expects(once()).method("rollback");
        mockery.checking(new Expectations() {
            {
                oneOf(resource).rollback(null);
            }
        });

        try {
            LocalTranCurrentSet.instance().complete(LocalTransactionCoordinator.EndModeCommit);
            System.out.println("testExceptionDuringBeforeCompletionCausesRollback didn't throw a RolledbackException");
            throw new Exception();
        } catch (RolledbackException rbe) {

        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testExceptionDuringBeforeCompletionCausesRollback unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }

        mockery.assertIsSatisfied();
    }

    @Test
    public void testExceptionDuringContainerSyncBeforeCompletionCausesRollback() throws Exception {
        LocalTranCurrentSet.instance().begin();

        // final Mock sync = enlistContainerSync();
        final ContainerSynchronization sync = enlistContainerSync();
        // containerSync.expects(once()).method("setCompleting").with(eq(true));
        mockery.checking(new Expectations() {
            {
                oneOf(sync).setCompleting(true);
            }
        });

        // sync.expects(once()).method("beforeCompletion")
        // .will(throwException(new RuntimeException()));
        mockery.checking(new Expectations() {
            {
                oneOf(sync).beforeCompletion();
                will(throwException(new RuntimeException()));
            }
        });

        // sync.expects(once()).method("setCompleting").with(eq(true));
        mockery.checking(new Expectations() {
            {
                oneOf(sync).setCompleting(true);
            }
        });

        // sync.expects(once()).method("afterCompletion")
        // .with(eq(Status.STATUS_ROLLEDBACK));
        mockery.checking(new Expectations() {
            {
                oneOf(sync).afterCompletion(Status.STATUS_ROLLEDBACK);
            }
        });

        final OnePhaseXAResource resource = enlistResource(true, "resource");
        // resource.expects(once()).method("rollback");
        mockery.checking(new Expectations() {
            {
                oneOf(resource).rollback(null);
            }
        });

        try {
            LocalTranCurrentSet.instance().complete(LocalTransactionCoordinator.EndModeCommit);
            System.out.println("testExceptionDuringBeforeCompletionCausesRollback didn't throw a RolledbackException");
            throw new Exception();
        } catch (RolledbackException rbe) {

        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testExceptionDuringContainerSyncBeforeCompletionCausesRollback unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }

        mockery.assertIsSatisfied();
    }

    @Test
    public void testSyncEnlistDuringCommit() throws Exception {
        LocalTranCurrentSet.instance().begin(false, false, true);

        try {
            LocalTranCurrentSet.instance().getLocalTranCoord().enlist(new DummyOnePhaseXAResource() {
                public void commit() {
                    try {
                        enlistSync();
                        System.out.println("testSyncEnlistDuringCommit didn't throw an IllegalStateException");
                        // throw new Exception();
                    } catch (IllegalStateException ise) {

                    }
                }
            });
        } catch (LTCSystemException ltcse) {
            System.out.println("testSyncEnlistDuringCommit unexpectedly caught InconsistentLocalTranException: "
                               + ltcse);
            ltcse.printStackTrace(System.out);
            throw new Exception();
        }

        try {
            LocalTranCurrentSet.instance().end(LocalTransactionCoordinator.EndModeCommit);
        } catch (RolledbackException rbe) {
            System.out.println("testSyncEnlistDuringCommit unexpectedly caught RolledbackException: "
                               + rbe);
            rbe.printStackTrace(System.out);
            throw new Exception();
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testSyncEnlistDuringCommit unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }
    }

    @Test
    public void testDuplicateContainerSynchronizationEnlistment() throws Exception {
        LocalTranCurrentSet.instance().begin(false, false, true);

        // final Mock sync = enlistContainerSync();
        final ContainerSynchronization sync = enlistContainerSync();
        // containerSync.expects(once()).method("setCompleting").with(eq(true));
        mockery.checking(new Expectations() {
            {
                oneOf(sync).setCompleting(true);
            }
        });

        // sync.expects(once()).method("beforeCompletion")
        mockery.checking(new Expectations() {
            {
                oneOf(sync).beforeCompletion();
            }
        });

        // sync.expects(once()).method("setCompleting").with(eq(true));
        mockery.checking(new Expectations() {
            {
                oneOf(sync).setCompleting(true);
            }
        });

        // sync.expects(once()).method("afterCompletion")
        // .with(eq(Status.STATUS_ROLLEDBACK));
        mockery.checking(new Expectations() {
            {
                oneOf(sync).afterCompletion(Status.STATUS_COMMITTED);
            }
        });

        try {
            // attempt to enlist sync again
            LocalTranCurrentSet.instance().getLocalTranCoord().enlistSynchronization(sync);
            System.out.println("testDuplicateContainerSynchronizationEnlistment didn't throw an IllegalStateException");
            throw new Exception();
        } catch (IllegalStateException ise) {

        }

        try {
            LocalTranCurrentSet.instance().end(LocalTransactionCoordinator.EndModeCommit);
        } catch (RolledbackException rbe) {
            System.out.println("testDuplicateContainerSynchronizationEnlistment unexpectedly caught RolledbackException: "
                               + rbe);
            rbe.printStackTrace(System.out);
            throw new Exception();
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testDuplicateContainerSynchronizationEnlistment unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }

        mockery.assertIsSatisfied();
    }

    @Test
    public void testNullSynchronizationEnlistment() throws Exception {
        LocalTranCurrentSet.instance().begin(false, false, true);

        try {
            LocalTranCurrentSet.instance().getLocalTranCoord().enlistSynchronization(null);
            System.out.println("testNullSynchronizationEnlistment didn't throw an IllegalStateException");
            throw new Exception();
        } catch (IllegalStateException ise) {

        }

        try {
            LocalTranCurrentSet.instance().end(LocalTransactionCoordinator.EndModeCommit);
        } catch (RolledbackException rbe) {
            System.out.println("testNullSynchronizationEnlistment unexpectedly caught RolledbackException: "
                               + rbe);
            rbe.printStackTrace(System.out);
            throw new Exception();
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testNullSynchronizationEnlistment unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }
    }

    @Test
    public void testEnlistSynchronizationDuringBeforeCompletion() throws Exception {
        LocalTranCurrentSet.instance().begin(false, false, true);

        try {
            LocalTranCurrentSet.instance().getLocalTranCoord().enlistSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {
                    // Mock sync = enlistSync();
                    final Synchronization mockSync = enlistSync();

                    // mockSync.expects(once()).method("beforeCompletion");
                    mockery.checking(new Expectations() {
                        {
                            oneOf(mockSync).beforeCompletion();
                        }
                    });
                    // mockSync.expects(once()).method("afterCompletion");
                    mockery.checking(new Expectations() {
                        {
                            oneOf(mockSync).afterCompletion(javax.transaction.Status.STATUS_COMMITTED); // Status
                                                                                                        // Committedtest
                        }
                    });
                }

                @Override
                public void afterCompletion(int arg0) {
                    // TODO Auto-generated method stub
                }

            });
        } catch (IllegalStateException ise) {
            System.out.println("testEnlistSynchronizationDuringBeforeCompletion unexpectedly caught IllegalStateException: "
                               + ise);
            ise.printStackTrace(System.out);
            throw new Exception();
        }

        try {
            LocalTranCurrentSet.instance().end(LocalTransactionCoordinator.EndModeCommit);
        } catch (RolledbackException rbe) {
            System.out.println("testEnlistSynchronizationDuringBeforeCompletion unexpectedly caught RolledbackException: "
                               + rbe);
            rbe.printStackTrace(System.out);
            throw new Exception();
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testEnlistSynchronizationDuringBeforeCompletion unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }
    }

    @Test
    public void testSyncEnlistDuringAfterCompletion() throws Exception {
        LocalTranCurrentSet.instance().begin(false, false, true);

        LocalTranCurrentSet.instance().getLocalTranCoord().enlistSynchronization(new Synchronization() {
            @Override
            public void afterCompletion(int status) {
                try {
                    enlistSync();
                    System.out.println("testSyncEnlistDuringAfterCompletion didn't throw an IllegalStateException");
                    // throw new Exception();
                } catch (IllegalStateException ise) {

                }
            }

            @Override
            public void beforeCompletion() {
                // TODO Auto-generated method stub

            }
        });

        try {
            LocalTranCurrentSet.instance().end(LocalTransactionCoordinator.EndModeCommit);
        } catch (RolledbackException rbe) {
            System.out.println("testSyncEnlistDuringAfterCompletion unexpectedly caught RolledbackException: "
                               + rbe);
            rbe.printStackTrace(System.out);
            throw new Exception();
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testSyncEnlistDuringAfterCompletion unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }
    }

    @Test
    public void testContainerSynchronizationForRollback() throws Exception {
        LocalTranCurrentSet.instance().begin();

        // final Mock sync = enlistContainerSync();
        final ContainerSynchronization sync = enlistContainerSync();
        // containerSync.expects(once()).method("setCompleting").with(eq(true));
        mockery.checking(new Expectations() {
            {
                oneOf(sync).setCompleting(true);
            }
        });

        // sync.expects(once()).method("beforeCompletion")
        mockery.checking(new Expectations() {
            {
                oneOf(sync).beforeCompletion();
            }
        });

        // sync.expects(once()).method("setCompleting").with(eq(true));
        mockery.checking(new Expectations() {
            {
                oneOf(sync).setCompleting(true);
            }
        });

        // sync.expects(once()).method("afterCompletion")
        // .with(eq(Status.STATUS_COMMITTED));
        mockery.checking(new Expectations() {
            {
                oneOf(sync).afterCompletion(Status.STATUS_COMMITTED);
            }
        });

        try {
            LocalTranCurrentSet.instance().end(LocalTransactionCoordinator.EndModeRollBack);
        } catch (RolledbackException rbe) {
            System.out.println("testContainerSynchronizationForRollback unexpectedly caught RolledbackException: "
                               + rbe);
            rbe.printStackTrace(System.out);
            throw new Exception();
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testContainerSynchronizationForRollback unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }

        mockery.assertIsSatisfied();
    }

    @Test
    public void testContainerSynchronizationIsOnlyDrivenWithAfterCompletionWhenSyncChangesOutcomeFromCommitToRollback() throws Exception {
        LocalTranCurrentSet.instance().begin();

        final Synchronization mockSync = enlistSync();

        // mockSync.expects(once()).method("beforeCompletion");
        mockery.checking(new Expectations() {
            {
                oneOf(mockSync).beforeCompletion();
                will(throwException(new RuntimeException()));
            }
        });

        // final Mock sync = enlistContainerSync();
        final ContainerSynchronization sync = enlistContainerSync();
        // containerSync.expects(once()).method("setCompleting").with(eq(true));
        mockery.checking(new Expectations() {
            {
                oneOf(sync).setCompleting(true);
            }
        });

        // sync.expects(once()).method("afterCompletion")
        // .with(eq(Status.STATUS_ROLLEDBACK));
        mockery.checking(new Expectations() {
            {
                oneOf(mockSync).afterCompletion(Status.STATUS_ROLLEDBACK);
            }
        });

        // sync.expects(once()).method("setCompleting").with(eq(true));
        mockery.checking(new Expectations() {
            {
                oneOf(sync).setCompleting(true);
            }
        });

        try {
            LocalTranCurrentSet.instance().end(LocalTransactionCoordinator.EndModeCommit);
            System.out.println("testContainerSynchronizationIsOnlyDrivenWithAfterCompletionWhenSyncChangesOutcomeFromCommitToRollback didn't throw a RolledbackException");
            throw new Exception();
        } catch (RolledbackException rbe) {
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testContainerSynchronizationIsOnlyDrivenWithAfterCompletionWhenSyncChangesOutcomeFromCommitToRollback unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }

        mockery.assertIsSatisfied();
    }

    @Test
    public void testTaskID() throws Exception {
        LocalTranCurrentSet.instance().begin();

        // assertNull(((LocalTranCoordImpl) LocalTranCurrentSet.instance()
        // .getLocalTranCoord()).getTaskId());
        if (((LocalTranCoordImpl) LocalTranCurrentSet.instance().getLocalTranCoord()).getTaskId() != null) {
            System.out.println("testSuspendAndResume - error, LocalTranCurrentSet.instance().getLocalTranCoord() is not null");
            throw new Exception();
        }

        final String taskId = "My task id";

        ((LocalTranCoordImpl) LocalTranCurrentSet.instance().getLocalTranCoord()).setTaskId(taskId);
        // assertEquals(taskId,
        // ((LocalTranCoordImpl)LocalTranCurrentSet.instance().getLocalTranCoord()).getTaskId());
        if (!(taskId.equals(((LocalTranCoordImpl) LocalTranCurrentSet.instance().getLocalTranCoord()).getTaskId()))) {
            System.out.println("testTaskID - not equal");
            throw new Exception();
        }

        try {
            LocalTranCurrentSet.instance().end(LocalTranCurrentSet.EndModeCommit);
        } catch (RolledbackException rbe) {
            System.out.println("testTaskID unexpectedly caught RolledbackException: "
                               + rbe);
            rbe.printStackTrace(System.out);
            throw new Exception();
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testTaskID unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        }

        mockery.assertIsSatisfied();
    }

    @Test
    public void testUserTranCallbackNoResources() throws Exception {
        LocalTranCurrentSet.instance().begin();
        LocalTranCurrentSet.instance().getLocalTranCoord().setRollbackOnly();

        UserTransaction ut = getMyUserTransaction();
        try {
            ut.begin();
        } catch (Exception e) {
            System.out.println("testUserTranCallbackNoResources begin unexpectedly caught Exception: "
                               + e);
            e.printStackTrace(System.out);
            throw new Exception();
        }

        // this should have completed the current ltc
        // assertNull(LocalTranCurrentSet.instance().getLocalTranCoord());
        if (LocalTranCurrentSet.instance().getLocalTranCoord() != null) {
            System.out.println("testUserTranCallbackNoResources - error, LocalTranCurrentSet.instance().getLocalTranCoord() is not null");
            throw new Exception();
        }

        try {
            ut.commit();
        } catch (Exception e) {
            System.out.println("testUserTranCallbackNoResources commit unexpectedly caught Exception: "
                               + e);
            e.printStackTrace(System.out);
            throw new Exception();
        }

        // this should have started a new ltc ... which will not be marked for
        // rollback
        // assertNotNull(LocalTranCurrentSet.instance().getLocalTranCoord());
        if (LocalTranCurrentSet.instance().getLocalTranCoord() == null) {
            System.out.println("testUserTranCallbackNoResources - error, LocalTranCurrentSet.instance().getLocalTranCoord() is null");
            throw new Exception();
        }

        // assertFalse(LocalTranCurrentSet.instance().getLocalTranCoord()
        // .getRollbackOnly());
        if (LocalTranCurrentSet.instance().getLocalTranCoord().getRollbackOnly()) {
            System.out.println("testUserTranCallbackNoResources - error, rollback only");
            throw new Exception();
        }

        try {
            LocalTranCurrentSet.instance().complete(LocalTransactionCoordinator.EndModeCommit);
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testUserTranCallbackNoResources unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        } catch (RolledbackException re) {
            System.out.println("testUserTranCallbackNoResources unexpectedly caught RolledbackException: "
                               + re);
            re.printStackTrace(System.out);
            throw new Exception();
        }

        mockery.assertIsSatisfied();
    }

    @Test
    public void testUserTranCallbackWithResources() throws Exception {
        LocalTranCurrentSet.instance().begin();

        // Mock mockSync = enlistSync();
        final Synchronization mockSync = enlistSync();

        // Mock mockResource = enlistResource(true);
        final OnePhaseXAResource mockResource = enlistResource(true, "resource");

        // mockSync.expects(once()).method("beforeCompletion");
        mockery.checking(new Expectations() {
            {
                oneOf(mockSync).beforeCompletion();
            }
        });

        // mockResource.expects(once()).method("commit");
        mockery.checking(new Expectations() {
            {
                oneOf(mockResource).commit(null, true);
            }
        });

        // mockSync.expects(once()).method("afterCompletion");
        mockery.checking(new Expectations() {
            {
                oneOf(mockSync).afterCompletion(javax.transaction.Status.STATUS_COMMITTED);
            }
        });

        UserTransaction ut = getMyUserTransaction();

        try {
            ut.begin();
        } catch (IllegalStateException ilte) {
            // this is what we expect
        } catch (Exception e) {
            System.out.println("testUserTranCallbackWithResources commit unexpectedly caught Exception: "
                               + e);
            e.printStackTrace(System.out);
            throw new Exception();
        }

        try {
            LocalTranCurrentSet.instance().complete(LocalTransactionCoordinator.EndModeCommit);
        } catch (InconsistentLocalTranException ilte) {
            System.out.println("testUserTranCallbackWithResources unexpectedly caught InconsistentLocalTranException: "
                               + ilte);
            ilte.printStackTrace(System.out);
            throw new Exception();
        } catch (RolledbackException re) {
            System.out.println("testUserTranCallbackWithResources unexpectedly caught RolledbackException: "
                               + re);
            re.printStackTrace(System.out);
            throw new Exception();
        }

        mockery.assertIsSatisfied();
    }

    private OnePhaseXAResource enlistResourceForCleanup(String name) throws Exception {

        final OnePhaseXAResource mockResource = mockery.mock(OnePhaseXAResource.class, name);
        try {
            LocalTranCurrentSet.instance().getLocalTranCoord().enlistForCleanup(mockResource);
        } catch (LTCSystemException ltcse) {
            System.out.println("enlistResourceForCleanup unexpectedly caught LTCSystemException: "
                               + ltcse);
            ltcse.printStackTrace(System.out);
            throw new Exception();
        }

        return mockResource;
    }

    // private Mock enlistResource(boolean willBeStarted) {
    private OnePhaseXAResource enlistResource(boolean willBeStarted, String name) throws Exception {

        final OnePhaseXAResource mockResource = mockery.mock(OnePhaseXAResource.class, name);

        if (willBeStarted) {
            // mockResource.expects(once()).method("start");
            mockery.checking(new Expectations() {
                {
                    oneOf(mockResource).start(null, 0);
                }
            });
        }

        try {
            // LocalTranCurrentSet.instance().getLocalTranCoord().enlist((OnePhaseXAResource)
            // mockResource.proxy());
            LocalTranCurrentSet.instance().getLocalTranCoord().enlist(mockResource);
        } catch (LTCSystemException ltcse) {
            System.out.println("enlistResource unexpectedly caught LTCSystemException: "
                               + ltcse);
            ltcse.printStackTrace(System.out);
            throw new Exception();
        }

        return mockResource;
    }

    private void enlistDummyResource() throws Exception {
        try {
            LocalTranCurrentSet.instance().getLocalTranCoord().enlist(new DummyOnePhaseXAResource());
        } catch (LTCSystemException ltcse) {
            System.out.println("enlistDummyResource unexpectedly caught LTCSystemException: "
                               + ltcse);
            ltcse.printStackTrace(System.out);
            throw new Exception();
        }
    }

    private void enlistDummyResourceForCleanup() throws Exception {
        try {
            LocalTranCurrentSet.instance().getLocalTranCoord().enlistForCleanup(new DummyOnePhaseXAResource());
        } catch (LTCSystemException ltcse) {
            System.out.println("enlistDummyResourceForCleanup unexpectedly caught LTCSystemException: "
                               + ltcse);
            ltcse.printStackTrace(System.out);
            throw new Exception();
        }
    }

    // private Mock enlistSync() {
    private Synchronization enlistSync() {
        // final Mock mockSync = mock(Synchronization.class);
        final Synchronization mockSync = mockery.mock(Synchronization.class);

        // LocalTranCurrentSet.instance().getLocalTranCoord().enlistSynchronization((Synchronization)
        // mockSync.proxy());
        LocalTranCurrentSet.instance().getLocalTranCoord().enlistSynchronization(mockSync);

        return mockSync;
    }

    private ContainerSynchronization enlistContainerSync() {
        final ContainerSynchronization mockSync = mockery.mock(ContainerSynchronization.class);
        LocalTranCurrentSet.instance().getLocalTranCoord().enlistSynchronization(mockSync);
        return mockSync;
    }

    private UserTransaction getMyUserTransaction() throws Exception {

        if (_ut == null) {
            try {
                final Class<?> c = Class.forName("com.ibm.tx.jta.impl.UserTransactionImpl");

                final Method m = c.getMethod("instance");

                _ut = (UserTransaction) m.invoke(null);
            } catch (Exception e) {
                System.out.println("getMyUserTransaction unexpectedly caught Exception: "
                                   + e);
                e.printStackTrace(System.out);
                throw new Exception();
            }
        }

        return _ut;
    }

    private class DummyOnePhaseXAResource implements OnePhaseXAResource {

        @Override
        public String getResourceName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void commit(Xid arg0, boolean arg1) throws XAException {
            // TODO Auto-generated method stub

        }

        @Override
        public void end(Xid arg0, int arg1) throws XAException {
            // TODO Auto-generated method stub

        }

        @Override
        public void forget(Xid arg0) throws XAException {
            // TODO Auto-generated method stub

        }

        @Override
        public int getTransactionTimeout() throws XAException {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public boolean isSameRM(XAResource arg0) throws XAException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public int prepare(Xid arg0) throws XAException {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public Xid[] recover(int arg0) throws XAException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void rollback(Xid arg0) throws XAException {
            // TODO Auto-generated method stub

        }

        @Override
        public boolean setTransactionTimeout(int arg0) throws XAException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void start(Xid arg0, int arg1) throws XAException {
            // TODO Auto-generated method stub

        }
    }

}
