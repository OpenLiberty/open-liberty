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
package com.ibm.wsspi.concurrent.persistent;

import java.util.Arrays;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskStatus;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.persistent.internal.PersistentExecutorImpl;
import com.ibm.ws.concurrent.persistent.internal.TaskStatusImpl;
import com.ibm.ws.concurrent.persistent.internal.Utils;

/**
 * A collection of attributes for a persistent task.
 * An instance of this class might have only a subset of attributes specified.
 */
@Trivial
public final class TaskRecord {
    /**
     * Queryable flags stored in the MiscBinaryFlags field
     */
    @Trivial
    public enum Flags {
        /**
         * Task record should be removed from the persistent store upon completion (successful or otherwise)
         */
        AUTO_PURGE_ALWAYS((short) 0x1),

        /**
         * Task record should be removed from the persistent store only upon successful completion.
         */
        AUTO_PURGE_ON_SUCCESS((short) 0x2),

        /**
         * Indicates that the task is an EJB singleton, which requires special collaboration with the container
         * with regards to locking in order to avoid deadlocks.
         */
        EJB_SINGLETON((short) 0x8),

        /**
         * Indicates that the task is an EJB timer task.
         */
        EJB_TIMER((short) 0x10),

        /**
         * Task is scheduled as a one-shot task (and not with a trigger, which could be any number of executions including one).
         */
        ONE_SHOT_TASK((short) 0x100),

        /**
         * Task should run with the persistent executor's transaction suspended.
         */
        SUSPEND_TRAN_OF_EXECUTOR_THREAD((short) 0x1000);

        public final short bit;

        private Flags(short bit) {
            this.bit = bit;
        }
    }

    /**
     * End of line character.
     */
    private static final String EOLN = String.format("%n");

    /**
     * Bits corresponding to each specified attribute.
     */
    private static final int ID = 0x1,
                    ID_OF_CLASSLOADER = 0x2,
                    ID_OF_OWNER = 0x4,
                    CLAIM_EXPIRY_OR_PARTITION = 0x8,
                    MISC_BINARY_FLAGS = 0x10,
                    NAME = 0x20,
                    NEXT_EXEC_TIME = 0x40,
                    ORIG_SUBMIT_TIME = 0x80,
                    PREV_SCHED_START_TIME = 0x100,
                    PREV_START_TIME = 0x200,
                    PREV_STOP_TIME = 0x400,
                    RESULT = 0x800,
                    RFAILS = 0x1000,
                    STATE = 0x2000,
                    TASK = 0x4000,
                    TASK_INFO = 0x8000,
                    TRIGGER = 0x10000,
                    TX_TIMEOUT = 0x20000,
                    VERSION = 0x40000;

    /**
     * Bits indicating which attributes are set.
     */
    private int attrs;

    /**
     * Number of consecutive failed attempts to execute the task.
     */
    private short consecutiveFailureCount;

    /**
     * Unique identifier for the task.
     */
    private long id;

    /**
     * Identifier of the thread context class loader of the thread from which the task was originally submitted.
     */
    private String idOfClassLoader;

    /**
     * Identifier of the owner of the task. The owner is typically the application that submitted the task,
     * but could also be a component of a user feature or internal feature.
     */
    private String idOfOwner;

    /**
     * The expiry of the current claim on task execution (if fail over is enabled), or otherwise the partition to which the task is assigned.
     */
    private long claimExpiryOrPartition;

    /**
     * Value that represents a combination of miscellaneous boolean flags.
     */
    private short miscBinaryFlags;

    /**
     * Name of the task. There is no requirement for the name to be unique.
     */
    private String name;

    /**
     * Milliseconds at which the next execution of the task should occur.
     */
    private long nextExecTime;

    /**
     * Milliseconds at which the task was submitted.
     */
    private long origSubmitTime;

    /**
     * Milliseconds at which the task was previously scheduled to start. Can be null if no previous execution.
     */
    private Long prevSchedStartTime;

    /**
     * Milliseconds at which the task previously started. Can be null if no previous execution.
     */
    private Long prevStartTime;

    /**
     * Milliseconds at which the task previously stopped. Can be null if no previous execution.
     */
    private Long prevStopTime;

