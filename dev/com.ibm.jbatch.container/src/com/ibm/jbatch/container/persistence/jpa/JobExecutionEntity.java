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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import javax.batch.runtime.JobExecution;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import org.eclipse.persistence.annotations.ClassExtractor;

import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.websphere.ras.annotation.Trivial;

@NamedQueries({
                @NamedQuery(name = JobExecutionEntity.UPDATE_JOB_EXECUTION_AND_INSTANCE_SERVER_NOT_SET, query = "UPDATE JobExecutionEntity x SET x.batchStatus = :batchStatus, x.lastUpdatedTime = :lastUpdatedTime WHERE x.jobExecId = :jobExecId AND x.serverId IS NULL"),

                @NamedQuery(name = JobExecutionEntity.UPDATE_JOB_EXECUTION_SERVERID_AND_RESTURL_FOR_STARTING_JOB, query = "UPDATE JobExecutionEntity x SET x.serverId = :serverId, x.restUrl = :restUrl WHERE x.jobExecId = :jobExecId AND x.batchStatus = javax.batch.runtime.BatchStatus.STARTING"),

                @NamedQuery(name = JobExecutionEntity.GET_JOB_EXECUTIONS_MOST_TO_LEAST_RECENT_BY_INSTANCE, query = "SELECT e FROM JobExecutionEntity e WHERE e.jobInstance.instanceId = :instanceId ORDER BY e.executionNumberForThisInstance DESC"),
                /*
                 * A single result query that could be used for finding the single most recent execution #
                 * query="SELECT e.jobExecId from JobExecutionEntity e" +
                 * " WHERE e.jobInstance.instanceId = :id AND e.jobInstance.e.executionNumberForThisInstance = " +
                 * " (SELECT MAX(e.executionNumberForThisInstance) FROM JobExecutionEntity e WHERE e.jobInstance.instanceId = :id)"),
                 */

                @NamedQuery(name = JobExecutionEntity.GET_JOB_EXECUTIONIDS_BY_JOB_INST_ID, query = "SELECT e.jobExecId FROM JobExecutionEntity e" +
                                                                                                   " WHERE e.jobInstance.instanceId in :instanceList ORDER BY e.createTime DESC"),

                @NamedQuery(name = JobExecutionEntity.GET_JOB_EXECUTIONIDS_BY_NAME_AND_STATUSES_QUERY, query = "SELECT e.jobExecId FROM JobExecutionEntity e" +
                                                                                                               " WHERE e.jobInstance.jobName=:name AND e.batchStatus IN :status ORDER BY e.createTime DESC"),
                @NamedQuery(name = JobExecutionEntity.GET_JOB_EXECUTIONS_BY_SERVERID_AND_STATUSES_QUERY, query = "SELECT e FROM JobExecutionEntity e" +
                                                                                                                 " WHERE e.serverId=:serverid AND e.batchStatus IN :status ORDER BY e.createTime DESC"),
                @NamedQuery(name = JobExecutionEntity.GET_JOB_EXECUTIONS_BY_JOB_INST_ID_AND_JOB_EXEC_NUM, query = "SELECT e FROM JobExecutionEntity e" +
                                                                                                                  " WHERE e.jobInstance.instanceId = :instanceId AND e.executionNumberForThisInstance = :jobExecNum"),
})
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@ClassExtractor(JobExecutionEntityExtractor.class)
public class JobExecutionEntity extends JobThreadExecutionBase implements JobExecution, WSJobExecution {

    // Repeat everywhere we use so caller has to think through granting privilege
    protected static String eol = AccessController.doPrivileged(new PrivilegedAction<String>() {
        @Override
        public String run() {
            return System.getProperty("line.separator");
        }
    });

    public static final String UPDATE_JOB_EXECUTION_AND_INSTANCE_SERVER_NOT_SET = "JobExecutionEntity.updateJobExecutionAndInstanceServerNotSet";
    public static final String UPDATE_JOB_EXECUTION_SERVERID_AND_RESTURL_FOR_STARTING_JOB = "JobExecutionEntity.updateJobExecutionServerIdAndRestUrlForStartingJob";
    public static final String GET_JOB_EXECUTIONIDS_BY_NAME_AND_STATUSES_QUERY = "JobExecutionEntity.getJobExecutionsByNameAndStatusesQuery";
    public static final String GET_JOB_EXECUTIONS_BY_SERVERID_AND_STATUSES_QUERY = "JobExecutionEntity.getJobExecutionsByServerIdAndStatusesQuery";
    public static final String GET_JOB_EXECUTIONS_MOST_TO_LEAST_RECENT_BY_INSTANCE = "JobExecutionEntity.getJobExecutionsMostToLeastRecentByInstanceQuery";
    public static final String GET_JOB_EXECUTIONS_BY_JOB_INST_ID_AND_JOB_EXEC_NUM = "JobExecutionEntity.getJobExecutionsByJobInstanceIdAndJobExecNumberQuery";
    public static final String GET_JOB_EXECUTIONIDS_BY_JOB_INST_ID = "JobExecutionEntity.getJobExecutionsByJobInstanceId";
    /*
     * Key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "JOBEXECID", nullable = false)
    private long jobExecId;

    /*
     * SPEC fields for JobInstance
     */
    @Lob
    @Column(name = "JOBPARAMETERS")
    protected Properties jobParameters;

