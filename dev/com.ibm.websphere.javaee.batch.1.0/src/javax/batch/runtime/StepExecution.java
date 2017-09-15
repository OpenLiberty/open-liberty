/*
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

package javax.batch.runtime;

import java.util.Date;
import java.io.Serializable;

/**
 * Provides a view of a step execution to the JobOperator.
 */
public interface StepExecution {
	/**
	 * Get unique id for this StepExecution.
	 * @return StepExecution id 
	 */
	public long getStepExecutionId();
	/**
	 * Get step name.
	 * @return value of 'id' attribute from <step>
	 */
	public String getStepName();	
	/**
	 * Get batch status of this step execution.
	 * @return batch status.
	 */
	public BatchStatus getBatchStatus();
	/**
	 * Get time this step started.
	 * @return date (time)
	 */
	public Date getStartTime();
	/**
	 * Get time this step ended.
	 * @return date (time)
	 */
	public Date getEndTime();
	/**
	 * Get exit status of step.
	 * @return exit status
	 */
	public String getExitStatus();
	/**
	 * Get persistent user data.
	 * <p>
	 * For a partitioned step, this returns
	 * the persistent user data of the 
	 * <code>StepContext</code> of the "top-level"
	 * or main thread (the one the <code>PartitionAnalyzer</code>, etc.
	 * execute on).   It does not return the persistent user
	 * data of the partition threads. 
	 * @return persistent data 
	 */
	public Serializable getPersistentUserData();
	/**
	 * Get step metrics.
	 * @return array of metrics 
	 */
	public Metric[] getMetrics();
}
