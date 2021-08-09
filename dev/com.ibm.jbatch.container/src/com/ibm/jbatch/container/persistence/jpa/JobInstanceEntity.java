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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobInstance;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;

import org.eclipse.persistence.annotations.ClassExtractor;

import com.ibm.jbatch.container.ws.InstanceState;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.websphere.ras.annotation.Trivial;

@NamedQueries({
                @NamedQuery(name = JobInstanceEntity.GET_JOBINSTANCEIDS_BY_NAME_AND_STATUSES_QUERY, query = "SELECT i.instanceId FROM JobInstanceEntity i WHERE i.batchStatus IN :status AND i.jobName = :name"),
                @NamedQuery(name = JobInstanceEntity.GET_JOBINSTANCES_SORT_BY_CREATETIME_FIND_ALL_QUERY, query = "SELECT i FROM JobInstanceEntity i ORDER BY i.createTime DESC"),
                @NamedQuery(name = JobInstanceEntity.GET_JOBINSTANCES_SORT_BY_CREATETIME_FIND_BY_SUBMITTER_QUERY, query = "SELECT i FROM JobInstanceEntity i WHERE i.submitter = :submitter ORDER BY i.createTime DESC"),
                @NamedQuery(name = JobInstanceEntity.GET_JOB_NAMES_SET_QUERY, query = "SELECT DISTINCT i.jobName FROM JobInstanceEntity i"),
                @NamedQuery(name = JobInstanceEntity.GET_JOB_NAMES_SET_BY_SUBMITTER_QUERY, query = "SELECT DISTINCT i.jobName FROM JobInstanceEntity i WHERE i.submitter = :submitter"),
                @NamedQuery(name = JobInstanceEntity.GET_JOBINSTANCE_COUNT_BY_JOBNAME_QUERY, query = "SELECT COUNT(i.instanceId) FROM JobInstanceEntity i WHERE i.jobName = :name"),
                @NamedQuery(name = JobInstanceEntity.GET_JOBINSTANCE_COUNT_BY_JOBNAME_AND_SUBMITTER_QUERY, query = "SELECT COUNT(i.instanceId) FROM JobInstanceEntity i WHERE i.jobName = :name AND i.submitter = :submitter"),
                @NamedQuery(name = JobInstanceEntity.GET_JOBINSTANCES_SORT_CREATETIME_BY_JOBNAME_QUERY, query = "SELECT i FROM JobInstanceEntity i WHERE i.jobName=:name ORDER BY i.createTime DESC"),
                @NamedQuery(name = JobInstanceEntity.GET_JOBINSTANCES_SORT_CREATETIME_BY_JOBNAME_AND_SUBMITTER_QUERY, query = "SELECT i FROM JobInstanceEntity i WHERE i.jobName=:name AND i.submitter = :submitter ORDER BY i.createTime DESC")
})
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@ClassExtractor(JobInstanceEntityExtractor.class)
public class JobInstanceEntity implements JobInstance, WSJobInstance, EntityConstants {

    public static final String GET_JOBINSTANCES_SORT_CREATETIME_BY_JOBNAME_QUERY = "JobInstanceEntity.getJobInstancesSortCreateTimeByJobNameQuery";
    public static final String GET_JOBINSTANCES_SORT_CREATETIME_BY_JOBNAME_AND_SUBMITTER_QUERY = "JobInstanceEntity.getJobInstancesSortCreateTimeByJobNameAndSubmitterQuery";
    public static final String GET_JOBINSTANCES_SORT_BY_CREATETIME_FIND_ALL_QUERY = "JobInstanceEntity.getJobInstancesSortIdAllQuery";
    public static final String GET_JOBINSTANCES_SORT_BY_CREATETIME_FIND_BY_SUBMITTER_QUERY = "JobInstanceEntity.getJobInstancesSortIdBySubmitterQuery";
    public static final String GET_JOBINSTANCE_COUNT_BY_JOBNAME_QUERY = "JobInstanceEntity.getJobInstanceCountByJobNameQuery";
    public static final String GET_JOBINSTANCE_COUNT_BY_JOBNAME_AND_SUBMITTER_QUERY = "JobInstanceEntity.getJobInstanceCountByJobNameAndSubmitterQuery";
    public static final String GET_JOB_NAMES_SET_QUERY = "JobInstanceEntity.getJobNamesSetQuery";
    public static final String GET_JOB_NAMES_SET_BY_SUBMITTER_QUERY = "JobInstanceEntity.getJobNamesSetBySubmitterQuery";
    public static final String GET_JOBINSTANCEIDS_BY_NAME_AND_STATUSES_QUERY = "JobInstanceEntity.getJobInstancesByNameAndStatus";

