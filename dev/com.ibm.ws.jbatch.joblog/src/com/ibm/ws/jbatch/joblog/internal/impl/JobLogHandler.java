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
package com.ibm.ws.jbatch.joblog.internal.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.ibm.jbatch.container.instance.WorkUnitDescriptor;
import com.ibm.jbatch.container.ws.events.BatchEventsPublisher;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * The batch feature's very own j.u.l.Handler.
 *
 * This handler is registered with the root logger, so it gets called for every
 * log record issued by the runtime, and routes them to the joblog.
 *
 */
public class JobLogHandler extends Handler {

    private final ThreadLocal<Boolean> loggingInProgress = new ThreadLocal<Boolean>();

    /**
     * One JobLogFileHandler per thread. Handles logging for the job running
     * on that thread.
     */
    private final ThreadLocal<JobLogFileHandler> threadHandler = new ThreadLocal<JobLogFileHandler>();

    private final ThreadLocal<JobLogEntryDetail> threadDetail = new ThreadLocal<JobLogEntryDetail>();

    private int maxRecords = 0;

    private int maxTime = 0;

    private Boolean purgeOnPublish = null;

    private Boolean sendFinalNotification = false;

    /**
     * Keep track of all JobLogFileHandlers, so that we can flush() and close() them all.
     */
    private final Set<JobLogFileHandler> jobLogFileHandlers = Collections.synchronizedSet(new HashSet<JobLogFileHandler>());

    /**
     * For resolving joblog dir names.
     */
    private JobLogManagerImpl jobLogManagerImpl;

    /**
     * Keep track of the Loggers to which we've added this JobLogHandler, so
     * we can remove ourself from those Loggers at deactivation/config update.
     */
    private final List<Logger> registeredWithLoggers = new ArrayList<Logger>();

    /**
     * CTOR.
     */
    public JobLogHandler() {
        setFormatter(new JobLogFormatter());
    }

    /**
     * @param maxRecords
     * @return this
     */
    public JobLogHandler setMaxRecords(int maxRecords) {
        this.maxRecords = maxRecords;
        return this;
    }

    /**
     * @param maxTime
     * @return this
     */
    public JobLogHandler setMaxTime(int maxTimeFromConfig) {
        maxTime = maxTimeFromConfig;
        return this;
    }

    /**
     * Sets the boolean value of whether or not job logs should be purged after job log events are published
     *
     * Default is False
     *
     * @param purgeOnPublish
     * @return this
     */
    public JobLogHandler setPurgeOnPublish(Boolean purgeOnPublish) {
        //purgeOnPublish will only be null only if the job logging property is commented out
        if (purgeOnPublish == null) {
            this.purgeOnPublish = false;
        } else {
            this.purgeOnPublish = purgeOnPublish;
        }
        return this;

    }

    /**
     * @param sendFinalNotification (true or false)
     * @return this
     */
    public JobLogHandler setFinalNotification(Boolean sendFinalNotification) {
        this.sendFinalNotification = sendFinalNotification;
        return this;
    }

    /**
     * The j.u.l.Handler's callback method, called for each LogRecord issued by the runtime.
     *
     * Routes the LogRecord to the correct joblog file via the threadHandler ThreadLocal.
     */
    @Override
    @Trivial
    public void publish(LogRecord record) {
        if (loggingInProgress.get() == null) {
            loggingInProgress.set(false);
        }
        if (!isLoggable(record) || loggingInProgress.get()) {
            return;
        }

        // Prevent infinite loops when entry/exit trace is enabled
        loggingInProgress.set(true);

        try {

            JobLogFileHandler fileHandler = threadHandler.get();
            if (fileHandler != null) {
                addJobDetails(record);
                fileHandler.publish(record);
            }
        } finally {
            loggingInProgress.set(false);
        }
    }

    /**
     * Use the job details (name, instance ID, execution ID) to generate the directory
     * path for the job logs.
     *
     * @param ctx Context for the currently active job
     * @return File path pattern for the currently active job
     */
    private String getLogDirPath(WorkUnitDescriptor ctx) {
        String jobName = ctx.getTopLevelJobName();
        long instanceId = ctx.getTopLevelInstanceId();
        long executionId = ctx.getTopLevelExecutionId();

        String result = jobLogManagerImpl.getJobExecutionLogDirName(jobName, instanceId, executionId) + File.separator;

        switch (ctx.getWorkUnitType()) {
            case PARTITIONED_STEP:
                result = result.concat(ctx.getPartitionedStepName() + File.separator +
                                       ctx.getPartitionNumber() + File.separator);
                break;
            case SPLIT_FLOW:
                result = result.concat(ctx.getSplitName() + File.separator +
                                       ctx.getFlowName() + File.separator);
                break;
            case TOP_LEVEL_JOB:
            default:
                // nothing else to do for now
                break;
        }

        return result;
    }

