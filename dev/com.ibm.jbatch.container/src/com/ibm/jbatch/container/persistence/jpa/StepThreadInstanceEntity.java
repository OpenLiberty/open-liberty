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
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.ibm.jbatch.container.persistence.CheckpointData;
import com.ibm.websphere.ras.annotation.Trivial;

@IdClass(StepThreadInstanceKey.class)
@DiscriminatorColumn(name="THREADTYPE", discriminatorType=DiscriminatorType.CHAR)
@DiscriminatorValue("P") // The base level is (P)artition, and (T)op-level extends this
@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames = {"FK_JOBINSTANCEID", "STEPNAME", "PARTNUM"})) 
public class StepThreadInstanceEntity implements EntityConstants {

	/*
	 * Keys
	 */
	@Id @ManyToOne
	@JoinColumn(name="FK_JOBINSTANCEID", nullable=false)
	private JobInstanceEntity jobInstance;
	
	@Id
	@Column(name="STEPNAME", nullable=false, length=MAX_STEP_NAME)
	private String stepName;
	
	@Id @Column(name="PARTNUM", nullable=false)
	private int partitionNumber = TOP_LEVEL_THREAD;

	/*
	 * Implementation-specific
	 */
	@Lob
	@Column(name="CHECKPOINTDATA")
	private byte[] checkpointData;
	
	/*
	 * Relationships
	 */
	@OneToOne @JoinColumn(name="FK_LATEST_STEPEXECID", nullable=false)
	private StepThreadExecutionEntity latestStepThreadExecution;

	// Not a useful constructor from the "real" flow of creating a step execution for the first time,
	// for which the other constructor below encapsulates important logic. I think JPA impl uses it though.
    @Trivial
	public StepThreadInstanceEntity() {}

	public StepThreadInstanceEntity(JobInstanceEntity jobInstance, String stepName, int partitionNumber) {
		this.jobInstance = jobInstance;
		this.stepName = stepName;
		this.partitionNumber = partitionNumber;
	}
	
	/**
	 * @return the jobInstance
	 */
	@Trivial
	public JobInstanceEntity getJobInstance() {
		return jobInstance;
	}

	/**
	 * @param jobInstance the jobInstance to set
	 */
	public void setJobInstance(JobInstanceEntity jobInstance) {
		this.jobInstance = jobInstance;
	}

	/**
	 * @return the stepName
	 */
	@Trivial
	public String getStepName() {
		return stepName;
	}

	/**
	 * @param stepName the stepName to set
	 */
	public void setStepName(String stepName) {
		this.stepName = stepName;
	}

	/**
	 * @return the latestStepExecution
	 */
	public StepThreadExecutionEntity getLatestStepThreadExecution() {
		return latestStepThreadExecution;
	}

	/**
	 * @param latestStepExecution the latestStepExecution to set
	 */
	public void setLatestStepThreadExecution(StepThreadExecutionEntity latestStepThreadExecution) {
		this.latestStepThreadExecution = latestStepThreadExecution;
	}

	@Trivial
	public int getPartitionNumber() {
		return partitionNumber;
	}

	public void setPartitionNumber(int partitionNumber) {
		this.partitionNumber = partitionNumber;
	}
	
	/*
	 * Convenience-methods beyond property/field getter/setters
	 */
	public CheckpointData getCheckpointData() {
		return (checkpointData == null ? null : new CheckpointData(checkpointData));
	}
	
	@Trivial
	public void setCheckpointData(CheckpointData data) {
		checkpointData = data.getBytesForDB();
	}

	public void deleteCheckpointData() {
		checkpointData = null;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("For StepThreadInstanceEntity:");
		buf.append(" step Name = " + stepName);
		buf.append(", partition Number = " + partitionNumber);
		buf.append(", instance = " + jobInstance);
		return buf.toString();
	}
}
