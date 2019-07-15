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
import com.ibm.ws.sip.container.failover.repository.SASAttrRepository;
import com.ibm.ws.sip.container.failover.repository.StandAloneSASAttrRepoMgr;

/**
 * A factory class for SASAttrRepositoryMgr.
 * It uses reflection to load the relevant implementation class (except for a special case:
 * standalone case)
 * @author mordechai
 * @update Moti: 11 Nov added JDBC constants to object grid
 *
 */
public class SASAttrRepositoryFactoryImpl extends SASAttrRepositoryFactory
{
	/**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(SASAttrRepositoryFactoryImpl.class);

    /**
     * The class to load in case of Object grid support
     */
	
    /**
     * Tries to dynamically load a class which implements some repository interface.
     * @return a Sip application session attribute manager
     * @throws ClassNotFoundException - when failed to find the implementation class by name
     * @throws InstantiationException - succeeded to find the class but failed to create an instance
     * @throws IllegalAccessException - found the class but security violation occured.
     */
	public SASAttrRepository createRepository() 
		throws ClassNotFoundException,InstantiationException, IllegalAccessException
	{
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "createRepository", "Standalone type");
		}
		return new StandAloneSASAttrRepoMgr();
		
	}

}

