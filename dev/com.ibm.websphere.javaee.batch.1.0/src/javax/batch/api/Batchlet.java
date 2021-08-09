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

package javax.batch.api;

/**
 * 
 * A batchlet is type of batch step
 * that can be used for any type of 
 * background processing that does not 
 * explicitly call for a chunk oriented 
 * approach. 
 * <p>
 * A well-behaved batchlet responds
 * to stop requests by implementing
 * the stop method. 
 *
 */
public interface Batchlet {
	
	/**
	 * The process method does the work
	 * of the batchlet. If this method
	 * throws an exception, the batchlet
	 * step ends with a batch status of 
	 * FAILED.
	 * @return exit status string  
	 * @throws Exception if an error occurs.  
	 */
	public String process() throws Exception;
	/**
	 * The stop method is invoked by the batch
	 * runtime as part of JobOperator.stop()
	 * method processing.  This method is invoked
	 * on a thread other than the thread on which
	 * the batchlet process method is running. 
	 * 
	 * @throws Exception if an error occurs.
	 */
	public void stop() throws Exception;  
}
