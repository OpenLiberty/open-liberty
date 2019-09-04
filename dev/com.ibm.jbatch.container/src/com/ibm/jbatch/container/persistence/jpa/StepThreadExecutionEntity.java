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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.UniqueConstraint;

import com.ibm.jbatch.container.context.impl.MetricImpl;
import com.ibm.jbatch.container.ws.WSPartitionStepThreadExecution;
import com.ibm.ws.serialization.DeserializationObjectInputStream;

@DiscriminatorColumn(name = "THREADTYPE", discriminatorType = DiscriminatorType.CHAR)
@DiscriminatorValue("P")
// The base level is (P)artition, and (T)op-level extends this
@NamedQuery(name = StepThreadExecutionEntity.GET_STEP_THREAD_EXECUTIONIDS_BY_JOB_EXEC_AND_STATUSES_QUERY,
                query = "SELECT e FROM StepThreadExecutionEntity e" +
                        " WHERE e.jobExec.jobExecId=:jobExecutionId AND e.batchStatus IN :status ORDER BY e.stepExecutionId ASC")
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "FK_JOBEXECID", "STEPNAME", "PARTNUM" }))
public class StepThreadExecutionEntity implements WSPartitionStepThreadExecution, StepExecution, EntityConstants {

    public static final String GET_STEP_THREAD_EXECUTIONIDS_BY_JOB_EXEC_AND_STATUSES_QUERY = "StepThreadExecutionEntity.getStepThreadExecutionsByJobExecIdAndStatusesQuery";

    // Not a useful constructor from the "real" flow of creating a step execution for the first time,
    // for which the other constructor below encapsulates important logic.
    public StepThreadExecutionEntity() {
        super();
    }

    public StepThreadExecutionEntity(JobExecutionEntity jobExecution, String stepName, int partitionNumber) {
        this.jobExec = jobExecution;
        this.stepName = stepName;
        this.partitionNumber = partitionNumber;
        this.batchStatus = BatchStatus.STARTING;

        this.internalStatus = 0; // Not used currently, wondering if it will have some value with partitions later on.
    }

    // For in-memory, which plays the "key generation" role
    public StepThreadExecutionEntity(long stepExecutionId, JobExecutionEntity jobExecution, String stepName, int partitionNumber) {
        this(jobExecution, stepName, partitionNumber);
        setStepExecutionId(stepExecutionId);
    }

    /*
     * Key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "STEPEXECID", nullable = false)
    private long stepExecutionId;

    /*
     * spec
     */
    @Column(name = "STEPNAME", nullable = false, length = MAX_STEP_NAME)
    private String stepName;

    @Column(name = "BATCHSTATUS", nullable = false)
    private BatchStatus batchStatus;

    @Column(name = "EXITSTATUS", length = MAX_EXIT_STATUS_LENGTH)
    private String exitStatus;

    @Column(name = "M_READ", nullable = false)
    private long readCount = 0;

    @Column(name = "M_WRITE", nullable = false)
    private long writeCount = 0;

    @Column(name = "M_COMMIT", nullable = false)
    private long commitCount = 0;

    @Column(name = "M_ROLLBACK", nullable = false)
    private long rollbackCount = 0;

    @Column(name = "M_READSKIP", nullable = false)
    private long readSkipCount = 0;

    @Column(name = "M_PROCESSSKIP", nullable = false)
    private long processSkipCount = 0;

    @Column(name = "M_WRITESKIP", nullable = false)
    private long writeSkipCount = 0;

    @Column(name = "M_FILTER", nullable = false)
    private long filterCount = 0;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(name = "STARTTIME")
    private Date startTime;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(name = "ENDTIME")
    private Date endTime;

    @Lob
    @Column(name = "USERDATA")
    private byte[] persistentUserDataBytes;

    /*
     * Implementation details
     */
    @Column(name = "PARTNUM", nullable = false)
    private int partitionNumber = TOP_LEVEL_THREAD;

    @Column(name = "INTERNALSTATUS", nullable = false)
    private int internalStatus;

    /*
     * Relationships
     */
    @ManyToOne
    @JoinColumn(name = "FK_JOBEXECID", nullable = false)
    private JobExecutionEntity jobExec;

    /* 220050 - Backout 205106
    @OneToOne(optional = true, mappedBy = "stepExecutionEntity", cascade = CascadeType.REMOVE)
    private RemotablePartitionEntity remotablePartition;
    */

    // Easiest to allow this to be null otherwise we'd have to worry about
    // how to supply the generated key value to this column too.
    @ManyToOne
    @JoinColumn(name = "FK_TOPLVL_STEPEXECID")
    private TopLevelStepExecutionEntity topLevelStepExecution;

    /**
     * @return the stepExecutionId
     */
    @Override
    public long getStepExecutionId() {
        return stepExecutionId;
    }

