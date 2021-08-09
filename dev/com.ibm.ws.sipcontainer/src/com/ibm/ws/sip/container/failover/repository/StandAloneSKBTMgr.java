/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.failover.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.tu.SessionKeyBase;

public class StandAloneSKBTMgr implements SKBTRepository {
	

	/**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(StandAloneSKBTMgr.class);

    
    /**
     * local hash map that will store references from session key based keys to
     * sip application session id's.
     */
	private Map<String, SessionKeyBase> m_appKeyBaseSessions = Collections.synchronizedMap(new HashMap<String, SessionKeyBase>(19));
	
	
	/**
	 * @see SKBTRepository#get(String)
	 */
	public SessionKeyBase get(String keyBaseSession) {
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "get",new Object[] {keyBaseSession});
        }

		return m_appKeyBaseSessions.get(keyBaseSession);
	}

	
	/**
	 * @see SKBTRepository#put(String, SessionKeyBase)
	 */	
	public SessionKeyBase put(String keyBaseSession, SessionKeyBase appSessionId) {
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "put",new Object[] {keyBaseSession, appSessionId});
        }
		return m_appKeyBaseSessions.put(keyBaseSession, appSessionId);
	}

	
	/**
	 * @see SKBTRepository#remove(String)
	 */	
	public SessionKeyBase remove(String keyBaseSession) {
		return m_appKeyBaseSessions.remove(keyBaseSession);
	}

	/**
	 * @see SKBTRepository#getAll()
	 */
	public Collection<SessionKeyBase> getAll() {
		return m_appKeyBaseSessions.values();
	}
	
	/**
	 * @see SKBTRepository#beginTx()
	 */	
	public Object beginTx() {
		return null;
	}
	
	/**
	 * @see SKBTRepository#commitTx(Object)
	 */
	public Object commitTx(Object txKey) {
		return null;
	}

	/**
	 * @see SKBTRepository#rollback(Object)
	 */	
	public Object rollback(Object txKey) {
		return null;
	}
}
