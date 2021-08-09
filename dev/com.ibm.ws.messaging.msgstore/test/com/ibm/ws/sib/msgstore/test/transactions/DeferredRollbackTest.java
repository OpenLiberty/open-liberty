/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.msgstore.test.transactions;

/*
 * Change activity:
 *
 *  Reason         Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 * PK81848.2       08/07/09 gareth   Allow deferred rollback processing to take place
 * ============================================================================
 */

import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.RollbackException;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.impl.XidParticipant;

public class DeferredRollbackTest extends MessageStoreTestCase {
    private AtomicInteger _successfulPrepares = new AtomicInteger(0);
    private AtomicInteger _failedPrepares = new AtomicInteger(0);

    private AtomicInteger _successfulCommits = new AtomicInteger(0);
    private AtomicInteger _failedCommits = new AtomicInteger(0);

    private AtomicInteger _successfulRollbacks = new AtomicInteger(0);
    private AtomicInteger _failedRollbacks = new AtomicInteger(0);

    public DeferredRollbackTest(String name) {
        super(name);

        //turnOnTrace();
    }

    public static TestSuite suite() {
        return new TestSuite(DeferredRollbackTest.class);
    }

    public void testDeferredRollbackInPrepare() {
        print("|-----------------------------------------------------");
        print("| DeferredRollbackInPrepare:");
        print("|---------------------------");
        print("|");

        XidParticipant tran;
        Object pmLock = new Object();

        Thread prepareThread = null;
        Thread rollbackThreads[] = new Thread[10];

        // Reset the counters
        _successfulPrepares = new AtomicInteger(0);
        _failedPrepares = new AtomicInteger(0);
        _successfulRollbacks = new AtomicInteger(0);
        _failedRollbacks = new AtomicInteger(0);

        // This test will use a pausing persistence manager to help coordinate a
        // set of threads into the situation where we can get rollback being called
        // on the XID participant at the same time as it is being called to prepare.
        try {
            // Create tran with PM set up to pause in prepare
            tran = new XidParticipant(null, null, new PausablePersistenceManager(this, pmLock, PausablePersistenceManager.PAUSE_IN_PREPARE), 0);

            try {
                tran.addWork(new NullWorkItem(this));

                print("| WorkItem added to transaction");
            } catch (Exception e) {
                print("| Add WorkItem to transaction   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception caught adding WorkItem to transaction!");
            }

            // Create and start the prepare thread. This should run up to the point
            // where it calls the persistence manager at which point it should wait.
            try {
                prepareThread = new Thread(new PrepareThread(tran, this));

                prepareThread.start();

                print("| Started prepare thread");
            } catch (Exception e) {
                print("| Starting prepare thread   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception caught starting prepare thread!");
            }

            // Wait a little while to try and make sure our prepare thread 
            // reaches its pause point before we start the rollback threads
            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException ie) {
                }
            }

            // Create and start the rollback threads. They should all start and 
            // wait against the supplied lock.
            try {
                for (int i = 0; i < 10; i++) {
                    rollbackThreads[i] = new Thread(new RollbackThread(tran, this));
                }

                print("| Created 10 rollback threads");

                for (int i = 0; i < 10; i++) {
                    rollbackThreads[i].start();
                }

                print("| Started 10 rollback threads");
            } catch (Exception e) {
                print("| Starting rollback threads   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception caught starting rollback threads!");
            }

            // We need to wait for the rollback threads to finish now so that we avoid a race
            // between them going through the deferred rollback/exception processing and the 
            // prepare thread finishing prepare before they get going.
            try {
                for (int i = 0; i < 10; i++) {
                    rollbackThreads[i].join();
                }

                print("| Rollback threads have completed");
            } catch (Exception e) {
                print("| Waiting for rollback threads to complete   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception caught waiting for rollback threads to complete!");
            }

            // Finally we notify the persistence manager to continue so that the 
            // prepare thread can 
            try {
                synchronized (pmLock) {
                    pmLock.notify();
                }

                print("| Persistence manager notified");
            } catch (Exception e) {
                print("| Notifying persistence manager   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception caught notifying persistence manager!");
            }