    /**
     * Serialized result of the task. Can be null if no previous execution or for tasks that do not return any result or have a null result.
     */
    private byte[] result;

    /**
     * State of the task.
     */
    private short state;

    /**
     * Serialized callable or runnable, if any, for this task. Null if the task is not serializable.
     */
    private byte[] task;

    /**
     * Serialized non-queryable persistent task information.
     */
    private byte[] taskInfo;

    /**
     * Serialized trigger, if any, for this task. Null if there is no trigger or the trigger is not serializable.
     */
    private byte[] trigger;

    /**
     * Transaction timeout.
     */
    private int txTimeout;

    /**
     * Row version.
     */
    private int version;

    /**
     * Construct an empty task record.
     *
     * @param allAttributesAreSpecified indicates whether all attributes should be considered specified or unspecified.
     */
    public TaskRecord(boolean allAttributesAreSpecified) {
        attrs = allAttributesAreSpecified ? 0xffff : 0;
    }

    /**
     * Deep comparison of attributes for equality. Only specified attributes are compared.
     * Both instances must specify the same sets of attributes.
     *
     * @param obj instance with which to compare.
     * @return true if equal, otherwise false.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof TaskRecord) {
            TaskRecord other = (TaskRecord) obj;
            return attrs == other.attrs
                   && ((attrs & ID) == 0 || id == other.id)
                   && ((attrs & ID_OF_CLASSLOADER) == 0 || match(idOfClassLoader, other.idOfClassLoader))
                   && ((attrs & ID_OF_OWNER) == 0 || match(idOfOwner, other.idOfOwner))
                   && ((attrs & CLAIM_EXPIRY_OR_PARTITION) == 0 || claimExpiryOrPartition == other.claimExpiryOrPartition)
                   && ((attrs & MISC_BINARY_FLAGS) == 0 || miscBinaryFlags == other.miscBinaryFlags)
                   && ((attrs & NAME) == 0 || match(name, other.name))
                   && ((attrs & NEXT_EXEC_TIME) == 0 || nextExecTime == other.nextExecTime)
                   && ((attrs & ORIG_SUBMIT_TIME) == 0 || origSubmitTime == other.origSubmitTime)
                   && ((attrs & PREV_SCHED_START_TIME) == 0 || match(prevSchedStartTime, other.prevSchedStartTime))
                   && ((attrs & PREV_START_TIME) == 0 || match(prevStartTime, other.prevStartTime))
                   && ((attrs & PREV_STOP_TIME) == 0 || match(prevStopTime, other.prevStopTime))
                   && ((attrs & RESULT) == 0 || Arrays.equals(result, other.result))
                   && ((attrs & RFAILS) == 0 || consecutiveFailureCount == other.consecutiveFailureCount)
                   && ((attrs & STATE) == 0 || state == other.state)
                   && ((attrs & TASK) == 0 || Arrays.equals(task, other.task))
                   && ((attrs & TASK_INFO) == 0 || Arrays.equals(taskInfo, other.taskInfo))
                   && ((attrs & TRIGGER) == 0 || Arrays.equals(trigger, other.trigger))
                   && ((attrs & TX_TIMEOUT) == 0 || txTimeout == other.txTimeout)
                   && ((attrs & VERSION) == 0 || version == other.version);
        }
        return false;
    }

    /**
     * Returns the number of consecutive failed attempts to execute the task.
     *
     * @return the number of consecutive failed attempts to execute the task.
     */
    public final short getConsecutiveFailureCount() {
        if ((attrs & RFAILS) == 0)
            throw new IllegalStateException();
        else
            return consecutiveFailureCount;
    }

    /**
     * Returns the unique identifier for the task.
     *
     * @return the unique identifier for the task.
     */
    public final long getId() {
        if ((attrs & ID) == 0)
            throw new IllegalStateException();
        else
            return id;
    }

    /**
     * Returns the identifier of the thread context class loader of the thread from which the task was originally submitted.
     *
     * @return the identifier of the class loader.
     */
    public final String getIdentifierOfClassLoader() {
        if ((attrs & ID_OF_CLASSLOADER) == 0)
            throw new IllegalStateException();
        else
            return idOfClassLoader;
    }

