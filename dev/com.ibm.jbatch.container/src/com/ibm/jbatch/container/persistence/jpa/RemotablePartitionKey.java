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
package com.ibm.jbatch.container.persistence.jpa;

import java.io.Serializable;

/**
 * Uniquely identifies a partition. Used as a DB key.
 * 
 * @author skurz
 *
 */
public class RemotablePartitionKey implements Serializable {

	private static final long serialVersionUID = 1L;

	public RemotablePartitionKey(long jobExecutionId, String stepName, Integer partitionNumber) {
		this.jobExec = jobExecutionId;
		this.stepName = stepName;
		this.partitionNumber = partitionNumber;
	}
	
	public RemotablePartitionKey(StepThreadExecutionEntity stepExecution){
		this.jobExec = stepExecution.getJobExecution().getExecutionId();
		this.stepName = stepExecution.getStepName();
		this.partitionNumber = stepExecution.getPartitionNumber();
	}

	public RemotablePartitionKey(RemotablePartitionEntity partition) {
		this.jobExec = partition.getJobExecution().getExecutionId();
		this.stepName = partition.getStepName();
		this.partitionNumber = partition.getPartitionNumber();
	}

	private long jobExec;
	
	private String stepName;

	private int partitionNumber;

    public int hashCode() {
        return (new Long(jobExec).intValue() + partitionNumber + stepName.hashCode()) / 37;
    }
    
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof RemotablePartitionKey)) return false;
        RemotablePartitionKey pk = (RemotablePartitionKey) obj;
        return (pk.stepName.equals(this.stepName) && pk.partitionNumber == this.partitionNumber && pk.jobExec == this.jobExec);
    }

	/**
	 * @return the stepName
	 */
	public String getStepName() {
		return stepName;
	}

	/**
	 * @return the partitionNumber
	 */
	public int getPartitionNumber() {
		return partitionNumber;
	}

	public long getJobExec() {
		return jobExec;
	}
	
	@Override
	public String toString() {
		return "RemotablePartitionKey [job execution id=" + jobExec + ", step name="
				+ stepName + ", partition number=" + partitionNumber + "]";
	}
}
