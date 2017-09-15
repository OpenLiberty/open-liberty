/**
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
package com.ibm.jbatch.container.util;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.ibm.jbatch.container.callback.IJobExecutionEndCallbackService;
import com.ibm.jbatch.container.callback.IJobExecutionStartCallbackService;
import com.ibm.jbatch.container.execution.impl.RuntimeSplitFlowExecution;
import com.ibm.jbatch.container.services.IBatchKernelService;

public class BatchSplitFlowWorkUnit extends BatchWorkUnit {

	public BatchSplitFlowWorkUnit(IBatchKernelService batchKernelService,
			RuntimeSplitFlowExecution runtimeFlowInSplitExecution,
			BlockingQueue<BatchSplitFlowWorkUnit> completedThreadQueue,
			List<IJobExecutionStartCallbackService> beforeCallbacks, 
			List<IJobExecutionEndCallbackService> afterCallbacks) {
		super(batchKernelService, runtimeFlowInSplitExecution, beforeCallbacks, afterCallbacks, true);
		this.completedThreadQueue = completedThreadQueue;
		this.runtimeFlowInSplitExecution = runtimeFlowInSplitExecution;
	}

	private final RuntimeSplitFlowExecution runtimeFlowInSplitExecution;

	private final BlockingQueue<BatchSplitFlowWorkUnit> completedThreadQueue;

	public BlockingQueue<BatchSplitFlowWorkUnit> getCompletedThreadQueue() {
		return completedThreadQueue;
	}

	@Override
	protected void markThreadCompleted() {
		super.markThreadCompleted();
		if (this.completedThreadQueue != null) {
			completedThreadQueue.add(this);
		}
	}

	@Override
	public RuntimeSplitFlowExecution getRuntimeWorkUnitExecution() {
		return runtimeFlowInSplitExecution;
	}

	@Override 
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("BatchSplitFlowWorkUnit with ");
		if (runtimeFlowInSplitExecution != null) {
			sb.append("jobExecutionId =" + runtimeFlowInSplitExecution.getTopLevelExecutionId());
			sb.append(",splitName =" + runtimeFlowInSplitExecution.getSplitName());
			sb.append(",flowName =" + runtimeFlowInSplitExecution.getFlowName());
		} else {
			sb.append("<Not initialized>");
		}
		return sb.toString();
	}

}

