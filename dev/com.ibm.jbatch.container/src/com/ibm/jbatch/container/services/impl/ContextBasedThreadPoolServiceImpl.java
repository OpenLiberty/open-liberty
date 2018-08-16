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
package com.ibm.jbatch.container.services.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.util.BatchContainerConstants;
import com.ibm.jbatch.spi.services.IBatchConfig;
import com.ibm.jbatch.spi.services.IBatchThreadPoolService;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;
import com.ibm.wsspi.threading.WSExecutorService;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class ContextBasedThreadPoolServiceImpl implements IBatchThreadPoolService, BatchContainerConstants  {

	private final static String sourceClass = ContextBasedThreadPoolServiceImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

	/**
     * The collection of contexts to capture under captureThreadContext.
     * Currently only security context is captured.
     */
    @SuppressWarnings("unchecked")
    private static final Map<String, ?>[] CapturedContexts = new Map[] { Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER, "com.ibm.ws.security.context.provider"),
    	Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER, "com.ibm.ws.javaee.metadata.context.provider"),
    	Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER, "com.ibm.ws.classloader.context.provider"),
	Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER,"com.ibm.ws.security.thread.zos.context.provider")};
	
	
    /**
     * Liberty executor
     */
    WSExecutorService execSvc;
    
    @Reference
    protected void setExecutorService(ExecutorService ref){
        this.execSvc = (WSExecutorService)ref;
    }

    
    /**
     * For capturing thread context.
     */
    private WSContextService contextService;

    /**
     * DS injection.
     */
    @Reference(target = "(service.pid=com.ibm.ws.context.manager)")
    protected void setContextService(WSContextService contextService) {
        this.contextService = contextService;
    }
	

	@Override
	public void init(IBatchConfig batchConfig) {
		//nothing to do here any more since DS will set our references for us when 
		//the component is activated
	}

	public void shutdown() throws BatchContainerServiceException {
		String method = "shutdown";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);	}

		// We don't want to be responsible for cleaning up.

		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);	}
	}

	public Future<?> executeTask(Runnable work, Object config) {
		String method = "executeTask";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);	}

		Future<?> retVal = executeWork(work,config);

		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);	}
		
		return retVal;
	}

	private Future<?> executeWork(Runnable work, Object config) {
		Runnable batchWork = createContextualProxy(work);
		return execSvc.submit(batchWork);
	}

	public Future<?> executeParallelTask(Runnable work, Object config) {
		String method = "executeParallelTask";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);  }

		Future<?> retVal = executeWork(work,config);

		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);   }
		
		return retVal;
	}


	
    /**
     * @param work The batch work unit that is to run. This is the actual job.
     * @return A proxy work unit that has all the necessary thread contexts from the parent thread.
     */
    private Runnable createContextualProxy(Runnable work) {
        	ThreadContextDescriptor tcDescriptor = contextService.captureThreadContext(new HashMap<String, String>(), CapturedContexts);
            return contextService.createContextualProxy(tcDescriptor, work, Runnable.class);
    }
	
}
