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

import java.util.Properties;

/**
 * The PartitionPlanImpl class provides a basic implementation
 * of the PartitionPlan interface. 
 */
public class PartitionPlanImpl implements PartitionPlan {

	private int partitions= 0;
	private boolean override= false; 
	private int threads= 0;
	Properties[] partitionProperties= null;  
	
	@Override
	public void setPartitions(int count) {
		partitions= count;
		// default thread count to partition count 
		if (threads == 0) threads= count; 
	}

	@Override
	public void setThreads(int count) {
		threads= count; 		
	}

	@Override
	public void setPartitionsOverride(boolean override) {
		this.override= override; 
		
	}

	@Override
	public boolean getPartitionsOverride() {
		return override;
	}
	
	@Override
	public void setPartitionProperties(Properties[] props) {
		partitionProperties= props;		
	}

	@Override
	public int getPartitions() {
		return partitions;
	}

	@Override
	public int getThreads() {
		return threads;
	}

	@Override
	public Properties[] getPartitionProperties() {
		return partitionProperties; 
	}

}
