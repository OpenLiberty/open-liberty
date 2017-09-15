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

package javax.batch.api.chunk;

/**
 * The AbstractCheckpointAlgorithm provides default 
 * implementations of less commonly implemented
 * methods.
 */
public abstract class AbstractCheckpointAlgorithm implements
		CheckpointAlgorithm {
	/**
	 * Override this method if the CheckpointAlgorithm
	 * establishes a checkpoint timeout.   
	 * The default implementation returns 0, which means
	 * the maximum permissible timeout allowed by the
	 * runtime environment.  
	 * 
	 * @return the timeout interval (expressed in seconds) 
	 * to use for the next checkpoint interval 
	 * @throws Exception (or subclass) if an error occurs. 
	 */
	@Override
	public int checkpointTimeout() throws Exception {
		return 0;
	}
	/**
	 * Override this method for the CheckpointAlgorithm 
	 * to do something before a checkpoint interval 
	 * begins (before the next chunk transaction begins). 
	 * The default implementation does nothing.   
	 * 
	 * @throws Exception (or subclass) if an error occurs.
	 */	
	@Override
	public void beginCheckpoint() throws Exception {}
	/**
	 * Implement logic in this method
	 * to decide if a checkpoint should be taken now. 
	 *    
	 * @return boolean indicating whether or not 
	 * to checkpoint now. 
	 * @throws Exception (or subclass) if an error occurs.
	 */
	@Override
	public abstract boolean isReadyToCheckpoint() throws Exception;
	/**
	 * Override this method for the CheckpointAlgorithm 
	 * to do something after a checkpoint is taken (after
	 * the chunk transaction is committed). 
	 * The default implementation does nothing.   
	 * 
	 * @throws Exception (or subclass) if an error occurs.
	 */
	@Override
	public void endCheckpoint() throws Exception {}
}
