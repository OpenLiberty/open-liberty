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
package com.ibm.ws.jain.protocol.ip.sip.message;

/**
 * a CANCEL request that may be associated with an INVITE transaction
 * 
 * @author ran
 */
public class CancelRequest extends RequestImpl
{
	/** identifier of the associated INVITE transaction */
	private long m_inviteTransaction;

	/**
	 * constructor
	 */
	public CancelRequest() {
		m_inviteTransaction = -1;
	}

	/**
	 * gets the identifier of the associated INVITE transaction
	 * @return the INVITE transaction id,
	 *  or -1 if no INVITE transaction associated
	 * @see RequestImpl.getOriginInviteTransaction()
	 */
	public long getOriginInviteTransaction() {
		return m_inviteTransaction;
	}

	/**
	 * associates this CANCEL request with an INVITE transaction
	 * @param inviteTransaction id of the INVITE transaction
	 */
	public void setOriginInviteTransaction(long inviteTransaction) {
		m_inviteTransaction = inviteTransaction;
	}
}
