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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.sip.SipSession;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

public class StandAloneSSAttrRepoMgr implements SSAttrRepository {

	/**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(StandAloneSSAttrRepoMgr.class);
    
    /**
     * key = sip session id (String)
     * value = a hashmap - attributes connected to that Sip  Session
     */
	private Hashtable<String, HashMap> m_sessionsTbl = null;
	
	
	public Object get(SipSession session, String name) {
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "get",new Object[] {session,name});
        }
		Object object = null;
		Hashtable<String, HashMap> sessionsTbl = getSessionsTbl(false);
		if (sessionsTbl != null) {
			HashMap attributes = sessionsTbl.get(session.getId());
			if (attributes != null) {
				object = attributes.get(name);
			}
		}
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "get", object);
		}
		return object;
	}
	
	/**
	 * Retrieves a map of all attributes related to a specific Sip Session
	 * @param sessionId - Sip session Id we are looking for.
	 * @return all attributes related to a Sip Session
	 */
	public Map getAttributes(String sessionId)
	{
		Hashtable<String, HashMap> sessionsTbl = getSessionsTbl(false);
		Map result = sessionsTbl == null ? null : sessionsTbl.get(sessionId);
		if (c_logger.isTraceEntryExitEnabled()) {
			int size = -1;
			if (result != null) {
				size  =  result.size();
			}
            c_logger.traceExit(this, "getAttributes",new Object[] {sessionId, size});
        }
		return  result;
	}

	public Object put(SipSession session, String name, Object value) {
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "put",new Object[] {session,name,value});
        }

		Hashtable<String, HashMap> sessionsTbl = getSessionsTbl(true);
		HashMap<String, Object> attributes = sessionsTbl.get(session.getId());
		if (attributes == null) {
			attributes = new HashMap<String, Object>();
			sessionsTbl.put(session.getId(), attributes);
		}
		return attributes.put(name,value);
	}

	public Object remove(SipSession session, String name) {
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "remove",new Object[] {session,name});
        }
		Object removed = null;
		Hashtable<String, HashMap> sessionsTbl = getSessionsTbl(false);
		if (sessionsTbl != null) {
			HashMap<String, Object> attributes = sessionsTbl.get(session.getId());
			if (attributes != null) {
				removed = attributes.remove(name);
				if (attributes.isEmpty()) {
					sessionsTbl.remove(session.getId());
				}
			}
		}
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "remove", removed);
		}
		return removed;
	}

	/**
	 * gets the sessions table. instantiates it if modified for the first time
	 * @param create true to create if does not exist, false to just peek
	 * @return the sessions table. null if table not created and create=false.
	 */
	private Hashtable<String, HashMap> getSessionsTbl(boolean create) {
		Hashtable<String, HashMap> sessionsTbl = m_sessionsTbl;
		if (sessionsTbl == null && create) {
			synchronized (this) {
				if (m_sessionsTbl == null) {
					m_sessionsTbl = new Hashtable<String, HashMap>();
				}
			}
			sessionsTbl = m_sessionsTbl;
		}
		return sessionsTbl;
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

}
