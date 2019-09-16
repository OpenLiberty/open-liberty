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

import javax.batch.operations.NoSuchJobExecutionException;

public class BatchNoSuchJobExecutionException extends NoSuchJobExecutionException {

    private static final long serialVersionUID = 1L;
    
    private long jobExecutionId;
    
    public BatchNoSuchJobExecutionException(Throwable initCause, long jobExecutionId) {
        super("No job execution found for execution id " + jobExecutionId, initCause);
        this.jobExecutionId = jobExecutionId;
    }
    
    public BatchNoSuchJobExecutionException(long jobExecutionId) {
        super("No job execution found for execution id " + jobExecutionId);
        this.jobExecutionId = jobExecutionId;
    }

    public long getJobExecutionId() {
        return jobExecutionId;
    }
}
