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
 * ChunkListener intercepts chunk processing. 
 *
 */
public interface ChunkListener {
	/**
	 * The beforeChunk method receives control
	 * before processing of the next 
	 * chunk begins. This method is invoked
	 * in the same transaction as the chunk
	 * processing. 
	 * @throws Exception throw if an error occurs.
	 */
	public void beforeChunk() throws Exception;
	/**
	 * The onError method receives control
	 * before the chunk transaction is rolled back. 
	 * Note afterChunk is not invoked in this case.
	 * @param ex specifies the exception that
	 * caused the roll back. 
	 * @throws Exception throw if an error occurs.
	 */
	public void onError(Exception ex) throws Exception;
	/**
	 * The afterChunk method receives control
	 * after processing of the current 
	 * chunk ends. This method is invoked
	 * in the same transaction as the chunk
	 * processing.  
	 * @throws Exception throw if an error occurs.
	 */
	public void afterChunk() throws Exception;

}
