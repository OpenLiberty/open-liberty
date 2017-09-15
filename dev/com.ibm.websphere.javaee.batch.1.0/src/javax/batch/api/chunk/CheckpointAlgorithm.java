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
 * CheckpointAlgorithm provides a custom checkpoint
 * policy for chunk steps. 
 *
 */
public interface CheckpointAlgorithm {

	/**
	 * The checkpointTimeout is invoked at the beginning of a new 
	 * checkpoint interval for the purpose of establishing the checkpoint 
	 * timeout.
	 * It is invoked before the next chunk transaction begins. This 
	 * method returns an integer value, which is the timeout value 
	 * (expressed in seconds) which will be used for the next chunk
	 * transaction.  
	 * This method is useful to automate the setting of the 
	 * checkpoint timeout based on factors known outside the job 	
	 * definition. 
	 * A value of '0' signifies no maximum established by this 
	 * CheckpointAlgorithm, i.e. the maximum permissible timeout allowed by 
	 * the runtime environment.
	 * @return the timeout interval (expressed in seconds) 
	 * to use for the next checkpoint interval 
	 * @throws Exception thrown for any errors.
	 */	
	public int checkpointTimeout() throws Exception;
	/**
	 * The beginCheckpoint method is invoked before the 
	 * next checkpoint interval begins (before the next
	 * chunk transaction begins).
	 * @throws Exception thrown for any errors.
	 */
	public void beginCheckpoint() throws Exception;
	/**
	 * The isReadyToCheckpoint method is invoked by 
	 * the batch runtime after each item is processed 
	 * to determine if now is the time to checkpoint 
	 * the current chunk.  
	 * @return boolean indicating whether or not 
	 * to checkpoint now.
	 * @throws Exception thrown for any errors.
	 */
	public boolean isReadyToCheckpoint() throws Exception;
	/**
	 * The endCheckpoint method is invoked after the   
	 * last checkpoint is taken (after the chunk
	 * transaction is committed).
	 * @throws Exception thrown for any errors.
	 */
	public void endCheckpoint() throws Exception;
	
}
