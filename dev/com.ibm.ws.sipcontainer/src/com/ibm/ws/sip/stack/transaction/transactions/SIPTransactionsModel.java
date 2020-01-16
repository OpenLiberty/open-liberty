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
package com.ibm.ws.sip.stack.transaction.transactions;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.TransactionDoesNotExistException;
import jain.protocol.ip.sip.header.CSeqHeader;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.message.Message;
import jain.protocol.ip.sip.message.Request;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.message.CancelRequest;
import com.ibm.ws.sip.stack.transaction.SIPTransactionConstants;
import com.ibm.ws.sip.stack.transaction.transactions.ct.SIPClientTranaction;
import com.ibm.ws.sip.stack.transaction.transactions.ct.SIPInviteClientTransactionImpl;
import com.ibm.ws.sip.stack.transaction.transactions.st.SIPInviteServerTransactionImpl;
import com.ibm.ws.sip.stack.transaction.transactions.st.SIPServerTransaction;

/**
 * singleton class that manages all transactions
 */
public class SIPTransactionsModel
{
	/** class Logger */
	private static final LogMgr s_logger = Log.get(SIPTransactionsModel.class);

	/** singleton instance */
	private static SIPTransactionsModel s_instance = new SIPTransactionsModel();

	/** initial table size */
	private final static int TRANSACTIONS_TABLE_BEGIN_NUMBER = 50;
	
	/** table of client transactions by assigned numeric id */
	private Map m_clientTransactionsById;

	/** table of client transactions by method+branch */
	private Map m_clientTransactionsByBranchMethodKey;

	/** table of server transactions by assigned numeric id */
	private Map m_serverTransactionsById;

	/** table of server transactions by method+branch */
	private Map m_serverTransactionsByBranchMethodKey;

	/** set of merged-request identifiers (rfc3261 8.2.2.2) */
	private Set m_mergedRequests;
	
	/**
	 * @return singleton instance
	 */
	public static SIPTransactionsModel instance() {
		return s_instance;
	}
	
	/**
	 * private constructor 
	 */
	private SIPTransactionsModel() {
		m_clientTransactionsById = new HashMap(TRANSACTIONS_TABLE_BEGIN_NUMBER);
		m_clientTransactionsByBranchMethodKey = new HashMap(TRANSACTIONS_TABLE_BEGIN_NUMBER);
		m_serverTransactionsById = new HashMap(TRANSACTIONS_TABLE_BEGIN_NUMBER);
		m_serverTransactionsByBranchMethodKey = new HashMap(TRANSACTIONS_TABLE_BEGIN_NUMBER);
		m_mergedRequests = new HashSet(TRANSACTIONS_TABLE_BEGIN_NUMBER);
	}

	/**
	 * finds client transaction by assigned numeric id
	 * @param transactionId the associated numeric id
	 * @return the corresponding client transaction
	 * @throws TransactionDoesNotExistException
	 */
	public synchronized SIPClientTranaction getClientTransaction(long transactionId)
		throws TransactionDoesNotExistException
	{
		SIPClientTranaction transaction = (SIPClientTranaction)m_clientTransactionsById.get(new Long(transactionId));
		if (transaction == null) {
			throw new TransactionDoesNotExistException("client transaction does not exist [" + transactionId + ']');
		}
		return transaction;
	}

	/**
	 * finds the client transaction given method+branch
	 * 
	 * @param key method+branch identifying the transaction
	 * @return the client transaction associated with given method+branch
	 */
	public synchronized SIPClientTranaction getClientTransaction(BranchMethodKey key) {
		return (SIPClientTranaction)m_clientTransactionsByBranchMethodKey.get(key);
	}

	/**
	 * finds server transaction by assigned numeric id
	 * @param transactionId the associated numeric id
	 * @return the corresponding server transaction
	 * @throws TransactionDoesNotExistException
	 */
	public synchronized SIPServerTransaction getServerTransaction(long transactionId)
			throws TransactionDoesNotExistException
	{
		SIPServerTransaction transaction = (SIPServerTransaction)m_serverTransactionsById.get(new Long(transactionId));
		if (transaction == null) {
			throw new TransactionDoesNotExistException("server transaction does not exist [" + transactionId + ']');
		}
		return transaction;
	}

