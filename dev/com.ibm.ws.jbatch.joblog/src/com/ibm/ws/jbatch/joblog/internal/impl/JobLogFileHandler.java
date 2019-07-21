/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.jbatch.container.instance.WorkUnitDescriptor;
import com.ibm.jbatch.container.ws.events.BatchEventsPublisher;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jbatch.joblog.JobLogConstants;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;

/**
 * Handler which maintains a job log for a particular thread. This could be a top-level job,
 * a partition, or a split flow. This handler manages writing to the file system and rotating
 * to a new log file part when required. This handler also send job log events if set up and
 * takes care of purging job logs if configured to do so.
 */
public class JobLogFileHandler extends StreamHandler {

    private final static String CLASSNAME = JobLogFileHandler.class.getName();
    private final Logger logger = Logger.getLogger(CLASSNAME,
                                                   JobLogConstants.BATCH_JOBLOG_MSG_BUNDLE);

    private int filePart;
    private int recordsWritten;
    private final int maxRecordsPerFile;
    private final int maxTimePerFile;
    private final String fileNamePattern;
    private File logFile;
    private Path previousLogPath;
    private boolean allowRotate = true;
    private final boolean purgeOnPublish;
    private final WorkUnitDescriptor execContext;
    private final String correlationId;
    private boolean timerCancelled = false;

    /**
     * Job log timer used to rotate and publish log files after the
     * configured interval in the server.xml
     */
    protected Timer _timer;

    /**
     *
     * @param logFileNamePattern
     * @param maxFileLines
     * @param jobLogEventsOnly
     * @param eventsPublisher
     * @param execution context
     *
     * @throws BatchLogPartNotCreatedException
     */
    public JobLogFileHandler(String logFileNamePattern, int maxFileLines, Boolean purgeOnPublishHandler, WorkUnitDescriptor ctx,
                             int maxTime) throws BatchLogPartNotCreatedException {
        super();
        setLevel(Level.FINEST); // We'll filter at the JobLogHandler level, everything that makes it here should be logged
        setFormatter(new JobLogFormatter());
        fileNamePattern = logFileNamePattern;
        maxRecordsPerFile = maxFileLines;
        maxTimePerFile = maxTime;
        purgeOnPublish = purgeOnPublishHandler;
        execContext = ctx;
        filePart = 0;
        correlationId = execContext == null ? null : execContext.getCorrelationId();

        rotate();
    }

    /**
     * Open the next log file part (setOutputStream handles closing of the current file).
     *
     * @throws BatchLogPartNotCreatedException
     */
    private synchronized void rotate() throws BatchLogPartNotCreatedException {
        filePart++;
        recordsWritten = 0;

        //set the path of the log file before it is closed (first time through no logFile exists)
        if (filePart > 1) {
            previousLogPath = Paths.get(logFile.getPath());
        }

	Object token = null;

        logFile = new File(String.format(fileNamePattern, filePart));

        try {
            token = ThreadIdentityManager.runAsServer();
            File logDir = new File(logFile.getParent());
            boolean dirExists = logDir.exists();
            if (!dirExists) {
                dirExists = logDir.mkdirs();
                // Retry once on the chance that a simultaneous purge operation caused mkdirs() to fail
                if (!dirExists) {
                    dirExists = logDir.mkdirs();
                }
            }

            if (!dirExists) {
                throw new BatchLogPartNotCreatedException("Batch Log Directory Not Created:" + logDir.toString());
            } else {
                setOutputStream(new FileOutputStream(logFile));
                // Got here because maximum records reached not time limit
                if (_timer != null) {
                    _timer.cancel();
                    _timer = null;
                }

                if (maxTimePerFile > 0) {
                    _timer = new Timer("JobLogTimer");
                    _timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            synchronized (JobLogFileHandler.this) {
                                if (timerCancelled)
                                    return;
                                if (recordsWritten > 0 && allowRotate == true) {
                                    _timer.cancel();
                                    _timer = null;
                                    try {
                                        rotate();
                                    } catch (BatchLogPartNotCreatedException e) {
                                        //Disable rotate for thread if part.#.log create fails.
                                        allowRotate = false;
                                        logger.log(Level.WARNING, "job.logging.create.next", new Object[] { ((e.getCause() != null) ? e.getCause() : e) });
                                    }
                                }
                            }
                        }
                    }, maxTimePerFile * 1000, maxTimePerFile * 1000);
                }

