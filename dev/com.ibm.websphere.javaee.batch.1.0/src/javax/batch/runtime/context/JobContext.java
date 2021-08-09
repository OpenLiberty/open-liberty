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

package javax.batch.runtime.context;

import java.util.Properties;
import javax.batch.runtime.BatchStatus;

/**
  * 
  * A JobContext provides information about the current  
  * job execution.
  *
  */
public interface JobContext  {
	
	/**
	 * Get job name.
	 * @return value of 'id' attribute from <job>
	 */
	public String getJobName();
	/**
	 * The getTransientUserData method returns a transient data object 
	 * belonging to the current Job XML execution element. 
	 * @return user-specified type
	 */
	public Object getTransientUserData();
	/**
	 * The setTransientUserData method stores a transient data object into 
	 * the current batch context. 
	 * @param data is the user-specified type
	 */
	public void setTransientUserData(Object data);

	/**
	 * The getInstanceId method returns the current job's instance 
       * id.  
	 * @return job instance id 
	 */
	public long getInstanceId();
	/**
	 * The getExecutionId method returns the current job's current    
       * execution id.  
	 * @return job execution id 
	 */

	public long getExecutionId();

	/**
	 * The getProperties method returns the job level properties
	 * specified in a job definition.  
	 * <p>
	 * A couple notes:
	 * <ul>
	 * <li> There is no guarantee that the same Properties object instance 
	 * is always returned in the same (job) scope. 
	 * <li> Besides the properties which are defined in JSL within a child 
	 * &lt;properties&gt; element of a &lt;job&gt; element, the batch runtime 
	 * implementation may choose to	include additional, 
	 * implementation-defined properties.
	 * </ul>
	 * 
	 * @return job level properties 
	 */
	public Properties getProperties();

	/**
	 * The getBatchStatus method simply returns the batch status value	 	 
	 * set by the batch runtime into the job context. 
	 * @return batch status string
	 */
	public BatchStatus getBatchStatus();
	/**
	 * The getExitStatus method simply returns the exit status value stored 
	 * into the job context through the setExitStatus method or null. 
	 * @return exit status string
	 */	
	public String getExitStatus();
	/**
	 * The setExitStatus method assigns the user-specified exit status for 
	 * the current job. When the job ends, the exit status of the job is 
	 * the value specified through setExitStatus. If setExitStatus was not 
	 * called or was called with a null value, then the exit status 
	 * defaults to the batch status of the job. 
	 * @param status exit status string 
	 */
	public void setExitStatus(String status);
}
