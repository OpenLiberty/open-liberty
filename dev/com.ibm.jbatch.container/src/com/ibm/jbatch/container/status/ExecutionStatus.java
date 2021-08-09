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
package com.ibm.jbatch.container.status;

public class ExecutionStatus {
	boolean batchStatusOnly;
	private ExtendedBatchStatus extendedBatchStatus;
	private String exitStatus;
	private String restartOn;
	
	// Makes it more explicit that this is not holding an exit status rather than the idea
	// that there should logically be an exit status whose value happens to be 'null'.
	public boolean isBatchStatusOnly() {
		return batchStatusOnly;
	}

	public ExecutionStatus() {
		this.batchStatusOnly = true;
	}
	
	public ExecutionStatus(ExtendedBatchStatus extendedBatchStatus) {
		this();
		this.extendedBatchStatus = extendedBatchStatus;
	}
	
	public ExecutionStatus(ExtendedBatchStatus extendedBatchStatus, String exitStatus) {
		super();
		this.extendedBatchStatus = extendedBatchStatus;
		this.exitStatus = exitStatus;
		this.batchStatusOnly = false;
	}
	
	public ExtendedBatchStatus getExtendedBatchStatus() {
		return extendedBatchStatus;
	}
	
	public void setExtendedBatchStatus(ExtendedBatchStatus extendedBatchStatus) {
		this.extendedBatchStatus = extendedBatchStatus;
	}
	
	public String getExitStatus() {
		return exitStatus;
	}
	
	public void setExitStatus(String exitStatus) {
		this.exitStatus = exitStatus;
		this.batchStatusOnly = false;
	}
	
	public String getRestartOn() {
		return restartOn;
	}

	public void setRestartOn(String restartOn) {
		this.restartOn = restartOn;
	}
	
	@Override
	public String toString() {
		return "BatchStatusOnly?: " + batchStatusOnly + ", extendedBatchStatus = " + (extendedBatchStatus == null ? "<null>" : extendedBatchStatus.name()) +
				", exitStatus = " + exitStatus + 
			    ", restartOn = " + restartOn;	
	}
}
