/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.util;

import com.ibm.jbatch.container.persistence.jpa.RemotablePartitionEntity;
import com.ibm.jbatch.container.persistence.jpa.StepThreadExecutionEntity;
import com.ibm.jbatch.container.ws.WSPartitionStepAggregate;
import com.ibm.jbatch.container.ws.WSPartitionStepThreadExecution;
import com.ibm.jbatch.container.ws.WSRemotablePartitionExecution;

public class WSPartitionStepAggregateImpl implements WSPartitionStepAggregate {
	

	private StepThreadExecutionEntity step;
	
	private RemotablePartitionEntity remotablePartition;
	
	
	
	public WSPartitionStepAggregateImpl(StepThreadExecutionEntity step,
									RemotablePartitionEntity remotablePartition) {
		this.step = step;
		this.remotablePartition = remotablePartition;
	}
	
	public WSPartitionStepAggregateImpl(Object[] entities){
		
		if(entities.length > 2){
			//Should never reach here unless the query is changed 
			//to return something other than (StepThreadExecutionEntity, RemotablePartitionEnity)
			throw new IllegalStateException("More than two entities found for a partition");
		}
		try{
			this.step = (StepThreadExecutionEntity) entities[0];
		}
		catch(ClassCastException e){
			//Should never reach here unless the query is changed 
			//to return something other than (StepThreadExecutionEntity, RemotablePartitionEnity)
			throw new IllegalArgumentException("Not able to process the stepThreadExecution obtained from the database :" + entities[0]);
		}
		if(entities.length == 2){
			try{
				this.remotablePartition = (RemotablePartitionEntity) entities[1];
			} catch(ClassCastException e){
				//Should never reach here unless the query is changed 
				//to return something other than (StepThreadExecutionEntity, RemotablePartitionEnity)
				throw new IllegalArgumentException("Not able to process the stepThreadExecution obtained from the database :" + entities[1]);
			}		
		}
	}

	@Override
	public WSPartitionStepThreadExecution getPartitionStepThread() {
		return step;
	}

	public void setPartitionStep(StepThreadExecutionEntity partitionStep) {
		this.step = partitionStep;
	}

	@Override
	public WSRemotablePartitionExecution getRemotablePartition() {
		return remotablePartition;
	}

	public void setRemotablePartition(
			RemotablePartitionEntity remotablePartition) {
		this.remotablePartition = remotablePartition;
	}

}
