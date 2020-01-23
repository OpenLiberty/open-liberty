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

import com.ibm.ws.jbatch.utility.rest.WSPurgeResponse;
import com.ibm.ws.jbatch.utility.utils.ArgumentRequiredException;
import com.ibm.ws.jbatch.utility.utils.ConsoleWrapper;
import com.ibm.ws.jbatch.utility.utils.TaskIO;

/**
 * jbatch utility task for purging a job.
 */
public class PurgeTask extends BaseBatchRestTask<PurgeTask> {

    /**
     * CTOR.
     */
    public PurgeTask(String scriptName) {
        super("purge", scriptName);
    }

    /**
     * Purge the job instance data
     */
    @Override
    public int handleTask(ConsoleWrapper stdin, 
                           PrintStream stdout,
                           PrintStream stderr, 
                           String[] args) throws Exception {

        setTaskIO( new TaskIO(stdin, stdout, stderr) );
        
        setTaskArgs(args);
        
        // multi-purge
        if((getInstanceId() != null && !isNumeric(getInstanceId())) || getCreateTime() != null || getInstanceState() != null || getExitStatus() != null) {
        	// Uses ibm/api/batch/v2/ url
            List<WSPurgeResponse> purgeResponseList = getBatchRestClient().purge(getPurgeJobStoreOnly(),getPage(), getPageSize(), getInstanceId(), 
                        getCreateTime(), getInstanceState(), getExitStatus());
            
            for(int i=0; i < purgeResponseList.size(); i++) {
                issueJobPurgedMessage(purgeResponseList.get(i));
            }
        } else if (getJobInstanceId() != -1L) { // single purge
        	 JobInstance jobInstance = getBatchRestClient().getJobInstance( getJobInstanceId());
             getBatchRestClient().purge(getJobInstanceId(), getPurgeJobStoreOnly());
             issueJobPurgedMessage(jobInstance);
        } else { // no arguments specified
        	throw new ArgumentRequiredException("Must specify at least one of --jobInstanceId, --createTime, --instanceState or --exitStatus");
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
    
    /**
     * @return the --purgeJobStoreOnly arg.
     */
    protected boolean getPurgeJobStoreOnly() {
        return getTaskArgs().isSpecified("--purgeJobStoreOnly");
    }
    
    /**
     * @return the --jobInstanceId arg as a String.
     */
    protected String getInstanceId() {
        return getTaskArgs().getStringValue("--jobInstanceId");
    }
    
    /**
     * @return the --jobInstanceId arg as a Long.
     */
    protected Long getJobInstanceId() {
        return getTaskArgs().getLongValue("--jobInstanceId", -1L);
    }
    
    /**
     * Utility method for validating the jobInstanceId input.
     * A non-numeric value means an input range was specified
     * for multi purge
     */
    private boolean isNumeric(String s) {
    	return java.util.regex.Pattern.matches("\\d+", s);
    }
    
}

