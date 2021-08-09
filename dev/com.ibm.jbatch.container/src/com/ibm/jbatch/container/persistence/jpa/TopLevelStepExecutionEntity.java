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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import com.ibm.jbatch.container.ws.WSTopLevelStepExecution;

import com.ibm.websphere.ras.annotation.Trivial;

@Entity
@DiscriminatorValue("T") // The base level is (P)artition, and (T)op-level extends this
@NamedQueries({
                @NamedQuery(name = TopLevelStepExecutionEntity.GET_ALL_RELATED_STEP_THREAD_EXECUTIONS_SORT_BY_PART_NUM_ASC, query = "SELECT s FROM StepThreadExecutionEntity s WHERE s.topLevelStepExecution.stepExecutionId = :topLevelStepExecutionId ORDER BY s.partitionNumber ASC"),
                @NamedQuery(name = TopLevelStepExecutionEntity.GET_ALL_RELATED_STEP_THREAD_EXECUTIONS_BY_JOB_EXEC_AND_STEP_NAME_SORT_BY_PART_NUM_ASC, query = "SELECT s FROM StepThreadExecutionEntity s WHERE s.jobExec.jobExecId = :jobExecId AND s.stepName = :stepName  ORDER BY s.partitionNumber ASC"),
                @NamedQuery(name = TopLevelStepExecutionEntity.GET_TOP_LEVEL_STEP_EXECUTION_BY_JOB_EXEC_AND_STEP_NAME, query = "SELECT s FROM StepThreadExecutionEntity s WHERE (s.jobExec.jobExecId = :jobExecId AND s.stepName = :stepName AND TYPE(s) = TopLevelStepExecutionEntity) "),
                @NamedQuery(name = TopLevelStepExecutionEntity.GET_TOP_LEVEL_STEP_EXECUTIONS_BY_JOB_EXEC_SORT_BY_START_TIME_ASC, query = "SELECT s FROM StepThreadExecutionEntity s WHERE (s.jobExec.jobExecId = :jobExecId AND TYPE(s) = TopLevelStepExecutionEntity) ORDER BY s.startTime ASC"),
                @NamedQuery(name = TopLevelStepExecutionEntity.GET_TOP_LEVEL_STEP_EXECUTION_BY_JOB_INSTANCE_JOB_EXEC_NUM_AND_STEP_NAME, query = "SELECT s FROM StepThreadExecutionEntity s WHERE (s.jobExec.jobInstance.instanceId = :jobInstanceId AND s.jobExec.executionNumberForThisInstance = :jobExecNum AND s.stepName = :stepName) ORDER BY s.partitionNumber ASC "),

})
public class TopLevelStepExecutionEntity extends StepThreadExecutionEntity implements WSTopLevelStepExecution {

    public static final String GET_ALL_RELATED_STEP_THREAD_EXECUTIONS_SORT_BY_PART_NUM_ASC = "TopLevelStepExecutionEntity.getAllRelatedStepThreadExecsSortByPartNumAscQuery";
    public static final String GET_ALL_RELATED_STEP_THREAD_EXECUTIONS_BY_JOB_EXEC_AND_STEP_NAME_SORT_BY_PART_NUM_ASC = "TopLevelStepExecutionEntity.getAllRelatedStepThreadExecsByJobExecAndStepNameSortByPartNumAscQuery";
    public static final String GET_TOP_LEVEL_STEP_EXECUTION_BY_JOB_EXEC_AND_STEP_NAME = "TopLevelStepExecutionEntity.getTopLevelStepExecutionByJobExecAndStepNameQuery";
    public static final String GET_TOP_LEVEL_STEP_EXECUTIONS_BY_JOB_EXEC_SORT_BY_START_TIME_ASC = "TopLevelStepExecutionEntity.getTopLevelStepExecutionsByJobExecSortByStartTimeAscQuery";
    public static final String GET_TOP_LEVEL_STEP_EXECUTION_BY_JOB_INSTANCE_JOB_EXEC_NUM_AND_STEP_NAME = "TopLevelStepExecutionEntity.getTopLevelStepExecutionByJobInstanceJobExecNumAndStepNameQuery";

    // Not a useful constructor from the "real" flow of creating a step execution for the first time,
    // for which the other constructor below encapsulates important logic.
    @Trivial
    public TopLevelStepExecutionEntity() {
        super();
    }

    //JPA
    public TopLevelStepExecutionEntity(JobExecutionEntity jobExecution, String stepName, boolean isPartitionedStep) {
        super(jobExecution, stepName, TOP_LEVEL_THREAD);
        // For top-level steps this points to itself.
        setTopLevelStepExecution(this);
        this.isPartitionedStep = isPartitionedStep;
    }

    // For in-memory, which plays the "key generation" role
    public TopLevelStepExecutionEntity(long stepExecutionId, JobExecutionEntity jobExecution, String stepName, boolean isPartitionedStep) {
        this(jobExecution, stepName, isPartitionedStep);
        setStepExecutionId(stepExecutionId);
        this.topLevelAndPartitionStepExecutions = Collections.synchronizedList(new ArrayList<StepThreadExecutionEntity>());
    }

    /*
     * Implementation-specific
     */
    private boolean isPartitionedStep;

    // This does not necessarily contain one for every partition at the job instance level, some might have completed on previous executions.
    @OneToMany(mappedBy = "topLevelStepExecution", cascade = CascadeType.REMOVE)
    private Collection<StepThreadExecutionEntity> topLevelAndPartitionStepExecutions;

    /**
     * @return the partitioned
     */
    public boolean isPartitionedStep() {
        return isPartitionedStep;
    }

    /**
     * @param partitioned the partitioned to set
     */
    public void setPartitionedStep(boolean partitioned) {
        this.isPartitionedStep = partitioned;
    }

    /**
     * @return the topLevelAndPartitionStepExecutions
     */
    public Collection<StepThreadExecutionEntity> getTopLevelAndPartitionStepExecutions() {
        return topLevelAndPartitionStepExecutions;
    }

    /**
     * @param topLevelAndPartitionStepExecutions the topLevelAndPartitionStepExecutions to set
     */
    public void setTopLevelAndPartitionStepExecutions(
                                                      Collection<StepThreadExecutionEntity> topLevelAndPartitionStepExecutions) {
        this.topLevelAndPartitionStepExecutions = topLevelAndPartitionStepExecutions;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("For TopLevelStepExecutionEntity:");
        buf.append(" step Name = " + getStepName());
        buf.append(", step exec id = " + getStepExecutionId());
        return buf.toString();
    }

    // Convenience methods

    /**
     * @return the JobInstanceId
     */
    @Override
    public long getJobInstanceId() {
        return getJobExecution().getJobInstance().getInstanceId();
    }

    /**
     * @return the JobExecutionId
     */
    @Override
    public long getJobExecutionId() {
        return getJobExecution().getExecutionId();
    }
}
