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
import java.util.Properties;

/**
 * Provides a view of a job execution to the JobOperator.
 */
public interface JobExecution {
	/**
	 * Get unique id for this JobExecution.
	 * @return execution id
	 */
	public long getExecutionId();
	/**
	 * Get job name.
	 * @return value of 'id' attribute from <job>
	 */
	public String getJobName(); 
	/**
	 * Get batch status of this execution.
	 * @return batch status value.
	 */
	public BatchStatus getBatchStatus();
	/**
	 * Get time execution entered STARTED status. 
	 * @return date (time)
	 */
	public Date getStartTime();
	/**
	 * Get time execution entered end status: COMPLETED, STOPPED, FAILED.
	 * @return date (time)
	 */
	public Date getEndTime();
	/**
	 * Get execution exit status.
	 * @return exit status.
	 */
	public String getExitStatus();
	/**
	 * Get time execution was created.
	 * @return date (time)
	 */
	public Date getCreateTime();
	/**
	 * Get time execution was last updated updated.
	 * @return date (time)
	 */
	public Date getLastUpdatedTime();
	/**
	 * Get job parameters for this execution.
	 * @return job parameters  
	 */
	public Properties getJobParameters();
	
}
