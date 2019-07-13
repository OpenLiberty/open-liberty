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

import javax.servlet.sip.SipApplicationSession;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

public class StandAloneSASAttrRepoMgr implements SASAttrRepository {

	/**
	 * Class Logger.
	 */
	private static final LogMgr c_logger = Log.get(StandAloneSASAttrRepoMgr.class);

	/**
	 * key = app session id
	 * value = a hashmap of attributes connected to that Sip Application Session
	 */
	private Hashtable<String, HashMap> m_sessionsTbl = null;

	public Object get(SipApplicationSession appSession, String name) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "get",new Object[] {appSession,name});
		}
		Object object = null;
		Hashtable<String, HashMap> sessionsTbl = getSessionsTbl(false);
		if (sessionsTbl != null) {
			HashMap attributes = sessionsTbl.get(appSession.getId());
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
	 * Retrieves a map of all attributes related to a specific Sip application Session
	 * @param sessionId - Sip App session Id we are looking for.
	 * @return all attributes related to the Sip Application Session
	 */
	public Map getAttributes(String sessionId)
	{
		Hashtable<String, HashMap> sessionsTbl = getSessionsTbl(false);
		if (sessionsTbl == null) {
			return null;
		}
		return sessionsTbl.get(sessionId); 
	}

	/**
	 * @see SASAttrRepository#put(SipApplicationSession, String, Object)
	 */
	public Object put(SipApplicationSession appSession, String name, Object value) 
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "put",new Object[] {appSession,name,value});
		}
		Hashtable<String, HashMap> sessionsTbl = getSessionsTbl(true);
		HashMap<String, Object> attributes = sessionsTbl.get(appSession.getId());
		if (attributes == null) {
			attributes = new HashMap<String, Object>();
			sessionsTbl.put(appSession.getId(), attributes);
		}
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "put",sessionsTbl.size());
		}
		return attributes.put(name,value);
	}
	

	public Object remove(SipApplicationSession appSession, String name) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "remove",new Object[] {appSession,name});
		}
		Object removed = null;
		Hashtable<String, HashMap> sessionsTbl = getSessionsTbl(false);
		if (sessionsTbl != null) {
			HashMap<String, Object> attributes = sessionsTbl.get(appSession.getId());
			if (attributes != null) {
				removed = attributes.remove(name);
				if (attributes.isEmpty()) {
					m_sessionsTbl.remove(appSession.getId());
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

	/**
	 * no transactional support in standalone mode.
	 * Method has empty implementation.
	 */
	public Object beginTx() {
		return null;
	}

	/**
	 * no transactional support in standalone mode.
	 * Method has empty implementation.
	 */
	public Object commitTx(Object txKey) {
		return null;
	}

	/**
	 * no transactional support in standalone mode.
	 * Method has empty implementation.
	 */
	public Object rollback(Object txKey) {
		return null;
	}

}
