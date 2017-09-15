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
 * If this represents a top-level thread (any non-sub-job-partition thread),
 * then the partitionNumber will be -1, and it will actually be an instance of
 * TopLevelStepInstanceKey (subclass).
 * 
 */
public class StepThreadInstanceKey implements Serializable, EntityConstants {
	
	private static final long serialVersionUID = 1L;

	public StepThreadInstanceKey() { }

	public StepThreadInstanceKey(long topLevelJobInstanceId, String stepName, Integer partitionNumber) {
		this.jobInstance = topLevelJobInstanceId;
		this.stepName = stepName;
		this.partitionNumber = partitionNumber;
	}
	public StepThreadInstanceKey(StepThreadInstanceEntity stepThreadInstance) {
		this.jobInstance = stepThreadInstance.getJobInstance().getInstanceId();
		this.stepName = stepThreadInstance.getStepName();
		this.partitionNumber = stepThreadInstance.getPartitionNumber();
	}

	private String stepName;

	private int partitionNumber;
	
	private long jobInstance;

    public int hashCode() {
        return (new Long(jobInstance).intValue() + partitionNumber + stepName.hashCode()) / 37;
    }
    
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof StepThreadInstanceKey)) return false;
        StepThreadInstanceKey pk = (StepThreadInstanceKey) obj;
        return (pk.stepName.equals(this.stepName) && pk.partitionNumber == this.partitionNumber && pk.jobInstance == this.jobInstance);
    }

	/**
	 * @return the stepName
	 */
	public String getStepName() {
		return stepName;
	}

	/**
	 * @return the jobInstance
	 */
	public long getJobInstance() {
		return jobInstance;
	}

	/**
	 * @return the partitionNumber
	 */
	public int getPartitionNumber() {
		return partitionNumber;
	}

	@Override
	public String toString() {
		return "Type: StepThreadInstanceKey, fields:  jobInstanceId = " + jobInstance + ", stepName = " + stepName + ", partitionNumber = " + partitionNumber;
	}
}
