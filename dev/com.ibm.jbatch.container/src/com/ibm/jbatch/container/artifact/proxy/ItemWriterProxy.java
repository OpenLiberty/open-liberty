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

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.api.chunk.ItemWriter;

import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.ws.JoblogUtil;

public class ItemWriterProxy extends AbstractProxy<ItemWriter> implements ItemWriter {
	
	private static Logger logger = Logger.getLogger(ItemWriterProxy.class.getName());

    ItemWriterProxy(ItemWriter delegate) {     	
        super(delegate);
   }

    @Override
    public Serializable checkpointInfo() {
        
        try {
            return this.delegate.checkpointInfo();
        } catch (Exception e) {
        	this.stepContext.setException(e);
            throw new BatchContainerRuntimeException(e);
        }
    }

    @Override
    public void close() {
    	
        try {
            this.delegate.close();
        } catch (Exception e) {
        	this.stepContext.setException(e);
            throw new BatchContainerRuntimeException(e);
        }
        JoblogUtil.logToJobLogAndTraceOnly(Level.FINER, "item.writer.closed", new Object[]{this.stepContext.getStepName()}, logger);
    }

    @Override
    public void open(Serializable checkpoint) {
    	
        try {
            this.delegate.open(checkpoint);
        } catch (Exception e) {
        	this.stepContext.setException(e);
            throw new BatchContainerRuntimeException(e);
        }
        JoblogUtil.logToJobLogAndTraceOnly(Level.FINER, "item.writer.opened", new Object[]{this.stepContext.getStepName()}, logger);
    }

    
    /*
     * In order to provide skip/retry logic, these exceptions
     * are thrown as-is rather than beeing wrapped.
     * @see javax.batch.api.ItemReader#readItem()
     */
    @Override
    public void writeItems(List items) throws Exception{
            this.delegate.writeItems(items);
    }


}
