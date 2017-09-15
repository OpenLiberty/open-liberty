/**
 * Copyright 2012 International Business Machines Corp.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.jbatch.container.ws;

import java.io.Serializable;
import java.util.Date;
import java.util.Properties;

/**
 * Represents the config for a single partition.
 *
 * Most of the config is the same for all partitions. The only unique parts are
 * partitionNum and partitionPlanProperties.
 *
 */
public class PartitionPlanConfig implements Serializable {

    /**
     * default.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The partition number for this partition.
     */
    private final int partitionNum;

    /**
     * The properties from the partition plan for this specific partition.
     */
    private final Properties partitionPlanProperties;

    /**
     * Top level info (jobname, instanceid, execid).
     */
    private TopLevelNameInstanceExecutionInfo topLevelNameInstanceExecutionInfo;

    /**
     * Top level step exec id
     */
    private long topLevelStepExecutionId = 0L;

    /**
     * Step name
     */
    private String stepName;

    /**
     * Job-level properties.
     */
    private Properties jobProperties;

    /**
     * Correlation Id
     */
    private String correlationId;

    /**
     * Partition createTime
     */
    private Date createTime;

    /**
     * CTOR.
     */
    public PartitionPlanConfig(int partitionNum, Properties partitionProperties) {
        this.partitionNum = partitionNum;
        this.partitionPlanProperties = partitionProperties;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * @return the partition num
     */
    public int getPartitionNumber() {
        return partitionNum;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public Properties getPartitionPlanProperties() {
        return partitionPlanProperties;
    }

    public TopLevelNameInstanceExecutionInfo getTopLevelNameInstanceExecutionInfo() {
        return topLevelNameInstanceExecutionInfo;
    }

    public void setTopLevelNameInstanceExecutionInfo(
                                                     TopLevelNameInstanceExecutionInfo topLevelNameInstanceExecutionInfo) {
        this.topLevelNameInstanceExecutionInfo = topLevelNameInstanceExecutionInfo;
    }

    /**
     * @return the top-level execution id
     */
    public long getTopLevelExecutionId() {
        return getTopLevelNameInstanceExecutionInfo().getExecutionId();
    }

    /**
     * @return the top-level instance id
     */
    public long getTopLevelInstanceId() {
        return getTopLevelNameInstanceExecutionInfo().getInstanceId();
    }

    /**
     * @return the topLevelStepExecutionId
     */
    public long getTopLevelStepExecutionId() {
        return topLevelStepExecutionId;
    }

    /**
     * @param topLevelStepExecutionId the topLevelStepExecutionId to set
     */
    public void setTopLevelStepExecutionId(long topLevelStepExecutionId) {
        this.topLevelStepExecutionId = topLevelStepExecutionId;
    }

    /**
     * @param jobProperties job-level props (retrieved from JobContext.getProperties)
     */
    public void setJobProperties(Properties jobProperties) {
        this.jobProperties = jobProperties;
    }

    /**
     * @return job-level properties
     */
    public Properties getJobProperties() {
        return jobProperties;
    }

    /**
     * @param correlationId the correlation id of the job
     */
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    /**
     * @return correlation Id
     */
    public String getCorrelationId() {
        return correlationId;
    }
}
