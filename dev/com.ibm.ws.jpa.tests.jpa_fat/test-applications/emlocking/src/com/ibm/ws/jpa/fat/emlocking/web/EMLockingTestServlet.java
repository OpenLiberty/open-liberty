/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fat.emlocking.web;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import com.ibm.ws.jpa.fat.emlocking.entity.TaskEntity;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/EMLockingTestServlet")
public class EMLockingTestServlet extends FATServlet {

    @PersistenceContext(unitName = "EntityManagerLock_JEE")
    EntityManager em_EntityManagerLock_JEE;

    @Resource
    UserTransaction tx;

    @Resource
    ManagedExecutorService exec;

    @Override
    public void init() throws ServletException {
        super.init();
        // Poke the EntityManager to trigger drop-and-create before any tests start running
        em_EntityManagerLock_JEE.clear();
    }

    /**
     * Test Logic: testScenario01
     *
     * <p>
     * JPA 2.0 Specifications Tested (per V2 spec):
     * <ul>
     * <li>3.4 Locking and Concurrency
     * <li>3.4.1 Optimistic Locking
     * <li>3.4.2 Version Attributes
     * <li>3.4.3 Pessimistic Locking
     * <li>3.4.4 Lock Modes
     * <li>3.4.4.1 OPTIMISTIC, OPTIMISTIC_FORCE_INCREMENT
     * <li>3.4.4.2 PESSIMISTIC_READ, PESSIMISTIC_WRITE,
     * PESSIMISTIC_FORCE_INCREMENT
     * <li>3.4.5 OptimisticLockException
     * </ul>
     *
     * <p>
     * Description: For LockManager=mixed, a non-repeatable read condition
     * ensures OptimisticLockException is thrown somewhere for a versioned,
     * annotated or XML entity.
     *
     * <p>
     * A non-repeatable read is defined as:
     * <ol>
     * <li>For optimistic locks: Transaction T1 reads a row. Another transaction
     * T2 then modifies or deletes that row, before T1 has committed. Both
     * transactions eventually commit successfully.
     * <li>For pessimistic locks: Transaction T1 reads a row. Another
     * transaction T2 then modifies or deletes that row, before T1 has committed
     * or rolled back.
     * </ol>
     *
     * <p>
     * For non-versioned objects, OpenJPA does not provide any special
     * protections (i.e. OptimisticLockException or PessimisticLockException)
     * against non-repeatable reads.
     *
     * <p>
     * The JPA spec says that if a pessimistic lock mode type is specified, the
     * persistence provider must also perform optimistic version checks when
     * obtaining the database lock. If these checks fail, the
     * OptimisticLockException will be thrown. This will ONLY occur for
     * version-based entities.
     *
     * @param id                     The ID to use for the entity
     * @param lockMode               The LockModeType to use
     * @param expectServletException whether or not to expect an OptimisticLockException for this thread
     * @param expectTaskException    whether or not to expect an OptimisticLockException for the competing task
     */
    public void testScenario01(int id, LockModeType lockMode, boolean expectServletException, boolean expectTaskException) throws Exception {

        print("Scenario01:  LockModeType=" + lockMode +
              "  expectServletException=" + expectServletException +
              "  expectTaskException=" + expectTaskException);

        tx.begin();

        // 1) Create a new instance of TaskEntity
        TaskEntity newEntity = new TaskEntity(id, 1, "Servlet-1");

        // 2) Persist new entity to the database.");
        em_EntityManagerLock_JEE.persist(newEntity);
        tx.commit();
        em_EntityManagerLock_JEE.clear();

        // 3) Begin transaction, and set a lock on the entity using em.find(lockMode)
        tx.begin();
        print("Begin transaction and issue em.find(lockMode)");
        TaskEntity findEntity = em_EntityManagerLock_JEE.find(TaskEntity.class, id, lockMode);
        LockModeType entityLockMode = em_EntityManagerLock_JEE.getLockMode(findEntity);

        // JPA 2.0: Sec 3.4.4.2
        // It is permissible for an implementation to use LockModeType.PESSIMISTIC_WRITE where
        // LockModeType.PESSIMISTIC_READ was requested, but not vice versa.
        if (LockModeType.PESSIMISTIC_READ == lockMode)
            assertTrue("Entity expected lock mode = PESSIMISTIC_READ/PESSIMISTIC_WRITE, actual = " + entityLockMode,
                       entityLockMode == LockModeType.PESSIMISTIC_READ || entityLockMode == LockModeType.PESSIMISTIC_WRITE);
        else
            assertTrue("Entity expected lock mode = " + lockMode + ", actual = " + entityLockMode,
                       entityLockMode.equals(lockMode));

        // 4) Kick off a Task and await its completion.
        //    The Task will find and set an optimistic lock on an entity, and then update it.
        CountDownLatch commitLatch = expectServletException ? new CountDownLatch(0) : // we want the Servlet to commit first
                        new CountDownLatch(1); // we want the Task to commit first
        LockCompetingTask task = new LockCompetingTask(LockModeType.OPTIMISTIC, id, commitLatch);
        print("submitting the LockCompetingTask");
        Future<Exception> future = exec.submit(task);
        task.waitToStart();
        print("the task has started, servlet is now proceeding");

        Exception taskException;
        if (expectServletException) {
            print("waiting for the task to complete");
            taskException = future.get(15, TimeUnit.SECONDS);
        }

        // 5) Clear the persistence context, update the detached entity, merge it (which also auto-persists it to the database), and resolve transaction
        em_EntityManagerLock_JEE.clear();
        print("Updating data on task id=" + id);
        findEntity.setIntData(3);
        findEntity.setStrData("Servlet-3");

        try {
            print("invoking em.merge()");
            TaskEntity mergedFindEntity = em_EntityManagerLock_JEE.merge(findEntity);

            if (em_EntityManagerLock_JEE.contains(findEntity))
                fail("findEntity is managed, but it should NOT be managed.");
            if (!em_EntityManagerLock_JEE.contains(mergedFindEntity))
                fail("mergedFindEntity is NOT managed, but it should be managed.");

            tx.commit();
        } catch (Exception e) {
            if (expectServletException && isOptimisticLockException(e))
                print("Got expected OptimisticLockException from the servlet.");
            else
                throw new Exception("Got unexpected exception from the servlet commit: ", e);
        } finally {
            commitLatch.countDown();
        }

        taskException = future.get(15, TimeUnit.SECONDS);
        if (expectTaskException) {
            if (isOptimisticLockException(taskException))
                print("Got expected OptimisticLockException from the task.");
            else
                throw new Exception("Did not get expected OptimisticLockException from the task: ", taskException);
        } else if (taskException != null)
            throw new Exception("Task should not have raised any exception, but got: ", taskException);
    }

