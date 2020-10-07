/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.util.Date;
import java.util.concurrent.Phaser;

import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

import com.ibm.websphere.concurrent.persistent.AutoPurge;
import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskIdAccessor;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

/**
 * Task that cancels itself after a specified number of updates to the database.
 */
public class SelfCancelingTask extends DBIncrementTask {
    private static final long serialVersionUID = -4749866518696581232L;

    int maxUpdates;
    boolean runInNewTransaction;

    public SelfCancelingTask(String key, int maxUpdates, boolean runInNewTransaction) {
        super(key);
        this.maxUpdates = maxUpdates;
        this.runInNewTransaction = runInNewTransaction;
    }

    @Override
    public Integer call() throws Exception {
        InitialContext initialContext = new InitialContext();
        UserTransaction tran = (UserTransaction) initialContext.lookup("java:comp/UserTransaction");

        int numUpdates;
        if (runInNewTransaction)
            tran.begin();
        try {
            numUpdates = super.call();
        } finally {
            if (runInNewTransaction)
                tran.commit();
        }

        if (numUpdates >= maxUpdates) {
            long taskId = TaskIdAccessor.get();
            PersistentExecutor persistentExecutor = (PersistentExecutor) initialContext.lookup("java:comp/env/web.SchedulerFATServlet/scheduler");

            TaskStatus<Integer> status = persistentExecutor.getStatus(taskId);
            if (taskId != status.getTaskId())
                throw new Exception("Task id doesn't match. Expecting " + taskId + " for " + status);

            Date nextExecutionTime = status.getNextExecutionTime();
            long delay = nextExecutionTime.getTime() - System.currentTimeMillis() - 5000; // extra allowance in case system clock changes while running
            if (delay > 0)
                throw new Exception("We shouldn't be running a task if the delay is still positive (" + delay + ") " + status);

            if (runInNewTransaction)
                tran.begin();
            try {
                if (!status.cancel(true))
                    throw new Exception("Task failed to cancel itself. Status is: " + status);
            } finally {
                if (runInNewTransaction)
                    tran.commit();
            }

            // coordinate timing with certain test cases
            Phaser phaser = SchedulerFATServlet.phaserRef.get();
            if (phaser != null) {
                int phase = phaser.arrive();
                int nextPhase = phaser.awaitAdvance(phase + 1);
                if (nextPhase != phase + 2)
                    throw new Exception("Expected next phase " + (phase + 2) + ". Instead, next phase is: " + nextPhase);
            }

            status = persistentExecutor.getStatus(taskId);

            String autoPurge = execProps.get(AutoPurge.PROPERTY_NAME);
            if (AutoPurge.ALWAYS.toString().equals(autoPurge)) {
                if (status != null)
                    throw new Exception("Task status not properly removed after cancel. " + status);
            } else {
                if (!status.isCancelled() || !status.isDone())
                    throw new Exception("Task status not properly updated after cancel. " + status);
            }
        }
        return numUpdates;
    }
}