    /**
     * Set the ThreadLocal execution context for the job currently active on this thread.
     *
     * @param ctx
     *
     * @throws BatchLogPartNotCreatedException
     */
    protected String setExecutionContext(WorkUnitDescriptor ctx) throws BatchLogPartNotCreatedException {
        loggingInProgress.set(false);
        String logDirPath = getLogDirPath(ctx);
        switch (ctx.getWorkUnitType()) {
            case PARTITIONED_STEP:
                threadDetail.set(new JobLogEntryDetail(ctx.getPartitionedStepName(), "partition" + ctx.getPartitionNumber()));
                break;
            case SPLIT_FLOW:
                threadDetail.set(new JobLogEntryDetail(ctx.getSplitName(), ctx.getFlowName()));
                break;
            case TOP_LEVEL_JOB:
            default:
                // nothing else to do for now
                break;
        }
        BatchEventsPublisher publisher = jobLogManagerImpl.getEventsPublisher();

        //Job log events can only be sent if a publisher exists. Only use the job logging property in this case.
        boolean purgeOnPublishHandler = false;
        if (publisher != null) {
            purgeOnPublishHandler = purgeOnPublish;
        }

        JobLogFileHandler handler = new JobLogFileHandler(logDirPath.concat("part.%d.log"), maxRecords, purgeOnPublishHandler, ctx, maxTime);
        jobLogFileHandlers.add(handler);
        threadHandler.set(handler);

        return logDirPath;
    }

    /**
     * Remove the ThreadLocal execution context when there is no longer a job on this thread.
     *
     * Also send the final job log if job logging is enabled.
     */
    protected void clearExecutionContext() {
        JobLogFileHandler handler = threadHandler.get();
        if (handler != null) {
            if (this.hasLoggers() || sendFinalNotification) {
                handler.handleFinalJobLogPart();
            }
            handler.close();
            jobLogFileHandlers.remove(handler);
        }

        threadHandler.set(null);
        threadDetail.set(null);
        loggingInProgress.set(false);
    }

    /**
     * Manual (non-DS) inject.
     */
    protected void setJobLogManagerImpl(JobLogManagerImpl jobLogManagerImpl) {
        this.jobLogManagerImpl = jobLogManagerImpl;
    }

    /**
     * Flush ALL handlers for all threads.
     */
    @Override
    public void flush() {

        for (JobLogFileHandler fileHandler : jobLogFileHandlers) {
            fileHandler.flush();
        }
    }

    /**
     * Close ALL handlers for all threads.
     *
     * This method is called when JobLogManagerImpl is deactivating, which means this
     * instance of JobLogHandler will soon be dead (it's a member of JobLogManagerImpl,
     * which is going away).
     *
     *
     */
    @Override
    public void close() {

        for (Iterator<JobLogFileHandler> iter = jobLogFileHandlers.iterator(); iter.hasNext();) {
            //iter.next().close();
            // This will publish the final job log if required and then close the stream.
            iter.next().handleFinalJobLogPart();
            iter.remove();
        }
    }

    /**
     * Add JobLogEntryDetail to the record, if there is any.
     */
    @Trivial
    private void addJobDetails(LogRecord record) {
        if (threadDetail.get() != null) {
            addParams(record, threadDetail.get());
        }
    }

    /**
     * Add the specified objects as parameters on a LogRecord.
     *
     * @param record
     * @param additionalParams
     */
    @Trivial
    private void addParams(LogRecord record, Object... additionalParams) {

        Object[] curParams = record.getParameters();
        if (curParams != null) {
            Object[] newParams = new Object[curParams.length + additionalParams.length];
            int i = 0;
            for (Object o : curParams) {
                newParams[i] = o;
                i++;
            }
            for (Object o : additionalParams) {
                newParams[i] = o;
                i++;
            }
            record.setParameters(newParams);
        } else {
            record.setParameters(additionalParams);
        }
    }

    /**
     * Add this handler to the given Logger, if we haven't already.
     */
    public synchronized void addToLogger(Logger logger) {
        if (!registeredWithLoggers.contains(logger)) {
            logger.addHandler(this);
            registeredWithLoggers.add(logger);
        }
    }

    /**
     * Remove ourself from all the Loggers we previously attached to.
     *
     * For each removed Logger, this method calls:
     *
     * Logger.setUseParentHandlers(true)
     * Logger.setLevel(null)
     *
     * ...to effectively reset the Logger state to its defaults.
     */
    public synchronized void removeFromLoggers() {
        for (Logger logger : registeredWithLoggers) {
            logger.removeHandler(this);
            logger.setUseParentHandlers(true);
        }

        registeredWithLoggers.clear();
    }

    /**
     * @return true if this handler has been registered with any loggers.
     */
    public synchronized boolean hasLoggers() {
        return !registeredWithLoggers.isEmpty();
    }

}
