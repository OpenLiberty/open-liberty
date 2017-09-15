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

import java.io.Serializable;

import javax.batch.runtime.BatchStatus;
/**
 * The AbstractPartitionAnalyzer provides default 
 * implementations of less commonly implemented methods.
 */
public abstract class AbstractPartitionAnalyzer implements PartitionAnalyzer {
	/**
	 * Override this method to analyze PartitionCollector payloads.
	 * 
	 * @param data specifies the payload sent by the
	 * PartitionCollector. 
	 * @throws Exception is thrown if an error occurs. 
	 */
	@Override
	public void analyzeCollectorData(Serializable data) throws Exception {}
	/**
	 * Override this method to analyze partition end status.
	 * @param batchStatus specifies the batch status of a partition.
	 * @param exitStatus specifies the exit status of a partition. 
	 * @throws Exception is thrown if an error occurs.  
	 */
	@Override
	public void analyzeStatus(BatchStatus batchStatus, String exitStatus)
			throws Exception {}
}
