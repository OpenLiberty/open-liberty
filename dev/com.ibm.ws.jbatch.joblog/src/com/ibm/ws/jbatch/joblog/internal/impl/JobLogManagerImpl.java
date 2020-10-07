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
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.jbatch.container.instance.WorkUnitDescriptor;
import com.ibm.jbatch.container.ws.BatchJobNotLocalException;
import com.ibm.jbatch.container.ws.BatchLocationService;
import com.ibm.jbatch.container.ws.JoblogUtil;
import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.WSJobRepository;
import com.ibm.jbatch.container.ws.WSRemotablePartitionExecution;
import com.ibm.jbatch.container.ws.events.BatchEventsPublisher;
import com.ibm.jbatch.spi.services.IBatchConfig;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.ws.jbatch.joblog.JobExecutionLog;
import com.ibm.ws.jbatch.joblog.JobExecutionLog.LogLocalState;
import com.ibm.ws.jbatch.joblog.JobInstanceLog;
import com.ibm.ws.jbatch.joblog.JobLogConstants;
import com.ibm.ws.jbatch.joblog.services.IJobLogManagerService;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

/**
 * Batch joblog manager.
 */
@Component(configurationPid = "com.ibm.ws.jbatch.joblog",
           configurationPolicy = ConfigurationPolicy.REQUIRE)
public class JobLogManagerImpl implements IJobLogManagerService {

    private final static String CLASSNAME = JobLogManagerImpl.class.getName();
    private final Logger logger = Logger.getLogger(CLASSNAME,
                                                   JobLogConstants.BATCH_JOBLOG_MSG_BUNDLE);

    private static final int MAX_SUBDIRECTORIES = 32000;
    private static final String ENV_LOG_DIR = "LOG_DIR";
    private static final String SERVER_OUTPUT_DIR = "server.output.dir";

    /**
     * For locating the server's logs dir
     */
    private WsLocationAdmin locationService;

    /**
     * For reading from the db.
     */
    private WSJobRepository jobRepository;

    /**
     * For publishing job log events
     */
    private BatchEventsPublisher eventsPublisher;

    /**
     * Our special java.util.logging.Handler. This guy writes to the job logs.
     * All Loggers that this guy is added to, will log to the job log (in addition
     * to wherever else they're configured to log to, e.g. the server log).
     *
     * This guy is registered with all loggers named in the "jobLoggers" config attribute.
     * It's registered with the root logger if includeServerLogging=true, so that
     * all server logging is routed to the job log as well, in addition to the server log.
     */
    private final JobLogHandler joblogHandler = new JobLogHandler();

    /**
     * For checking whether jobexecutions ran locally.
     */
    private BatchLocationService batchLocationService;

    /**
     * DS activate
     */
    @Activate
    protected synchronized void activate(ComponentContext componentContext, Map<String, Object> config) {
        configureJobLogHandler(config);
    }

    /**
     * DS deactivate
     */
    @Deactivate
    protected void deactivate() {
        JoblogUtil.setIncludeServerLogging(true);
        joblogHandler.removeFromLoggers();
        joblogHandler.close();
    }

    /**
     * DS modify. For dynamic config updates.
     */
    @Modified
    protected void modified(Map<String, Object> config) {
        configureJobLogHandler(config);
    }

