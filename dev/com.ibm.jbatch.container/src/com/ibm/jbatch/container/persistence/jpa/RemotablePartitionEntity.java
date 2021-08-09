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
import java.util.Date;

import javax.batch.runtime.JobExecution;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;

import com.ibm.jbatch.container.ws.WSRemotablePartitionExecution;
import com.ibm.jbatch.container.ws.WSRemotablePartitionState;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * @author skurz
 *
 */
@NamedQueries({

                @NamedQuery(name = RemotablePartitionEntity.GET_ALL_RELATED_REMOTABLE_PARTITIONS, query = "SELECT r FROM RemotablePartitionEntity r WHERE r.stepExecutionEntity.stepExecutionId IN (SELECT s.stepExecutionId FROM StepThreadExecutionEntity s WHERE s.topLevelStepExecution.stepExecutionId = :topLevelStepExecutionId AND TYPE(s) = StepThreadExecutionEntity ) ORDER BY r.stepExecutionEntity.partitionNumber ASC"),

                @NamedQuery(name = RemotablePartitionEntity.GET_PARTITION_STEP_THREAD_EXECUTIONIDS_BY_SERVERID_AND_STATUSES_QUERY, query = "SELECT r FROM RemotablePartitionEntity r WHERE r.serverId = :serverid AND r.stepExecutionEntity.batchStatus IN :status ORDER BY r.stepExecutionEntity.startTime DESC"),
                @NamedQuery(name = RemotablePartitionEntity.GET_RECOVERED_REMOTABLE_PARTITIONS, query = "SELECT r.partitionNumber FROM RemotablePartitionEntity r WHERE r.internalStatus = com.ibm.jbatch.container.ws.WSRemotablePartitionState.RECOVERED AND r.stepExecutionEntity.topLevelStepExecution.stepExecutionId = :topLevelStepExecutionId ORDER BY r.stepExecutionEntity.partitionNumber ASC"),
})

@IdClass(RemotablePartitionKey.class)
@Table
@Entity
public class RemotablePartitionEntity implements WSRemotablePartitionExecution {

    // Repeat everywhere we use so caller has to think through granting privilege
    protected static String eol = AccessController.doPrivileged(new PrivilegedAction<String>() {
        @Override
        public String run() {
            return System.getProperty("line.separator");
        }
    });

    public static final String GET_ALL_RELATED_REMOTABLE_PARTITIONS = "RemotablePartitionEntity.getAllRelatedRemotablePartitions";
    public static final String GET_PARTITION_STEP_THREAD_EXECUTIONIDS_BY_SERVERID_AND_STATUSES_QUERY = "RemotablePartitionEntity.getPartitionStepExecutionByServerIdAndStatusesQuery";
    public static final String GET_RECOVERED_REMOTABLE_PARTITIONS = "RemotablePartitionEntity.getRecoveredRemotablePartitions";

    @Id
    @ManyToOne()
    @JoinColumn(name = "FK_JOBEXECUTIONID", nullable = false)
    private JobExecutionEntity jobExec;

    @JoinColumn(name = "FK_STEPEXECUTIONID")
    private StepThreadExecutionEntity stepExecutionEntity;

    @Id
    @Column(name = "STEPNAME")
    private String stepName;

    @Column(name = "PARTNUM")
    @Id
    private int partitionNumber;

    @Column(name = "INTERNALSTATE")
    private WSRemotablePartitionState internalStatus;

    @Column(name = "SERVERID", length = 256)
    private String serverId;

    @Column(name = "RESTURL", length = 512)
    private String restUrl;

    @Column(name = "LOGPATH", nullable = true, length = 512)
    private String logpath;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(name = "LASTUPDATED")
    private Date lastUpdated;

    @Trivial
    public RemotablePartitionEntity() {
    }

    public RemotablePartitionEntity(JobExecutionEntity jobExecution,
                                    RemotablePartitionKey partitionKey) {
        this.jobExec = jobExecution;
        this.stepName = partitionKey.getStepName();
        this.partitionNumber = partitionKey.getPartitionNumber();
    }

    public RemotablePartitionEntity(JobExecutionEntity jobExecution, String stepName, int partitionNum) {
        this.jobExec = jobExecution;
        this.stepName = stepName;
        this.partitionNumber = partitionNum;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date queuedTime) {
        this.lastUpdated = queuedTime;
    }

    /**
     * @return the stepName
     */
    @Override
    public String getStepName() {
        return stepName;
    }

    /**
     * @param stepName the stepName to set
     */
    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    @Override
    public int getPartitionNumber() {
        return partitionNumber;
    }

    public void setPartitionNumber(int partitionNumber) {
        this.partitionNumber = partitionNumber;
    }

    public WSRemotablePartitionState getInternalStatus() {
        return internalStatus;
    }

    public void setInternalStatus(WSRemotablePartitionState execution) {
        this.internalStatus = execution;
    }

    public JobExecutionEntity getJobExec() {
        return jobExec;
    }

    @Override
    public JobExecution getJobExecution() {
        return jobExec;
    }

    public void setJobExec(JobExecutionEntity jobExecutionId) {
        this.jobExec = jobExecutionId;
    }

    @Override
    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    @Override
    public String getRestUrl() {
        return restUrl;
    }

    public void setRestUrl(String restUrl) {
        this.restUrl = restUrl;
    }

    @Override
    public String getLogpath() {
        return logpath;
    }

    public void setLogpath(String logpath) {
        this.logpath = logpath;
    }

    public StepThreadExecutionEntity getStepExecution() {
        return stepExecutionEntity;
    }

    public void setStepExecution(StepThreadExecutionEntity stepExecution) {
        this.stepExecutionEntity = stepExecution;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        Long jobExecutionId = jobExec == null ? null : jobExec.getExecutionId();
        buf.append(super.toString() + eol);
        buf.append("For RemotablePartitionExecutionEntity:");
        buf.append(", job executionId = " + jobExecutionId);
        buf.append(" stepName = " + stepName);
        buf.append(", partition number = " + partitionNumber);
        return buf.toString();
    }

}
