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

import java.util.Hashtable;

import javax.servlet.sip.SipSession;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.servlets.SipSessionImplementation;

public class StandAloneSSRepoMgr implements SSRepository {

	/**
	 * Class Logger.
	 */
	private static final LogMgr c_logger = Log.get(StandAloneSSRepoMgr.class);

	/**
	 * key = sip session id (String)
	 * value = Sip  Session instance
	 */
	private Hashtable<String, SipSession> m_sessionsTbl 
	= new Hashtable<String, SipSession>();


	/**
	 * @see SSRepository#get(String)
	 */
	public SipSession get(String sessionId) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "get",sessionId);
		}

		return m_sessionsTbl.get(sessionId);
	}

	/**
	 * @see SSRepository#put(String, SipSession)
	 */
	public SipSession put(String sessionId,SipSession session) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "put",new Object[] {sessionId,session});
		}
		SipSessionImplementation impl = (SipSessionImplementation)session;
		return m_sessionsTbl.put(sessionId, session);
	}

	/**
	 * @see SSRepository#remove(SipSession)
	 */
	public SipSession remove(SipSession session) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "remove",session);
		}
		return m_sessionsTbl.remove(session.getId());
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
