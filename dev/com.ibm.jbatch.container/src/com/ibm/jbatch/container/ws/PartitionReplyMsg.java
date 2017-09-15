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
package com.ibm.jbatch.container.ws;

import java.io.Serializable;

import javax.batch.runtime.BatchStatus;

/**
 * These messages are sent by sub-job partition threads back to the top-level thread
 * via the PartitionReplyQueue.
 * 
 */
public class PartitionReplyMsg implements Serializable {
    
    /**
     * default.
     */
    private static final long serialVersionUID = 1L;

    private byte[] collectorData;

    private BatchStatus batchStatus;
    
    private String exitStatus;
    
    private PartitionReplyMsgType msgType;
    
    /**
     * The partition plan for the partition that created this msg.
     */
    private PartitionPlanConfig partitionPlanConfig;

    
    // We don't send PARTITION_THREAD_COMPLETE any more. It's only there for legacy.  
    public enum PartitionReplyMsgType { PARTITION_COLLECTOR_DATA, PARTITION_FINAL_STATUS, PARTITION_THREAD_COMPLETE }
    
    public PartitionReplyMsg( PartitionReplyMsgType msgType ) {
        this.msgType = msgType;
    }
    
    public BatchStatus getBatchStatus() {
        return batchStatus;
    }

    public PartitionReplyMsg setBatchStatus(BatchStatus batchStatus) {
        this.batchStatus = batchStatus;
        return this;
    }

    public String getExitStatus() {
        return exitStatus;
    }

    public PartitionReplyMsg setExitStatus(String exitStatus) {
        this.exitStatus = exitStatus;
        return this;
    }

    public byte[] getCollectorData() {
        return collectorData;
    }

    public PartitionReplyMsg setCollectorData(byte[] collectorData) {
        this.collectorData = collectorData;
        return this;
    }

    public PartitionReplyMsgType getMsgType() {
        return msgType;
    }
    
    public String toString() {
    	StringBuilder buf = new StringBuilder();
    	buf.append("PartitionReplyMsg: ");
    	if (partitionPlanConfig == null) {
    		buf.append("plan not initialized; ");
    	} else {
    		buf.append("partition #: " + partitionPlanConfig.getPartitionNumber());
    		buf.append("exec info: " + partitionPlanConfig.getTopLevelNameInstanceExecutionInfo());
    	}
    	buf.append(", " + getMsgType());
    	buf.append(", " + getBatchStatus());
    	buf.append(", " + getExitStatus());
    	return buf.toString();
    }

    public PartitionReplyMsg setPartitionPlanConfig(PartitionPlanConfig partitionPlanConfig) {
        this.partitionPlanConfig = partitionPlanConfig;
        return this;
    }
    
    public PartitionPlanConfig getPartitionPlanConfig() {
        return partitionPlanConfig;
    }
    
}
