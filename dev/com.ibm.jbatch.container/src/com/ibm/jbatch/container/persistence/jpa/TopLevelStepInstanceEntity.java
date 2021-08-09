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

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import com.ibm.websphere.ras.annotation.Trivial;

@Entity
@DiscriminatorValue("T") // The base level is (P)artition, and (T)op-level extends this

// Is it possible to use TYPE(s) below to match just the partition types, given that the both partition and top-level types will be (instanceof) the base type?
@NamedQueries({ 
	@NamedQuery(name=TopLevelStepInstanceEntity.GET_RELATED_PARTITION_LEVEL_STEP_THREAD_INSTANCES,
     query="SELECT s FROM StepThreadInstanceEntity s WHERE s.jobInstance.instanceId = :instanceId AND s.stepName = :stepName AND TYPE(s) <> TopLevelStepInstanceEntity ORDER BY s.partitionNumber ASC"),
	@NamedQuery(name=TopLevelStepInstanceEntity.GET_RELATED_PARTITION_LEVEL_COMPLETED_PARTITION_NUMBERS,
    query="SELECT s.partitionNumber FROM StepThreadInstanceEntity s WHERE s.jobInstance.instanceId = :instanceId AND s.stepName = :stepName AND TYPE(s) <> TopLevelStepInstanceEntity" + 
	" AND s.latestStepThreadExecution.batchStatus = javax.batch.runtime.BatchStatus.COMPLETED ORDER BY s.partitionNumber ASC")

})
public class TopLevelStepInstanceEntity extends StepThreadInstanceEntity {

	public static final String GET_RELATED_PARTITION_LEVEL_STEP_THREAD_INSTANCES = "TopLevelStepInstanceEntity.getRelatedPartitionLevelStepThreadInstancesQuery";

	public static final String GET_RELATED_PARTITION_LEVEL_COMPLETED_PARTITION_NUMBERS = "TopLevelStepInstanceEntity.getRelatedPartitionLevelCompletedPartitionNumbersQuery";
	
	@Column(name="STARTCOUNT")
	private int startCount;
	
	// This could have been a logical point to try a 'null' value instead, but since
	// we have an easy way to separate the uninitialized value (with -1), we go out of
	// our way to avoid null, though not sure if that's better.  Still, we will initialize
	// with 'null', so allow nullable via this
	@Column(name="PARTITIONPLANSIZE")
	private int partitionPlanSize = PARTITION_PLAN_SIZE_UNINITIALIZED;

	// This is meant to signify whether the step is partitioned or not.
	// It is not meant, in contract, to say whether for a partitioned step, 
	// this object represents the top-level or partition-level thread of the partitioned step.
	@Column(name="PARTITIONED", nullable=false)
	private boolean isPartitionedStep;
	
	// Not a useful constructor from the "real" flow of creating a step execution for the first time,
	// for which the other constructor below encapsulates important logic.
    @Trivial
	public TopLevelStepInstanceEntity() {}

	public TopLevelStepInstanceEntity(JobInstanceEntity jobInstance, String stepName, boolean isPartitionedStep) {
		super(jobInstance, stepName, EntityConstants.TOP_LEVEL_THREAD);
		this.startCount = 1;
		this.isPartitionedStep = isPartitionedStep;
	}

	/**
	 * @return the planSize
	 */
	public int getPartitionPlanSize() {
		return partitionPlanSize;
	}

	/**
	 * @param planSize the planSize to set
	 */
	public void setPartitionPlanSize(int planSize) {
		this.partitionPlanSize = planSize;
	}

	/**
	 * @return the isPartitioned
	 */
	public boolean isPartitionedStep() {
		return isPartitionedStep;
	}

	/**
	 * @param isPartitioned the isPartitioned to set
	 */
	public void setPartitionedStep(boolean isPartitioned) {
		this.isPartitionedStep = isPartitioned;
	}

    public int getStartCount() {
        return startCount;
    }

    public void setStartCount(int startCount) {
        this.startCount = startCount;
    }
	
	public void incrementStartCount() {
		this.startCount++;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("For TopLevelStepInstanceEntity:");
		buf.append(" step Name = " + getStepName());
		buf.append(", job instance = " +  getJobInstance());
		return buf.toString();
		
	}

}
