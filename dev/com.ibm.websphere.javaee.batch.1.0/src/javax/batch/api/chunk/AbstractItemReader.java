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

import java.io.Serializable;

/**
 * The AbstractItemReader provides default implementations 
 * of less commonly implemented methods.
 *
 */
public abstract class AbstractItemReader implements ItemReader {
	/**
	 * Override this method if the ItemReader requires
	 * any open time processing.  
	 * The default implementation does nothing. 
	 * 
	 * @param checkpoint last checkpoint for this ItemReader - may be null.
	 * @throws Exception (or subclass) if an error occurs. 
	 */
	@Override
	public void open(Serializable checkpoint) throws Exception { }
	/**
	 * Override this method if the ItemReader requires
	 * any close time processing.   
	 * The default implementation does nothing. 
	 * 
	 * @throws Exception (or subclass) if an error occurs.
	 */
	@Override
	public void close() throws Exception { }
	/**
	 * Implement read logic for the ItemReader in this 
	 * method.
	 * 
	 * @return next item or null
	 * @throws Exception (or subclass) if an error occurs.
	 */
	@Override
	public abstract Object readItem() throws Exception;
	/**
	 * Override this method if the ItemReader supports 
	 * checkpoints.
	 * The default implementation returns null.  
	 * 
	 * @return checkpoint data 
	 * @throws Exception (or subclass) if an error occurs.
	 */
	@Override
	public Serializable checkpointInfo() throws Exception { 
		return null; 
	}
}
