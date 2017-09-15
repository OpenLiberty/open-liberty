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
package com.ibm.jbatch.container.services.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import com.ibm.jbatch.container.execution.impl.RuntimeStepExecution;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Automatically performs a task based on the transaction commit status.
 */
class TranSynchronization implements Synchronization {
    private final static String sourceClass = TranSynchronization.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);
	private RuntimeStepExecution runtimeStepExecution;
	
    /**
     * Construct a new instance.
     */
	TranSynchronization(RuntimeStepExecution runtimeStepExecution) {
		this.runtimeStepExecution = runtimeStepExecution;
    }

    /**
     * Upon successful transaction commit status, store the value of the committed metrics.
     * Upon any other status value roll back the metrics to the last committed value.
     * 
     * @see javax.transaction.Synchronization#afterCompletion(int)
     */
    @Override
    public void afterCompletion(int status) {

    	
	    logger.log(Level.FINE, "The status of the transaction commit is: " + status);
		
        if (status == Status.STATUS_COMMITTED){
        	//Save the metrics object after a successful commit
        	runtimeStepExecution.setCommittedMetrics();
        } else{
        	//status = 4 = STATUS_ROLLEDBACK;
        	runtimeStepExecution.rollBackMetrics();
        }
    }

    /**
     * @see javax.transaction.Synchronization#beforeCompletion()
     */
    @Override
    @Trivial
    public void beforeCompletion() {}
}
