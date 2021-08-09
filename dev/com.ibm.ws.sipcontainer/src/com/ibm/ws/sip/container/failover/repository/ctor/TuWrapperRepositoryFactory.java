/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.failover.repository.ctor;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.failover.repository.StandAloneTuWrapperRepoMgr;
import com.ibm.ws.sip.container.failover.repository.TuWrapperRepository;

/**
 * since TuWrappers are not replicated yet , we will only use this interface for uniformity.
 * Tu Wrappers will be, for now,  in-memory hashtable.
 * @author mordechai
 *
 */
public class TuWrapperRepositoryFactory {
	/**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(TuWrapperRepositoryFactory.class);
    
	
    public static TuWrapperRepository createRepository()
    {
    	// Transaction user wrapper is not replicated therefore, we create it in -memory
    	if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceDebug("TuWrapperRepositoryFactory", "createRepository", "Standalone type");
    	}
    	return new StandAloneTuWrapperRepoMgr();
    }
}
