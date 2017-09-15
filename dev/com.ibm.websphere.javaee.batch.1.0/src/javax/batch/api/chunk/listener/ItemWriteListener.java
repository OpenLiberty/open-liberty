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
 * ItemWriteListener intercepts item writer
 * processing. 
 *
 */
public interface ItemWriteListener {

	/**
	 * The beforeWrite method receives control before 
	 * an item writer is called to write its items.  The 
	 * method receives the list of items sent to the item 
	 * writer as an input.
	 * @param items specifies the items about to be 
	 * written.
	 * @throws Exception is thrown if an error occurs.
	 */
	public void beforeWrite(List<Object> items) throws Exception;
	/**
	 * The afterWrite method receives control after an 
	 * item writer writes its items.  The method receives the 
	 * list of items sent to the item writer as an input.  
	 * @param items specifies the items written by the item writer.
	 * @throws Exception is thrown if an error occurs.
	 */
	public void afterWrite(List<Object> items) throws Exception;
	
	/**
	 * The onWriteError method receives control after an 
	 * item writer writeItems throws an exception.  The method 
	 * receives the list of items sent to the item writer as input. 
	 * @param items specifies the items which the item writer
	 * attempted to write.
	 * @param ex specifies the exception thrown by the item 
	 * writer.
	 * @throws Exception is thrown if an error occurs.
	 */
	public void onWriteError(List<Object> items, Exception ex) throws Exception;
}