    /**
     * Returns the identifier of the owner of the task.
     *
     * @return the identifier of the owner of the task.
     */
    public final String getIdentifierOfOwner() {
        if ((attrs & ID_OF_OWNER) == 0)
            throw new IllegalStateException();
        else
            return idOfOwner;
    }

    /**
     * Returns the expiry of the current claim on task execution (if fail over is enabled),
     * or otherwise the partition to which the task is assigned.
     *
     * @return claim expiry or partition id value.
     */
    public final long getClaimExpiryOrPartition() {
        if ((attrs & CLAIM_EXPIRY_OR_PARTITION) == 0)
            throw new IllegalStateException();
        else
            return claimExpiryOrPartition;
    }

    /**
     * Returns a value that represents a combination of miscellaneous boolean flags.
     *
     * @return a value that represents a combination of miscellaneous boolean flags.
     */
    public final short getMiscBinaryFlags() {
        if ((attrs & MISC_BINARY_FLAGS) == 0)
            throw new IllegalStateException();
        else
            return miscBinaryFlags;
    }

    /**
     * Returns the name of the task. There is no requirement for the name to be unique.
     *
     * @return the name task name
     */
    public final String getName() {
        if ((attrs & NAME) == 0)
            throw new IllegalStateException();
        else
            return name;
    }

    /**
     * Returns the time at which the next execution of the task should occur.
     *
     * @return milliseconds at which the next execution of the task should occur.
     */
    public final long getNextExecutionTime() {
        if ((attrs & NEXT_EXEC_TIME) == 0)
            throw new IllegalStateException();
        else
            return nextExecTime;
    }

    /**
     * Returns the time at which the task was submitted.
     *
     * @return milliseconds at which the task was submitted.
     */
    public final long getOriginalSubmitTime() {
        if ((attrs & ORIG_SUBMIT_TIME) == 0)
            throw new IllegalStateException();
        else
            return origSubmitTime;
    }

    /**
     * Returns the time at which the task was previously scheduled to start.
     *
     * @return milliseconds at which the task was previously scheduled to start. Can be null if no previous execution.
     */
    public final Long getPreviousScheduledStartTime() {
        if ((attrs & PREV_SCHED_START_TIME) == 0)
            throw new IllegalStateException();
        else
            return prevSchedStartTime;
    }

    /**
     * Returns the time at which the task previously started.
     *
     * @return milliseconds at which the task previously started. Can be null if no previous execution.
     */
    public final Long getPreviousStartTime() {
        if ((attrs & PREV_START_TIME) == 0)
            throw new IllegalStateException();
        else
            return prevStartTime;
    }

    /**
     * Returns the time at which the task previously stopped.
     *
     * @return milliseconds at which the task previously stopped. Can be null if no previous execution.
     */
    public final Long getPreviousStopTime() {
        if ((attrs & PREV_STOP_TIME) == 0)
            throw new IllegalStateException();
        else
            return prevStopTime;
    }

    /**
     * Returns the serialized result, if any, for this task.
     *
     * @return the result of the task, serialized as bytes. Can be null if no previous execution or if the result is null or if there is not any result.
     */
    public final byte[] getResult() {
        if ((attrs & RESULT) == 0)
            throw new IllegalStateException();
        else
            return result;
    }

    /**
     * Returns the state of the task.
     *
     * @return the state of the task.
     */
    public final short getState() {
        if ((attrs & STATE) == 0)
            throw new IllegalStateException();
        else
            return state;
    }

    /**
     * Returns the serialized callable or runnable, if any, for this task.
     *
     * @return the serialized callable or runnable. Null if the task is not serializable.
     */
    public final byte[] getTask() {
        if ((attrs & TASK) == 0)
            throw new IllegalStateException();
        else
            return task;
    }

    /**
     * Returns the serialized non-queryable persistent task information.
     *
     * @return the serialized non-queryable persistent task information.
     */
    public final byte[] getTaskInformation() {
        if ((attrs & TASK_INFO) == 0)
            throw new IllegalStateException();
        else
            return taskInfo;
    }

