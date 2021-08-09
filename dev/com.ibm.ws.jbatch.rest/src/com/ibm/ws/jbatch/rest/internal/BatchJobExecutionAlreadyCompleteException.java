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
package com.ibm.ws.jbatch.rest.internal;

import javax.batch.operations.JobExecutionAlreadyCompleteException;

/**
 * Wrapper around JobExecutionAlreadyCompleteException.
 * 
 * Caches job exec ID and job instance ID for easy retrieval.
 */
public class BatchJobExecutionAlreadyCompleteException extends JobExecutionAlreadyCompleteException {
    
    private static final long serialVersionUID = 1L;
    
    private long jobExecutionId;
    private long jobInstanceId;
    
    public BatchJobExecutionAlreadyCompleteException(Throwable initCause, long jobExecutionId, long jobInstanceId) {
        super("Job execution " + jobExecutionId + " for job instance " + jobInstanceId + " is already complete", initCause);
        this.jobExecutionId = jobExecutionId;
        this.jobInstanceId = jobInstanceId;
    }
    
    public BatchJobExecutionAlreadyCompleteException(Throwable initCause, long jobExecutionId) {
        super("Job execution " + jobExecutionId + " is already complete", initCause);
        this.jobExecutionId = jobExecutionId;
    }

    public long getJobExecutionId() {
        return jobExecutionId;
    }
    
    public long getJobInstanceId() {
        return jobInstanceId;
    }

}
