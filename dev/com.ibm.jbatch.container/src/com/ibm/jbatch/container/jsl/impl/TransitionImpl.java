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
package com.ibm.jbatch.container.jsl.impl;

import com.ibm.jbatch.jsl.model.helper.ExecutionElement;
import com.ibm.jbatch.jsl.model.helper.Transition;
import com.ibm.jbatch.jsl.model.helper.TransitionElement;

public class TransitionImpl implements Transition {

    private TransitionElement transitionElement;
    private ExecutionElement executionElement;
    
    boolean finishedTransitioning = false; 
    boolean noTransitionElementMatchedAfterException = false; 
    
	public TransitionImpl() {
        super();
    }

    @Override
    public TransitionElement getTransitionElement() {
        return transitionElement;
    }

    @Override
    public ExecutionElement getNextExecutionElement() {
        return executionElement;
    }
    
    @Override
    public void setTransitionElement(TransitionElement transitionElement) {
        this.transitionElement = transitionElement;
    }
    
    @Override
    public void setNextExecutionElement(ExecutionElement executionElement) {
        this.executionElement = executionElement;
    }    
    
    @Override
    public boolean isFinishedTransitioning() {
		return finishedTransitioning;
	}

    @Override
	public void setFinishedTransitioning() {
		this.finishedTransitioning = true;
	}
    
    @Override
	public void setNoTransitionElementMatchAfterException() {
		this.noTransitionElementMatchedAfterException = true;
    }
    
    @Override
	public boolean noTransitionElementMatchedAfterException() {
		return noTransitionElementMatchedAfterException;
    }

}
