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

package javax.batch.api.partition;


/**
 * The AbstractBatchlet provides default 
 * implementations of less commonly implemented methods.
 */
public abstract class AbstractPartitionReducer implements PartitionReducer {
	/**
	 * Override this method to take action before 
	 * partitioned step processing begins. 
	 * 
	 * @throws Exception is thrown if an error occurs. 
	 */
	@Override
	public void beginPartitionedStep() throws Exception {}
	/**
	 * Override this method to take action before 
	 * normal partitioned step processing ends. 
	 * 
	 * @throws Exception is thrown if an error occurs. 
	 */
	@Override
	public void beforePartitionedStepCompletion() throws Exception {}
	/**
	 * Override this method to take action when a  
	 * partitioned step is rolling back.  
	 * 
	 * @throws Exception is thrown if an error occurs. 
	 */
	@Override
	public void rollbackPartitionedStep() throws Exception {}
	/**
	 * Override this method to take action after 
	 * partitioned step processing ends. 
	 *
	 * @param status specifies the outcome of the partitioned step. 
	 * Values are "COMMIT" or "ROLLBACK".  
	 * @throws Exception is thrown if an error occurs. 
	 */
	@Override
	public void afterPartitionedStepCompletion(PartitionStatus status)
			throws Exception {}

}
