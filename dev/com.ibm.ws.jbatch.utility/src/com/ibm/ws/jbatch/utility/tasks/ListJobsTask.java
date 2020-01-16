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
package com.ibm.ws.jbatch.utility.tasks;

import java.io.PrintStream;
import java.util.List;

import javax.batch.runtime.JobInstance;

import com.ibm.ws.jbatch.utility.utils.ConsoleWrapper;
import com.ibm.ws.jbatch.utility.utils.TaskIO;

/**
 * jbatch utility task that lists job instances.
 */
public class ListJobsTask extends BaseBatchRestTask<ListJobsTask> {

    /**
     * CTOR.
     */
    public ListJobsTask(String scriptName) {
        super("listJobs", scriptName);
    }

    /**
     * 
     * List jobs.
     */
    @Override
    public int handleTask(ConsoleWrapper stdin, 
                           PrintStream stdout,
                           PrintStream stderr, 
                           String[] args) throws Exception {

        setTaskIO( new TaskIO(stdin, stdout, stderr) );
        
        setTaskArgs(args);
        
        List<JobInstance> jobInstances;
        
        // Check for multi-pre-purge query
        if(getJobInstanceId() != null || getCreateTime() != null || getInstanceState() != null || getExitStatus() != null) {
            // Uses ibm/api/batch/v2/ url
            jobInstances = getBatchRestClient().getJobInstances(getPage(), getPageSize(), getJobInstanceId(), 
                        getCreateTime(), getInstanceState(), getExitStatus());
        } else { // single query
            jobInstances = getBatchRestClient().getJobInstances(getPage(), getPageSize());
        }

        return handleResult(jobInstances) ;

    }
    
    /**
     * Print messages for each job instance.
     * 
     * @return 0
     */
    protected int handleResult(List<JobInstance> jobInstances) {
        for (JobInstance jobInstance : jobInstances) {
            issueJobInstanceMessage(jobInstance);
        }
        
        return 0;
    }

    /**
     * @return the --page arg.
     */
    protected int getPage() {
        return getTaskArgs().getIntValue("--page", 0);
    }
    
    /**
     * @return the --pageSize arg.
     */
    protected int getPageSize() {
        return getTaskArgs().getIntValue("--pageSize", 50);
    }
    
    /**
     * @return the --jobInstanceId arg.
     */
    protected String getJobInstanceId() {
        return getTaskArgs().getStringValue("--jobInstanceId");
    }
    
    /**
     * @return the --createTime arg.
     */
    protected String getCreateTime() {
        return getTaskArgs().getStringValue("--createTime");
    }
    
    /**
     * @return the --instanceState arg.
     */
    protected String getInstanceState() {
        return getTaskArgs().getStringValue("--instanceState");
    }
    
    /**
     * @return the --exitStatus arg.
     */
    protected String getExitStatus() {
        return getTaskArgs().getStringValue("--exitStatus");
    }
   
}

