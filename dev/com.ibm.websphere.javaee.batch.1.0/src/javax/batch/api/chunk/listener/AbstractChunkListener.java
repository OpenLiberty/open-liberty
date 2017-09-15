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
 * The AbstractChunkListener provides default 
 * implementations of less commonly implemented methods.
 */
public abstract class AbstractChunkListener implements ChunkListener {
	/**
	 * Override this method if the ChunkListener
	 * will do something before the chunk begins.  
	 * The default implementation does nothing. 
	 * 
	 * @throws Exception (or subclass) if an error occurs.
	 */
	@Override
	public void beforeChunk() throws Exception {}
	/**
	 * Override this method if the ChunkListener will do
	 * something before the chunk transaction is rolled back. 
	 * Note afterChunk is not invoked in this case.
	 * @param ex specifies the exception that
	 * caused the roll back.  
	 * @throws Exception (or subclass) throw if an error occurs.
	 */
	@Override
	public void onError(Exception ex) throws Exception {}
	/**
	 * Override this method if the ChunkListener
	 * will do something after the chunk ends.  
	 * The default implementation does nothing. 
	 * 
	 * @throws Exception (or subclass) if an error occurs.
	 */
	@Override
	public void afterChunk() throws Exception {}
}
