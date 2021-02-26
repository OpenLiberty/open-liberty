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
package com.ibm.ws.jbatch.joblog;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.batch.runtime.JobInstance;

import com.ibm.ws.jbatch.joblog.JobExecutionLog.LogLocalState;

public class JobInstanceLog {

    /**
     * The associated JobInstance.
     */
    private final JobInstance jobInstance;

    /**
     * A ref to "logs/joblogs/{jobname}/{date}/instance.{instanceid}".
     * This is used to help resolve relative names for the jobLogFiles.
     * There may be more than one of these paths (e.g. same instance run
     * on different dates, or executions on different endpoints).
     */
    private final List<File> instanceLogRootDirs;

    /**
     * List of JobExecutionLogs for this job instance.
     */
    private final List<JobExecutionLog> jobExecutionLogs = new ArrayList<JobExecutionLog>();

    /**
     * CTOR.
     */
    public JobInstanceLog(JobInstance jobInstance, List<File> rootDirs) {
        this.jobInstance = jobInstance;
        this.instanceLogRootDirs = rootDirs;
    }

    /**
     * @return the joblogs root dirs, for resolving the relative names
     *         of job log files.
     */
    public List<File> getInstanceLogRootDirs() {
        return instanceLogRootDirs;
    }

    /**
     * Add the given JobExecutionLog to the list.
     */
    public void addJobExecutionLog(JobExecutionLog jobExecutionLog) {
        jobExecutionLogs.add(jobExecutionLog);
    }

    public JobInstance getJobInstance() {
        return jobInstance;
    }

    /**
     * @return the complete list of job log files for this job instance.
     */
    public List<File> getJobLogFiles() {
        List<File> retMe = new ArrayList<File>();

        for (JobExecutionLog jobExecutionLog : jobExecutionLogs) {
            retMe.addAll(jobExecutionLog.getJobLogFiles());
        }

        return retMe;
    }

    public List<JobExecutionLog> getJobExecutionLogs() {
        return jobExecutionLogs;
    }

    /**
     * Delete this instance log, including the top-level directory and all execution logs.
     *
     * @return true if all files were successfully deleted or the files do not exist.
     */
    public boolean purge() {
        boolean success = true;
        for (JobExecutionLog execLog : jobExecutionLogs) {
            //Defect 191586: if the execution logs do not exist true will be returned
            success = success && execLog.purge();
        }
        for (final File instanceDir : instanceLogRootDirs) {
            //Defect 191586: check to ensure the directory exists before purging
            Boolean isThere = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return instanceDir.exists();
                }
            });
            if (success && isThere) {
                success = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                    @Override
                    public Boolean run() {
                        return instanceDir.delete();
                    }
                });
            }
            // Defect 174344 - go up one level to delete empty date directories.
            // Note that if this delete fails, it will simply return false, but we don't use that result in the loop logic.
            //
            // Defect 191586: Added check to see if the file exists. Do not want to waste time trying to delete if so,
            // even though a failure will not change the value of success
            if (success && isThere) {
                AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                    @Override
                    public Boolean run() {
                        return instanceDir.getParentFile().delete();
                    }
                });
            }
        }

        return success;
    }

    /**
     * @return true if all executions of this job instance ran on this endpoint
     */
    public boolean areExecutionsLocal() {
        for (JobExecutionLog execLog : jobExecutionLogs) {
            if (execLog.getLocalState() != LogLocalState.EXECUTION_LOCAL) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return true if any executions of this job instance had remote partitions
     */
    public boolean hasRemotePartitionLogs() {
        for (JobExecutionLog execLog : jobExecutionLogs) {
            if (execLog.getRemotePartitionLogs() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return a set of unique URLs, one for each endpoint that ran a remote partition
     */
    public HashSet<String> getRemotePartitionLogURLs() {
        HashSet<String> retMe = new HashSet<String>();
        for (JobExecutionLog execLog : jobExecutionLogs) {
            retMe.addAll(execLog.getRemotePartitionEndpointURLs());
        }
        return retMe;
    }
}