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
import com.ibm.ws.sip.container.failover.repository.SASRepository;
import com.ibm.ws.sip.container.failover.repository.StandAloneSASRepoMgr;

/**
 * This class knows how to create a SASRepository implementation based on 
 * current WAS configuration. Currently it supports non-WAS env,  DRS service enabled or
 * ObjectGrid service enabled.
 * The order of trials: first try Object Grid then DRS and if all fails try standalone.
 * @author mordechai
 * @update Moti: 11 Nov added JDBC constants to object grid
 *
 */
public class SASRepositoryFactoryImpl  extends SASRepositoryFactory
{
	/**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(SASRepositoryFactoryImpl.class);
    
    /**
     * 
     * @return some SASRepository concrete class or null if not found such. 
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public SASRepository createRepository() 
    throws ClassNotFoundException,InstantiationException, IllegalAccessException
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "createRepository", "Standalone type");
		}
		return new StandAloneSASRepoMgr();

    }
	
}
