/*
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.spi.services.IBatchConfig;
import com.ibm.jbatch.spi.services.IBatchThreadPoolService;

public abstract class AbstractThreadPoolServiceImpl implements IBatchThreadPoolService {
	
	private final static String sourceClass = AbstractThreadPoolServiceImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);
	
	protected ExecutorService executorService;
	
	public AbstractThreadPoolServiceImpl() {
		if(logger.isLoggable(Level.FINE)) { 
			logger.fine("Instantiating instance of thread pool impl: " + this.getClass().getCanonicalName());
		}
	}
	
	public abstract void init(IBatchConfig pgcConfig) throws BatchContainerServiceException;

	public void shutdown() throws BatchContainerServiceException {
		String method = "shutdown";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);	}
		
		executorService = null;
		
		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);	}
		
	}

	public Future<?> executeTask(Runnable work, Object config) {
		String method = "executeTask";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);	}
		
		Future<?> retVal = executorService.submit(work);
		
		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);	}
		return retVal;
	}

    public Future<?> executeParallelTask(Runnable work, Object config) {
        String method = "executeParallelTask";
        if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);  }
        
        Future<?> retVal =  executorService.submit(work);
        
        if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);   }
        return retVal;
    }



	
}
