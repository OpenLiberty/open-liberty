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
 * SkipReadListener intercepts skippable 
 * itemReader exception handling. 
 */
public interface SkipReadListener {
	/**
	 * The onSkipReadItem method receives control 
	 * when a skippable exception is thrown from an 
	 * ItemReader readItem method. This method receives the 
	 * exception as an input. 
	 * @param ex specifies the exception thrown by the ItemReader.
	 * @throws Exception is thrown if an error occurs.
	 */
	public void onSkipReadItem(Exception ex) throws Exception;	
}
