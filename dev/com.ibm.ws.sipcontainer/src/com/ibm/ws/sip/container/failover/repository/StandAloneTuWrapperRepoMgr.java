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

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

public class StandAloneTuWrapperRepoMgr implements TuWrapperRepository
{
	/**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(StandAloneTuWrapperRepoMgr.class);
	
	private Hashtable<String, TransactionUserWrapper> m_wrappers = new Hashtable<String, TransactionUserWrapper>(19);
	
	public TransactionUserWrapper get(String sessionId) {
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "get",sessionId);
        }
		TransactionUserWrapper result  = m_wrappers.get(sessionId); 
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "get",result);
        }
		return result;
	}

	
	public TransactionUserWrapper put(String sipSessionId,
			TransactionUserWrapper tu) {
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "put",sipSessionId);
        }
		return m_wrappers.put(sipSessionId,tu);
	}

	
	public TransactionUserWrapper remove(String sipSessionId) {
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "remove",sipSessionId);
        }
		return m_wrappers.remove(sipSessionId);
	}

	/**
	 * @return a list of all the known TransactionUserWrappers
	 */
	public List getAll() {
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "getAll");
        }

		//TODO: locking an sync
		if (m_wrappers.isEmpty()) {
			return Collections.EMPTY_LIST;
		}
		List<TransactionUserWrapper> result = 
			new ArrayList<TransactionUserWrapper>(m_wrappers.size());
		// Moti: 21Apr2008 : while looking in the logs of defect 513136 
		// I noticed a ConcurrentModificationException , so I removed the
		// older iterator we had here.
		result.addAll(m_wrappers.values());
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "getAll",result.size());
        }
		return result;
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