    public void testScenario01_CMTS_Annotated_NoLock() throws Exception {
        testScenario01(10, LockModeType.NONE, true, false);
    }

    public void testScenario01_CMTS_Annotated_Optimistic() throws Exception {
        testScenario01(11, LockModeType.OPTIMISTIC, true, false);
    }

    public void testScenario01_CMTS_Annotated_Optimistic_Force() throws Exception {
        testScenario01(12, LockModeType.OPTIMISTIC_FORCE_INCREMENT, true, false);
    }

    public void testScenario01_CMTS_Annotated_Pessimistic_Force() throws Exception {
        testScenario01(13, LockModeType.PESSIMISTIC_FORCE_INCREMENT, false, true);
    }

    public void testScenario01_CMTS_Annotated_Pessimistic_Read() throws Exception {
        testScenario01(14, LockModeType.PESSIMISTIC_READ, false, true);
    }

    public void testScenario01_CMTS_Annotated_Pessimistic_Write() throws Exception {
        testScenario01(15, LockModeType.PESSIMISTIC_WRITE, false, true);
    }

    /**
     * Test Logic: testScenario02
     *
     * <p>
     * JPA 2.0 Specifications Tested (per V2 spec):
     * <ul>
     * <li>3.4 Locking and Concurrency
     * <li>3.4.1 Optimistic Locking
     * <li>3.4.2 Version Attributes
     * <li>3.4.3 Pessimistic Locking
     * <li>3.4.4 Lock Modes
     * <li>3.4.4.1 OPTIMISTIC, OPTIMISTIC_FORCE_INCREMENT
     * <li>3.4.4.2 PESSIMISTIC_READ, PESSIMISTIC_WRITE,
     * PESSIMISTIC_FORCE_INCREMENT
     * <li>3.4.5 OptimisticLockException
     * </ul>
     *
     * <p>
     * Description: For LockManager=mixed, a dirty read ensures
     * OptimisticLockException is thrown somewhere for a versioned, annotated or
     * XML entity.
     *
     * <p>
     * A dirty read is defined as:
     * <ol>
     * <li>For optimistic locks: Transaction T1 modifies a row. Another
     * transaction T2 then reads that row and obtains the modified value, before
     * T1 has committed or rolled back. Transaction T2 eventually commits
     * successfully; it does not matter whether T1 commits or rolls back and
     * whether it does so before or after T2 commits.
     * <li>For pessimistic locks: Transaction T1 modifies a row. Another
     * transaction T2 then reads that row and obtains the modified value, before
     * T1 has committed or rolled back.
     * </ol>
     *
     * <p>
     * For non-versioned objects, OpenJPA does not provide any special
     * protections (i.e. OptimisticLockException or PessimisticLockException)
     * against dirty reads.
     *
     * <p>
     * The JPA spec says that if a pessimistic lock mode type is specified, the
     * persistence provider must also perform optimistic version checks when
     * obtaining the database lock. If these checks fail, the
     * OptimisticLockException will be thrown. This will ONLY occur for
     * version-based entities.
     *
     * @param id       The ID to use for the entity
     * @param lockMode The LockModeType to use
     */
    public void testScenario02(int id, LockModeType lockMode) throws Exception {

        print("Scenario02:  LockModeType=" + lockMode);

        tx.begin();

        // 1) Create a new instance of TaskEntity
        TaskEntity newEntity = new TaskEntity(id, 1, "ClientA-1");

        // 2) Persist new entity to the database
        em_EntityManagerLock_JEE.persist(newEntity);
        tx.commit();
        em_EntityManagerLock_JEE.clear();

        // 3) Begin transaction, and find the entity
        print("beginning transaction and finding entity");
        tx.begin();
        TaskEntity findEntity = em_EntityManagerLock_JEE.find(TaskEntity.class, id);

        // 4) Set a lock on the entity using em.lock(lockMode)
        em_EntityManagerLock_JEE.lock(findEntity, lockMode);
        print("Entitiy locked with mode=" + lockMode);
        LockModeType entityLockMode = em_EntityManagerLock_JEE.getLockMode(findEntity);

        // JPA 2.0: Sec 3.4.4.2
        // It is permissible for an implementation to use LockModeType.PESSIMISTIC_WRITE where
        // LockModeType.PESSIMISTIC_READ was requested, but not vice versa.
        if (LockModeType.PESSIMISTIC_READ == lockMode)
            assertTrue("Entity expected lock mode = PESSIMISTIC_READ/PESSIMISTIC_WRITE, actual = " + entityLockMode,
                       entityLockMode == LockModeType.PESSIMISTIC_READ || entityLockMode == LockModeType.PESSIMISTIC_WRITE);
        else
            assertTrue("Entity expected lock mode = " + lockMode + ", actual = " + entityLockMode,
                       entityLockMode.equals(lockMode));

        // 5) Update the entity, and flush it to the database - this enables the dirty read on the Task
        findEntity.setIntData(3);
        findEntity.setStrData("ClientA-3");
        print("updating entity");
        em_EntityManagerLock_JEE.flush();
        print("flushing updates to the db");

        // 6) Kick off the task, and await its completion
        //    The task will find and set an optimistic lock on an entity, and then update it
        CountDownLatch commitLatch = new CountDownLatch(1);
        LockCompetingTask task = new LockCompetingTask(LockModeType.OPTIMISTIC, id, commitLatch);
        print("starting task and waiting for task to begin...");
        Future<Exception> future = exec.submit(task);
        task.waitToStart();
        print("Task has started.  Servlet is proceeding.");

        // Implementation note: Spec section 3.4.4.2.: "When an application locks an entity with LockModeType.PESSIMISTIC_READ and later updates that entity, the lock must
        //   be converted to an exclusive lock when the entity is flushed to the database".  Albert says the ONLY way to test for this upgrade is by negative behavior.
        //   In other words, if an exception is NOT thrown for dirty reads on PESSIMISTIC_READ, we assume the lock conversion worked.  A bit ugly..but all we have.

        // 7) Resolve transaction
        try {
            tx.rollback();
            print("tran is rolled back");
        } finally {
            commitLatch.countDown();
        }

        Exception taskException = future.get(15, TimeUnit.SECONDS);
        if (isOptimisticLockException(taskException))
            print("Got expected OptimisticLockException from the task.");
        else
            throw new Exception("Did not get expected OptimisticLockException from the task: ", taskException);

    }

