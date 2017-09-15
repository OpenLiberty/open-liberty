/*
 * Copyright 2012,2013 International Business Machines Corp.
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

import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.spi.services.IBatchConfig;

@Component(configurationPolicy=ConfigurationPolicy.REQUIRE)
public class GrowableThreadPoolServiceImpl extends AbstractThreadPoolServiceImpl {
	
	private final static String sourceClass = GrowableThreadPoolServiceImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);
	
	public GrowableThreadPoolServiceImpl() {
		super();
	}
	
	public void init(IBatchConfig pgcConfig) throws BatchContainerServiceException {
		String method = "init";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);	}
		
		executorService = Executors.newCachedThreadPool();
		
		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);	}

	}


}
