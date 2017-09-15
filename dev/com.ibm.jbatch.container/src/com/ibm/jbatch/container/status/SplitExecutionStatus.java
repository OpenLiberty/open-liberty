/**
 * Copyright 2013 International Business Machines Corp.
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

public class SplitExecutionStatus extends ExecutionStatus {
	
	boolean couldMoreThanOneFlowHaveTerminatedJob = false;
	
	public SplitExecutionStatus() {
		super();
	}
	
	public SplitExecutionStatus(ExtendedBatchStatus extendedBatchStatus) {
		super(extendedBatchStatus);
	}

	public boolean couldMoreThanOneFlowHaveTerminatedJob() {
		return couldMoreThanOneFlowHaveTerminatedJob;
	}

	public void setCouldMoreThanOneFlowHaveTerminatedJob(boolean flag) {
		this.couldMoreThanOneFlowHaveTerminatedJob = flag;
	}
	
	@Override
	/* Splits don't have a meaningful exit status at the split level, since we don't elevate
	 * one flow's exit status above another.
	 */
	public String getExitStatus() {
		return null;
	}
	
	@Override
	public String toString() {
		return super.toString() + ", couldMoreThanOneFlowHaveTerminatedJob = " + couldMoreThanOneFlowHaveTerminatedJob; 
	}

}