    public void testScenario02_CMTS_Annotated_NoLock() throws Exception {
        testScenario02(20, LockModeType.NONE);
    }

    public void testScenario02_CMTS_Annotated_Optimistic() throws Exception {
        testScenario02(21, LockModeType.OPTIMISTIC);
    }

    public void testScenario02_CMTS_Annotated_Optimistic_Force() throws Exception {
        testScenario02(22, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
    }

    public void testScenario02_CMTS_Annotated_Pessimistic_Force() throws Exception {
        testScenario02(23, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
    }

    public void testScenario02_CMTS_Annotated_Pessimistic_Read() throws Exception {
        testScenario02(24, LockModeType.PESSIMISTIC_READ);
    }

    public void testScenario02_CMTS_Annotated_Pessimistic_Write() throws Exception {
        testScenario02(25, LockModeType.PESSIMISTIC_WRITE);
    }

    /**
     * Test Logic: testScenario03
     *
     * <p>
     * JPA 2.0 Specifications Tested (per V2 spec):
     * <ul>
     * <li>3.4 Locking and Concurrency
     * <li>3.4.1 Optimistic Locking
     * <li>3.4.2 Version Attributes
     * <li>3.4.3 Pessimistic Locking
     * <li>3.4.4 Lock Modes
     * <li>3.4.4.1 OPTIMISTIC, OPTIMISTIC_FORCE_INCREMENT
     * <li>3.4.4.2 PESSIMISTIC_READ, PESSIMISTIC_WRITE,
     * PESSIMISTIC_FORCE_INCREMENT
     * <li>3.4.5 OptimisticLockException
     * </ul>
     *
     * <p>
     * Description: For LockManager=mixed, an exclusive lock ensures a
     * PessimisticLockException is thrown somewhere for a versioned, annotated
     * or XML entity.
     *
     * @param id                     The ID to use for the entity
     * @param lockMode               The LockModeType to use
     * @param expectServletException whether or not to expect an OptimisticLockException for this thread
     */
    public void testScenario03(int id, LockModeType lockMode, boolean expectServletException) throws Exception {

        print("Scenario03:  LockModeType=" + lockMode +
              "  expectServletException=" + expectServletException);

        // Informix requires the lock timeout to be set directly on the dictionary
        //if (((JDBCConfiguration) oemf.getConfiguration()).getDBDictionaryInstance().getClass().getName().contains("Informix")) {
        //    InformixDictionary ifxDict = (InformixDictionary) ((JDBCConfiguration) oemf.getConfiguration()).getDBDictionaryInstance();
        //    ifxDict.lockModeEnabled = true;
        //    ifxDict.lockWaitSeconds = 15;
        //}

        tx.begin();

        // 1) Create a new instance of TaskEntity
        TaskEntity newEntity = new TaskEntity(id, 1, "ClientA-1");

        // 2) Persist new entity to the database
        em_EntityManagerLock_JEE.persist(newEntity);
        tx.commit();
        em_EntityManagerLock_JEE.clear();

        // 3) Kick off the task and await its completion
        //    The task will find and set an exclusive lock on an entity
        LockCompetingTask task = new LockCompetingTask(LockModeType.PESSIMISTIC_WRITE, id, new CountDownLatch(0));
        Future<Exception> future = exec.submit(task);
        task.waitToStart();

        // 4) Begin transaction, and set a lock on the entity using em.find(lockMode)
        print("beginning transaction and finding the entity");
        tx.begin();
        TaskEntity findEntity = em_EntityManagerLock_JEE.find(TaskEntity.class, id, lockMode);
        LockModeType entityLockMode = em_EntityManagerLock_JEE.getLockMode(findEntity);

        // JPA 2.0: Sec 3.4.4.2
        // It is permissible for an implementation to use LockModeType.PESSIMISTIC_WRITE where
        // LockModeType.PESSIMISTIC_READ was requested, but not vice versa.
        if (LockModeType.PESSIMISTIC_READ == lockMode)
            assertTrue("Entity expected lock mode = PESSIMISTIC_READ/PESSIMISTIC_WRITE, actual = " + entityLockMode,
                       entityLockMode == LockModeType.PESSIMISTIC_READ || entityLockMode == LockModeType.PESSIMISTIC_WRITE);
        else
            assertTrue("Entity expected lock mode = " + lockMode + ", actual = " + entityLockMode,
                       entityLockMode.equals(lockMode));

        // 5) Update the entity, and resolve the transaction
        print("updating the entity");
        findEntity.setIntData(3);
        findEntity.setStrData("ClientA-3");

        try {
            print("performing final commit");
            tx.commit();
            print("final commit complete");
        } catch (Exception e) {
            if (expectServletException && isOptimisticLockException(e))
                print("Got expected OptimisticLockException from the servlet.");
            else
                throw new Exception("Got unexpected exception from the servlet commit: ", e);
        }

        Exception taskException = future.get(15, TimeUnit.SECONDS);
        if (taskException != null)
            throw new Exception("Got unexpected Exception from the task: ", taskException);

    }

    public void testScenario03_CMTS_Annotated_NoLock() throws Exception {
        testScenario03(30, LockModeType.NONE, true);
    }

    public void testScenario03_CMTS_Annotated_Optimistic() throws Exception {
        testScenario03(31, LockModeType.OPTIMISTIC, true);
    }

    public void testScenario03_CMTS_Annotated_Optimistic_Force() throws Exception {
        testScenario03(32, LockModeType.OPTIMISTIC_FORCE_INCREMENT, true);
    }

    public void testScenario03_CMTS_Annotated_Pessimistic_Force() throws Exception {
        testScenario03(33, LockModeType.PESSIMISTIC_FORCE_INCREMENT, false);
    }

    public void testScenario03_CMTS_Annotated_Pessimistic_Read() throws Exception {
        testScenario03(34, LockModeType.PESSIMISTIC_READ, false);
    }

    public void testScenario03_CMTS_Annotated_Pessimistic_Write() throws Exception {
        testScenario03(35, LockModeType.PESSIMISTIC_WRITE, false);
    }

    /**
     * Check if there is an OptimisticLockException anywhere in the Exception chain
     */
    private boolean isOptimisticLockException(Throwable original) {
        if (original == null)
            return false;

        Throwable cause = original;
        while (cause != null && cause.getCause() != null) {
            if (cause instanceof OptimisticLockException)
                return true;
            cause = cause.getCause();
        }

        return cause instanceof OptimisticLockException;
    }

    private void print(String msg) {
        System.out.println("[Servlet] : " + msg);
    }
}