    @Column(name = "EXECNUM", nullable = false)
    private int executionNumberForThisInstance = 0;

    /*
     * Relationships
     */
    @ManyToOne
    @JoinColumn(name = "FK_JOBINSTANCEID", nullable = false)
    private JobInstanceEntity jobInstance;

    //@OneToMany(mappedBy = "jobExec", cascade = CascadeType.REMOVE)
    //private Collection<RemotableSplitFlowEntity> splitFlowExecutions;

    @OneToMany(mappedBy = "jobExec", cascade = CascadeType.REMOVE)
    private Collection<StepThreadExecutionEntity> stepThreadExecutions;

    // For JPA
    @Trivial
    public JobExecutionEntity() {
    }

    // For in-memory persistence
    public JobExecutionEntity(long jobExecId) {
        this.jobExecId = jobExecId;
        //this.splitFlowExecutions = new ArrayList<RemotableSplitFlowEntity>();
        this.stepThreadExecutions = Collections.synchronizedList(new ArrayList<StepThreadExecutionEntity>());
    }

    @Override
    public JobInstanceEntity getJobInstance() {
        return jobInstance;
    }

    public void setJobInstance(JobInstanceEntity jobInstance) {
        this.jobInstance = jobInstance;
    }

    public Collection<StepThreadExecutionEntity> getStepThreadExecutions() {
        return stepThreadExecutions;
    }

    public void setStepThreadExecutions(Collection<StepThreadExecutionEntity> stepThreadExecutions) {
        this.stepThreadExecutions = stepThreadExecutions;
    }

    public Collection<RemotablePartitionEntity> getRemotablePartitions() {
        return null;
    }

    public void setRemotablePartitions(Collection<RemotablePartitionEntity> remotablePartitions) {
        // Intentionally empty, see JobExecutionEntityV3
    }

    @Override
    public Properties getJobParameters() {
        return jobParameters;
    }

    public void setJobParameters(Properties jobParameters) {
        this.jobParameters = trimJESParameters(jobParameters);
    }

    protected Properties trimJESParameters(Properties jobParameters) {
        if (jobParameters != null) {
            String jesJobName = jobParameters.getProperty("com.ibm.ws.batch.submitter.jobName");
            String jesJobID = jobParameters.getProperty("com.ibm.ws.batch.submitter.jobId");

            if (jesJobName != null) {
                jobParameters.put("com.ibm.ws.batch.submitter.jobName", jesJobName.trim());
            }
            if (jesJobID != null) {
                jobParameters.put("com.ibm.ws.batch.submitter.jobId", jesJobID.trim());
            }
        }

        return jobParameters;
    }

    @Override
    public long getExecutionId() {
        return jobExecId;
    }

    @Override
    public String getJobName() {
        return (jobInstance.getJobName() == null ? "" : jobInstance.getJobName());
    }

    // Convenience method
    @Override
    public long getInstanceId() {
        return jobInstance.getInstanceId();
    }

    @Override
    public int getExecutionNumberForThisInstance() {
        return executionNumberForThisInstance;
    }

    public void setExecutionNumberForThisInstance(
                                                  int executionNumberForThisInstance) {
        this.executionNumberForThisInstance = executionNumberForThisInstance;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(super.toString() + eol);
        buf.append("For JobExecutionEntity:");
        buf.append(" execution Id = " + jobExecId);
        buf.append(", execution sequence num = " + executionNumberForThisInstance);
        buf.append(", instance = " + jobInstance);
        return buf.toString();
    }

//	public Collection<RemotableSplitFlowEntity> getSplitFlowExecutions() {
//		return splitFlowExecutions;
//	}
//
//	public Collection<RemotablePartitionEntity> getPartitionExecutions() {
//		return partitionExecutions;
//	}
}
