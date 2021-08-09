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

import java.util.List;

import javax.batch.api.chunk.listener.SkipWriteListener;

import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;

public class SkipWriteListenerProxy extends AbstractProxy<SkipWriteListener> implements SkipWriteListener {
	
	SkipWriteListenerProxy(SkipWriteListener delegate) { 
        super(delegate);

    }
	
	@Override
    public void onSkipWriteItem(List items, Exception ex) {
        try {
            this.delegate.onSkipWriteItem(items, ex);
        } catch (Exception e) {
        	this.stepContext.setException(e);
            throw new BatchContainerRuntimeException(e);
        }
    }

}
