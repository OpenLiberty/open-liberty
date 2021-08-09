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

package javax.batch.api.chunk.listener;

/**
 * ItemProcessListener intercepts item processing. 
 *
 */
public interface ItemProcessListener {
	
	/**
	 * The beforeProcess method receives control before 
	 * an item processor is called to process the next item.  
	 * The method receives the item to be processed as an input.  
	 * @param item specifies the item about to be processed. 
	 * @throws Exception if an error occurs.
	 */
	public void beforeProcess(Object item) throws Exception;
	/**
	 * The afterProcess method receives control after an item 
	 * processor processes an item.  The method receives the item processed 
	 * and the result item as an input.
	 * @param item specifies the item processed by the item processor.
	 * @param result specifies the item to pass to the item writer. 
	 * @throws Exception if an error occurs.
	 */
	public void afterProcess(Object item, Object result) throws Exception;
	/**
	 * The onProcessError method receives control after an 
	 * item processor processItem throws an exception.  The method 
	 * receives the item sent to the item processor as input. 
	 * @param item specifies the item the processor attempted to process.
	 * @param ex specifies the exception thrown by the item processor.
	 * @throws Exception if an error occurs
	 */
	public void onProcessError(Object item, Exception ex) throws Exception;
	
}
