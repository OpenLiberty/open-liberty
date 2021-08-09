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
 * The AbstractItemReadListener provides default 
 * implementations of less commonly implemented methods.
 */
public abstract class AbstractItemReadListener implements
		ItemReadListener {
	/**
	 * Override this method if the ItemReadListener
	 * will do something before the item is read.  
	 * The default implementation does nothing. 
	 * 
	 * @throws Exception (or subclass) if an error occurs.
	 */	
	@Override
	public void beforeRead() throws Exception {}
	/**
	 * Override this method if the ItemReadListener
	 * will do something after the item is read.  
	 * The default implementation does nothing. 
	 * 
	 * @param item last item read by ItemReader
	 * @throws Exception (or subclass) if an error occurs.
	 */	
	@Override
	public void afterRead(Object item) throws Exception {}
	/**
	 * Override this method if the ItemReadListener
	 * will do something when the ItemReader readItem
	 * method throws an exception.  
	 * The default implementation does nothing. 
	 * 
	 * @param ex exception thrown from readItem method
	 * @throws Exception (or subclass) if an error occurs.
	 */	
	@Override
	public void onReadError(Exception ex) throws Exception {}
}
