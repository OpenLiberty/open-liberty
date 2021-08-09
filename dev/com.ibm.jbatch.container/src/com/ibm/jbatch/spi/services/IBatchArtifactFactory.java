/**
 * Copyright 2013 International Business Machines Corp.
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
	/**
	 * Loads an instance of a batch artifact referenced in a JSL job definition XML
	 * document.   By "batch artifact", we mean an object implementing some aspect of the
	 * batch programming model defined in JSR352, e.g. a batchlet, a job or step listener, etc.
	 * 
	 * <p> The artifacts will be reference in JSL, e.g.:
	 * 
	 * <pre>
	 * 
	 *  JSL document snippet:
	 *  ---------------------
	 *  {@literal <}batchlet ref="MyBatchlet"{@literal >} 
	 *  
	 *  Java code snippet: 
	 *  ------------------
	 *  {@literal @}Batchlet("MyBatchlet")
	 *  public class MyBatchletImpl
	 * 
	 * </pre>
	 * 
	 * <p>  No particular classloader scope is assumed by the interface, as the different 
	 * implementations will define behavior here. 
	 * 
	 */
package com.ibm.jbatch.spi.services;



public interface IBatchArtifactFactory extends IBatchServiceBase  {

	    /**
	     * @param batchId The value of a @ref attribute in a JSL element, e.g. 'batchlet' 
	     * 
	     * @return An object instance of the artifact. 
	     */
	    public Object load(String batchId);
}
