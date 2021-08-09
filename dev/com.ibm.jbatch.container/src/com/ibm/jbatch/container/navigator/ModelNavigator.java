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
package com.ibm.jbatch.container.navigator;

import com.ibm.jbatch.container.status.ExecutionStatus;
import com.ibm.jbatch.jsl.model.helper.ExecutionElement;
import com.ibm.jbatch.jsl.model.helper.Transition;

public interface ModelNavigator<T> {

	/**
	 * 
	 * @param restartOn
	 * @return
	 * @throws IllegalTransitionException 
	 */
    public ExecutionElement getFirstExecutionElement(String restartOn) throws IllegalTransitionException;
    
	/**
	 * 
	 * @return
	 * @throws IllegalTransitionException 
	 */
    public ExecutionElement getFirstExecutionElement() throws IllegalTransitionException;
    
	/**
	 * Enforces "can't revisit already visited steps rule". 
	 */
    public Transition getNextTransition(ExecutionElement currentExecutionElem, ExecutionStatus currentExecutionStatus) 
    		throws IllegalTransitionException;
    
    /**
     * E.g. the JSLJob for a job, the Flow for a flow, etc.
     * @return
     */
    public T getRootModelElement();

}
