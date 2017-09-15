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
 * The AbstractItemWriteListener provides default 
 * implementations of less commonly implemented methods.
 */
public abstract class AbstractItemWriteListener implements
		ItemWriteListener {
    /**
	 * Override this method if the ItemWriteListener
	 * will do something before the items are written.  
	 * The default implementation does nothing. 
	 *
	 * @param items specifies the items about to be 
	 * written. 
	 * @throws Exception (or subclass) if an error occurs.
	 */	
	@Override
	public void beforeWrite(List<Object> items) throws Exception {}
    /**
	 * Override this method if the ItemWriteListener
	 * will do something after the items are written.  
	 * The default implementation does nothing. 
	 *
	 * @param items specifies the items about to be 
	 * written. 
	 * @throws Exception (or subclass) if an error occurs.
	 */	
	@Override
	public void afterWrite(List<Object> items) throws Exception {}
    /**
	 * Override this method if the ItemWriteListener
	 * will do something when the ItemWriter writeItems
	 * method throws an exception.  
	 * The default implementation does nothing.
	 *
	 * @param items specifies the items about to be 
	 * written.
	 * @param ex specifies the exception thrown by the item 
	 * writer.
	 * @throws Exception (or subclass) if an error occurs.
	 */	
	@Override
	public void onWriteError(List<Object> items, Exception ex) throws Exception {}
}
