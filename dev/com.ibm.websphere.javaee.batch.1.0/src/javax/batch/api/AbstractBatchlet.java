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
 * The AbstractBatchlet provides default 
 * implementations of less commonly implemented methods.
 */
public abstract class AbstractBatchlet implements Batchlet {
	/**
	 * Implement process logic for the Batchlet in this 
	 * method.
	 * 
	 * @return exit status string
	 * @throws Exception (or subclass) if an error occurs.
	 */
	@Override
	public abstract String process() throws Exception; 

	/**
	 * Override this method if the Batchlet will
	 * end in response to the JobOperator.stop() 
	 * operation. 
	 * The default implementation does nothing. 
	 * 
	 * @throws Exception (or subclass) if an error occurs.
	 */
	@Override
	public void stop() throws Exception {}
}
