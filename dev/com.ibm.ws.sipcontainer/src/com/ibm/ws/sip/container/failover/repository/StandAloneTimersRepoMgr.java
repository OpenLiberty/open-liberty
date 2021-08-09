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

import javax.servlet.sip.SipApplicationSession;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.timer.BaseTimer;

public class StandAloneTimersRepoMgr implements TimerRepository {

	/**
	 * Class Logger.
	 */
	private static final LogMgr c_logger = Log.get(StandAloneTimersRepoMgr.class);

	/**
	 * key = app session id
	 * value = a hashmap of timers connected to that Sip Application Session.
	 *  pairs of (timerID, ServletTimer)
	 */
	private Hashtable<String, HashMap> m_sessionsTbl = null;


	public BaseTimer get(String appSessionId, Integer timerId) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "get",new Object[] {appSessionId,timerId});
		}
		BaseTimer baseTimer = null;
		Hashtable<String, HashMap> sessionsTbl = getSessionsTbl(false);
		if (sessionsTbl != null) {
			HashMap attributes = sessionsTbl.get(appSessionId);
			if (attributes != null) {
				baseTimer = (BaseTimer)attributes.get(timerId);
			}
		}
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "get", baseTimer);
		}
		return baseTimer;
	}

	public BaseTimer put(SipApplicationSession sipAppSession, BaseTimer timer) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "put",new Object[] {sipAppSession,timer});
		}

		Hashtable<String, HashMap> sessionsTbl = getSessionsTbl(true);
		HashMap<Object, BaseTimer> timers = sessionsTbl.get(sipAppSession.getId());
		if (timers == null) {
			timers = new HashMap<Object, BaseTimer>();
			sessionsTbl.put(sipAppSession.getId(), timers);
		}
		return timers.put(timer.getTimerId(),timer);
	}

	public BaseTimer remove(String appSessionId, Integer timerId) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "remove",new Object[] {appSessionId,timerId});
		}
		BaseTimer removed = null;
		Hashtable<String, HashMap> sessionsTbl = getSessionsTbl(false);
		if (sessionsTbl != null) {
			HashMap<Integer, BaseTimer> attributes = sessionsTbl.get(appSessionId);
			if (attributes != null) {
				removed = attributes.remove(timerId);
				if (attributes.isEmpty()) {
					sessionsTbl.remove(appSessionId);
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
