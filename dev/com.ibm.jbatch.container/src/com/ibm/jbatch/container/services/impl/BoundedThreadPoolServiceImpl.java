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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.util.BatchContainerConstants;
import com.ibm.jbatch.spi.services.IBatchConfig;

public class BoundedThreadPoolServiceImpl extends AbstractThreadPoolServiceImpl implements BatchContainerConstants {

	private final static String sourceClass = BoundedThreadPoolServiceImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);
	
	public BoundedThreadPoolServiceImpl() {
		super();
	}
	
	private String defaultMaxThreadPoolSize = "5";
	int idleThreadTimeout = 900; //threadPoolConfig.getIdleThreadTimeout();
	int maxQueueSize = 4096; //threadPoolConfig.getMaxQueueSize();
	int minThreadPoolSize = 3; // threadPoolConfig.getMinThreadPoolSize();
	
	@Override
	public void init(IBatchConfig batchConfig)
			throws BatchContainerServiceException {
		
		String method = "init";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);	}
		
		String maxThreadPoolSizeStr = batchConfig.getConfigProperties().
				getProperty(BOUNDED_THREADPOOL_MAX_POOL_SIZE, defaultMaxThreadPoolSize); 
		
		int maxThreadPoolSize = Integer.parseInt(maxThreadPoolSizeStr);
		
		if(logger.isLoggable(Level.FINE)) { 
			logger.fine("Glassfish thread pool settings: (" + minThreadPoolSize + "," +  maxThreadPoolSize + "," 
		                 + idleThreadTimeout + "," + TimeUnit.SECONDS + "," + maxQueueSize + ")");  
		}
		
		BlockingQueue<Runnable> queue = null;
		if (maxQueueSize == -1) {
			queue = new LinkedBlockingQueue<Runnable>();
		} else {
			queue = new LinkedBlockingQueue<Runnable>(maxQueueSize);
		}
		
		executorService = new ThreadPoolExecutor(minThreadPoolSize, maxThreadPoolSize, idleThreadTimeout, TimeUnit.SECONDS, queue);  
		
		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);	}
	}



}