    /**
     * Returns the transaction timeout, if any, for this task record.
     *
     * @return the transaction timeout.
     */
    public int getTransactionTimeout() {
        if ((attrs & TX_TIMEOUT) == 0)
            throw new IllegalStateException();
        else
            return txTimeout;
    }

    /**
     * Returns the serialized trigger, if any, for this task.
     *
     * @return the trigger, serialized as bytes. Null if there is no trigger or the trigger is not serializable.
     */
    public final byte[] getTrigger() {
        if ((attrs & TRIGGER) == 0)
            throw new IllegalStateException();
        else
            return trigger;
    }

    /**
     * Returns the row version for this task record.
     *
     * @return the row version.
     */
    public final int getVersion() {
        if ((attrs & VERSION) == 0)
            throw new IllegalStateException();
        else
            return version;
    }

    /**
     * Hash code for the task record is computed from the id.
     *
     * @return the hash code.
     */
    @Override
    public final int hashCode() {
        return (int) id;
    }

    /**
     * Returns true if the ConsecutiveFailureCount attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasConsecutiveFailureCount() {
        return (attrs & RFAILS) != 0;
    }

    /**
     * Returns true if the Id attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasId() {
        return (attrs & ID) != 0;
    }

    /**
     * Returns true if the IdentifierOfClassLoader attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasIdentifierOfClassLoader() {
        return (attrs & ID_OF_CLASSLOADER) != 0;
    }

    /**
     * Returns true if the IdentifierOfOwner attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasIdentifierOfOwner() {
        return (attrs & ID_OF_OWNER) != 0;
    }

    /**
     * Returns true if the ClaimExpiryOrPartition attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasClaimExpiryOrPartition() {
        return (attrs & CLAIM_EXPIRY_OR_PARTITION) != 0;
    }

    /**
     * Returns true if the MiscBinaryFlags attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasMiscBinaryFlags() {
        return (attrs & MISC_BINARY_FLAGS) != 0;
    }

    /**
     * Returns true if the Name attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasName() {
        return (attrs & NAME) != 0;
    }

    /**
     * Returns true if the NextExecutionTime attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasNextExecutionTime() {
        return (attrs & NEXT_EXEC_TIME) != 0;
    }

    /**
     * Returns true if the OriginalSubmitTime attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasOriginalSubmitTime() {
        return (attrs & ORIG_SUBMIT_TIME) != 0;
    }

    /**
     * Returns true if the PreviousScheduledStartTime attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasPreviousScheduledStartTime() {
        return (attrs & PREV_SCHED_START_TIME) != 0;
    }

    /**
     * Returns true if the PreviousStartTime attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasPreviousStartTime() {
        return (attrs & PREV_START_TIME) != 0;
    }

    /**
     * Returns true if the PreviousStopTime attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasPreviousStopTime() {
        return (attrs & PREV_STOP_TIME) != 0;
    }

    /**
     * Returns true if the Result attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasResult() {
        return (attrs & RESULT) != 0;
    }

    /**
     * Returns true if the State attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasState() {
        return (attrs & STATE) != 0;
    }

    /**
     * Returns true if the Task attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasTask() {
        return (attrs & TASK) != 0;
    }

    /**
     * Returns true if the TaskInformation attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasTaskInformation() {
        return (attrs & TASK_INFO) != 0;
    }

    /**
     * Returns true if the Transaction Timeout attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasTransactionTimeout() {
        return (attrs & TX_TIMEOUT) != 0;
    }

    /**
     * Returns true if the Trigger attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasTrigger() {
        return (attrs & TRIGGER) != 0;
    }

    /**
     * Returns true if the Version attribute is set. Otherwise false.
     *
     * @return true if the attribute is set. Otherwise false.
     */
    public final boolean hasVersion() {
        return (attrs & VERSION) != 0;
    }

    /**
     * Utility method to compare for equality.
     *
     * @param obj1 first object.
     * @param obj2 second object.
     * @return true if equal or both null. False otherwise.
     */
    private static final boolean match(Object obj1, Object obj2) {
        return obj1 == obj2 || obj1 != null && obj1.equals(obj2);
    }