    /** {@inheritDoc} */
    @Override
    public void init(IBatchConfig batchConfig) {
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() {
    }

    /**
     * Called by DS to inject location service ref
     */
    @Reference
    protected synchronized void setLocationService(WsLocationAdmin locSvc) {
        locationService = locSvc;
    }

    /**
     * DS inject
     */
    @Reference
    protected void setWSJobRepository(WSJobRepository ref) {
        jobRepository = ref;
    }

    /**
     * DS injection
     */
    @Reference
    protected void setBatchLocationService(BatchLocationService batchLocationService) {
        this.batchLocationService = batchLocationService;
    }

    /**
     * DS injection
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setEventsPublisher(BatchEventsPublisher publisher) {
        this.eventsPublisher = publisher;
    }

    protected void unsetEventsPublisher(BatchEventsPublisher publisher) {
        if (this.eventsPublisher == publisher)
            this.eventsPublisher = null;
    }

    protected BatchEventsPublisher getEventsPublisher() {
        return this.eventsPublisher;
    }

    /**
     * @return true if job logging is currently enabled; false otherwise.
     */
    private synchronized boolean isJobLoggingEnabled() {
        return (joblogHandler.hasLoggers());
    }

    /**
     * {@inheritDoc}
     *
     * Only set the execution context if job logging is enabled, because
     * setting the execution context will cause the first joblog file to be created.
     *
     */
    @Override
    public void workUnitStarted(WorkUnitDescriptor ctx) {
        if (isJobLoggingEnabled()) {
            try {
                String logDirPath = joblogHandler.setExecutionContext(ctx);
                ctx.updateExecutionJobLogDir(logDirPath);
            } catch (BatchLogPartNotCreatedException e) {
                //No initial log handler added to this job.
                logger.log(Level.SEVERE, "job.logging.create.new",
                           new Object[] { ctx.getTopLevelJobName(),
                                          ctx.getTopLevelInstanceId(),
                                          ctx.getTopLevelExecutionId(),
                                          e.toString() });

            }

        }
    }

    /**
     * {@inheritDoc}
     *
     * Clear the execution context regardless of whether job logging is enabled.
     * This ensures that the context is cleared in the event that job logging was
     * initially enabled when the job started but was subsequently disabled.
     */
    @Override
    public void workUnitEnded(WorkUnitDescriptor ctx) {
        joblogHandler.clearExecutionContext();
    }

    /**
     * {@inheritDoc}
     *
     * Retrieve the JobExecutionLogs for all executions associated with the given instance.
     *
     * @throws BatchJobNotLocalException
     */
    @Override
    public JobInstanceLog getJobInstanceLog(long jobInstanceId) throws BatchJobNotLocalException {

        ArrayList<JobExecutionLog> execLogs = new ArrayList<JobExecutionLog>();
        HashSet<File> instanceDirs = new HashSet<File>();

        JobInstance jobInstance = jobRepository.getJobInstance(jobInstanceId);

        for (JobExecution jobExecution : jobRepository.getJobExecutionsFromInstance(jobInstanceId)) {
            // Find any remote partitions, they'll be part of the exec log
            List<WSRemotablePartitionExecution> remotePartitions = jobRepository.getRemotablePartitionsForJobExecution(jobExecution.getExecutionId());
            JobExecutionLog execLog = getJobExecutionLog(jobInstance, jobExecution.getExecutionId(), remotePartitions);
            execLogs.add(execLog);
            File execLogRoot = execLog.getExecLogRootDir();
            if (execLogRoot != null) {
                instanceDirs.add(execLogRoot.getParentFile());
            }
        }

        JobInstanceLog retMe = new JobInstanceLog(jobInstance, new ArrayList<File>(instanceDirs));

        for (JobExecutionLog log : execLogs) {
            retMe.addJobExecutionLog(log);
        }

        return retMe;
    }

    /**
     * Gets the local Job Instance Log file list.
     *
     * @param jobInstanceId
     */
    @Override
    public JobInstanceLog getLocalJobInstanceLog(long jobInstanceId) {

        ArrayList<JobExecutionLog> execLogs = new ArrayList<JobExecutionLog>();
        HashSet<File> instanceDirs = new HashSet<File>();

        JobInstance jobInstance = jobRepository.getJobInstance(jobInstanceId);

        for (JobExecution jobExecution : jobRepository.getJobExecutionsFromInstance(jobInstanceId)) {

            JobExecutionLog execLog = getJobExecutionLog(jobExecution.getExecutionId());

            // If the execution OR a remote partition ran locally, include it
            if (execLog.getLocalState() != LogLocalState.NOT_LOCAL) {
                execLogs.add(execLog);
                if (execLog.getExecLogRootDir() != null) {
                    instanceDirs.add(execLog.getExecLogRootDir().getParentFile());
                }
            }

        }

        JobInstanceLog retMe = new JobInstanceLog(jobInstance, new ArrayList<File>(instanceDirs));

        for (JobExecutionLog log : execLogs) {
            retMe.addJobExecutionLog(log);
        }

        return retMe;

    }

    /**
     * Gets Job Instance Log file list, including non-local executions.
     *
     * @param jobInstanceId
     */
    @Override
    public JobInstanceLog getJobInstanceLogAllExecutions(long jobInstanceId) {

        ArrayList<JobExecutionLog> execLogs = new ArrayList<JobExecutionLog>();
        HashSet<File> instanceDirs = new HashSet<File>();

        JobInstance jobInstance = jobRepository.getJobInstance(jobInstanceId);

        for (JobExecution jobExecution : jobRepository.getJobExecutionsFromInstance(jobInstanceId)) {
            JobExecutionLog execLog = getJobExecutionLog(jobExecution.getExecutionId());
            execLogs.add(execLog);
            if (execLog.getLocalState() == LogLocalState.EXECUTION_LOCAL) {
                instanceDirs.add(execLog.getExecLogRootDir().getParentFile());
            }
        }

        JobInstanceLog retMe = new JobInstanceLog(jobInstance, new ArrayList<File>(instanceDirs));

        for (JobExecutionLog log : execLogs) {
            retMe.addJobExecutionLog(log);
        }

        return retMe;

    }

    /**
     * {@inheritDoc}
     *
     * Scan the filesystem for all job log parts associated with the given jobExecutionId.
     *
     * @throws BatchJobNotLocalException
     */
    @Override
    public JobExecutionLog getJobExecutionLog(long jobExecutionId) {

        JobInstance jobInstance = jobRepository.getJobInstanceFromExecution(jobExecutionId);

        List<WSRemotablePartitionExecution> remotePartitions = jobRepository.getRemotablePartitionsForJobExecution(jobExecutionId);

        return getJobExecutionLog(jobInstance, jobExecutionId, remotePartitions);
    }

    /**
     * @return JobExecutionLog for the given JobInstance and execution ID.
     * @throws BatchJobNotLocalException
     */
    private JobExecutionLog getJobExecutionLog(JobInstance jobInstance, long jobExecutionId,
                                               List<WSRemotablePartitionExecution> remotePartitions) {

        WSJobExecution jobExecution = (WSJobExecution) BatchRuntime.getJobOperator().getJobExecution(jobExecutionId);
        LogLocalState localState = LogLocalState.NOT_LOCAL;
        String jobLogDirName = null;
        File jobLogDir = null;
        List<File> jobLogFiles = new ArrayList<File>();

        // First, check if the execution itself ran locally
        if (batchLocationService.isLocalJobExecution(jobExecution)) {
            localState = LogLocalState.EXECUTION_LOCAL;
            jobLogDirName = jobExecution.getLogpath();
            jobLogDir = StringUtils.isEmpty(jobLogDirName) ? getServerOutputDir(getJobExecutionLogDirName(jobInstance, jobExecutionId)) : new File(jobLogDirName);
            jobLogFiles = StringUtils.isEmpty(jobLogDirName) ? Collections.EMPTY_LIST : FileUtils.findFiles(new File(jobLogDirName), JobLogFileFilter);

            // If not, check if the execution has any remote partitions that ran locally instead
        } else if (remotePartitions != null) {
            for (WSRemotablePartitionExecution partition : remotePartitions) {
                if (batchLocationService.isLocalRemotablePartition(partition)) {
                    jobLogDirName = partition.getLogpath();
                    jobLogDir = StringUtils.isEmpty(jobLogDirName) ? getServerOutputDir(getJobExecutionLogDirName(jobInstance,
                                                                                                                  jobExecutionId)) : new File(jobLogDirName).getParentFile().getParentFile(); // Go two steps back to exclude the step name/partition num directories
                    jobLogFiles.addAll(FileUtils.findFiles(new File(jobLogDirName), JobLogFileFilter));
                    localState = LogLocalState.PARTITION_LOCAL;
                }
            }
        }

        return new JobExecutionLog(jobExecution, jobLogFiles, jobLogDir, localState, remotePartitions);

    }

    /**
     * @return the given resource
     */
    protected File getServerOutputDir(String dirname) {
        return getServerOutputFile(dirname + File.separator); // Append sep for dirs or else locationService gets cranky
    }

    /**
     * @return the given resource
     */
    protected File getServerOutputFile(String filename) {
        return locationService.resolveResource(filename).asFile();
    }

    /**
     * @return "LOG_DIR/joblogs", i.e. the joblogs root dir name, appended to the LOG_DIR system property
     */
    protected String getAbsoluteJobLogsRootDirName() {
        String logLocation = TrConfigurator.getLogLocation();
        if (logLocation == null) {
            logLocation = System.getenv(ENV_LOG_DIR);
            if (logLocation == null) {
                logLocation = System.getProperty(SERVER_OUTPUT_DIR) + File.separator + "logs" + File.separator + "joblogs";
            } else {
                logLocation = logLocation + File.separator + "joblogs";
            }
        } else {
            logLocation = logLocation + File.separator + "joblogs";
        }
        return logLocation;
    }

    /**
     * @return "LOG_DIR/joblogs/{jobname}/{date}/instance.{instanceId}", i.e. the joblog directory name for the given jobinstance.
     */
    protected String getJobInstanceLogDirName(JobInstance jobInstance) {
        return getJobInstanceLogDirName(jobInstance.getJobName(), jobInstance.getInstanceId());
    }

    /**
     * @return "LOG_DIR/joblogs/{jobname}/{date}/instance.{instanceId}", i.e. the joblog directory name for the given jobinstance.
     */
    protected String getJobInstanceLogDirName(String jobName, long instanceId) {
        Object token = null;
        File dateDir = null;

        try {
            token = ThreadIdentityManager.runAsServer();

            String dateDirBase = String.format(getAbsoluteJobLogsRootDirName() + File.separator
                                               + "%s" + File.separator
                                               + new SimpleDateFormat("yyyy-MM-dd").format(new Date()),
                                               jobName);
            dateDir = new File(dateDirBase);
            int extraDir = 2;

            // Rotate directory names until we find one that doesn't exist or isn't full.
            while (dateDir.exists() && dateDir.isDirectory() && (dateDir.list().length >= MAX_SUBDIRECTORIES)) {
                dateDir = new File(dateDirBase + "_" + extraDir);
                extraDir++;
            }
        } finally {
            if (token != null)
                ThreadIdentityManager.reset(token);
        }

        return String.format(dateDir.getPath() + File.separator
                             + "instance.%d",
                             instanceId);
    }

    /**
     * @return "LOG_DIR/joblogs/{jobname}/instance.{instanceId}/execution.{executionId}", i.e the joblog directory name
     *         for the given jobinstance and executionId.
     */
    protected String getJobExecutionLogDirName(JobInstance jobInstance, long executionId) {
        return getJobExecutionLogDirName(jobInstance.getJobName(), jobInstance.getInstanceId(), executionId);
    }

    /**
     * @return "LOG_DIR/joblogs/{jobname}/instance.{instanceId}/execution.{executionId}",
     */
    protected String getJobExecutionLogDirName(String jobName, long instanceId, long executionId) {
        return String.format(getJobInstanceLogDirName(jobName, instanceId) + File.separator + "execution.%d",
                             executionId);
    }

    /**
     *
     * A FileFilter for job log files: must be a file (not dir) and must end with ".log".
     */
    protected static final FileFilter JobLogFileFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.isFile() && pathname.getName().endsWith(".log");
        }
    };

    /**
     * @return the writeToServerLog setting
     */
    protected boolean shouldUseParentHandlers(Map<String, Object> config) {
        return Boolean.parseBoolean((String) config.get("writeToServerLog"));
    }

    /**
     * @return the includeServerLogging setting.
     */
    protected boolean shouldRegisterWithRootLogger(Map<String, Object> config) {
        String bool = (String) config.get("includeServerLogging");
        return StringUtils.isEmpty(bool) ? true : Boolean.parseBoolean(bool);
    }

    /**
     * Add our handler to our internal JobLogger and set its Level if it has not
     * been explicitly configured OFF.
     */
    protected void registerWithBatchJobLogger(Map<String, Object> config) {

        Logger jobLogger = Logger.getLogger(JoblogUtil.JobLogLoggerName);
        joblogHandler.addToLogger(jobLogger);

        // Set the level to *at least* FINE (unless it's specifically been turned OFF).
        Level level = jobLogger.getLevel();
        if (level == null || (level.intValue() > Level.FINE.intValue() && level.equals(Level.OFF) == false)) {
            jobLogger.setLevel(Level.FINE);
        }

        // Only log to the job log.
        jobLogger.setUseParentHandlers(shouldUseParentHandlers(config));
    }

    /**
     * Set the Level, Filter, and maxRecords props for the JobLogHandler.
     *
     * The Filter is parsed from the configured traceSpecification.
     */
    protected JobLogHandler configureJobLogHandler(Map<String, Object> config) {

        if (isJobLoggingEnabled() && (!(Boolean) config.get("enabled"))) {
            // Job logging was enabled and now is not.  Need to let the
            // job log handler know so that, if needed, it can send the final
            // log notification at the end of the job.
            joblogHandler.setFinalNotification(true);
        }

        // Remove any previously configured loggers...
        joblogHandler.removeFromLoggers();

        joblogHandler.setMaxRecords((Integer) config.get("maxRecords"));
        joblogHandler.setMaxTime((Integer) config.get("maxTime"));
        joblogHandler.setPurgeOnPublish((Boolean) config.get("purgeOnPublish"));
        joblogHandler.setJobLogManagerImpl(this);
        joblogHandler.setLevel(Level.ALL);

        if ((Boolean) config.get("enabled")) {

            registerWithBatchJobLogger(config);

            List<TraceSpecElement> traceSpecElements = parseTraceSpecElements((String) config.get("jobLoggers"));

            // Add our handler to the loggers identified in the trace spec
            for (TraceSpecElement traceSpecElement : traceSpecElements) {
                Logger logger = traceSpecElement.getLogger();
                joblogHandler.addToLogger(logger);

                logger.setUseParentHandlers(shouldUseParentHandlers(config));
            }

            // Register with the root logger, to include server logging in the job log
            if (shouldRegisterWithRootLogger(config)) {
                joblogHandler.addToLogger(Logger.getLogger(""));
            } else {
                JoblogUtil.setIncludeServerLogging(false);
            }
            // Set final notification flag to false since it is not needed if logging is enabled.
            joblogHandler.setFinalNotification(false);
        }

        // TODO: issue message.

        return joblogHandler;
    }

    /**
     * @return the parsed traceSpec string, parsed into TraceSpecElements.
     */
    protected List<TraceSpecElement> parseTraceSpecElements(String traceSpec) {

        List<TraceSpecElement> traceSpecElements = new ArrayList<TraceSpecElement>();

        // TODO: split on common separators: ":,; "
        for (String spec : StringUtils.split(traceSpec, ":")) {
            if (!StringUtils.isEmpty(spec)) {
                traceSpecElements.add(new TraceSpecElement(spec));
            }
        }

        return traceSpecElements;
    }

}
