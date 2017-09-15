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
package com.ibm.jbatch.container.instance;

import java.util.Properties;

import javax.batch.runtime.BatchStatus;

import com.ibm.jbatch.container.ws.TopLevelNameInstanceExecutionInfo;


public interface WorkUnitDescriptor {
	
	public enum WorkUnitType {
		TOP_LEVEL_JOB,
		PARTITIONED_STEP,
		SPLIT_FLOW
	}

	public abstract String getExitStatus();

	public abstract void setExitStatus(String exitStatus);

	public abstract BatchStatus getBatchStatus();
	
	public abstract String getTopLevelJobName();

	public abstract long getTopLevelInstanceId();

	public abstract long getTopLevelExecutionId();

	public abstract Properties getTopLevelJobProperties();

	public abstract TopLevelNameInstanceExecutionInfo getTopLevelNameInstanceExecutionInfo();
	
	public abstract WorkUnitType getWorkUnitType();

	public abstract String getFlowName();

	public abstract String getSplitName();

	public abstract String getPartitionedStepName();

	public abstract Integer getPartitionNumber();
	
	public abstract boolean isRemotePartitionDispatch();

	/**
	 * @param logDirPath
	 */
	public abstract void updateExecutionJobLogDir(String logDirPath);
	
	public abstract String getCorrelationId();
	

}