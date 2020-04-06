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
import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;

public interface SIPTransaction 
{
	/** 
	 * gets the request that started the transaction 
	 **/
	public Request getFirstRequest();	

	/** 
	 * gets the transaction branch
	 **/	
	public String getBranch();
	
	/** sets the transaction state , for state machine prossecing*/
	public void setState(int newState) ;
		
	
	/** return true if the transaction has initiated */
	public boolean hasInitiated();
	
	/**gets the transaction state */
	public int getState() ;
			
	/** should return false on UDP , true on TCP , and can indicate  */
	public boolean isTransportReliable();
		
	/** gets the id for this transaction */
	public long getId();
	
	
	/**
	 * Id by METOHD + BRANCH
	 * @return String
	 */
	public BranchMethodKey getBranchMethodId();
		
		
	/** destroy the transaction */
	public void destroyTransaction();
	
	/** returns the most recent response asociated with this transactio  */
	public Response getMostRecentResponse();
	
	/** return the parent stack */
	public SIPTransactionStack getParentStack();
	
	/** the contect in which this transaction interacts */
	public SipProvider getProviderContext();
	
	/** the connection to the transport layer */
	public void setTransportConnection( SIPConnection connection );	
	
	/** the connection to the transport layer */
	public SIPConnection getTransportConnection();
		
	/**
	 * get the logging object to the class 
	 * @return LogMgr
	 */
	public LogMgr getLoger();

	
	/*******************************************************
	 *  these methods are the entery point for events
	 *  that the transaction should handle as a state machine
	 *******************************************************/
	
	/** process transaction request */
	public void processRequest(Request sipRequest)
		throws SipParseException;
		
	/** process transaction response */
	public void processResponse(Response sipResponse)
		throws SipParseException;	
			
	/** process transaction transport error */
	public void prossesTransportError();		
		
}
