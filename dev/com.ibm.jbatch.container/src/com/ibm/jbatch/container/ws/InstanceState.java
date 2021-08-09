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
 * InstanceState for the JobInstance record.
 * 
 */
public enum InstanceState {

    /**
     * The JobInstance has been submitted but not yet dispatched.
     */
    SUBMITTED,
    
    /**
     * The JobInstance has been queued to JMS, but not yet consumed by an endpoint.
     */
    JMS_QUEUED,
    
    /**
     * The JobInstance has been consumed by an endpoint, but a JobExecution 
     * has not yet started.
     */
    JMS_CONSUMED,
    
    /**
     * The JobInstance has been dispatched and a JobExecution has been started.
     */
    DISPATCHED,
    
    /**
     * The JobInstance failed.  This matches its BatchStatus.
     */
    FAILED,
    
    /**
     * The JobInstance has been stopped.  This matches its BatchStatus.
     */
    STOPPED,
    
    /**
     * The JobInstance completed.  This matches its BatchStatus.
     */
    COMPLETED,
    
    /**
     * The JobInstance was abandoned.  This matches its BatchStatus.
     */
    ABANDONED;
}