    /*
     * Key, and constructors
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "JOBINSTANCEID", nullable = false)
    private long instanceId;

    // JPA
    @Trivial
    public JobInstanceEntity() {
    }

    // in-memory
    public JobInstanceEntity(long instanceId) {
        this.instanceId = instanceId;
        this.jobExecutions = Collections.synchronizedList(new ArrayList<JobExecutionEntity>());
        this.stepThreadInstances = Collections.synchronizedList(new ArrayList<StepThreadInstanceEntity>());
    }

    /*
     * SPEC fields for JobInstance
     */
    @Column(name = "JOBNAME", length = 256)
    private String jobName;

    /*
     * Internal-only, (implementation details)
     */
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(name = "CREATETIME", nullable = false)
    private Date createTime;

    @Column(name = "SUBMITTER", length = 256)
    private String submitter;

    @Column(name = "AMCNAME", length = 512)
    private String amcName;

    @Column(name = "JOBXMLNAME", length = 128)
    private String jobXMLName;

    @Column(name = "BATCHSTATUS", nullable = false)
    private BatchStatus batchStatus;

    @Column(name = "EXITSTATUS", length = MAX_EXIT_STATUS_LENGTH)
    private String exitStatus;

    @Lob
    @Column(name = "JOBXML")
    private byte[] jobXml;

    @Column(name = "RESTARTON", length = MAX_STEP_NAME)
    private String restartOn;

    @Column(name = "NUMEXECS", nullable = false)
    private int numberOfExecutions = 0;

    @Column(name = "INSTANCESTATE", nullable = false)
    private InstanceState instanceState;

    /*
     * Relationships
     */
    @OneToMany(mappedBy = "jobInstance", cascade = CascadeType.REMOVE)
    @OrderBy("executionNumberForThisInstance DESC")
    private List<JobExecutionEntity> jobExecutions;

    @OneToMany(mappedBy = "jobInstance", cascade = CascadeType.REMOVE)
    private Collection<StepThreadInstanceEntity> stepThreadInstances; // top-level and partition both

    @Override
    public long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(long instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    public void setJobName(String name) {
        this.jobName = name;
    }

    @Override
    public String getSubmitter() {
        return submitter;
    }

    public void setSubmitter(String submitter) {
        this.submitter = submitter;
    }

    @Override
    public String getAmcName() {
        return amcName;
    }

    public void setAmcName(String appName) {
        this.amcName = appName;
    }

    @Override
    public String getJobXMLName() {
        return jobXMLName;
    }

    public void setJobXmlName(String jobXMLName) {
        this.jobXMLName = jobXMLName;
    }

    @Override
    public String getJobXml() {
        return (jobXml == null ? null : new String(jobXml, StandardCharsets.UTF_8));
    }

    public void setJobXml(String jobXml) {
        if (jobXml != null) {
            this.jobXml = jobXml.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String getRestartOn() {
        return restartOn;
    }

    public void setRestartOn(String restartOn) {
        this.restartOn = restartOn;
    }

    @Override
    public List<JobExecutionEntity> getJobExecutions() {
        return jobExecutions;
    }

    public void setJobExecutions(
                                 List<JobExecutionEntity> jobExecutions) {
        this.jobExecutions = jobExecutions;
    }

    /**
     * @return the batchStatus
     */
    @Override
    public BatchStatus getBatchStatus() {
        return batchStatus;
    }

    /**
     * @param batchStatus the batchStatus to set
     */
    public void setBatchStatus(BatchStatus batchStatus) {
        this.batchStatus = batchStatus;
    }

    /**
     * @return the exitStatus
     */
    public String getExitStatus() {
        return exitStatus;
    }

    /**
     * @param exitStatus the exitStatus to set
     */
    public void setExitStatus(String exitStatus) {
        this.exitStatus = exitStatus;
    }

    @Override
    public InstanceState getInstanceState() {
        return instanceState;
    }

    public void setInstanceState(InstanceState instanceState) {
        this.instanceState = instanceState;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public Date getLastUpdatedTime() {
        return null;
    }

    public void setLastUpdatedTime(Date lastUpdatedTime) {
    }

    public int getNumberOfExecutions() {
        return numberOfExecutions;
    }

    public void setNumberOfExecutions(int numberOfExecutions) {
        this.numberOfExecutions = numberOfExecutions;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("For JobInstanceEntity: ");
        buf.append(" instanceId = " + instanceId);
        buf.append(", batchStatus = " + batchStatus);
        buf.append(", instanceState = " + instanceState);
        return buf.toString();
    }

    public Collection<StepThreadInstanceEntity> getStepThreadInstances() {
        return stepThreadInstances;
    }

    public void setStepThreadInstances(
                                       Collection<StepThreadInstanceEntity> stepThreadInstances) {
        this.stepThreadInstances = stepThreadInstances;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getGroupNames() {
        return null;
    }

}
