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

import jain.protocol.ip.sip.header.CSeqHeader;

/**
 * identifier of the source request of a forked transaction,
 * combined of:
 * 1. From header tag
 * 2. Call-ID
 * 3. CSeq
 * 
 * this is needed for resolving merged requests in a UAS
 * per rfc3261 8.2.2.2
 * 
 * @author ran
 */
public class MergedRequestKey 
{
	/** From tag */
	private String m_fromTag;
	
	/** Call-ID header */
	private String m_callId;
	
	/** CSeq header (method+number) */
	private CSeqHeader m_cseq;
	
	/**
	 * constructor
	 */
	public MergedRequestKey(String fromTag, String callId, CSeqHeader cseq) {
		m_fromTag = fromTag == null ? "" : fromTag;
		m_callId = callId;
		m_cseq = cseq;
	}
	
	/**
	 * calculates the hash code of this transaction identifier
	 */
	public int hashCode() {
		int hash = m_fromTag.hashCode()
			^ m_callId.hashCode()
			^ m_cseq.hashCode();
		return hash;
	}
	
	/**
	 * compares this identifier with another
	 */
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		
		if (!(other instanceof MergedRequestKey)) {
			return false;
		}
		
		MergedRequestKey o = (MergedRequestKey)other;
		
		return
			m_callId.equals(o.m_callId) &&
			m_fromTag.equals(o.m_fromTag) &&
			m_cseq.equals(o.m_cseq);
	}
	
	/**
	 * string representation
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer(
			m_fromTag.length() + m_callId.length() + 16);
		
		buf.append(m_fromTag);
		buf.append(':');
		buf.append(m_callId);
		buf.append(':');
		buf.append(m_cseq.getMethod());
		buf.append(' ');
		buf.append(m_cseq.getSequenceNumber());
		
		return buf.toString();
	}
}
