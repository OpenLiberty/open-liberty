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
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializer;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Task information that gets persisted to the task store in binary form and so is not queryable.
 */
public class TaskInfo implements Serializable {
    private static final long serialVersionUID = 114469733965157007L;
    private static final TraceComponent tc = Tr.register(TaskInfo.class);

    /**
     * Names of serializable fields.
     * A single character is used for each to reduce the space required in the task store.
     */
    private static final String
                    INITIAL_DELAY = "D",
                    INTERVAL = "I",
                    IS_FIXED_RATE = "F",
                    NONSER_TASK_CLASS_NAME = "T",
                    NONSER_TRIGGER_CLASS_NAME = "G",
                    SUBMITTED_AS_CALLABLE = "c",
                    THREAD_CONTEXT_DESCRIPTOR = "C";

    /**
     * Fields to serialize
     */
    private static final ObjectStreamField[] serialPersistentFields =
                    new ObjectStreamField[] {
                                             new ObjectStreamField(INITIAL_DELAY, long.class),
                                             new ObjectStreamField(INTERVAL, long.class),
                                             new ObjectStreamField(IS_FIXED_RATE, boolean.class),
                                             new ObjectStreamField(NONSER_TASK_CLASS_NAME, String.class),
                                             new ObjectStreamField(NONSER_TRIGGER_CLASS_NAME, String.class),
                                             new ObjectStreamField(SUBMITTED_AS_CALLABLE, boolean.class),
                                             new ObjectStreamField(THREAD_CONTEXT_DESCRIPTOR, byte[].class)
                    };

    /**
     * Types that are valid for task results must meet the following criteria
     * - must be serializable
     * - must be constructable from String, such that instance.equals(new Instance(instance.toString())) or be java.lang.Character
     * - must be defined in a java.* package such that no special classloading is necessary
     * - must not be able to contain other types that do not meet these criteria
     */
    // TODO allow for arrays of these types?
    private static final Set<Class<?>> RESULT_TYPES = new HashSet<Class<?>>();
    static {
        RESULT_TYPES.add(BigDecimal.class);
        RESULT_TYPES.add(BigInteger.class);
        RESULT_TYPES.add(Boolean.class);
        RESULT_TYPES.add(Byte.class);
        RESULT_TYPES.add(Character.class);
        RESULT_TYPES.add(Double.class);
        RESULT_TYPES.add(Float.class);
        RESULT_TYPES.add(Integer.class);
        RESULT_TYPES.add(Long.class);
        RESULT_TYPES.add(Short.class);
        RESULT_TYPES.add(String.class);
        RESULT_TYPES.add(StringBuffer.class);
        RESULT_TYPES.add(StringBuilder.class);
    }

    /**
     * Initial delay (in milliseconds) before first execution for one-shot, fixed-rate, or fixed-delay tasks. -1 if a trigger is used.
     */
    private transient long initialDelay = -1;

    /**
     * Interval in milliseconds for fixed rate or fixed delay tasks. Otherwise -1.
     */
    private transient long interval = -1;

    /**
     * Indicates whether or not the task was submitted as a Callable.
     * This allows us to distinguish whether to invoke Callable.call or Runnable.run if a task implements both.
     */
    private transient boolean submittedAsCallable;

    /**
     * Indicates whether or not this is a fixed-rate task.
     */
    private transient boolean isFixedRate;

    /**
     * Class name of non-serializable, stateless task.
     */
    private transient String nonserTaskClassName;

    /**
     * Class name of non-serializable, stateless trigger.
     */
    private transient String nonserTriggerClassName;

    /**
     * Thread context that was captured at the point when the task was submitted.
     */
    private transient ThreadContextDescriptor threadContext;

    /**
     * Thread context serialized as bytes. This is only present after deserialization, prior to invocation of postReadObject.
     */
    private transient byte[] threadContextBytes;

    /**
     * Construct persistent task information.
     * 
     * @param executorName jndiName or config.displayId
     * @param submittedAsCallable indicates whether or not the task is submitted as a Callable.
     */
    TaskInfo(boolean submittedAsCallable) {
        this.submittedAsCallable = submittedAsCallable;
    }

    /**
     * Returns the thread context that was captured at the point when the task was submitted.
     * 
     * @param execProps execution properties for the persistent task.
     * @return the thread context that was captured at the point when the task was submitted.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public ThreadContextDescriptor deserializeThreadContext(Map<String, String> execProps) throws IOException, ClassNotFoundException {
        return threadContextBytes == null ? null : ThreadContextDeserializer.deserialize(threadContextBytes, execProps);
    }

    /**
     * Returns the class name of a non-serializable, stateless task. Null if the task is serializable.
     * 
     * @return the class name of a non-serializable, stateless task. Null if the task is serializable.
     */
    @Trivial
    public String getClassNameForNonSerializableTask() {
        return nonserTaskClassName;
    }

    /**
     * Returns the class name of a non-serializable, stateless trigger. Null if the trigger is serializable.
     * 
     * @return the class name of a non-serializable, stateless trigger. Null if the trigger is serializable.
     */
    @Trivial
    public String getClassNameForNonSerializableTrigger() {
        return nonserTriggerClassName;
    }

