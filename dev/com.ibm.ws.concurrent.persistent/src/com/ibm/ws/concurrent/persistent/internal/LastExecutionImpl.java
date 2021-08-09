/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.internal;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.concurrent.LastExecution;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.concurrent.persistent.serializable.TaskFailure;
import com.ibm.ws.concurrent.persistent.serializable.TaskSkipped;

/**
 * Represents an execution of a task.
 */
public class LastExecutionImpl implements LastExecution {
    private static final TraceComponent tc = Tr.register(LastExecutionImpl.class);

    private final ClassLoader classLoader;
    private final long id;
    private final String identityName;
    private final PersistentExecutorImpl persistentExecutor;
    private final byte[] resultBytes;
    private final AtomicReference<Object> resultRef = new AtomicReference<Object>();
    private final long runEnd;
    private final long runStart;
    private final long scheduledStart;

    /**
     * Construct a new LastExecution record for a task entry that we read from the persistent store.
     * 
     * @param persistentExecutor persistent executor instance.
     * @param id task id.
     * @param identityName value of ManagedTask.IDENTITY_NAME execution property, if any, for the task.
     * @param resultInfoBytes serialized bytes for the result, if any.
     * @param runEnd time at which the task execution ended.
     * @param runStart time at which the task execution started.
     * @param scheduledStart time at which the task execution should have started.
     * @param classLoader class loader for the task result.
     */
    LastExecutionImpl(PersistentExecutorImpl persistentExecutor, long id, String identityName, @Sensitive byte[] resultInfoBytes,
                      long runEnd, long runStart, long scheduledStart, ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.id = id;
        this.identityName = identityName;
        this.persistentExecutor = persistentExecutor;
        this.resultBytes = resultInfoBytes;
        this.runEnd = runEnd;
        this.runStart = runStart;
        this.scheduledStart = scheduledStart;
    }

    /**
     * Construct a new LastExecution record for a task that just completed.
     * 
     * @param persistentExecutor persistent executor instance.
     * @param id task id.
     * @param identityName value of ManagedTask.IDENTITY_NAME execution property, if any, for the task.
     * @param result result of the task, if any.
     * @param runEnd time at which the task execution ended.
     * @param runStart time at which the task execution started.
     * @param scheduledStart time at which the task execution should have started.
     */
    LastExecutionImpl(PersistentExecutorImpl persistentExecutor, long id, String identityName, Object result,
                      long runEnd, long runStart, long scheduledStart) {
        this.id = id;
        this.identityName = identityName;
        this.persistentExecutor = persistentExecutor;
        this.resultBytes = null;
        this.resultRef.set(result);
        this.runEnd = runEnd;
        this.runStart = runStart;
        this.scheduledStart = scheduledStart;
        this.classLoader = null;
    }

    /**
     * Returns text formatted with both the task id and name (if any).
     * For example: "1001 (My Task Name)" or just "1001"
     * 
     * @return text formatted with both the task id and name (if any).
     */
    private final String getIdAndName() {
        StringBuilder sb = new StringBuilder().append(id);
        if (identityName != null && identityName.length() > 0)
            sb.append(" (").append(identityName).append(')');
        return sb.toString();
    }

    /**
     * @see javax.enterprise.concurrent.LastExecution#getIdentityName()
     */
    @Override
    public String getIdentityName() {
        return identityName;
    }

    /**
     * @see javax.enterprise.concurrent.LastExecution#getResult()
     */
    @Override
    public Object getResult() {
        Object result = resultRef.get();
        if (result == null && resultBytes != null)
            try {
                Object newResult = persistentExecutor.deserialize(resultBytes, classLoader);
                if (newResult instanceof TaskFailure) {
                    if (((TaskFailure) newResult).getReason() == TaskFailure.NONSER_RESULT)
                        ; // TODO warning message?
                } else if (newResult instanceof TaskSkipped) {
                    byte[] resultBytes = ((TaskSkipped) newResult).getPreviousResult();
                    newResult = resultBytes == null ? null : persistentExecutor.deserialize(resultBytes, classLoader);
                    if (resultRef.compareAndSet(null, newResult))
                        result = newResult;
                    else
                        result = resultRef.get();
                } else if (resultRef.compareAndSet(null, newResult))
                    result = newResult;
                else
                    result = resultRef.get();
            } catch (ClassNotFoundException x) {
                throw new RuntimeException(Tr.formatMessage(tc, "CWWKC1553.result.inaccessible", persistentExecutor.name, getIdAndName()), x);
            } catch (IOException x) {
                throw new RuntimeException(Tr.formatMessage(tc, "CWWKC1553.result.inaccessible", persistentExecutor.name, getIdAndName()), x);
            }
        return result;
    }

    /**
     * @see javax.enterprise.concurrent.LastExecution#getRunEnd()
     */
    @Override
    public Date getRunEnd() {
        return new Date(runEnd);
    }

    /**
     * @see javax.enterprise.concurrent.LastExecution#getRunStart()
     */
    @Override
    public Date getRunStart() {
        return new Date(runStart);
    }

    /**
     * @see javax.enterprise.concurrent.LastExecution#getScheduledStart()
     */
    @Override
    public Date getScheduledStart() {
        return new Date(scheduledStart);
    }
}
