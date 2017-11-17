/**
 * Copyright 2014 International Business Machines Corp.
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

import java.util.Date;
import java.util.List;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobInstance;

import com.ibm.jbatch.container.persistence.jpa.JobExecutionEntity;

/**
 * Additional JobInstance fields used by WAS.
 */
public interface WSJobInstance extends JobInstance {

    /**
     * @return the userId who submitted the job.
     */
    String getSubmitter();

    /**
     * @return the application/module/component name associated with the job.
     */
    String getAmcName();

    /**
     * @return the job's batch status
     */
    BatchStatus getBatchStatus();

    /**
     * @return the job's JSL file name.
     */
    String getJobXMLName();

    /**
     * @return the entire JSL as a String
     */
    String getJobXml();

    /**
     * @return instance state.
     */
    InstanceState getInstanceState();

    /**
     * @return last updated time stamp
     */
    Date getLastUpdatedTime();

    /**
     * @return the job executions associated with this instance, sorted by most to least recent
     */
    List<JobExecutionEntity> getJobExecutions();
}