    /**
     * Sets the number of consecutive failed attempts to execute the task.
     *
     * @param consecutiveFailureCount the number of consecutive failed attempts to execute the task.
     */
    public final void setConsecutiveFailureCount(short consecutiveFailureCount) {
        this.consecutiveFailureCount = consecutiveFailureCount;
        attrs |= RFAILS;
    }

    /**
     * Sets the unique identifier for the task.
     *
     * @param id unique identifier for the task.
     */
    public final void setId(long id) {
        this.id = id;
        attrs |= ID;
    }

    /**
     * Sets the identifier of the thread context class loader of the thread from which the task was originally submitted.
     *
     * @param identifier identifier of the class loader.
     */
    public final void setIdentifierOfClassLoader(String identifier) {
        this.idOfClassLoader = identifier;
        attrs |= ID_OF_CLASSLOADER;
    }

    /**
     * Sets the identifier of the owner of the task.
     *
     * @param identifier identifier of the owner of the task.
     */
    public final void setIdentifierOfOwner(String identifier) {
        this.idOfOwner = identifier;
        attrs |= ID_OF_OWNER;
    }

    /**
     * Sets the expiry of the current claim on task execution (if fail over is enabled),
     * or otherwise the partition to which the task is assigned.
     *
     * @param value the new value to use.
     */
    public final void setClaimExpiryOrPartition(long value) {
        this.claimExpiryOrPartition = value;
        attrs |= CLAIM_EXPIRY_OR_PARTITION;
    }

    /**
     * Sets a value that represents a combination of miscellaneous boolean flags.
     *
     * @param miscBinaryFlags value that represents a combination of miscellaneous boolean flags.
     */
    public final void setMiscBinaryFlags(short miscBinaryFlags) {
        this.miscBinaryFlags = miscBinaryFlags;
        attrs |= MISC_BINARY_FLAGS;
    }

    /**
     * Sets the name of the task. There is no requirement for the name to be unique.
     *
     * @param name name of the task.
     */
    public final void setName(String name) {
        this.name = name;
        attrs |= NAME;
    }

    /**
     * Sets the time at which the next execution of the task should occur.
     *
     * @param nextExecTime milliseconds at which the next execution of the task should occur.
     */
    public final void setNextExecutionTime(long nextExecTime) {
        this.nextExecTime = nextExecTime;
        attrs |= NEXT_EXEC_TIME;
    }

    /**
     * Sets the time at which the task was submitted.
     *
     * @param origSubmitTime milliseconds at which the task was submitted.
     */
    public final void setOriginalSubmitTime(long origSubmitTime) {
        this.origSubmitTime = origSubmitTime;
        attrs |= ORIG_SUBMIT_TIME;
    }

    /**
     * Sets the time at which the task was previously scheduled to start.
     *
     * @param prevSchedStartTime milliseconds at which the task was previously scheduled to start. Can be null if no previous execution.
     */
    public final void setPreviousScheduledStartTime(Long prevSchedStartTime) {
        this.prevSchedStartTime = prevSchedStartTime;
        attrs |= PREV_SCHED_START_TIME;
    }

    /**
     * Sets the time at which the task previously started.
     *
     * @param prevStartTime milliseconds at which the task previously started. Can be null if no previous execution.
     */
    public final void setPreviousStartTime(Long prevStartTime) {
        this.prevStartTime = prevStartTime;
        attrs |= PREV_START_TIME;
    }

    /**
     * Sets the time at which the task previously stopped.
     *
     * @param prevStopTime milliseconds at which the task previously stopped. Can be null if no previous execution.
     */
    public final void setPreviousStopTime(Long prevStopTime) {
        this.prevStopTime = prevStopTime;
        attrs |= PREV_STOP_TIME;
    }

    /**
     * Sets the serialized result of the task, if any.
     *
     * @param result result of the task. Can be null if no previous execution or there is no result or the result is null.
     */
    public final void setResult(byte[] result) {
        this.result = result;
        attrs |= RESULT;
    }

    /**
     * Sets the state of the task.
     *
     * @param state the state of the task.
     */
    public final void setState(short state) {
        this.state = state;
        attrs |= STATE;
    }

