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
package web;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskIdAccessor;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

/**
 * Task that sleeps when it runs.
 */
public class SleeperTask implements Callable<Integer>, ManagedTask, Runnable, Serializable {
    private static final long serialVersionUID = 209226992803003610L;

    private int counter;
    private final boolean createProp;
    private final Map<String, String> execProps;
    private final boolean getStatus;
    private final long sleepMS;

    public SleeperTask(String name, String tranExecPropValue, long sleepMS, boolean getStatus, boolean createProp) {
        Map<String, String> execProps = new TreeMap<String, String>();
        execProps.put(ManagedTask.IDENTITY_NAME, getClass().getSimpleName() + '-' + name);
        if (tranExecPropValue != null)
            execProps.put(ManagedTask.TRANSACTION, tranExecPropValue);
        this.execProps = Collections.unmodifiableMap(execProps);
        this.sleepMS = sleepMS;
        this.getStatus = getStatus;
        this.createProp = createProp;
    }

    @Override
    public Integer call() throws Exception {
        long startNanos = System.nanoTime();
        UserTransaction tran = null;
        try {
            ++counter;
            String taskName = execProps.get(ManagedTask.IDENTITY_NAME);
            System.out.println("Execution " + counter + " of task " + taskName);

            InitialContext initialContext = new InitialContext();
            if (ManagedTask.SUSPEND.equals(execProps.get(ManagedTask.TRANSACTION))) {
                tran = (UserTransaction) initialContext.lookup("java:comp/UserTransaction");
                tran.begin();
            }

            PersistentExecutor executor = (PersistentExecutor) initialContext.lookup("java:module/env/concurrent/myPersistentExecutorRef");

            // Request the TaskStatus for this task, which read locks the entry for this task in the TASK table
            Integer previousResult = null;
            if (getStatus) {
                TaskStatus<Integer> status = executor.getStatus(TaskIdAccessor.get());
                if (status.hasResult())
                    try {
                        previousResult = status.getResult();
                    } catch (CancellationException x) {
                        if (!ManagedTask.SUSPEND.equals(execProps.get(ManagedTask.TRANSACTION)))
                            throw x;
                    }
            }

            // Create a persistent executor property, which locks an entry in the PROP table
            if (createProp)
                executor.createProperty(taskName + '-' + counter, "previous:" + previousResult);
        } finally {
            if (tran != null)
                tran.commit();
            long durationNanos = System.nanoTime() - startNanos;
            long sleep = sleepMS - TimeUnit.NANOSECONDS.toMillis(durationNanos);
            if (sleep > 0)
                Thread.sleep(sleep);
        }

        return counter;
    }

    @Override
    public Map<String, String> getExecutionProperties() {
        return execProps;
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return null;
    }

    @Override
    public void run() {
        try {
            call();
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }
}
