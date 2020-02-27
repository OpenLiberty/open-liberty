/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.Serializable;
import java.util.concurrent.Callable;

import javax.naming.InitialContext;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import com.ibm.websphere.concurrent.persistent.TaskIdAccessor;

/**
 * Task that waits until it is rolled back. This is done to test transaction timeout.
 */
public class WaitForRollbackTask implements Callable<Integer>, Serializable {
    private static final long serialVersionUID = 1L;

    // PersistentErrorTestServlet.init sets this value, because this task is run without context
    // and otherwise has no way of looking up UserTransaction.
    static UserTransaction tran;

    @Override
    public Integer call() throws Exception {
        long taskId = TaskIdAccessor.get();
        System.out.println("Started task " + taskId);

        do Thread.sleep(200);
        while (tran.getStatus() != Status.STATUS_MARKED_ROLLBACK);

        System.out.println("Completed task " + taskId);
        return tran.getStatus();
    }
}