    /**
     * Sets the serialized callable or runnable, if any, for this task.
     *
     * @param task callable or runnable, serialized as bytes. Null indicates the task is not serializable.
     */
    public final void setTask(byte[] task) {
        this.task = task;
        attrs |= TASK;
    }

    /**
     * Sets the serialized non-queryable persistent task information.
     *
     * @param taskInfo serialized non-queryable persistent task information.
     */
    public final void setTaskInformation(byte[] taskInfo) {
        this.taskInfo = taskInfo;
        attrs |= TASK_INFO;
    }

    /**
     * Sets the transaction timeout for this task.
     *
     * @param txTimeout the transaction timeout.
     */
    public final void setTransactionTimeout(int txTimeout) {
        this.txTimeout = txTimeout;
        attrs |= TX_TIMEOUT;
    }

    /**
     * Sets the serialized trigger, if any, for this task.
     *
     * @param trigger the trigger, serialized as bytes. Null if there is no trigger or the trigger is not serializable.
     */
    public final void setTrigger(byte[] trigger) {
        this.trigger = trigger;
        attrs |= TRIGGER;
    }

    /**
     * Sets the row version for this task record.
     *
     * @param version the row version.
     */
    public final void setVersion(int version) {
        this.version = version;
        attrs |= VERSION;
    }

    /**
     * Returns a textual representation of this instance.
     *
     * @return a textual representation of this instance.
     */
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder(500).append("TaskRecord");
        if ((attrs & ID) != 0)
            output.append('[').append(id).append(']');
        output.append('@').append(Integer.toHexString(System.identityHashCode(this)));
        if ((attrs & ID_OF_CLASSLOADER) != 0)
            output.append(EOLN).append("CLASSLOADER=").append(idOfClassLoader);
        if ((attrs & ID_OF_OWNER) != 0)
            output.append(EOLN).append("OWNER=").append(idOfOwner);
        if ((attrs & MISC_BINARY_FLAGS) != 0)
            output.append(EOLN).append("FLAGS=").append(Integer.toBinaryString(miscBinaryFlags));
        if ((attrs & NAME) != 0)
            output.append(EOLN).append("NAME=").append(name);
        if ((attrs & CLAIM_EXPIRY_OR_PARTITION) != 0)
            if (claimExpiryOrPartition > 1500000000000l)
                Utils.appendDate(output.append(EOLN).append("CLAIMTIL="), claimExpiryOrPartition);
            else
                output.append(EOLN).append("PARTITION=").append(claimExpiryOrPartition);
        if ((attrs & NEXT_EXEC_TIME) != 0)
            Utils.appendDate(output.append(EOLN).append("NEXTEXEC="), nextExecTime);
        if ((attrs & ORIG_SUBMIT_TIME) != 0)
            Utils.appendDate(output.append(EOLN).append("ORIGSBMT="), origSubmitTime);
        if ((attrs & PREV_SCHED_START_TIME) != 0)
            Utils.appendDate(output.append(EOLN).append("PREVSCHD="), prevSchedStartTime);
        if ((attrs & PREV_START_TIME) != 0)
            Utils.appendDate(output.append(EOLN).append("PREVSTRT="), prevStartTime);
        if ((attrs & PREV_STOP_TIME) != 0)
            Utils.appendDate(output.append(EOLN).append("PREVSTOP="), prevStopTime);
        if ((attrs & RESULT) != 0) {
            output.append(EOLN).append("RESULT=");
            if (result != null)
                output.append("byte[").append(result.length).append(']');
        }
        if ((attrs & RFAILS) != 0)
            output.append(EOLN).append("FAILURES=").append(consecutiveFailureCount);
        if ((attrs & STATE) != 0)
            Utils.appendState(output.append(EOLN).append("STATE="), state);
        if ((attrs & TASK) != 0) {
            output.append(EOLN).append("TASK=");
            if (task != null)
                output.append("byte[").append(task.length).append(']');
        }
        if ((attrs & TASK_INFO) != 0) {
            output.append(EOLN).append("TASKINFO=");
            if (taskInfo != null)
                output.append("byte[").append(taskInfo.length).append(']');
        }
        if ((attrs & TRIGGER) != 0) {
            output.append(EOLN).append("TRIGGER=");
            if (trigger != null)
                output.append("byte[").append(trigger.length).append(']');
        }
        if ((attrs & TX_TIMEOUT) != 0)
            output.append(EOLN).append("TXTIMEOUT=").append(txTimeout);
        if ((attrs & VERSION) != 0)
            output.append(EOLN).append("VERSION=").append(version);

