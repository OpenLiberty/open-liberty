/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskIdAccessor;

/**
 * Task that cancels itself after a specified number of updates to the database.
 */
public class SelfRemovingTask extends DBIncrementTask {
    private static final long serialVersionUID = -2618001162981063401L;

    int maxUpdates;
    boolean runInNewTransaction;

    public SelfRemovingTask(String key, int maxUpdates, boolean runInNewTransaction) {
        super(key);
        this.maxUpdates = maxUpdates;
        this.runInNewTransaction = runInNewTransaction;
    }

    @Override
    public Integer call() throws Exception {
        int numUpdates;
        InitialContext initialContext = new InitialContext();
        UserTransaction tran = (UserTransaction) initialContext.lookup("java:comp/UserTransaction");
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

            if (!persistentExecutor.remove(taskId))
                throw new Exception("Task failed to remove itself. Task id is: " + taskId);
        }
        return numUpdates;
    }
}
