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

import javax.batch.runtime.BatchStatus;

import com.ibm.jbatch.container.callback.IJobExecutionEndCallbackService;
import com.ibm.jbatch.container.callback.IJobExecutionStartCallbackService;
import com.ibm.jbatch.container.controller.impl.WorkUnitThreadControllerImpl;
import com.ibm.jbatch.container.execution.impl.RuntimePartitionExecution;
import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.ws.PartitionPlanConfig;
import com.ibm.jbatch.container.ws.PartitionReplyMsg;
import com.ibm.jbatch.container.ws.PartitionReplyMsg.PartitionReplyMsgType;
import com.ibm.jbatch.container.ws.PartitionReplyQueue;

/** 
 * The Runnable work unit for sub-job partition threads.
 */
public class BatchPartitionWorkUnit extends BatchWorkUnit {
    
    /**
     * Config details for the partition, including exec IDs, partition number, props, etc.
     */
    private PartitionPlanConfig partitionPlanConfig;
    
    /**
     * The queue by which the sub-job partition threads sends msgs back to the top-level thread.
     */
    private PartitionReplyQueue partitionReplyQueue;

    /**
     * CTOR.
     */
	public BatchPartitionWorkUnit(IBatchKernelService batchKernelService,
			                      RuntimePartitionExecution runtimePartitionExecution,
			                      PartitionPlanConfig config,
			                      List<IJobExecutionStartCallbackService> beforeCallbacks, 
			                      List<IJobExecutionEndCallbackService> afterCallbacks,
			                      PartitionReplyQueue partitionReplyQueue) {
		super(batchKernelService, runtimePartitionExecution, beforeCallbacks, afterCallbacks, true);
		this.partitionReplyQueue = partitionReplyQueue;
		this.partitionPlanConfig = config;
		this.controller = new WorkUnitThreadControllerImpl(runtimePartitionExecution, partitionReplyQueue);
	}

	/**
	 * This method is (basically) the last thing the sub-job partition thread does before ending.
	 * 
	 * It sends the "partition thread complete" message back to the top-level thread.
	 * 
	 * Then it closes the partitionReplyQueue.  
	 * 
	 * In the case where the partition thread and top-level thread are running in the same JVM,
	 * then the partitionReplyQueue is an instance of PartitionReplyQueueLocal, and the close
	 * does nothing.  
	 * 
	 * In the case where the partition thread and top-level thread are running in separate JVMs
	 * (multi-JVM mode), then it's an instance of PartitionReplyQueueJms, and the close() call
	 * closes the JMS connection that was used to send msgs back to the top-level thread.
	 * 
	 * Note: in the top-level thread, the queue is closed in PartitionedStepControllerImpl.invokeCoreStep.
	 * 
	 */
	@Override
	protected void markThreadCompleted() {
		super.markThreadCompleted();
		boolean finalStatusSent = ((RuntimePartitionExecution) getRuntimeWorkUnitExecution()).isFinalStatusSent();
		
		if(!finalStatusSent){
			try{
				// We only need to send Failed FINAL_STATUS message here if there was an exception and the message was not sent previously
				partitionReplyQueue.add( new PartitionReplyMsg( PartitionReplyMsgType.PARTITION_FINAL_STATUS )
											.setBatchStatus( BatchStatus.FAILED)
											.setExitStatus(BatchStatus.FAILED.toString())
											.setPartitionPlanConfig( partitionPlanConfig ) );

			}
			catch(Exception e){
				//Just ffdc it. 
			}
			finally{
				partitionReplyQueue.close();
			}
		}
	}

	/**
	 * @return stringified partition info
	 */
	@Override 
	public String toString() {
		
		if (partitionPlanConfig == null) {
			return "PartitionWorkUnit <not initialized>";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("PartitionWorkUnit with ");
		sb.append("jobExecutionId =" + partitionPlanConfig.getTopLevelExecutionId());
		sb.append(",stepName =" + partitionPlanConfig.getStepName());
		sb.append(",partitionNumber =" + partitionPlanConfig.getPartitionNumber());
		return sb.toString();
	}

}