        return output.toString();
    }

    /**
     * Constructs a new TaskStatus instance corresponding to the state of this task record as of the point in time
     * when the method is invoked. The following attributes must be set on the task record before invoking this method:
     * (Id, IdentifierOfClassLoader, MiscBinaryFlags, Name, NextExecutionTime, Result, State, Trigger)
     *
     * @param executor persistent executor instance.
     * @return a new TaskStatus instance corresponding to this task record.
     */
    public <T> TaskStatus<T> toTaskStatus(PersistentExecutor executor) {
        return new TaskStatusImpl<T>(this, (PersistentExecutorImpl) executor);
    }

    /**
     * Unsets the ConsecutiveFailureCount attribute.
     */
    public final void unsetConsecutiveFailureCount() {
        attrs &= ~RFAILS;
    }

    /**
     * Unsets the Id attribute.
     */
    public final void unsetId() {
        attrs &= ~ID;
    }

    /**
     * Unsets the IdentifierOfClassLoader attribute.
     */
    public final void unsetIdentifierOfClassLoader() {
        attrs &= ~ID_OF_CLASSLOADER;
    }

    /**
     * Unsets the IdentifierOfOwner attribute.
     */
    public final void unsetIdentifierOfOwner() {
        attrs &= ~ID_OF_OWNER;
    }

    /**
     * Unsets the ClaimExpiryOrPartition attribute.
     */
    public final void unsetClaimExpiryOrPartition() {
        attrs &= ~CLAIM_EXPIRY_OR_PARTITION;
    }

    /**
     * Unsets the MiscBinaryFlags attribute.
     */
    public final void unsetMiscBinaryFlags() {
        attrs &= ~MISC_BINARY_FLAGS;
    }

    /**
     * Unsets the Name attribute.
     */
    public final void unsetName() {
        attrs &= ~NAME;
    }

    /**
     * Unsets the NextExecutionTime attribute.
     */
    public final void unsetNextExecutionTime() {
        attrs &= ~NEXT_EXEC_TIME;
    }

    /**
     * Unsets the OriginalSubmitTime attribute.
     */
    public final void unsetOriginalSubmitTime() {
        attrs &= ~ORIG_SUBMIT_TIME;
    }

    /**
     * Unsets the PreviousScheduledStartTime attribute.
     */
    public final void unsetPreviousScheduledStartTime() {
        attrs &= ~PREV_SCHED_START_TIME;
    }

    /**
     * Unsets the PreviousStartTime attribute.
     */
    public final void unsetPreviousStartTime() {
        attrs &= ~PREV_START_TIME;
    }

    /**
     * Unsets the PreviousStopTime attribute.
     */
    public final void unsetPreviousStopTime() {
        attrs &= ~PREV_STOP_TIME;
    }

    /**
     * Unsets the Result attribute.
     */
    public final void unsetResult() {
        attrs &= ~RESULT;
    }

    /**
     * Unsets the State attribute.
     */
    public final void unsetState() {
        attrs &= ~STATE;
    }

    /**
     * Unsets the Task attribute.
     */
    public final void unsetTask() {
        attrs &= ~TASK;
    }

    /**
     * Unsets the TaskInformation attribute.
     */
    public final void unsetTaskInformation() {
        attrs &= ~TASK_INFO;
    }

    /**
     * Unsets the TransactionTimeout attribute.
     */
    public final void unsetTransactionTimeout() {
        attrs &= ~TX_TIMEOUT;
    }

    /**
     * Unsets the Trigger attribute.
     */
    public final void unsetTrigger() {
        attrs &= ~TRIGGER;
    }

    /**
     * Unsets the Version attribute.
     */
    public final void unsetVersion() {
        attrs &= ~VERSION;
    }
}