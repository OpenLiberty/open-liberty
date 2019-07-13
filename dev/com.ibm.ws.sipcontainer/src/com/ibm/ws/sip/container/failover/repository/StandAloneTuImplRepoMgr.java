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

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.tu.TransactionUserImpl;

public class StandAloneTuImplRepoMgr implements TuImplRepository {
	/**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(StandAloneTuImplRepoMgr.class);
    
    /**
     * key = sip session id (String)
     * value = transaction impl instance
     */
	private Hashtable<String, TransactionUserImpl> m_sessionsTbl 
		= new Hashtable<String, TransactionUserImpl>();

	public TransactionUserImpl get(String sessionId) {
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "get",new Object[] {sessionId});
        }

		return m_sessionsTbl.get(sessionId);
	}

	public TransactionUserImpl put(String sessionId, TransactionUserImpl tuImpl) {
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "put",new Object[] {sessionId,tuImpl});
        }
		return m_sessionsTbl.put(sessionId,tuImpl);

	}

	public TransactionUserImpl remove(TransactionUserImpl impl) {
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "remove",new Object[] {impl});
        }
		return m_sessionsTbl.remove(impl.getWrapper().getId());
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
