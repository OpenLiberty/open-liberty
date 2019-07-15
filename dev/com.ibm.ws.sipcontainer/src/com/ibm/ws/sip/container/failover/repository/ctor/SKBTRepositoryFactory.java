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
import com.ibm.ws.sip.container.failover.repository.SKBTRepository;
import com.ibm.ws.sip.container.failover.repository.StandAloneSKBTMgr;

public class SKBTRepositoryFactory {
	/**
	 * Class Logger.
	 */
	private static final LogMgr c_logger = Log.get(SKBTRepositoryFactory.class);	


	public SKBTRepository createRepository() throws ClassNotFoundException,InstantiationException, IllegalAccessException 
	{
		/*
		if(FailoverMgrLoader.isUsingObjectGridv2()) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createRepository", "ObjectGrid type");
			}
			String className = PropertiesStore.getInstance().getProperties().getString(HAProperties.SAS_OGv2_NAME);
			Class c = Thread.currentThread().getContextClassLoader().loadClass(className);
			return (SKBTRepository)c.newInstance();
		} else if(FailoverMgrLoader.isUsingJDBC()) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createRepository", "JDBC type");
			}
			String className = PropertiesStore.getInstance().getProperties().getString(HAProperties.SAS_JDBC_NAME);
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createRepository", "loaded :" + className);
			}			
			
			Class c = Thread.currentThread().getContextClassLoader().loadClass(className);
			return (SKBTRepository)c.newInstance();
		} else 
		if(FailoverMgrLoader.isUsingDRS() || FailoverMgrLoader.isUsingObjectGrid()) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createRepository", "DRS type");
			}
			//Class helper = com.ibm.ws.sip.container.servlets.SASDRSReplicationHelper.class;
			//SipApplicationSessionImpl.setReplicationHelperClassName(helper);
			return new StandAloneSKBTMgr();
		} else {*/
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createRepository", "Standalone type");
			}
			return new StandAloneSKBTMgr();
//		}
	}
	
}