    /**
     * @param stepExecutionId the stepExecutionId to set
     */
    public void setStepExecutionId(long stepExecutionId) {
        this.stepExecutionId = stepExecutionId;
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
    public BatchStatus getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(BatchStatus batchStatus) {
        this.batchStatus = batchStatus;
    }

    @Override
    public String getExitStatus() {
        return exitStatus;
    }

    public void setExitStatus(String exitStatus) {
        this.exitStatus = exitStatus;
    }

    public long getReadCount() {
        return readCount;
    }

    public void setReadCount(long readCount) {
        this.readCount = readCount;
    }

    /**
     * @return the remotablePartition
     */
    public RemotablePartitionEntity getRemotablePartition() {
        //222050 - Backout 205106
        //return remotablePartition;
        return null;
    }

    /**
     * @param remotablePartition the remotablePartition to set
     */
    public void setRemotablePartition(RemotablePartitionEntity remotablePartition) {
        //222050 Backout 205106
        //this.remotablePartition = remotablePartition;
    }

    public long getWriteCount() {
        return writeCount;
    }

    public void setWriteCount(long writeCount) {
        this.writeCount = writeCount;
    }

    public long getCommitCount() {
        return commitCount;
    }

    public void setCommitCount(long commitCount) {
        this.commitCount = commitCount;
    }

    public long getRollbackCount() {
        return rollbackCount;
    }

    public void setRollbackCount(long rollbackCount) {
        this.rollbackCount = rollbackCount;
    }

    public long getReadSkipCount() {
        return readSkipCount;
    }

    public void setReadSkipCount(long readSkipCount) {
        this.readSkipCount = readSkipCount;
    }

    public long getProcessSkipCount() {
        return processSkipCount;
    }

    public void setProcessSkipCount(long processSkipCount) {
        this.processSkipCount = processSkipCount;
    }

    public long getFilterCount() {
        return filterCount;
    }

    public void setFilterCount(long filterCount) {
        this.filterCount = filterCount;
    }

    public long getWriteSkipCount() {
        return writeSkipCount;
    }

    public void setWriteSkipCount(long writeSkipCount) {
        this.writeSkipCount = writeSkipCount;
    }

    @Override
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @Override
    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public byte[] getPersistentUserDataBytes() {
        return persistentUserDataBytes;
    }

    public void setPersistentUserDataBytes(byte[] persistentUserData) {
        this.persistentUserDataBytes = persistentUserData;
    }

    private Serializable getPersistentUserDataObject() {
        Serializable retVal = null;
        try {
            retVal = deserializeObject(persistentUserDataBytes);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Problem while trying to deserialize persistent user data");
        } catch (IOException e) {
            throw new IllegalStateException("Problem while trying to deserialize persistent user data");
        }
        return retVal;
    }

    @Override
    public Serializable getPersistentUserData() {
        return getPersistentUserDataObject();
    }

    /**
     * SPEC API
     */
    /**
     * @return the partitionNumber
     */
    @Override
    public int getPartitionNumber() {
        return partitionNumber;
    }

    /**
     * @param partitionNumber the partitionNumber to set
     */
    public void setPartitionNumber(int partitionNumber) {
        this.partitionNumber = partitionNumber;
    }

    /**
     * @return the jobExecution
     */
    public JobExecutionEntity getJobExecution() {
        return jobExec;
    }

    /**
     * @param jobExecution the jobExecution to set
     */
    public void setJobExecution(JobExecutionEntity jobExecution) {
        this.jobExec = jobExecution;
    }

    /**
     * @return the topLevelStepExecution
     */
    public TopLevelStepExecutionEntity getTopLevelStepExecution() {
        return topLevelStepExecution;
    }

    /**
     * @param topLevelStepExecution the topLevelStepExecution to set
     */
    public void setTopLevelStepExecution(TopLevelStepExecutionEntity topLevelStepExecution) {
        this.topLevelStepExecution = topLevelStepExecution;
    }

    public void addMetrics(StepThreadExecutionEntity step) {
        this.readCount += step.getReadCount();
        this.writeCount += step.getWriteCount();
        this.processSkipCount += step.getProcessSkipCount();
        this.commitCount += step.getCommitCount();
        this.rollbackCount += step.getRollbackCount();
        this.readSkipCount += step.getReadSkipCount();
        this.filterCount += step.getFilterCount();
        this.writeSkipCount += step.getWriteSkipCount();
    }

    /**
     * This method is used to de-serialized a table BLOB field to its original object form.
     *
     * @param buffer the byte array save a BLOB
     * @return the object saved as byte array
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private Serializable deserializeObject(byte[] buffer) throws IOException, ClassNotFoundException {

        Serializable theObject = null;
        DeserializationObjectInputStream objectIn = null;

        if (buffer != null) {
            objectIn = new DeserializationObjectInputStream(new ByteArrayInputStream(buffer), Thread.currentThread().getContextClassLoader());
            theObject = (Serializable) objectIn.readObject();
            objectIn.close();
        }
        return theObject;
    }

    /**
     * @return the internalStatus
     */
    public int getInternalStatus() {
        return internalStatus;
    }

    /**
     * @param internalStatus the internalStatus to set
     */
    public void setInternalStatus(int internalStatus) {
        this.internalStatus = internalStatus;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.batch.runtime.StepExecution#getMetrics()
     */
    @Override
    public Metric[] getMetrics() {
        Metric[] metrics = new MetricImpl[8];
        metrics[0] = new MetricImpl(MetricImpl.MetricType.READ_COUNT, readCount);
        metrics[1] = new MetricImpl(MetricImpl.MetricType.WRITE_COUNT, writeCount);
        metrics[2] = new MetricImpl(MetricImpl.MetricType.COMMIT_COUNT, commitCount);
        metrics[3] = new MetricImpl(MetricImpl.MetricType.ROLLBACK_COUNT, rollbackCount);
        metrics[4] = new MetricImpl(MetricImpl.MetricType.READ_SKIP_COUNT, readSkipCount);
        metrics[5] = new MetricImpl(MetricImpl.MetricType.PROCESS_SKIP_COUNT, processSkipCount);
        metrics[6] = new MetricImpl(MetricImpl.MetricType.FILTER_COUNT, filterCount);
        metrics[7] = new MetricImpl(MetricImpl.MetricType.WRITE_SKIP_COUNT, writeSkipCount);

        return metrics;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("For StepThreadExecutionEntity:");
        buf.append(" step Name = " + stepName);
        buf.append(", partition Number = " + partitionNumber);
        buf.append(", step exec id = " + stepExecutionId);
        return buf.toString();
    }
}
