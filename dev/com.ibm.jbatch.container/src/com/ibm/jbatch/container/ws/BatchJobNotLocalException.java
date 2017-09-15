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

/**
 * Thrown when a job request (e.g STOP, or GetJobLog) cannot be run in this
 * server because the job is not running here.
 */
public class BatchJobNotLocalException extends Exception {

    private final WSJobExecution jobExecution;

    private static final long serialVersionUID = 1L;

    public BatchJobNotLocalException(WSJobExecution jobExecution, String localBatchRestUrl, String localServerId) {
        super("The request cannot be completed because the job execution " + jobExecution.getExecutionId()
              + " did not run in this server.  The job execution's restUrl=" + jobExecution.getRestUrl()
              + " and serverId=" + jobExecution.getServerId()
              + ". This server's restUrl=" + localBatchRestUrl + " and serverId=" + localServerId);

        this.jobExecution = jobExecution;
    }

    public WSJobExecution getJobExecution() {
        return jobExecution;
    }
}
