/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.ws;

import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;

public class BatchDispatcherException extends BatchContainerRuntimeException {
    
    private long jobExecutionId;
    private long jobInstanceId;
    
    private static final long serialVersionUID = 1L;

    public BatchDispatcherException(String message, long jobInstanceId, long jobExecutionId) {
        super(message + " encounters when dispatching job instance " + jobInstanceId + ", job execution " + jobExecutionId);
        this.jobExecutionId = jobExecutionId;
        this.jobInstanceId = jobInstanceId;
    }
    
    public BatchDispatcherException(Throwable cause, long jobInstanceId, long jobExecutionId) {
        super("Unable to dispatch job instance " + jobInstanceId + ", job execution id " + jobExecutionId, cause);
    }
    
    public BatchDispatcherException(Throwable cause, long jobExecutionId) {
        super("Unable to dispatch job execution id " + jobExecutionId, cause);
        this.jobExecutionId = jobExecutionId;
    } 
    
    public BatchDispatcherException(Throwable cause ){
        super(cause);
    }
    
    public BatchDispatcherException(String message, Throwable cause ){
        super(message, cause);
    }
    
    public BatchDispatcherException(String message, long jobExecutionId) {
        super(message + " encounters when dispatching job execution " + jobExecutionId);
        this.jobExecutionId = jobExecutionId;        
    }
    
    public BatchDispatcherException(String message) {
        super(message + " encounters when dispatching job execution ");
         
    }
    public long getJobExecutionId() {
        return jobExecutionId;
    }

    public long getJobInstanceId() {
        return jobInstanceId;
    }
}