                //first time through file part will be one and will not have any log content when first created
                if (filePart > 1) {
                    handleNewJobLogPart();
                }
            }

        } catch (SecurityException e) { // Catch for FFDC injection
            throw new BatchLogPartNotCreatedException(e);
        } catch (FileNotFoundException e) { // Catch for FFDC injection
            throw new BatchLogPartNotCreatedException(e);
        } finally {
	    if (token != null)
		ThreadIdentityManager.reset(token);
	}
    }

    /**
     * @see java.util.logging.StreamHandler#publish(java.util.logging.LogRecord)
     *
     */
    @Trivial
    @Override
    public synchronized void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        super.publish(record);
        flush();
        recordsWritten++;

        if (recordsWritten >= maxRecordsPerFile && allowRotate == true) {
            try {
                rotate();
            } catch (BatchLogPartNotCreatedException e) {
                //Disable rotate for thread if part.#.log create fails.
                allowRotate = false;
                logger.log(Level.WARNING, "job.logging.create.next", new Object[] { ((e.getCause() != null) ? e.getCause() : e) });
            }
        }
    }

    /**
     * Handle a new job log part being created part by:
     * -retrieving the job log content
     * -sending the job log event
     *
     * Commented out until purgeOnPublish is enabled:
     * -deleting the job log part file
     */
    private void handleNewJobLogPart() {
        //Ensure a publisher has been injected, otherwise do not send job log event to be published
        if (getBatchEventsPublisher() != null) {
            String logContent = null;
            try {
                //get the content of the job log part
                logContent = new String(Files.readAllBytes(previousLogPath), StandardCharsets.UTF_8);
            } catch (IOException e) {
                logger.log(Level.WARNING, "job.logging.read.log", new Object[] { ((e.getCause() != null) ? e.getCause() : e) });
            } catch (OutOfMemoryError e) {
                logger.log(Level.WARNING, "job.logging.read.log", new Object[] { ((e.getCause() != null) ? e.getCause() : e) });
            } catch (SecurityException e) {
                logger.log(Level.WARNING, "job.logging.read.log", new Object[] { ((e.getCause() != null) ? e.getCause() : e) });
            }

            //If an exception was caught reading the log file then continue on without sending an event
            if (logContent != null) {
                //send the previous part number along with the content in the log
                sendJobLogEvent(filePart - 1, logContent, false);
            }
        }

// If purgeOnPublish is enabled then uncomment this to allow for the job log to be purged
//        if (purgeOnPublish) {
//            deleteFileOrDirectory(new File(previousLogPath.toString()));
//        }
    }

    /**
     * Note that it is possible over the life of this object instance (the life of a batch execution thread), that a config
     * change will cause the event publisher to activate/deactivate, or to be routed to a different location (topic).
     *
     * While this would mess up use cases like the batchManagerZos client doing its wait for the last job log part, we don't
     * worry about that here. It is a responsibility of the user not to change their config in this way with inflight jobs.
     *
     * But given that the rest of the feature has only a dynamic reference to the publisher we want to be dynamic as well, and
     * get a reference each time, rather than caching. That is, we sort of behave as if we have a optional, greedy, dynamic reference for this object
     * (which is not itself a DS component).
     *
     * @return Get highest ranked service, as do the DS components with their greedy references.
     */
    private BatchEventsPublisher getBatchEventsPublisher() {
        BundleContext bundleContext = FrameworkUtil.getBundle(BatchEventsPublisher.class).getBundleContext();
        ServiceReference<BatchEventsPublisher> eventsPublisherSR = bundleContext.getServiceReference(BatchEventsPublisher.class);
        return (eventsPublisherSR != null) ? bundleContext.getService(eventsPublisherSR) : null;
    }

    /**
     * Handle the final job log part by:
     * -closing the log part
     * -retrieving the job log content
     * -sending the job log event
     *
     * Commented out until purgeOnPublish is enabled:
     * -deleting the final job log part file
     * -recursively deleting the parent directory until it deletes the "joblogs" directory
     *
     * Note: This only deletes the directories if they are empty. So if previous jobs ran with
     * job logs then the recursive delete will stop at either the date or "joblogs" level.
     */

    public synchronized void handleFinalJobLogPart() {

        //Cancel the timer if there is one.
        if (_timer != null) {
            _timer.cancel();
            _timer = null;
            timerCancelled = true;
        }

        //Ensure a publisher has been injected, otherwise do not send job log event to be published
        if (getBatchEventsPublisher() != null) {
	    Object token = null;            

	    try {
                //get the content of the current job log part and send it to be published as a job log event
                token = ThreadIdentityManager.runAsServer();
		String logContent = new String(Files.readAllBytes(Paths.get(logFile.getPath())), StandardCharsets.UTF_8);
                sendJobLogEvent(filePart, logContent, true);

            } catch (IOException e) {
                logger.log(Level.WARNING, "job.logging.read.log", new Object[] { ((e.getCause() != null) ? e.getCause() : e) });
            } catch (OutOfMemoryError e) {
                logger.log(Level.WARNING, "job.logging.read.log", new Object[] { ((e.getCause() != null) ? e.getCause() : e) });
            } catch (SecurityException e) {
                logger.log(Level.WARNING, "job.logging.read.log", new Object[] { ((e.getCause() != null) ? e.getCause() : e) });
            } finally {
	       if (token != null)
	              ThreadIdentityManager.reset(token);
	    }
        }

        //close the log since setOutputStream is never called for the final part
        close();

// If purgeOnPublish is enabled then uncomment this to allow for the final job log along
// with the "joblogs" directory structure if the directory is empty to be deleted.
//        File logDir = new File(logFile.getParent());
//        if (purgeOnPublish) {
//            //Delete the final job log and its directory since it is the final part
//            boolean isDeleted = deleteFileOrDirectory(logFile);
//            if (isDeleted) {
//                while (logDir.getAbsolutePath().contains("joblogs")) {
//                    if (deleteFileOrDirectory(logDir)) {
//                        logDir = new File(logDir.getParent());
//                    } else {
//                        break;
//                    }
//                }
//            }
//        }
    }

    /**
     * Send the job log part to the events publisher if one exists.
     *
     * Made the method trivial so the entire log content is not printed in the trace
     *
     * @param partNum
     * @param logContent
     */
    @Trivial
    protected void sendJobLogEvent(int partNum, String logContent, boolean finalLog) {

        BatchEventsPublisher batchEventsPublisher = getBatchEventsPublisher();
        if (batchEventsPublisher == null) {
            return;
        }
        String partitionStep = null, splitName = null, flowName = null;
        Integer partitionNum = null;
        switch (execContext.getWorkUnitType()) {
            case PARTITIONED_STEP:
                partitionStep = execContext.getPartitionedStepName();
                partitionNum = execContext.getPartitionNumber();
                break;
            case SPLIT_FLOW:
                splitName = execContext.getSplitName();
                flowName = execContext.getFlowName();
                break;
            default:
                // nothing else needed for now
                break;
        }
        batchEventsPublisher.publishJobLogEvent(execContext.getTopLevelInstanceId(),
                                                execContext.getTopLevelExecutionId(),
                                                execContext.getTopLevelJobName(),
                                                partitionStep,
                                                partitionNum,
                                                splitName,
                                                flowName,
                                                partNum,
                                                finalLog,
                                                logContent,
                                                correlationId);
    }
    /**
     * Delete the file or directory provided by the path variable if it exists
     *
     *
     * Commented out until the purgeOnPublish property is enabled
     *
     * @param path
     * @return
     */
//    private boolean deleteFileOrDirectory(File file) {
//
//        //deleteResult will either be 0 (failed to deleted), 1 (deleted), or 2 (directory contains files)
//        Integer deleteResult = AccessController.doPrivileged(new FilePrivledge(file));
//
//        //If the result is 2 that means it is a directory and contains files it so don't print out a message
//        //This could occur if the user had job logs prior to enabling the jobLogEventsOnly property.
//        if (deleteResult == 2) {
//            return false;
//        } else if (deleteResult == 1) {
//            return true;
//        } else {
//            logger.log(Level.WARNING, "job.logging.delete.log", new Object[] { file.getAbsolutePath() });
//            return false;
//        }
//    }
//
//    class FilePrivledge implements PrivilegedAction<Integer> {
//
//        File file;
//
//        public FilePrivledge(File file) {
//            this.file = file;
//        }
//
//        @Override
//        public Integer run() {
//            if (file.isDirectory() && file.list().length > 0) {
//                return 2;
//            } else {
//                boolean isDeleted = file.delete();
//                if (isDeleted) {
//                    return 1;
//                } else {
//                    return 0;
//                }
//            }
//        }
//    }

}
