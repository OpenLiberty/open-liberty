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

import javax.servlet.sip.Proxy;
import javax.servlet.sip.SipServletResponse;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.proxy.StatefullProxy;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

/**
 * @author yaronr
 * Created on Jul 15, 2003
 *
 * Represents a transaction in the SIP container
 * Transaction is defined as one request and collection of reposnses 
 * 	to this request
 */
public abstract class SipTransaction 
{
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipTransaction.class);
       
    /**
     * Transaction is defined as a request and a list of responses
     */
	protected SipServletRequestImpl m_originalRequest;
    
    /** 
     * Holds transaction ID
     */  	
    private long m_transactionID = -1;
    
    /**
     * Holds the proxy if this transaction
     */
    private Proxy m_proxy = null;
    
    /**
   	 * The listener for this transaction
   	 */
   	protected TransactionListener m_listener; 
    
    
    /**
     * Inidcates whether the transaction has been terminated. Transactions
     * are terminated by final response and also by CANCEL message. 
     */
    private boolean m_isTerminated = false; 
    
       
    /**
     * Constructor
     * 
     * @param transactionID
     * @param request
     */    
    public SipTransaction(long transactionID, SipServletRequestImpl request)
    {
    	m_transactionID = transactionID;
    	m_originalRequest = request;
    }
    
	/**
	 * Constructor
	 * @param request 
	 */    
	public SipTransaction(SipServletRequestImpl request)
	{
		m_originalRequest = request;
	}
    
    /**
     * Get transaction ID
     */
    public long getTransactionID()
    {
    	return m_transactionID;
    }
	
	/**
	 * Sets the transaction Id associated with this transaction object. 
	 * @param transactionId
	 */
	protected void setId(long transactionId)
	{
		if(c_logger.isTraceEntryExitEnabled())
        {			
        	Object[] params = { new Long(transactionId)  }; 
        	c_logger.traceEntry(this, "setId", params); 
        }
        
		if(getTransactionID() != -1)
		{
			// This will called when NAPTR used in case when first IP didn't
			// response or 503 error received
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setId", 
						"Reuse this transaction in NAPTR usage case");
			}
//			if (c_logger.isErrorEnabled()){Object[] args = { new Long(m_transactionID), 
//							  new Long(transactionId) }; 
//            c_logger.error("error.transaction.id.alreay.set", 
//            				Situation.SITUATION_REQUEST, args);}
//			
//			return; 
		}

		m_transactionID = transactionId; 
	}
    
    /**
	 * @return the original request
	 */
	public SipServletRequestImpl getOriginalRequest() 
	{
		return m_originalRequest;
	}
	
	/**
	 * Get the session owner of this transaction
	 * 
	 * @return the session associated with this transaction
	 */	
	protected TransactionUserWrapper getTransactionUser()
	{
		return m_originalRequest.getTransactionUser();
	}

	/**
	 * return the proxy object associate with this transaction
	 *
	 * @param create should we create the proxy if it does not exist
	 * @return the proxy associate with this transaction
	 */   
    public Proxy getProxy(boolean create)
    {
    	
        if ( (null == m_proxy) && create)
        {
        	m_proxy = new StatefullProxy(m_originalRequest);
        }
        					
        return m_proxy;
    }

    
    /**
     * A final response to this transaction recieved or being sent
     * @param response
     */
	public void onFinalResponse(SipServletResponse response)
	{
		markAsTerminated();  
		removeFromTransactionTable(response != null ? response.getMethod():null); 
	}
	
	/**
	 * Removes the current transaction from the transaction table. 
	 */
	protected void removeFromTransactionTable(String method)
	{
		// Get a reference to the transaction table
		TransactionTable tt = TransactionTable.getInstance();
	
		// remove this transaction
		boolean removedTransaction = tt.removeTransaction(this);
	
	
		if(c_logger.isTraceDebugEnabled())
		{
			c_logger.traceDebug(this, "removeFromTransactionTable" ,
								"Cleaned Up Transaction: " + tt	+ " transaction found ? = " + removedTransaction);
		}
		
		if(removedTransaction){
			if(m_listener != null){
				m_listener.removeTransaction(method);
				notifyDerivedTUs();
			}
		}
		
		transactionTerminated(m_originalRequest);
	}


	/**
	 * Notify related DTU about add / removed transaction
	 */
	abstract protected void notifyDerivedTUs();
	
	/**
	 * Add reference to related DTU if needed.
	 */
	abstract public void addReferece(TransactionUserWrapper dtu);


	/**
	 * Method that will be impelmented by all extend classes.
	 * This Method notify them that the transaction was terminated.
	 * @param request 
	 */
	abstract protected void transactionTerminated(SipServletRequestImpl request);
	
	
	
	/**
	 * Process Timeout Event for the transaction. 
	 */
	public void processTimeout()
	{
		markAsTerminated(); 
		//clean up tranasaction table. 
		// TimeOut can be received on Client
		removeFromTransactionTable(getOriginalRequest().getMethod()); 
	}
	    
	 
    /**
     * Marks the transaction as terminated. 
     * @param b
     */
    public void markAsTerminated()
    {
        m_isTerminated = true;
    }
    
    /**
     * Checks whether the transaction is terminated. 
     * @return
     */
    public synchronized boolean isTerminated()
    {
    	return m_isTerminated; 
    }

    /**
	 * Set the transaction listener
	 * 
	 * @param listener
	 */
	public void setTransactionListener(TransactionListener listener)
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "setTransactionListener", "New listener=" +  listener);
		 }
        
		boolean replaceExistingListener = false;
		if(m_listener != listener){
			if(m_listener != null){
				replaceListener();
				replaceExistingListener = true;
				//m_listener.removeTransaction(null);
			}
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setTransactionListener",
						"Replace the old one = " + m_listener + " with the new one = " + listener);
			}
			m_listener = listener;
			
			//we need to addTransaction only for new listener (when m_listener is null yet). At any other scenario
			// (can happen only for ServerTransaction in case of Derived Sessions) - this transaction already counetd when
			// Derived created and should not be added one more time.
			if(!replaceExistingListener && m_originalRequest != null && !m_originalRequest.getMethod().equals(Request.ACK) ){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "setTransactionListener",
							"Add transaction to new listener");
				}
				m_listener.addTransaction(m_originalRequest.getMethod());
			}
			else{
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "setTransactionListener",
							"Not adding new transaction to related listener for ACK message");
				}
			}
		}

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "setTransactionListener");
		 }
	}

	/**
	 * This is a notification about situation when listener is going to be replaces by another one.
	 */
	abstract protected void replaceListener();
	
}