    /**
     * Returns the delay (in milliseconds) before first execution for one-shot, fixed-rate, or fixed-delay tasks.
     * 
     * @return the initial delay. If a trigger is used, then -1 is returned.
     */
    @Trivial
    public long getInitialDelay() {
        return initialDelay;
    }

    /**
     * Returns the interval in milliseconds for fixed rate or fixed delay tasks. Otherwise -1.
     * 
     * @return the interval in milliseconds for fixed rate or fixed delay tasks. Otherwise -1.
     */
    @Trivial
    public long getInterval() {
        return interval;
    }

    /**
     * Initialization for a non-serializable, stateless task.
     * 
     * @param className implementation class name of the task.
     */
    @Trivial
    void initForNonSerializableTask(String className) {
        this.nonserTaskClassName = className;
    }

    /**
     * Initialization for a non-serializable, stateless trigger.
     * 
     * @param className implementation class name of the trigger.
     */
    @Trivial
    void initForNonSerializableTrigger(String className) {
        this.nonserTriggerClassName = className;
    }

    /**
     * Initialization for a one-shot task.
     * 
     * @param initialDelay milliseconds before the task executes
     */
    @Trivial
    void initForOneShotTask(long initialDelay) {
        this.initialDelay = initialDelay;
    }

    /**
     * Initialization for a fixed-rate or fixed-delay task.
     * 
     * @param isFixedRate indicates if this is a fixed-rate or fixed-delay task
     * @param initialDelay milliseconds before first execution
     * @param interval milliseconds between task executions
     */
    @Trivial
    void initForRepeatingTask(boolean isFixedRate, long initialDelay, long interval) {
        this.initialDelay = initialDelay;
        this.interval = interval;
        this.isFixedRate = isFixedRate;
    }

    /**
     * Capture thread context.
     * 
     * @param contextSvc thread context service.
     * @param execProps execution properties for the persistent task.
     */
    @SuppressWarnings("unchecked")
    void initThreadContext(WSContextService contextSvc, Map<String, String> execProps) {
        threadContext = contextSvc.captureThreadContext(execProps);
    }

    /**
     * Indicates whether or not this is a fixed-rate task scheduled via the scheduleAtFixedRate method.
     * 
     * @return true if a fixed-rate task, otherwise false.
     */
    @Trivial
    public boolean isFixedRate() {
        return isFixedRate;
    }

    /**
     * Indicates whether or not this task was submitted as a Callable.
     * 
     * @return true if the task was submitted as a Callable, otherwise false.
     */
    @Trivial
    public boolean isSubmittedAsCallable() {
        return submittedAsCallable;
    }

    /**
     * Deserialize task information.
     * 
     * @param in The stream from which this object is read.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Trivial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "readObject");

        GetField fields = in.readFields();
        initialDelay = fields.get(INITIAL_DELAY, -1l);
        interval = fields.get(INTERVAL, -1l);
        isFixedRate = fields.get(IS_FIXED_RATE, false);
        nonserTaskClassName = (String) fields.get(NONSER_TASK_CLASS_NAME, null);
        nonserTriggerClassName = (String) fields.get(NONSER_TRIGGER_CLASS_NAME, null);
        submittedAsCallable = fields.get(SUBMITTED_AS_CALLABLE, false);
        threadContextBytes = (byte[]) fields.get(THREAD_CONTEXT_DESCRIPTOR, null);

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "readObject", new Object[] {
                                                          nonserTaskClassName,
                                                          nonserTriggerClassName,
                                                          "initial delay " + initialDelay,
                                                          "interval " + interval,
                                                          isFixedRate ? "fixed rate" : false,
                                                          submittedAsCallable ? "callable" : "runnable",
                                                          threadContextBytes == null ? null : threadContextBytes.length
            });
    }

    /**
     * Serialize task information.
     * 
     * @param out The stream to which this object is serialized.
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())

            Tr.entry(this, tc, "writeObject", new Object[] {
                                                            nonserTaskClassName,
                                                            nonserTriggerClassName,
                                                            "initial delay " + initialDelay,
                                                            "interval " + interval,
                                                            isFixedRate ? "fixed rate" : false,
                                                            submittedAsCallable ? "callable" : "runnable",
                                                            threadContext
            });

        if (threadContextBytes == null)
            threadContextBytes = threadContext == null ? null : threadContext.serialize();

        PutField fields = out.putFields();
        fields.put(INITIAL_DELAY, initialDelay);
        fields.put(INTERVAL, interval);
        fields.put(IS_FIXED_RATE, isFixedRate);
        fields.put(NONSER_TASK_CLASS_NAME, nonserTaskClassName);
        fields.put(NONSER_TRIGGER_CLASS_NAME, nonserTriggerClassName);
        fields.put(SUBMITTED_AS_CALLABLE, submittedAsCallable);
        fields.put(THREAD_CONTEXT_DESCRIPTOR, threadContextBytes);
        out.writeFields();

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "writeObject");
    }
}
