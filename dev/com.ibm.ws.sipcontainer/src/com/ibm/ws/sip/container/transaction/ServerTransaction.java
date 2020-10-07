/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.transaction;

import jain.protocol.ip.sip.message.Request;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.sip.SipServletRequest;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.servlets.OutgoingSipServletResponse;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.sessions.SipTransactionUserTable;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

/**
 * @author yaronr
 *
 * Represent a server transaction
 */
public class ServerTransaction extends SipTransaction 
{
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(ServerTransaction.class);
			
	/**
	 * Hold transaction listeners
	 */
	private List<TransactionUserWrapper> m_derivedTU= new LinkedList<TransactionUserWrapper>();
	
	/**
	 * Constructor
	 * 
	 * @param transactionID
	 * @param request
	 */
	protected ServerTransaction(long transactionID, SipServletRequestImpl request) 
	{
		super(transactionID, request);
		
		if(c_logger.isTraceDebugEnabled()){			
			c_logger.traceDebug(this, "ServerTransaction", "transID = " + transactionID + 
					"  request.method =" +  request.getMethod());
        }
	}

	
	/**
	 * @return the server transaction listener
	 */
	public ServerTransactionListener getServerTransactionLisener(){
		if(c_logger.isTraceDebugEnabled()){			
        	c_logger.traceDebug(this, "getServerTransactionLisener", "listener=" +  m_listener); 
        }
		return (ServerTransactionListener)m_listener;
	}
	
	/**
	 * A request arrived to this transaction
	 * 
	 * @param response
	 */	
	public void processRequest(SipServletRequest request)
	{
		if(c_logger.isTraceEntryExitEnabled())
        {			
        	Object[] params = { request.getMethod() }; 
        	c_logger.traceEntry(this, "processRequest", params); 
        }
		
        TransactionUserWrapper tu = SipTransactionUserTable.getInstance().getTransactionUserForInboundRequest((SipServletRequestImpl)request);
        if(tu != null){
        	if(c_logger.isTraceDebugEnabled())
            {			
            	c_logger.traceDebug(this, "processRequest", "processing request with TU="+tu); 
            }
        	tu.processRequest(request);
        }else{
        	if(c_logger.isTraceDebugEnabled())
            {			
            	c_logger.traceDebug(this, "processRequest", "processing request with transaction listener="+m_listener); 
            }
        	getServerTransactionLisener().processRequest(request);
        }
				
		//Acks for Invite request with non 2xx response should arrive on the
		//same transaction. After we get the ack we can remove the transaction
		//from the transaction table. 
		if(request.getMethod().equals(Request.ACK))	{
			removeFromTransactionTable(null);
		}
		
		if(c_logger.isTraceEntryExitEnabled())
        {			
        	c_logger.traceExit(this, "processRequest"); 
        }
	}
	
	/**
	 * 
	 * @param request
	 */
	public void sendResponse(OutgoingSipServletResponse response) 
		throws IOException
	{
		if(c_logger.isTraceEntryExitEnabled())
        {			
        	Object[] params = { response.getMethod() }; 
        	c_logger.traceEntry(this, "sendResponse", params); 
        }
        		
		ServerTransactionListener lstr = getServerTransactionLisener();
		// Announce the listener
		if (lstr != null)
			lstr.onSendingResponse(response);
		
		// send the respond
		response.sendImpl();
		
		// Remove from table if it's a final response
		int status = response.getStatus();  
		
		//Final response is any 2xx or above
		if(status >= 200){
			//Mark as terminated and remove from transaction table. 
			onFinalResponse(response);
		}
		if(c_logger.isTraceEntryExitEnabled())
        {			
        	c_logger.traceExit(this, "sendResponse"); 
        }
	}

	/**
	 * @see com.ibm.ws.sip.container.transaction.SipTransaction#transactionTerminated(com.ibm.ws.sip.container.servlets.SipServletRequestImpl)
	 */
	protected void transactionTerminated(SipServletRequestImpl request) {
		if(c_logger.isTraceEntryExitEnabled()){			
        	c_logger.traceEntry(this, "transactionTerminated"); 
        }
//		Notify listener prior to sending
//		The listener can be null if we came here from the
//		processTimeout()event. This happened in the overload state. Defect 358489
		if(m_listener != null){
			getServerTransactionLisener().serverTransactionTerminated(request);
			// Notify related TU about terminated transaction.
			// We need it to continue invalidation which interrupted
			// if there were ongoing transactions.
			if(!m_derivedTU.isEmpty()){
				TransactionUserWrapper activeListener = (TransactionUserWrapper)m_listener;
				if(c_logger.isTraceDebugEnabled()){			
		        	c_logger.traceDebug(this, "transactionTerminated", "Active listener ID = " +activeListener.getId()); 
		        }
				Iterator<TransactionUserWrapper> i = m_derivedTU.iterator();
				while (i.hasNext()) {
					TransactionUserWrapper dtu = i.next();
					if(c_logger.isTraceDebugEnabled()){			
		            	c_logger.traceDebug(this, "notifyDerivedTUs", "Found DTU = "+dtu.getId()); 
		            }
					if(activeListener != dtu){
						dtu.originalServerTransactionTerminated(request);
					}
				}
			}
		}
	}

	/**
	 * This method is responsible to update all related TUs about terminated transaction.
	 * This is only about ServerTransaction.
	 */
	@Override
	protected void notifyDerivedTUs() {
		if(c_logger.isTraceEntryExitEnabled()){			
        	c_logger.traceEntry(this, "notifyDerivedTUs"); 
        }
		
		TransactionUserWrapper activeListener = (TransactionUserWrapper)m_listener;
		
		if(c_logger.isTraceDebugEnabled()){			
        	c_logger.traceDebug(this, "notifyDerivedTUs", "Active listener TU = " +activeListener.getId()); 
        }
		
		if(!m_derivedTU.isEmpty()){
			Iterator<TransactionUserWrapper> i = m_derivedTU.iterator();
			while (i.hasNext()) {
				TransactionUserWrapper dtu = i.next();
				if(c_logger.isTraceDebugEnabled()){			
	            	c_logger.traceDebug(this, "notifyDerivedTUs", "Found DTU = "+dtu.getId()); 
	            }
				if(dtu != activeListener){
					dtu.removeTransaction(null);
				}
			}
		}
		if(c_logger.isTraceEntryExitEnabled()){			
        	c_logger.traceExit(this, "notifyDerivedTUs"); 
        }
	}

	/**
	 * This is a notification about situation when listener is going to be replaces by another one.
	 */
	protected void replaceListener() {
		if(!m_derivedTU.contains(m_listener)){
			addReferece((TransactionUserWrapper)m_listener);
			if(c_logger.isTraceDebugEnabled()){			
				c_logger.traceDebug(this, "replaceListener", "store listener in derived list");
	        }
		}
	}
	
	@Override
	public void addReferece(TransactionUserWrapper dtu) {
		m_derivedTU.add(dtu);
		
		if(c_logger.isTraceDebugEnabled()){			
			c_logger.traceDebug(this, "ServerTransaction", "New related DTU which should be updated when this ST is ended" +  dtu.getId());
        }
		
	}
}
