/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.failover.repository.ctor;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.failover.repository.StandAloneTuBaseRepoMgr;
import com.ibm.ws.sip.container.failover.repository.TuBaseRepository;

/**
 * 
 * @author mordechai
 * @update Moti: 11 Nov added JDBC constants to object grid and support new/old objectgrid impl
 */
public class TuBaseRepoFactoryImpl extends  TuBaseRepoFactory{
	/**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(TuBaseRepoFactoryImpl.class);
    
	public TuBaseRepository createRepository() 
	throws ClassNotFoundException,InstantiationException, IllegalAccessException
	{
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "createRepository", "Standalone type");
		}
		return new StandAloneTuBaseRepoMgr();
		
	}
}
