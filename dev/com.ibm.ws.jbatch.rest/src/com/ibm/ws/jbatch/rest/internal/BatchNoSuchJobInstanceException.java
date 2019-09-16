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

import javax.batch.operations.NoSuchJobInstanceException;

import com.ibm.ws.jbatch.rest.utils.ResourceBundleRest;

/**
 * Wrapper around NoSuchJobInstanceException.  
 * 
 * Caches the non-existent jobInstanceId in a separate field for easy retrieval.
 */
public class BatchNoSuchJobInstanceException extends NoSuchJobInstanceException {
    
    private static final long serialVersionUID = 1L;
    
    private long jobInstanceId;
    
    public BatchNoSuchJobInstanceException(Throwable initCause, long jobInstanceId) {
		//Defect 195757: changing exception to have a translated message
    	super(ResourceBundleRest.getMessage("job.instance.not.found", jobInstanceId), initCause);
        this.jobInstanceId = jobInstanceId;
    }
    
    public BatchNoSuchJobInstanceException(long jobInstanceId) {
		//Defect 195757: changing exception to have a translated message
        super(ResourceBundleRest.getMessage("job.instance.not.found", jobInstanceId));
        this.jobInstanceId = jobInstanceId;
    }

    public long getJobInstanceId() {
        return jobInstanceId;
    }

}
