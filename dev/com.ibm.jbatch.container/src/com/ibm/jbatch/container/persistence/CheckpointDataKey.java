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
package com.ibm.jbatch.container.persistence;

public class CheckpointDataKey {
	
	private long _jobInstanceId;
	private String _stepName;
	
	public CheckpointDataKey(long jobId, String stepName) {
		this._jobInstanceId = jobId;
		this._stepName = stepName;
	}
	
	public long getJobInstanceId() {
		return _jobInstanceId;
	}
	
	public String getStepName() {
		return _stepName;
	}
	
	public String getCommaSeparatedKey() {
		return stringify();
	}

	public String toString() {
		return stringify();
	}
	
	// Note that the use of xs:id schema type forbids ',' in attribute values,
	// so there's no ambiguity here.
	private String stringify() {
		return _jobInstanceId + "," + _stepName;
	}
}
