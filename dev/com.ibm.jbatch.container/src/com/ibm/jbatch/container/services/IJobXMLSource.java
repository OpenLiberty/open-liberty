/*
 * Copyright 2014 International Business Machines Corp.
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
package com.ibm.jbatch.container.services;

import javax.xml.transform.stream.StreamSource;

/**
 * Allows us to capture some of the benefits of carrying around 
 * the JSL as both a StreamSource with an id, and a raw String.
 */
public interface IJobXMLSource {

	/**
	 * Can only be called one time, since a StreamSource
	 * can only be used one time.
	 */
	public StreamSource getJSLStreamSource();

	/**
	 * Can be called more than once, after the StreamSource
	 * is consumed.
	 */
	public String getJSLString(); 
	
}
