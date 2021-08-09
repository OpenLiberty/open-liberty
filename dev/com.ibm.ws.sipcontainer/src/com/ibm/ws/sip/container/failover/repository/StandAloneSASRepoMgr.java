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
package com.ibm.ws.sip.container.failover.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.sip.SipApplicationSession;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;

public class StandAloneSASRepoMgr implements SASRepository {


	/**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(StandAloneSASRepoMgr.class);
	
	private Hashtable<String, SipApplicationSession> m_appSessions = new Hashtable<String, SipApplicationSession>(19);
	
	public SipApplicationSession get(String appSessionId) {
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "get",new Object[] {appSessionId});
        }

		return m_appSessions.get(appSessionId);
	}
	
	public SipApplicationSession put(String appSessionId,
			SipApplicationSession appsession) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "put",new Object[] {appSessionId,appsession});
		}
		//special fix for PMR: 24291,004,000
		SipApplicationSessionImpl ourapp =(SipApplicationSessionImpl)appsession;
		if (ourapp.isDuringInvalidate() || !ourapp.isValid()) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "put",appSessionId + " was invalidated. request ignored");
			}
			return null;
		}

		return m_appSessions.put(appSessionId,appsession);
	}
	
	public SipApplicationSession remove(SipApplicationSession appSession) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this,"remove",appSession.getId()); 
		}
		return m_appSessions.remove(appSession.getId());
	}

	public Object beginTx() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object commitTx(Object txKey) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object rollback(Object txKey) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public List getAll() {
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "getAll");
        }

		if (m_appSessions.isEmpty()) {
			return Collections.EMPTY_LIST;
		}
		List<SipApplicationSession> result = 
			new ArrayList<SipApplicationSession>(m_appSessions.size());
		
		synchronized (m_appSessions) {
			result.addAll(m_appSessions.values());
		}
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "getAll",result.size());
        }
		return result;
	}

}
