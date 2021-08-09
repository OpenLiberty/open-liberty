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
import java.util.List;
/**
 * RetryWriteListener intercepts retry processing for
 * an ItemWriter. 
 *
 */
public interface RetryWriteListener {
	/**
	 * The onRetryWriteException method receives control when a 
	 * retryable exception is thrown from an ItemWriter writeItems
	 * method. This method receives the exception and the list of items 
	 * being written as inputs.  
	 * This method receives control in same checkpoint scope as the 
	 * ItemWriter. If this method throws a an exception, the job ends 
	 * in the FAILED state. 
	 * @param items specify the items passed to an item writer.
	 * @param ex specifies the exception thrown by an item
	 * writer.
	 * @throws Exception is thrown if an error occurs.
	 */
	public void onRetryWriteException(List<Object> items, Exception ex) throws Exception;
}