            try {
                prepareThread.join();

                print("| Prepare thread has completed");
            } catch (Exception e) {
                print("| Waiting for prepare thread to complete   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception caught waiting for prepare thread to complete!");
            }

            // Check the counts to make sure we got the expected result
            if (_successfulPrepares.get() != 0) {
                print("| Prepare succeeded   !!!FAILED!!!");
                fail("Prepare succeeded when it should have rolled back!");
            }

            if (_failedPrepares.get() == 1) {
                print("| Prepare rolled back as expected");
            } else {
                print("| Prepare did not fail as expected   !!!FAILED!!!");
                fail("Prepare did not fail as expected!");
            }

            if (_failedRollbacks.get() != 9) {
                print("| Incorrect number of rollbacks failed   !!!FAILED!!!");
                fail("Incorrect number of rollbacks failed!");
            }

            if (_successfulRollbacks.get() == 1) {
                print("| Single rollback deferred successfully");
            } else {
                print("| Incorrect number of deferred rollbacks   !!!FAILED!!!");
                fail("Incorrect number of deferred rollbacks!");
            }
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            fail("Exception thrown during test: " + t.getMessage());
        } finally {
            print("|");
            print("|------------------------ END ------------------------");
        }
    }

    public void testDeferredRollbackInOnePhaseCommit() {
        print("|-----------------------------------------------------");
        print("| DeferredRollbackInOnePhaseCommit:");
        print("|----------------------------------");
        print("|");

        XidParticipant tran;
        Object itemLock = new Object();

        Thread commitThread = null;
        Thread rollbackThreads[] = new Thread[10];

        // Reset the counters
        _successfulCommits = new AtomicInteger(0);
        _failedCommits = new AtomicInteger(0);
        _successfulRollbacks = new AtomicInteger(0);
        _failedRollbacks = new AtomicInteger(0);

        // This test will use a pausing persistence manager to help coordinate a
        // set of threads into the situation where we can get rollback being called
        // on the XID participant at the same time as it is being called to prepare.
        try {
            // Create tran with PM set up to pause in prepare
            tran = new XidParticipant(null, null, new NullPersistenceManager(this), 0);

            try {
                tran.addWork(new PausableWorkItem(this, PausableWorkItem.PAUSE_IN_PRE_COMMIT, itemLock));

                print("| WorkItem added to transaction");
            } catch (Exception e) {
                print("| Add WorkItem to transaction   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception caught adding WorkItem to transaction!");
            }

            // Create and start the prepare thread. This should run up to the point
            // where it calls the persistence manager at which point it should wait.
            try {
                commitThread = new Thread(new CommitThread(tran, this, true));

                commitThread.start();

                print("| Started 1PC commit thread");
            } catch (Exception e) {
                print("| Starting 1PC commit thread   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception caught starting 1PC commit thread!");
            }

            // Wait a little while to try and make sure our commit thread 
            // reaches its pause point before we start the rollback threads
            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException ie) {
                }
            }

            // Create and start the rollback threads. They should all start and 
            // wait against the supplied lock.
            try {
                for (int i = 0; i < 10; i++) {
                    rollbackThreads[i] = new Thread(new RollbackThread(tran, this));
                }

                print("| Created 10 rollback threads");

                for (int i = 0; i < 10; i++) {
                    rollbackThreads[i].start();
                }

                print("| Started 10 rollback threads");
            } catch (Exception e) {
                print("| Starting rollback threads   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception caught starting rollback threads!");
            }

            // We need to wait for the rollback threads to finish now so that we avoid a race
            // between them going through the deferred rollback/exception processing and the 
            // prepare thread finishing prepare before they get going.
            try {
                for (int i = 0; i < 10; i++) {
                    rollbackThreads[i].join();
                }

                print("| Rollback threads have completed");
            } catch (Exception e) {
                print("| Waiting for rollback threads to complete   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception caught waiting for rollback threads to complete!");
            }

            // Finally we notify the persistence manager to continue so that the 
            // prepare thread can 
            try {
                synchronized (itemLock) {
                    itemLock.notify();
                }

                print("| Paused WorkItem notified");
            } catch (Exception e) {
                print("| Notifying paused WorkItem   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception caught notifying paused WorkItem!");
            }

            try {
                commitThread.join();

                print("| Commit thread has completed");
            } catch (Exception e) {
                print("| Waiting for commit thread to complete   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception caught waiting for commit thread to complete!");
            }

            // Check the counts to make sure we got the expected result
            if (_successfulCommits.get() != 0) {
                print("| Commit succeeded   !!!FAILED!!!");
                fail("Commit succeeded when it should have rolled back!");
            }

            if (_failedCommits.get() == 1) {
                print("| Commit rolled back as expected");
            } else {
                print("| Commit did not fail as expected   !!!FAILED!!!");
                fail("Commit did not fail as expected!");
            }

            if (_failedRollbacks.get() != 9) {
                print("| Incorrect number of rollbacks failed   !!!FAILED!!!");
                fail("Incorrect number of rollbacks failed!");
            }

            if (_successfulRollbacks.get() == 1) {
                print("| Single rollback deferred successfully");
            } else {
                print("| Incorrect number of deferred rollbacks   !!!FAILED!!!");
                fail("Incorrect number of deferred rollbacks!");
            }
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            fail("Exception thrown during test: " + t.getMessage());
        } finally {
            print("|");
            print("|------------------------ END ------------------------");
        }
    }

    public void testDeferredRollbackInTwoPhaseCommit() {
        print("|-----------------------------------------------------");
        print("| DeferredRollbackInTwoPhaseCommit:");
        print("|----------------------------------");
        print("|");

        XidParticipant tran;
        Object pmLock = new Object();

        Thread commitThread = null;
        Thread rollbackThreads[] = new Thread[10];

        // Reset the counters
        _successfulCommits = new AtomicInteger(0);
        _failedCommits = new AtomicInteger(0);
        _successfulRollbacks = new AtomicInteger(0);
        _failedRollbacks = new AtomicInteger(0);

        // This test will use a pausing persistence manager to help coordinate a
        // set of threads into the situation where we can get rollback being called
        // on the XID participant at the same time as it is being called to prepare.
        try {
            // Create tran with PM set up to pause in commit
            tran = new XidParticipant(null, null, new PausablePersistenceManager(this, pmLock, PausablePersistenceManager.PAUSE_IN_COMMIT), 0);

            try {
                tran.addWork(new NullWorkItem(this));

                print("| WorkItem added to transaction");
            } catch (Exception e) {
                print("| Add WorkItem to transaction   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception caught adding WorkItem to transaction!");
            }

            // Prepare the transaction normally so that we can attempt a two-phase
            // commit.
            try {
                tran.prepare();

                print("| Prepared transaction");
            } catch (Exception e) {
                print("| Preparing transaction   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception caught preparing transaction!");
            }

            // Create and start the prepare thread. This should run up to the point
            // where it calls the persistence manager at which point it should wait.
            try {
                commitThread = new Thread(new CommitThread(tran, this, false));

                commitThread.start();

                print("| Started 2PC commit thread");
            } catch (Exception e) {
                print("| Starting 2PC commit thread   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception caught starting 2PC commit thread!");
            }

            // Wait a little while to try and make sure our commit thread 
            // reaches its pause point before we start the rollback threads
            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException ie) {
                }
            }

            // Create and start the rollback threads. They should all start and 
            // wait against the supplied lock.
            try {
                for (int i = 0; i < 10; i++) {
                    rollbackThreads[i] = new Thread(new RollbackThread(tran, this));
                }

                print("| Created 10 rollback threads");

                for (int i = 0; i < 10; i++) {
                    rollbackThreads[i].start();
                }

                print("| Started 10 rollback threads");
            } catch (Exception e) {
                print("| Starting rollback threads   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception caught starting rollback threads!");
            }

            // We need to wait for the rollback threads to finish now so that we avoid a race
            // between them going through the deferred rollback/exception processing and the 
            // prepare thread finishing prepare before they get going.
            try {
                for (int i = 0; i < 10; i++) {
                    rollbackThreads[i].join();
                }

                print("| Rollback threads have completed");
            } catch (Exception e) {
                print("| Waiting for rollback threads to complete   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception caught waiting for rollback threads to complete!");
            }

            // Finally we notify the persistence manager to continue so that the 
            // prepare thread can 
            try {
                synchronized (pmLock) {
                    pmLock.notify();
                }

                print("| Paused persistence manager notified");
            } catch (Exception e) {
                print("| Notifying paused persistence manager   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception caught notifying paused persistence manager!");
            }

            try {
                commitThread.join();

                print("| Commit thread has completed");
            } catch (Exception e) {
                print("| Waiting for commit thread to complete   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception caught waiting for commit thread to complete!");
            }

            // Check the counts to make sure we got the expected result
            if (_failedCommits.get() != 0) {
                print("| Commit failed   !!!FAILED!!!");
                fail("Commit failed when it should have committed!");
            }

            if (_successfulCommits.get() == 1) {
                print("| Commit succeeded as expected");
            } else {
                print("| Commit did not succeed as expected   !!!FAILED!!!");
                fail("Commit did not succeed as expected!");
            }

            if (_failedRollbacks.get() != 10) {
                print("| Incorrect number of rollbacks failed   !!!FAILED!!!");
                fail("Incorrect number of rollbacks failed!");
            }

            if (_successfulRollbacks.get() != 0) {
                print("| Incorrect number of deferred rollbacks   !!!FAILED!!!");
                fail("Incorrect number of deferred rollbacks!");
            }
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            fail("Exception thrown during test: " + t.getMessage());
        } finally {
            print("|");
            print("|------------------------ END ------------------------");
        }
    }

    private class PrepareThread implements Runnable {
        private final XidParticipant _tran;
        private final MessageStoreTestCase _test;

        PrepareThread(XidParticipant tran, MessageStoreTestCase test) {
            _tran = tran;
            _test = test;
        }

        public void run() {
            try {
                _test.print("| Calling transaction with prepare");

                _tran.prepare();

                _test.print("| Transaction prepare completed normally   !!!FAILED!!!");

                _successfulPrepares.incrementAndGet();
            } catch (RollbackException rbe) {
                _test.print("| Transaction prepare rolled-back as expected");

                _failedPrepares.incrementAndGet();
            } catch (Exception e) {
                _test.print("| Prepare of transaction   !!!FAILED!!!");
                e.printStackTrace(System.err);
                _test.fail("Exception caught preparing transaction!");
            }
        }
    }

    private class CommitThread implements Runnable {
        private final XidParticipant _tran;
        private final MessageStoreTestCase _test;
        private final boolean _onePhase;

        CommitThread(XidParticipant tran, MessageStoreTestCase test, boolean onePhase) {
            _tran = tran;
            _test = test;
            _onePhase = onePhase;
        }

        public void run() {
            try {
                _test.print("| Calling transaction with commit. onePhase=" + _onePhase);

                _tran.commit(_onePhase);

                if (_onePhase) {
                    _test.print("| Transaction commit completed normally   !!!FAILED!!!");
                } else {
                    _test.print("| Transaction commit complete");
                }

                _successfulCommits.incrementAndGet();
            } catch (RollbackException rbe) {
                _failedCommits.incrementAndGet();

                if (_onePhase) {
                    _test.print("| Transaction commit rolled-back as expected");
                } else {
                    _test.print("| Commit of transaction   !!!FAILED!!!");
                    rbe.printStackTrace(System.err);
                    _test.fail("RollbackException caught commiting transaction!");
                }
            } catch (Exception e) {
                _test.print("| Commit of transaction   !!!FAILED!!!");
                e.printStackTrace(System.err);
                _test.fail("Exception caught commiting transaction!");
            }
        }
    }

    private class RollbackThread implements Runnable {
        private final XidParticipant _tran;
        private final MessageStoreTestCase _test;

        RollbackThread(XidParticipant tran, MessageStoreTestCase test) {
            _tran = tran;
            _test = test;
        }

        public void run() {
            _test.print("| Rollback thread started");

            try {
                _test.print("| Calling transaction with rollback");

                _tran.rollback();

                _test.print("| Transaction rollback complete");

                _successfulRollbacks.incrementAndGet();
            } catch (Exception e) {
                _test.print("| Rollback of transaction threw exception");

                _failedRollbacks.incrementAndGet();
            }
        }
    }
}
