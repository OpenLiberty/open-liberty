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

package com.ibm.ws.jbatch.jms.internal;

import com.ibm.jbatch.container.ws.BatchDispatcherException;

public class BatchJmsDispatcherException extends BatchDispatcherException {
    
    private static final long serialVersionUID = 1L;
    
    public BatchJmsDispatcherException(String message, long jobInstanceId, long jobExecutionId) {
        super(message, jobInstanceId, jobExecutionId);
    }

    public BatchJmsDispatcherException(Throwable cause, long jobInstanceId, long jobExecutionId) {
        super(cause, jobInstanceId, jobExecutionId);
    }
 
    public BatchJmsDispatcherException(Throwable cause, long jobExecutionId) {
        super(cause, jobExecutionId);
    }
    
    public BatchJmsDispatcherException(Throwable cause) {
        super(cause);
    }
    
    public BatchJmsDispatcherException(String msg, Throwable cause) {
        super(msg, cause);
    }
        
    public BatchJmsDispatcherException(String message, long jobExecutionId) {
        super(message, jobExecutionId);
    }
    
    public BatchJmsDispatcherException(String message) {
        super(message);
    }
    
}
