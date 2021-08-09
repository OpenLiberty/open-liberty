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

import java.io.Serializable;
import java.util.Properties;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;

/**
  * 
  * A StepContext provides information about the current step 
  * of a job execution.
  * 
  */

public interface StepContext {	
	/**
	 * Get step name.
	 * @return value of 'id' attribute from <step>
	 * 
	 */	
	public String getStepName();
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
	 * The getStepExecutionId method returns the current step's 
     * execution id.  
	 * @return step execution id 
	 */
	public long getStepExecutionId();
	/**
	 * The getProperties method returns the step level properties
	 * specified in a job definition.
	 * <p>
	 * A couple notes:
	 * <ul>
	 * <li> There is no guarantee that the same Properties object instance is 
	 * always returned in the same (step) scope. 
	 * <li> Besides the properties which are defined in JSL within a child 
	 * &lt;properties&gt; element of a &lt;step&gt; element, the batch runtime 
	 * implementation may choose to include additional, implementation-defined 
	 * properties.
	 * </ul>
	 * @return step level properties 
	 */
	public Properties getProperties();
	/**
	 * The getPersistentUserData method returns a persistent data object 
	 * belonging to the current step. The user data type must implement 
	 * java.util.Serializable. This data is saved as part of a step's 
	 * checkpoint. For a step that does not do checkpoints, it is saved 
	 * after the step ends. It is available upon restart. 
	 * @return user-specified type
	 */
	public Serializable getPersistentUserData();
	/**
	 * The setPersistentUserData method stores a persistent data object 
	 * into the current step. The user data type must implement 
	 * java.util.Serializable. This data is saved as part of a step's 
	 * checkpoint. For a step that does not do checkpoints, it is saved 
	 * after the step ends. It is available upon restart. 
	 * @param data is the user-specified type
	 */	
	public void setPersistentUserData(Serializable data);
	/**
	 * The getBatchStatus method returns the current batch status of the 
	 * current step. This value is set by the batch runtime and changes as 
	 * the batch status changes.
	 * @return batch status string
	 */
	public BatchStatus getBatchStatus();
	/**
	 * The getExitStatus method simply returns the exit status value stored 
	 * into the step context through the setExitStatus method or null. 
	 * @return exit status string
	 */	
	public String getExitStatus();
	/**
	 * The setExitStatus method assigns the user-specified exit status for 
	 * the current step. When the step ends, the exit status of the step is 
	 * the value specified through setExitStatus. If setExitStatus was not 
	 * called or was called with a null value, then the exit status 
	 * defaults to the batch status of the step. 
	 * @param status string 
	 */
	public void setExitStatus(String status);
	/**
	 * The getException method returns the last exception thrown from a 
	 * step level batch artifact to the batch runtime.
	 * @return the last exception
	 */
	public Exception getException();
	/**
	 * The getMetrics method returns an array of step level metrics. These 
	 * are things like commits, skips, etc. 
	 * @see javax.batch.runtime.metric.Metric for definition of standard 
	 * metrics.  
	 * @return metrics array 
	 */
	public Metric[] getMetrics();
}