	/**
	 * called when a request arrives from the network, to match a server
	 * transaction to this request per rfc 3261 section 17.2.3
	 * @param key method+branch identifying the transaction
	 * @param req the incoming request
	 */
	public synchronized SIPServerTransaction getServerTransaction(BranchMethodKey key, Request req) {
		SIPServerTransaction serverTransaction = (SIPServerTransaction)m_serverTransactionsByBranchMethodKey.get(key);
		if (serverTransaction == null) {
			return null;
		}
		// getting here means we seem to have a matching transaction,
		// but there are several more rules that need to be verified
		if (!serverTransaction.isRequestPartOfTransaction(req)) {
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug("Warning: server transaction [" + serverTransaction.toString()
					+ "] does not match request");
			}
			return null;
		}
		return serverTransaction;
	}

	/**
	 * called when a cancel request arrives from the network,
	 * to associate the CANCEL with the original INVITE 
	 * @param cancelReq the cancel request
	 */
	public void correlateCancelToInviteTransaction(CancelRequest cancelReq)
		throws SipParseException, TransactionDoesNotExistException
	{
		// find the matching INVITE transaction
		BranchMethodKey transactionId = createTransactionId(cancelReq, Request.INVITE);
		SIPInviteServerTransactionImpl st;
		synchronized (this) {
			Object transaction = m_serverTransactionsByBranchMethodKey.get(transactionId);
			if (transaction == null ||
				!(transaction instanceof SIPInviteServerTransactionImpl)) {
				// no matching INVITE found
				if (s_logger.isTraceDebugEnabled()) {
					s_logger.traceDebug(this, "correlateCancelToInviteTransaction",
						"Cannot match INVITE transaction to CANCEL request");
				}
				throw new TransactionDoesNotExistException(
					"no matching transaction found");
			}
			st = (SIPInviteServerTransactionImpl)transaction;
		}
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "correlateCancelToInviteTransaction",
				"successfully matched CANCEL to INVITE");
		}

		// point the cancel's transaction to the original invite
		cancelReq.setOriginInviteTransaction(st.getId());
	}
	
	/**
	 * finds the original INVITE transaction given a CANCEL request.
	 * used when sending outbound CANCEL to set a timer on the original INVITE transaction
	 * 
	 * @param cancelReq the CANCEL request
	 * @return the original INVITE transaction to be cancelled
	 * @throws SipParseException
	 */
	public SIPInviteClientTransactionImpl getInviteFromCancel(Request cancelReq)
		throws SipParseException
	{
		BranchMethodKey transactionId = createTransactionId(cancelReq, Request.INVITE);
		SIPInviteClientTransactionImpl ct;
		
		synchronized (this) {
			ct = (SIPInviteClientTransactionImpl)m_clientTransactionsByBranchMethodKey.get(transactionId);
		}
		return ct;
	}

	/**
	 * adds a server transaction
	 * @param st the server transaction to be added
	 */
	public synchronized void putServerTransaction(SIPServerTransaction st) {
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug("adding server transaction [" + st + ']');
		}
		m_serverTransactionsById.put(new Long(st.getId()), st);
		m_serverTransactionsByBranchMethodKey.put(st.getBranchMethodId(), st);
		
		MergedRequestKey sourceId = st.getMergedRequestKey();
		if (sourceId != null) {
			m_mergedRequests.add(sourceId);
		}
	}

	/**
	 * adds a client transaction
	 * @param ct the client transaction to be added
	 */
	public synchronized void putClientTransaction(SIPClientTranaction ct) {
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug("adding client transaction [" + ct + ']');
		}
		m_clientTransactionsById.put(new Long(ct.getId()), ct);
		m_clientTransactionsByBranchMethodKey.put(ct.getBranchMethodId(), ct);
	}

	/**
	 * removes a server transaction
	 * @param st the server transaction to be removed
	 */
	public synchronized void remove(SIPServerTransaction st) {
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug("removing server transaction [" + st + ']');
		}
		m_serverTransactionsById.remove(new Long(st.getId()));
		m_serverTransactionsByBranchMethodKey.remove(st.getBranchMethodId());
		
		MergedRequestKey sourceId = st.getMergedRequestKey();
		if (sourceId != null) {
			m_mergedRequests.remove(sourceId);
		}
	}

	/**
	 * removes a client transaction
	 * @param ct the client transaction to be removed
	 */
	public synchronized void remove(SIPClientTranaction ct) {
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug("removing client tranaction [" + ct + ']');
		}
		m_clientTransactionsById.remove(new Long(ct.getId()));
		m_clientTransactionsByBranchMethodKey.remove(ct.getBranchMethodId());
	}

	/**
	 * creates a unique transaction identifier per rfc3261 17.1.3 / 17.2.3
	 * based on a SIP message
	 * @param msg SIP message with a CSeq header
	 * @return the unique transaction identifier
	 * @throws SipParseException
	 */
	public BranchMethodKey createTransactionId(Message msg)
		throws SipParseException
	{
		CSeqHeader cseq = msg.getCSeqHeader();
		if (cseq == null) {
			throw new SipParseException("no CSeq header");
		}

		String method = cseq.getMethod();
		if (method == null) {
			throw new SipParseException("no method in CSeq header");
		}
		return createTransactionId(msg, method);
	}

	/**
	 * creates a unique transaction identifier per rfc3261 17.1.3 / 17.2.3
	 * based on a SIP message and method
	 * @param msg SIP message with cseq, via
	 * @param method method from CSeq header
	 * @return the unique transaction identifier
	 * @throws SipParseException
	 */
	private BranchMethodKey createTransactionId(Message msg, String method)
		throws SipParseException
	{
		ViaHeader via = (ViaHeader)msg.getHeader(ViaHeader.name, true);
		if (via == null) {
			throw new SipParseException("no Via header", null);
		}

		String branch = via.getBranch();
		if (branch == null || branch.length() == 0) {
			// 2543 did not mandate a branch in the via
			if (msg instanceof Request) {
				Request req = (Request)msg;
				branch = generateBranch(req);
			}
			else {
				throw new SipParseException("no branch in response", null);
			}
		}

		if (method.equals(Request.ACK)) {
			method = Request.INVITE;
		}
		BranchMethodKey key = new BranchMethodKey(method, branch, via.getHost(), via.getPort());
		return key;
	}

	/**
	 * genarates a temporary branch for a request that comes with no branch
	 * such as a request that comes from a 2543 element.
	 * 
	 * @param req the request message
	 * @return the new branch
	 * @throws SipParseException
	 */
	private String generateBranch(Request req) {
		String callId = req.getCallIdHeader().getCallId();
		String fromTag = req.getFromHeader().getTag();
		if (fromTag == null) {
			fromTag = "";
		}
		CSeqHeader cseq = req.getCSeqHeader();

		int hash = callId.hashCode()
			^ fromTag.hashCode()
			^ (int)cseq.getSequenceNumber();
		
		// return magic cookie + hash
		StringBuffer branch = new StringBuffer(
			SIPTransactionConstants.BRANCH_MAGIC_COOKIE_SIZE + 16);
		branch.append(SIPTransactionConstants.BRANCH_MAGIC_COOKIE);
		branch.append(hash);

		return branch.toString();
	}

	/**
	 * creates an identifier that is equal for all merged requests
	 * arriving in a UAS for the same source request, per rfc 3261 8.2.2.2
	 * 
	 * @param req the incoming request
	 * @return a key to the forked transaction
	 */
	public MergedRequestKey createMergedTransactionId(Request req) {
		String fromTag = req.getFromHeader().getTag();
		String callId = req.getCallIdHeader().getCallId();
		CSeqHeader cseq = req.getCSeqHeader();
		
		return new MergedRequestKey(fromTag, callId, cseq);
	}
	
	/**
	 * determines if a given request is one of several merged requests,
	 * per rfc 3261 8.2.2.2
	 * 
	 * @param sourceId identifier of the forked request
	 * @return true if given request is one of several merged requests
	 */
	public boolean isMergedServerTransaction(MergedRequestKey sourceId) {
		return m_mergedRequests.contains(sourceId);
	}
}
