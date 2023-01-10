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

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.transaction.UserTransaction;

import com.ibm.ws.jpa.fat.emlocking.entity.TaskEntity;

public class LockCompetingTask implements Callable<Exception>, ManagedTask {

    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(90);
    private static final long POLL_INTERVAL = 200;

    private final LockModeType lType;
    private final Object id;
    private final AtomicBoolean isStarted = new AtomicBoolean();
    private final CountDownLatch commitLatch;

    public LockCompetingTask(LockModeType lockType, Object id, CountDownLatch commitLatch) {
        this.lType = lockType;
        this.id = id;
        this.commitLatch = commitLatch;
    }

    @Override
    public Exception call() throws Exception {

        print("> Starting " + this);

        EntityManagerFactory emf = (EntityManagerFactory) new InitialContext().lookup("java:comp/env/jpa/TaskEMF");
        EntityManager em = emf.createEntityManager();
        UserTransaction tx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        print("got trans: " + tx);
        try {
            tx.begin();
            em.joinTransaction();
            print("started tran");
            TaskEntity entity = em.find(TaskEntity.class, id, lType);
            print("got entity: " + entity);
            entity.setIntData(2);
            entity.setStrData("Task-2");
            print("set some data");
            isStarted.set(true); // Let the servlet know that the data has been modified
            if (commitLatch.await(5, TimeUnit.SECONDS)) // Wait for the servlet to commit its tran
                print("Task is attempting commit now that servlet has committed.");
            else
                print("Task timed out waiting for the servlet to commit.");
            tx.commit();
            print("committed tran");
            return null;
        } catch (Exception e) {
            print("Got exception while running " + this);
            e.printStackTrace(System.out);
            return e;
        } finally {
            em.close();
            print("< Ending " + this);
        }
    }

    @Override
    public Map<String, String> getExecutionProperties() {
        return null;
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return null;
    }

    /**
     * Waits up to TIMEOUT_NS for the task to start running.
     * Throws an Exception if we hit TIMEOUT_NS and the task still hasn't started.
     */
    public void waitToStart() throws Exception {
        for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS && !isStarted.get();)
            Thread.sleep(POLL_INTERVAL);
        if (!isStarted.get())
            throw new Exception("Task failed to start in 90 seconds.  This must be a really really really slow build machine.");
    }

    @Override
    public String toString() {
        return super.toString() + " {id=" + id + ";  LockModeType=" + lType + ";  isStarted=" + isStarted.get() + "}";
    }

    private void print(String msg) {
        System.out.println("[Task   ] : " + msg);
    }
}
