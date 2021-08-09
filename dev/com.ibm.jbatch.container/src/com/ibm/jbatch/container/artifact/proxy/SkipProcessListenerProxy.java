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
package com.ibm.jbatch.container.artifact.proxy;

import javax.batch.api.chunk.listener.SkipProcessListener;

import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;

public class SkipProcessListenerProxy extends AbstractProxy<SkipProcessListener> implements SkipProcessListener {
	
	SkipProcessListenerProxy(SkipProcessListener delegate) { 
        super(delegate);

    }
	
	@Override
    public void onSkipProcessItem(Object item, Exception ex) {
        try {
            this.delegate.onSkipProcessItem(item, ex);
        } catch (Exception e) {
        	this.stepContext.setException(e);
            throw new BatchContainerRuntimeException(e);
        }
    }

}

