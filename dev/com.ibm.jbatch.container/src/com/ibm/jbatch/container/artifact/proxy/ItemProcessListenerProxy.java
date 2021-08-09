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

import javax.batch.api.chunk.listener.ItemProcessListener;

import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;

public class ItemProcessListenerProxy extends AbstractProxy<ItemProcessListener> implements ItemProcessListener {

    ItemProcessListenerProxy(ItemProcessListener delegate) {
        super(delegate);

    }

    @Override
    public void afterProcess(Object item, Object result) {
        try {
            this.delegate.afterProcess(item, result);
        } catch (Exception e) {
        	this.stepContext.setException(e);
            throw new BatchContainerRuntimeException(e);
        }
        

    }

    @Override
    public void beforeProcess(Object item) {
        try {
            this.delegate.beforeProcess(item);
        } catch (Exception e) {
        	this.stepContext.setException(e);
            throw new BatchContainerRuntimeException(e);
        }
    }

    @Override
    public void onProcessError(Object item, Exception ex) {
        try {
            this.delegate.onProcessError(item, ex);
        } catch (Exception e) {
        	this.stepContext.setException(e);
            throw new BatchContainerRuntimeException(e);
        }
    }

}
