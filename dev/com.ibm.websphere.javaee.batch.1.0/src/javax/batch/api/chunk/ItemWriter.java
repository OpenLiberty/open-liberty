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
import java.util.List;

/**
 * 
 * ItemWriter defines the batch artifact that writes to a 
 * list of items for chunk processing.
 *
 */

public interface ItemWriter {

	/**
	 * The open method prepares the writer to write items.
	 * 
	 * The input parameter represents the last checkpoint
	 * for this writer in a given job instance. The  
	 * checkpoint data is defined by this writer and is 
	 * provided by the checkpointInfo method. The checkpoint
	 * data provides the writer whatever information it needs 
	 * to resume writing items upon restart. A checkpoint value 
	 * of null is passed upon initial start.
	 * 
	 * @param checkpoint specifies the last checkpoint 
	 * @throws Exception is thrown for any errors.
	 */
	public void open(Serializable checkpoint) throws Exception;
	/**
	 * The close method marks the end of use of the 
	 * ItemWriter. The writer is free to do any cleanup 
	 * necessary.
	 * @throws Exception is thrown for any errors.
	 */
	public void close() throws Exception;
	
	/**
	 * The writeItems method writes a list of item
	 * for the current chunk. 
	 * @param items specifies the list of items to write.
	 * This may be an empty list (e.g. if all the
	 * items have been filtered out by the 
	 * ItemProcessor).
	 * @throws Exception is thrown for any errors.
	 */
	public void writeItems(List<Object> items) throws Exception;
	
	/**
	 * The checkpointInfo method returns the current 
	 * checkpoint data for this writer. It is 
	 * called before a chunk checkpoint is committed.  
	 * @return checkpoint data
	 * @throws Exception is thrown for any errors.
	 */
	public Serializable checkpointInfo() throws Exception; 

}
